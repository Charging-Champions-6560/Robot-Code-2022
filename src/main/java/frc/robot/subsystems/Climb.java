// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.revrobotics.CANSparkMax.ControlType;
import com.revrobotics.CANSparkMax.IdleMode;

import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.PneumaticsModuleType;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Robot;
import frc.robot.Constants.*;
import frc.robot.utility.NetworkTable.NtValueDisplay;


public class Climb extends SubsystemBase {
  /** Creates a new Climb. */

  private final Solenoid piston = new Solenoid(PneumaticsModuleType.CTREPCM, RobotIds.CLIMB_PISTON);

  private final CANSparkMax rotatorMotor = new CANSparkMax(RobotIds.CLIMB_ROTATOR_MOTOR, MotorType.kBrushless);
  private final CANSparkMax leftExtensionMotor = new CANSparkMax(RobotIds.CLIMB_LEFT_EXTENSION_MOTOR, MotorType.kBrushless);
  private final CANSparkMax rightExtensionMotor = new CANSparkMax(RobotIds.CLIMB_RIGHT_EXTENSION_MOTOR, MotorType.kBrushless);

  private final SlewRateLimiter accelLimiter = new SlewRateLimiter(3);

  private static final double EPSILON = 5.08;
  private static final double BETA_P = 0.001;
  private double rightComp = 1.0;
  private double leftComp = 1.0;

  private final double minPos = 0;
  private final double maxPos = 22; //TODO: change

  private double targetExtensionSpeed = 0;

  private NetworkTable nTable;
  private NetworkTableEntry rightCompensationConstant;
  private NetworkTableEntry ntOverideSoftLimit;

  private NetworkTableEntry ntOverrideOnlyRight;
  private NetworkTableEntry ntOverrideOnlyLeft;
  // private boolean ntOverrideOnlyLeftEnabled;
  // private boolean ntOverrideOnlyRightEnabled;

  private double extensionSpeed = 0.3;

  public Climb() {
    setupAllMotors();
    nTable = NetworkTableInstance.getDefault().getTable("Climb");

    rightCompensationConstant = nTable.getEntry("Right Compensation Constant");
    rightCompensationConstant.setDouble(0.0);

    ntOverideSoftLimit = nTable.getEntry("Climb Override");
    ntOverideSoftLimit.setBoolean(false);

    // ntOverrideOnlyLeft = nTable.getEntry("Override ONLY Left?");
    // ntOverrideOnlyLeft.setBoolean(false);

    // ntOverrideOnlyRight = nTable.getEntry("Override ONLY Right?");
    // ntOverrideOnlyRight.setBoolean(false);

    // ntOverrideOnlyLeftEnabled = false;
    // ntOverrideOnlyRightEnabled = false;

    NtValueDisplay.ntDispTab("Climb").add("Left Pos", this::getLeftPositionInches).add("Right Pos", this::getRightPositionInches);

  }

  private void setupAllMotors() {
    setupMotor(rotatorMotor, false);
    setupMotor(leftExtensionMotor, false);
    setupMotor(rightExtensionMotor, false);
  }

  private void setupMotor(CANSparkMax motor, boolean inverted) {
    motor.restoreFactoryDefaults();
    motor.setInverted(inverted);
    motor.getEncoder().setPosition(0);
    motor.setClosedLoopRampRate(1.0);
    motor.setIdleMode(IdleMode.kBrake);
    

    // Changes default motor controller "send speed" from 20ms to 10ms
    // Add if having issues with accuracy
    //motors[0].setPeriodicFramePeriod(PeriodicFrame.kStatus2, 10);
  }

  @Override
  public void periodic() {
    // This method will be called once per scheduler run
    double rightPos = getRightPositionInches();
    double leftPos = getLeftPositionInches();
    double diff = leftPos - rightPos;

    if (diff < 0) {
      leftComp += Math.copySign(BETA_P, -targetExtensionSpeed);
      rightComp = 1.0;
    }
    if (diff > 0) {
      leftComp = 1.0;
      rightComp += Math.copySign(BETA_P, -targetExtensionSpeed);
    }
    

    double leftTargetExtensionSpeed = targetExtensionSpeed * leftComp;
    double rightTargetExtensionSpeed = targetExtensionSpeed * rightComp;

    if(!ntOverideSoftLimit.getBoolean(false)){ //TODO: check w/ radin
      if (rightPos < minPos) rightTargetExtensionSpeed = Math.min(0, rightTargetExtensionSpeed);
      if (leftPos < minPos) leftTargetExtensionSpeed = Math.min(0, leftTargetExtensionSpeed);
      
      if (rightPos > maxPos) rightTargetExtensionSpeed = Math.max(0, rightTargetExtensionSpeed);
      if (leftPos > maxPos) leftTargetExtensionSpeed = Math.max(0, leftTargetExtensionSpeed);
    }
    // else {
    //   if (ntOverrideOnlyLeft.getBoolean(false)) {
    //     if (ntOverrideOnlyRightEnabled) {
    //       ntOverrideOnlyRightEnabled = false;
    //       ntOverrideOnlyRight.setBoolean(false);
    //     }
    //     if (ntOverrideOnlyRight.getBoolean(false)) {
    //       ntOverrideOnlyLeftEnabled = true;

    //     }
    //   }
    // }


    setLeftExtensionMotor(leftTargetExtensionSpeed);
    setRightExtensionMotor(rightTargetExtensionSpeed);
  }

  public void runRotatorMotor(double output) {
    if (getRotatorPosition() < -50 && output < 0.0) output = 0;
    rotatorMotor.set(accelLimiter.calculate(output));
  }

  public void initialize() {
    rotatorMotor.set(0);
    leftExtensionMotor.set(0);
    rightExtensionMotor.set(0);
  }

  public void setPiston(boolean extended) {
    piston.set(extended);
  }

  private void setLeftExtensionMotor(double output) {
    leftExtensionMotor.set(output);
  }

  private void setRightExtensionMotor(double output) {
    rightExtensionMotor.set(output);
  }

  public void setExtensionMotor(double output){
    targetExtensionSpeed = -output;
  }

  public double getRightPosition() {
    return -rightExtensionMotor.getEncoder().getPosition();
  }

  public double getLeftPosition() {
    return -leftExtensionMotor.getEncoder().getPosition();
  }

  public double getRightPositionInches() {
    return getRightPosition() * PhysicalConstants.CLIMB_EXTENSION_INCHES_PER_ROTATION;
  }

  public double getLeftPositionInches() {
    return getLeftPosition()* PhysicalConstants.CLIMB_EXTENSION_INCHES_PER_ROTATION;
  }

  public double getRotatorPosition() {
    return rotatorMotor.getEncoder().getPosition();
  }
}
