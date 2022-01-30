// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.utility;

import java.io.IOException;

import edu.wpi.first.math.controller.RamseteController;
import edu.wpi.first.math.kinematics.DifferentialDriveKinematics;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.trajectory.TrajectoryUtil;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj2.command.RamseteCommand;
import frc.robot.Constants.ConversionConstants;
import frc.robot.Constants.PhysicalConstants;
import frc.robot.subsystems.DriveTrain;

/** Add your docs here. */
public class AutoUtil {
    private Trajectory trajectory;
    private DriveTrain driveTrain;
    private RamseteCommand command;

    public AutoUtil(String filePath, DriveTrain driveTrain) {
        this.driveTrain = driveTrain;
        this.command = pathToCommand(filePath);  
    }

    public Trajectory getTrajectory(){
        return trajectory;
    }

    public RamseteCommand getCommand(){
        return command;
    }


    private RamseteCommand pathToCommand(String filePath) {
        try {
            trajectory = TrajectoryUtil.fromPathweaverJson(Filesystem.getDeployDirectory().toPath().resolve(filePath));

            return new RamseteCommand(
                trajectory,
                driveTrain::getCurrentPose,
                new RamseteController(PhysicalConstants.kRamseteB, PhysicalConstants.kRamseteZeta),
                new DifferentialDriveKinematics(PhysicalConstants.trackWidthMeters),
                (x, y) -> {
                    driveTrain.setLVelocity(x * ConversionConstants.METERS_TO_FEET, 0.8);
                    driveTrain.setRVelocity(y * ConversionConstants.METERS_TO_FEET, 0.8);
                },
                driveTrain);
        } catch (IOException ex) {
            DriverStation.reportError("Unable to open trajectory: " + filePath, ex.getStackTrace());
            return null;
        }
    }

}
