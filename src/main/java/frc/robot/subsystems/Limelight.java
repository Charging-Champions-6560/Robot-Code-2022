// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import static frc.robot.utility.NetworkTable.NtValueDisplay.ntDispTab;
import frc.robot.commands.controls.manualdrive.ManualControls;

public class Limelight extends SubsystemBase {
  /** Creates a new Limelight. */
  public static interface Controls {
    int getPipeline();
  }
    
  private final NetworkTableEntry ntX = NetworkTableInstance.getDefault().getTable("limelight").getEntry("tx");
  private final NetworkTableEntry ntY = NetworkTableInstance.getDefault().getTable("limelight").getEntry("ty");
  private final NetworkTableEntry ntV = NetworkTableInstance.getDefault().getTable("limelight").getEntry("tv");
  private final NetworkTableEntry ntPipeline = NetworkTableInstance.getDefault().getTable("limelight").getEntry("pipeline");

  private final ManualControls controls;
  private boolean forceOff = true;

  public Limelight(ManualControls controls) {
    this.controls = controls;
    setForceOff(true);

    ntDispTab("Limelight")
    .add("Distance",this::getDistance)
    .add("Angle", this::getAngle)
    ;
  }

   /**
   * 
   * @return distance to target from center of robot
   */
  public double getDistance() { // TODO calibrate this
    return 3.457 / Math.tan(Math.toRadians(23.6338 + ntY.getDouble(0.0)));
  }

  public double getAngle() {
    return ntX.getDouble(0.0);
  }

  public boolean hasTarget(){
    return ntV.getDouble(0.0) == 1.0 ? true : false;
  }

  public void setForceOff(boolean value) {
    forceOff = value;
  }

  @Override
  public void periodic() {
    ntPipeline.setNumber(forceOff ? 2 : controls.getLimelightPipeline());
  }
}