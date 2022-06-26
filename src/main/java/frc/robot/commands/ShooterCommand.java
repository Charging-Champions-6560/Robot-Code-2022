// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;


import com.revrobotics.ColorMatch;
import com.revrobotics.ColorMatchResult;

import edu.wpi.first.math.filter.Debouncer;
import edu.wpi.first.math.filter.Debouncer.DebounceType;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.Shooter;
import frc.robot.utility.ShootCalibrationMap;
import frc.robot.Constants;
import frc.robot.RobotContainer;
import frc.robot.subsystems.Limelight;

public class ShooterCommand extends CommandBase {

  public static interface Controls {
    double shooterHoodTest();
    double shooterTurretTest();
    boolean getAimShooter();
    boolean overrideTurretCenter();
    boolean getConstantAiming();

    boolean getHotRPMAddition();
    boolean getHotRPMReduction();

    boolean getManualMiss();
  }

  private Shooter shooter;
  private Controls controls;
  private Limelight limelight;

  private ColorMatch colorMatch = new ColorMatch();
  private final Color blueColor = new Color(0, 0, 255);
  private final Color redColor = new Color(255, 0, 0);

  private final boolean isRedAlliance;
  private boolean isAuto = false;

  private boolean missBall = false;
  private final double ballMissRPM = 500;
 
  private NetworkTable ntTable;
  private NetworkTable ntTableClimb;
  private NetworkTableEntry ntTestRPM;
  private NetworkTableEntry ntTestHood;

  private NetworkTableEntry ntUseCalibrationMap;

  private NetworkTableEntry hotRPMAddition;
  private NetworkTableEntry hotRPMReduction;

  private NetworkTableEntry ntTeleopBuff;

  private final double IDLE_RPM = 1000;
  private final double AutoBaseRPMBuff = 100;
  private double TeleOpBaseRPMBuff = 100;

  private double targetHoodPos = 0.0;
  
  private int targetBallCount = -1;
  private double doneShootingFrames = 0;

  private Debouncer debouncer = new Debouncer(2, DebounceType.kFalling);

  private double rpmBuff;
  private final double rpmBuffZeta = 1;

  public ShooterCommand(Shooter shooter, Controls controls, Limelight limelight) {
    // Use addRequirements() here to declare subsystem dependencies.
    this.shooter = shooter;
    this.controls = controls;
    this.limelight = limelight;

    addRequirements(shooter, limelight);

    colorMatch.addColorMatch(blueColor);
    colorMatch.addColorMatch(redColor);

    
    this.ntTable = NetworkTableInstance.getDefault().getTable("Shooter");
    this.ntTableClimb = NetworkTableInstance.getDefault().getTable("Climb");

    ntTestHood = ntTable.getEntry("Hood Angle");
    ntTestHood.setDouble(0.0);

    ntTestRPM = ntTable.getEntry("Target Cal RPM");
    ntTestRPM.setDouble(0.0);

    ntUseCalibrationMap = ntTable.getEntry("Use calibration map?");
    ntUseCalibrationMap.setBoolean(true);

    hotRPMAddition = ntTable.getEntry("hot RPM Addition");
    hotRPMAddition.setDouble(35.0);

    ntTeleopBuff = ntTable.getEntry("Teleop RPM Buff");
    ntTeleopBuff.setDouble(0);
    

    isRedAlliance =  NetworkTableInstance.getDefault().getTable("FMSInfo").getEntry("IsRedAlliance").getBoolean(false);

    this.isAuto = false;
  }
  

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {
    shooter.setHoodPos(0.0);
    shooter.setShooterRpm(0.0);
    shooter.resetBallCount();
    
  }

  public enum ShooterState {
    SHOOTING, AIMING, CHILLING
  }

  public synchronized void setShooterState(ShooterState state) {
    switch (state) {
      case SHOOTING:
        double dist = limelight.getDistance();
        TeleOpBaseRPMBuff = ntTeleopBuff.getDouble(0.0);

        rpmBuff = isAuto ? AutoBaseRPMBuff : TeleOpBaseRPMBuff;
        rpmBuff += missBall ? ballMissRPM : 0.0;

        shooter.setShooterRpm(getShooterRpm(dist) + rpmBuff);
        break;

      case AIMING:
        shooter.setShooterRpm(IDLE_RPM);
        targetHoodPos = debouncer.calculate(limelight.hasTarget()) ? limelight.getHorizontalAngle() : 0.0;
        if(targetHoodPos >= -1) {
          shooter.setHoodPos(targetHoodPos);// - (controls.getHotHoodChange() ? hotHoodAddition.getDouble(0.0) : 0.0) );
        }
        double turrTarget = limelight.getHorizontalAngle();
      
        if((shooter.getTurretPosDegrees() > 85 && turrTarget > 0) || (shooter.getTurretPosDegrees() < -85 && turrTarget < 0))
          turrTarget = 0;
        shooter.setTurretDeltaPos(turrTarget); // limelight controlled turret pos;
        break;
      case CHILLING:
        shooter.setShooterRpm(0.0);
        break;
    }
    if(targetBallCount != -1 && shooter.getBallShotCount() >= targetBallCount) doneShootingFrames++;

    
    if (ntTableClimb.getEntry("Left Climb Pos").getDouble(0.0) > 8.0 && ntTableClimb.getEntry("Right Climb Pos").getDouble(0.0) > 8.0) {
      shooter.setTurretPos(90.0); // turret is at 90 degrees when both climb arms are extended
    }
  }

  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {
    
    if(!RobotContainer.conveyorSensor.get()){
      ColorMatchResult closestColor = colorMatch.matchClosestColor(RobotContainer.colorSensor.getColor());

      System.out.println("Ball Color Detected: (" + closestColor.color.red + ", " +closestColor.color.green + ", " + closestColor.color.blue +")");
      
      if((closestColor.color.blue > 0.8 && isRedAlliance) || (closestColor.color.red > 0.8 && !isRedAlliance)){
        missBall = true;
        System.out.println("MISS BALL");
      } else{
        missBall = false;
      }
      
    }

    limelight.setForceOff(!(controls.getAimShooter() || controls.getConstantAiming()));

    
    if(controls.getAimShooter() || controls.getConstantAiming()) {
      
      if (controls.getAimShooter()) {
        setShooterState(ShooterState.SHOOTING);
      }
      else{
        setShooterState(ShooterState.AIMING);
      }
    }else{
      setShooterState(ShooterState.CHILLING);
    }
  }

  public double getShooterRpm(double distance) {
    if(ntUseCalibrationMap.getBoolean(false)){
      ShootCalibrationMap.Trajectory traj;
      try {
        // traj = Constants.ShooterCalibrations.SHOOT_CALIBRATION_MAP.get(distance);
        traj = Constants.ShooterCalibrations.SHOOT_CALIBRATION_MAP.getWithRpmAdjustment(distance, rpmBuff, rpmBuffZeta);
        
      } catch (ShootCalibrationMap.OutOfBoundsException e) {
        return 0.0;
      }

      return traj.shooterRpm;
    }
    return ntTestRPM.getDouble(0.0);
    // return distance; //TODO: add function
  }

  public double getShooterHoodAngle(double distance) {
    if(ntUseCalibrationMap.getBoolean(false)){
      ShootCalibrationMap.Trajectory traj;
      try {
        traj = Constants.ShooterCalibrations.SHOOT_CALIBRATION_MAP.get(distance);
        
      } catch (ShootCalibrationMap.OutOfBoundsException e) {
        return 0.0;
      }

      return traj.hoodPos;
    }
    return ntTestHood.getDouble(0.0); 
  }

  public boolean doneShooting(){
    return doneShootingFrames > 30;
  }


  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {
    shooter.setShooterRpm(0.0);
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    return doneShooting();
  }

  //method that supplies newcalibrationmap to networktables
  // public void saveNewCalibrationMap(){
  //   ntTable.getEntry("NEW Shoot Calibration Map").setString(ShooterCalibrations.NEW_SHOOT_CALIBRATION_MAP.toString());
  // }

}
