// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.commands.autonomous.AutonomousController;
import frc.robot.subsystems.Intake;

public class IntakeCommand extends CommandBase {

  private final Intake intake;
  private final Controls controls;

  public static interface Controls {
    boolean getIntakeOut();
    boolean getBallChainReverse();
    boolean isIntakeEngaged();
  }

  /** Creates a new IntakeCommand. */
  public IntakeCommand(Intake intake, Controls controls) {
    // Use addRequirements() here to declare subsystem dependencies.
    addRequirements(intake);
    this.intake = intake;
    this.controls = controls;
  }

  public IntakeCommand(Intake intake){ // Autonomouse
    this(intake, new AutonomousController(false, "Intake"));
  }

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {
    this.intake.setPiston(false);
  }

  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {
    this.intake.setPiston(controls.getIntakeOut());
    if(controls.getBallChainReverse()){
      intake.setReversed(true);
    } else {
      intake.setReversed(false);
    }
  }

  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {
    intake.setPiston(false);
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    return false;
  }
}
