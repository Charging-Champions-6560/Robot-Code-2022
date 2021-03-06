// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.TalonFXControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonFX;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.PneumaticsModuleType;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.PhysicalConstants;
import frc.robot.Constants.RobotIds;
import frc.robot.utility.Util;
import frc.robot.utility.NetworkTable.NtValueDisplay;

public class Climb extends SubsystemBase {

  private final Solenoid rotatorPiston = new Solenoid(PneumaticsModuleType.CTREPCM, 2);

  private final TalonFX leftExtensionMotor = new TalonFX(RobotIds.CLIMB_LEFT_EXTENSION_MOTOR);
  private final TalonFX rightExtensionMotor = new TalonFX(RobotIds.CLIMB_RIGHT_EXTENSION_MOTOR);

  private final double minPos = 0.02;
  private final double maxPos = 23.25;

  private final double proximityThresholdTop = 1.5;
  private final double proximityThresholdBottom = 1.5;
  private final double proximitySlow = 0.2;

  private double rightComp = 1;
  private double leftComp = 1;

  private NetworkTable nTable;
  private NetworkTableEntry ntOverideSoftLimit;
  private NetworkTableEntry ntExtensionSpeed;
  private NetworkTableEntry rightTestSpeed;
  private NetworkTableEntry leftTestSpeed;

  /** Creates a new Climb. */
  public Climb() {
    setupAllMotors();
    nTable = NetworkTableInstance.getDefault().getTable("Climb");

    ntExtensionSpeed = nTable.getEntry("Extension Speed");
    ntExtensionSpeed.setDouble(0.9);

    ntOverideSoftLimit = nTable.getEntry("Climb Override");
    ntOverideSoftLimit.setBoolean(false);
    
    rightTestSpeed = nTable.getEntry("Right Override speed");
    rightTestSpeed.setDouble(0.0);

    leftTestSpeed = nTable.getEntry("left Override speed");
    leftTestSpeed.setDouble(0.0);
    

    NtValueDisplay.ntDispTab("Climb")
        .add("Left Climb Pos", this::getLeftPositionInches)
        .add("Right Climb Pos", this::getRightPositionInches)
        .add("Left Climb Vel", this::getleftVelocity)
        .add("Right Climb Vel", this::getRightVelocity);

  }

  @Override
  public void periodic() {
    // This method will be called once per scheduler run

  }

  public void setRotatorPiston(boolean extended) {
    rotatorPiston.set(extended);
  }

  public void reset() {
    leftExtensionMotor.set(TalonFXControlMode.PercentOutput, 0);
    rightExtensionMotor.set(TalonFXControlMode.PercentOutput, 0);
  }

  public void setExtensionMotors(double output) {
    output = Math.abs(output) < 0.1 ? 0 : output;
    output = Util.getLimited(output, 1);

    double rightPos = getRightPositionInches();
    double leftPos = getLeftPositionInches();


    double extensionSpeed = ntExtensionSpeed.getDouble(0.5);

    double leftSpeed;
    double rightSpeed;
    
    if(!ntOverideSoftLimit.getBoolean(false)){

      if(output > 0){
        leftSpeed = (output * extensionSpeed);

        if (leftPos > maxPos - proximityThresholdTop){
          if(leftPos > maxPos) leftSpeed = 0;
          else leftSpeed = Util.getLimited(leftSpeed, proximitySlow);
        }

        rightSpeed = (output * extensionSpeed);
    
        if (rightPos > maxPos - proximityThresholdTop) {
          if(rightPos > maxPos) rightSpeed = 0;
          else rightSpeed = Util.getLimited(rightSpeed, proximitySlow);
        }
        
      }else{
        output /= 1.28;
        
        leftSpeed = (output * extensionSpeed);

        if (leftPos < minPos + proximityThresholdBottom) {
          if(leftPos < minPos) leftSpeed = 0;
          else leftSpeed = Util.getLimited(leftSpeed, proximitySlow);
        }
        
        rightSpeed = (output * extensionSpeed);
    
        if (rightPos < (minPos + proximityThresholdBottom)) {
          if(rightPos < minPos) rightSpeed = 0;
          else rightSpeed = Util.getLimited(rightSpeed, proximitySlow);
        }
        
      }
    }else{
      leftSpeed = (output * extensionSpeed);
      rightSpeed = (output * extensionSpeed);
    }

    double diff = getRightPositionInches() - getLeftPositionInches();
    // double dir = Math.copySign(1, output);

    if(output > 0){
      rightComp = 1;
      leftComp = Math.min(2, Math.max(1 + diff/0.5, 0));
    } else{
      leftComp = 1;
      rightComp = Math.min(2, Math.max(1 + diff/0.5, 0));
    }

    setLeftExtensionMotor(leftSpeed * leftComp);
    setRightExtensionMotor(rightSpeed * rightComp);
  }

  private void setLeftExtensionMotor(double output) {
    leftExtensionMotor.set(TalonFXControlMode.PercentOutput, output);
  }

  private void setRightExtensionMotor(double output) {
    rightExtensionMotor.set(TalonFXControlMode.PercentOutput, output);
  }

  private void setupAllMotors() {
    setupMotor(leftExtensionMotor, true);
    setupMotor(rightExtensionMotor, true);
  }

  private void setupMotor(TalonFX motor, boolean inverted) {
    motor.configFactoryDefault();
    motor.setInverted(inverted);
    motor.setSelectedSensorPosition(0.0);
    motor.setNeutralMode(NeutralMode.Brake);
  }

  public double getRightPosition() {
    return rightExtensionMotor.getSelectedSensorPosition();
  }

  public double getLeftPosition() {
    return leftExtensionMotor.getSelectedSensorPosition();
  }

  public double getRightPositionInches() {
    return getRightPosition() / 2048 * PhysicalConstants.CLIMB_EXTENSION_INCHES_PER_ROTATION;
  }

  public double getLeftPositionInches() {
    return getLeftPosition() / 2048 * PhysicalConstants.CLIMB_EXTENSION_INCHES_PER_ROTATION;
  }

  public double getRightVelocity() {
    return rightExtensionMotor.getSelectedSensorVelocity();
  }

  public double getleftVelocity() {
    return leftExtensionMotor.getSelectedSensorVelocity();
  }
}
