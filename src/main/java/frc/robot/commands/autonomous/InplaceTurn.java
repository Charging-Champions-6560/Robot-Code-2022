// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands.autonomous;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.DriveTrain;

public class InplaceTurn extends CommandBase {
  /** Creates a new InplaceTurn. */
  private final DriveTrain driveTrain;
  private final double degreesToTurn;
  private double startRotation;

  public InplaceTurn(DriveTrain driveTrain, double degreesToTurn) {
    // Use addRequirements() here to declare subsystem dependencies.
    addRequirements(driveTrain);
    this.driveTrain = driveTrain;
    this.degreesToTurn = degreesToTurn;
  }

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {
    startRotation = driveTrain.getAngleContinuous();
  }

  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {
    if(!isFinished()) driveTrain.setVelocity(0, 0.4 * Math.copySign(1, degreesToTurn));
    else driveTrain.setVelocity(0, 0);
  }

  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {
    driveTrain.setVelocity(0, 0);
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    double curRotation = driveTrain.getAngleContinuous();
    System.out.println(Math.abs(curRotation - startRotation));
    return  Math.abs(curRotation - startRotation) > Math.abs(degreesToTurn);
  }
}
