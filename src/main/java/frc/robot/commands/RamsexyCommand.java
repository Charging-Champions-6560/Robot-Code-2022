// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import static edu.wpi.first.wpilibj.util.ErrorMessages.requireNonNullParam;

import edu.wpi.first.math.controller.RamseteController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.DifferentialDriveKinematics;
import edu.wpi.first.math.kinematics.DifferentialDriveWheelSpeeds;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.CommandBase;
import edu.wpi.first.wpilibj2.command.Subsystem;
import frc.robot.utility.AutoWrapperInterface;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * A command that uses a RAMSETE controller ({@link RamseteController}) to
 * follow a trajectory
 * {@link Trajectory} with a differential drive.
 *
 * <p>
 * The command handles trajectory-following, PID calculations, and feedforwards
 * internally. This
 * is intended to be a more-or-less "complete solution" that can be used by
 * teams without a great
 * deal of controls expertise.
 *
 * <p>
 * Advanced teams seeking more flexibility (for example, those who wish to use
 * the onboard PID
 * functionality of a "smart" motor controller) may use the secondary
 * constructor that omits the PID
 * and feedforward functionality, returning only the raw wheel speeds from the
 * RAMSETE controller.
 *
 * <p>
 * This class is provided by the NewCommands VendorDep
 */
public class RamsexyCommand extends CommandBase {
  private final Timer m_timer = new Timer();

  private Trajectory m_trajectory;
  private AutoWrapperInterface autoWrapper;
  private final Supplier<Pose2d> m_pose;
  private final RamseteController m_follower;
  private final DifferentialDriveKinematics m_kinematics;
  private final BiConsumer<double[], double[]> m_output;
  private DifferentialDriveWheelSpeeds m_prevSpeeds;
  private double m_prevTime;

  /**
   * Constructs a new RamseteCommand that, when executed, will follow the provided
   * trajectory.
   * Performs no PID control and calculates no feedforwards; outputs are the raw
   * wheel speeds from
   * the RAMSETE controller, and will need to be converted into a usable form by
   * the user.
   *
   * @param trajectory            The trajectory to follow.
   * @param pose                  A function that supplies the robot pose - use
   *                              one of the odometry classes to
   *                              provide this.
   * @param follower              The RAMSETE follower used to follow the
   *                              trajectory.
   * @param kinematics            The kinematics for the robot drivetrain.
   * @param outputMetersPerSecond A function that consumes the computed left and
   *                              right wheel speeds.
   * @param requirements          The subsystems to require.
   */
  public RamsexyCommand(
      AutoWrapperInterface autoWrapper,
      Supplier<Pose2d> pose,
      RamseteController follower,
      DifferentialDriveKinematics kinematics,
      BiConsumer<double[], double[]> outputMetersPerSecond,
      Subsystem... requirements) {
    m_pose = requireNonNullParam(pose, "pose", "RamseteCommand");
    m_follower = requireNonNullParam(follower, "follower", "RamseteCommand");
    m_kinematics = requireNonNullParam(kinematics, "kinematics", "RamseteCommand");
    m_output = requireNonNullParam(outputMetersPerSecond, "outputMetersPerSecond", "RamseteCommand");
    this.autoWrapper = autoWrapper;

    addRequirements(requirements);
  }

  @Override
  public void initialize() {
    m_trajectory = autoWrapper.getTrajectory();
    m_prevTime = -1;
    var initialState = m_trajectory.sample(0);
    m_prevSpeeds = m_kinematics.toWheelSpeeds(
        new ChassisSpeeds(
            initialState.velocityMetersPerSecond,
            0,
            initialState.curvatureRadPerMeter * initialState.velocityMetersPerSecond));
    m_timer.reset();
    m_timer.start();
  }

  @Override
  public void execute() {
    double curTime = m_timer.get();
    double dt = curTime - m_prevTime;

    if (m_prevTime < 0) {
      m_output.accept(new double[] { 0.0, 0.0 }, new double[] { 0.0, 0.0 });
      m_prevTime = curTime;
      return;
    }

    var targetWheelSpeeds = m_kinematics.toWheelSpeeds(
        m_follower.calculate(m_pose.get(), m_trajectory.sample(curTime)));

    
    var leftSpeedSetpoint = targetWheelSpeeds.leftMetersPerSecond;
    var rightSpeedSetpoint = targetWheelSpeeds.rightMetersPerSecond;
    var leftAccelSetpoint = (leftSpeedSetpoint - m_prevSpeeds.leftMetersPerSecond) / dt;
    var rightAccelSetpoint = (rightSpeedSetpoint - m_prevSpeeds.rightMetersPerSecond) / dt;

    System.out.println("Current: " + m_pose.get());
    System.out.println("Trajectory: " + m_trajectory.sample(curTime).poseMeters);
    System.out.println("Left Target Speed: " + leftSpeedSetpoint);
    System.out.println("Right Target Speed: " + rightSpeedSetpoint);
    System.out.println("Left Target Accel: " + leftAccelSetpoint);
    System.out.println("Right Target Accel: " + rightAccelSetpoint);

    System.out.println("-------------------------------");

    m_output.accept(new double[]{leftSpeedSetpoint, leftAccelSetpoint}, new double[]{rightSpeedSetpoint, rightAccelSetpoint});
    m_prevSpeeds = targetWheelSpeeds;
    m_prevTime = curTime;
  }

  @Override
  public void end(boolean interrupted) {
    m_timer.stop();

    if (interrupted) {
      m_output.accept(new double[] { 0.0, 0.0 }, new double[] { 0.0, 0.0 });
    }
  }

  @Override
  public boolean isFinished() {
    return m_timer.hasElapsed(m_trajectory.getTotalTimeSeconds());
  }
}