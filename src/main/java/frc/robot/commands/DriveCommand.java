// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import frc.robot.subsystems.DriveTrain;

import com.dacubeking.AutoBuilder.robot.sender.pathpreview.RobotPositionSender;
import com.dacubeking.AutoBuilder.robot.sender.pathpreview.RobotState;

import org.jetbrains.annotations.NotNull;

import edu.wpi.first.math.controller.HolonomicDriveController;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.CommandBase;

/** An example command that uses an example subsystem. */
public class DriveCommand extends CommandBase {
  
  public interface Controls {
    double driveGetX();
    double driveGetY();
    double driveGetRotation();
  }

  private final DriveTrain drivetrain;
  private final Controls controls;


  private @NotNull HolonomicDriveController autoController;
  private @NotNull Trajectory currentTrajectory;

  private @NotNull DriveState currentState;
  private @NotNull double trajectoryStartTime;
  private @NotNull Rotation2d currentAutoRotation;
  private boolean trajectoryFinished;

  public enum DriveState {
    TELEOP, AUTO, DONE
  }

  public DriveCommand(DriveTrain drivetrain, Controls controls) {
    // Use addRequirements() here to declare subsystem dependencies.
    addRequirements(drivetrain);

    this.drivetrain = drivetrain;
    this.controls = controls;

    resetAuto();

  }

  public void resetAuto() {
    autoController = new HolonomicDriveController(
      new PIDController(1, 0, 0),
      new PIDController(1, 0, 0),
      new ProfiledPIDController(1, 0, 0,
        new TrapezoidProfile.Constraints(2*Math.PI, Math.PI)
      )
    );
    // Here our rotation profile constraints were a max velocity
    // of 2pi rad/s and a max acceleration of 2pi rad/s^2
  }

  public void setTrajectory(Trajectory trajectory) {
    currentTrajectory = trajectory;
    trajectoryStartTime = Timer.getFPGATimestamp();
    trajectoryFinished = false;
    currentState = DriveState.AUTO;
  }

  public void setAutoRotation(Rotation2d rotation)  {
    currentAutoRotation = rotation;
  }

  public void updateRamsete() {
    Trajectory.State goal = currentTrajectory.sample(Timer.getFPGATimestamp() - trajectoryStartTime);
    Rotation2d targetHeading = currentAutoRotation;

    if (autoController == null) resetAuto();

    ChassisSpeeds adjustedSpeeds = autoController.calculate(
      drivetrain.getPose(),
      goal,
      targetHeading);
    
    drivetrain.setModuleStates(drivetrain.getKinematics().toSwerveModuleStates(adjustedSpeeds));

    if (autoController.atReference() && (Timer.getFPGATimestamp() - trajectoryStartTime) >= currentTrajectory.getTotalTimeSeconds()) {
      currentState = DriveState.DONE;
      stopMovement();
  }

  }

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {}

  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {

    if (DriverStation.isTeleop()) currentState = DriveState.TELEOP;

    switch (currentState) {
      case TELEOP:
        drivetrain.drive(controls.driveGetX(), controls.driveGetY(), controls.driveGetRotation(), true, false);
        break;
      case AUTO:
        updateRamsete();
      case DONE:
        trajectoryFinished = true;
    }

    RobotPositionSender.addRobotPosition(
      new RobotState(
        drivetrain.getPose(), //The position that is graphed on the GUI
        drivetrain.getChassisSpeeds().vxMetersPerSecond, // Only shown as you hover over the path
        drivetrain.getChassisSpeeds().vyMetersPerSecond, // Only shown as you hover over the path
        drivetrain.getChassisSpeeds().omegaRadiansPerSecond // Only shown as you hover over the path
      )
    );
  
  }

  public boolean isTrajectoryDone() {
    return trajectoryFinished;
  }

  public double getTrajectoryElapsedTime() {
    return trajectoryStartTime == 0 ? 0 : Timer.getFPGATimestamp() - trajectoryStartTime;
  }

  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {}

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    return false;
  }

  public DriveTrain getDriveTrain() {
    return drivetrain;
  }

  public void stopMovement() {
    drivetrain.stop();
  }
}
