// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.util.List;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.trajectory.TrajectoryConfig;
import edu.wpi.first.math.trajectory.TrajectoryGenerator;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.LEDPattern;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.SwerveControllerCommand;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Constants.AutoConstants;
import frc.robot.commands.exampleCommand;
import frc.robot.commands.driveStraight;
import frc.robot.subsystems.LEDSubsystem;
import frc.robot.subsystems.buttonCommandTest;

public class Robot extends TimedRobot {
  private final XboxController m_controller = new XboxController(0);
  private Trigger yButton = new JoystickButton(m_controller, XboxController.Button.kY.value);
  private Trigger xButton = new JoystickButton(m_controller, XboxController.Button.kX.value);
  private Trigger aButton = new JoystickButton(m_controller, XboxController.Button.kA.value);
  private final Joystick m_joystick = new Joystick(2);

  private double[] driveInputs = {0,0,0};
  private Trigger mode = new JoystickButton(m_controller, 7);
  private boolean isJoystick = false;

  private final Drivetrain m_swerve = new Drivetrain();
  private final Field2d m_field = new Field2d();
  private buttonCommandTest bCmdTest = new buttonCommandTest();

  // Slew rate limiters to make joystick inputs more gentle; Passing in "3" means 1/3 sec from 0 to 1.
  private final SlewRateLimiter m_xspeedLimiter = new SlewRateLimiter(3);
  private final SlewRateLimiter m_yspeedLimiter = new SlewRateLimiter(3);
  private final SlewRateLimiter m_rotLimiter = new SlewRateLimiter(3);

  private Command m_autonomousCommand;

  private String m_autoSelected;
  private final SendableChooser<String> m_AutoChooser = new SendableChooser<>();

  private final LEDSubsystem ledSystem = new LEDSubsystem();

  @Override
  public void robotInit() {
    m_AutoChooser.setDefaultOption("None", Constants.AutoConstants.kAutoProgram[0]);
    m_AutoChooser.addOption("Auto 1", Constants.AutoConstants.kAutoProgram[1]);
    m_AutoChooser.addOption("Auto 2", Constants.AutoConstants.kAutoProgram[2]);

    SmartDashboard.putData("Auto Choices", m_AutoChooser);  //Sync the Autochooser
  }

  @Override
  public void robotPeriodic() {
    // Runs the Scheduler.  This is responsible for polling buttons, adding newly-scheduled
    // commands, running already-scheduled commands, removing finished or interrupted commands,
    // and running subsystem periodic() methods.  This must be called from the robot's periodic
    // block in order for anything in the Command-based framework to work.
    CommandScheduler.getInstance().run();

    SmartDashboard.putData("Auto Choices", m_AutoChooser);  //Sync the Autochooser
  }


  /** This autonomous runs the autonomous command selected by your {@link RobotContainer} class. */
  @Override
  public void autonomousInit() {
    m_autonomousCommand = getAutonomousCommand();

    m_autoSelected = m_AutoChooser.getSelected();

    // schedule the autonomous command (example)
    if (m_autonomousCommand != null) {
      m_autonomousCommand.schedule();
    }    
  }

  @Override
  public void teleopInit() {
    // Do this in either robot or subsystem init
    SmartDashboard.putData("Field", m_field);
    
    // This makes sure that the autonomous stops running when teleop starts running. If you want the autonomous to
    // continue until interrupted by another command, remove this line or comment it out.
    if (m_autonomousCommand != null) {
      m_autonomousCommand.cancel();
    }

    //Button Triggers
    yButton.onFalse(new exampleCommand(.1, bCmdTest));
    xButton.onTrue(bCmdTest.buttonTest2());
    mode.onTrue(toggleJoystick().alongWith(ledSystem.runPattern(LEDPattern.rainbow(255, 128))));

    //Pressing A button sends robot forward, releasing sends it back
    aButton.onTrue(new driveStraight(.25, getPeriod(), m_swerve));
    aButton.onFalse(new driveStraight(-.25, getPeriod(), m_swerve));

    

  }
  
  @Override
  public void autonomousPeriodic() {
    //driveWithJoystick(false);
    publishToDashboard();
    m_swerve.publishToDashboard();
    m_swerve.updateOdometry();

    // Do this in either robot periodic or subsystem periodic
    m_field.setRobotPose(m_swerve.m_odometry.getPoseMeters());
  }

  @Override
  public void teleopPeriodic() {
    publishToDashboard();
    //m_swerve.publishToDashboard();
    driveWithJoystick(false);
    m_swerve.updateOdometry();

    // Do this in either robot periodic or subsystem periodic
    m_field.setRobotPose(m_swerve.m_odometry.getPoseMeters());




  }

  public void driveWithJoystick(boolean fieldRelative) {
    
    if (isJoystick) {
        driveInputs[0] = m_joystick.getX();
        driveInputs[1] = m_joystick.getY();
        driveInputs[2]= m_joystick.getZ();
    } else {
        driveInputs[0] = m_controller.getLeftX();
        driveInputs[1] = m_controller.getLeftY();
        driveInputs[2] = m_controller.getRightX();
    }
    // Get the x speed. We are inverting this because Xbox controllers return
    // negative values when we push forward.
    final var xSpeed =
        -m_xspeedLimiter.calculate(MathUtil.applyDeadband(driveInputs[1], 0.1))
            * Constants.kMaxRobotSpeed;
    //final var xSpeed =
    //    -m_xspeedLimiter.calculate(MathUtil.applyDeadband(m_joystick.getY(), 0.1))
    //        * Constants.kMaxRobotSpeed;
    SmartDashboard.putNumber("xSpeed", xSpeed);

    // Get the y speed or sideways/strafe speed. We are inverting this because
    // we want a positive value when we pull to the left. Xbox controllers
    // return positive values when you pull to the right by default.
    final var ySpeed =
        -m_yspeedLimiter.calculate(MathUtil.applyDeadband(driveInputs[0], 0.1))
            * Constants.kMaxRobotSpeed;
    //final var ySpeed =
    //    -m_yspeedLimiter.calculate(MathUtil.applyDeadband(m_joystick.getX(), 0.1))
    //        * Constants.kMaxRobotSpeed;
    SmartDashboard.putNumber("ySpeed", ySpeed);

    // Get the rate of angular rotation. We are inverting this because we want a
    // positive value when we pull to the left (remember, CCW is positive in
    // mathematics). Xbox controllers return positive values when you pull to
    // the right by default.
    final var rot =
        -m_rotLimiter.calculate(MathUtil.applyDeadband(driveInputs[2], 0.1))
            * Constants.kMaxRobotAngularSpeed;
    //final var rot =
    //    -m_rotLimiter.calculate(MathUtil.applyDeadband(m_joystick.getZ(), 0.2))
    //        * Constants.kMaxRobotAngularSpeed;
    SmartDashboard.putNumber("rot", rot);
  
  
    m_swerve.drive(xSpeed, ySpeed, rot, fieldRelative, getPeriod());    
  }


  public void publishToDashboard()
  {
    SmartDashboard.putNumber("Controller Left X", m_controller.getLeftX());
    SmartDashboard.putNumber("Controller Left Y", m_controller.getLeftY());
    SmartDashboard.putNumber("Controller Right X", m_controller.getRightX());
    SmartDashboard.putNumber("Gyro Angle", m_swerve.m_gyro.getAngle());
    SmartDashboard.putBoolean("Joystick Enabled", isJoystick);
    
  }


  /* AUTO Stuff below here */
   public Command getAutonomousCommand() {
    // Create config for trajectory
    TrajectoryConfig fwdconfig = new TrajectoryConfig(
        AutoConstants.kMaxSpeedMetersPerSecond,
        AutoConstants.kMaxAccelerationMetersPerSecondSquared)
        .setKinematics(m_swerve.m_kinematics);  // Add kinematics to ensure max speed is actually obeyed
    
    // Create config for trajectory
    TrajectoryConfig backconfig = new TrajectoryConfig(
          AutoConstants.kMaxSpeedMetersPerSecond,
          AutoConstants.kMaxAccelerationMetersPerSecondSquared)
          .setKinematics(m_swerve.m_kinematics)  // Add kinematics to ensure max speed is actually obeyed
          .setReversed(true);           // "Reversed" will allow the robot to go backwards through trajs

    // An example trajectory to follow. All units in meters.
    Trajectory fwdTraj = TrajectoryGenerator.generateTrajectory(
        // Start at the origin facing the +X direction
        new Pose2d(0, 0, new Rotation2d(0)),
        // Pass through these two interior waypoints, making an 's' curve path
        List.of(new Translation2d(.75, 0)),
        // End 1.5 meters straight ahead of where we started, facing forward
        new Pose2d(1.5, 0, new Rotation2d(0)),
        fwdconfig);

     Trajectory backTraj = TrajectoryGenerator.generateTrajectory(
        // Start at the origin facing the +X direction
        new Pose2d(0, 0, new Rotation2d(0)),
        // Pass through these two interior waypoints, making an 's' curve path
        List.of(new Translation2d(-.10, 0)),
        // End 1.5 meters straight ahead of where we started, facing forward
        new Pose2d(-1.5, 0, new Rotation2d(0)),
        backconfig);

        Trajectory leftTraj = TrajectoryGenerator.generateTrajectory(
          // Start at the origin facing the +X direction
          new Pose2d(0, 0, new Rotation2d(0)),
          // Pass through these two interior waypoints, making an 's' curve path
          List.of(new Translation2d(0, .75)),
          // End 1.5 meters straight ahead of where we started, facing forward
          new Pose2d(0,1.5, new Rotation2d(0)),
          backconfig);

    Trajectory conTrajectory = fwdTraj.concatenate(backTraj);

    var thetaController = new ProfiledPIDController(
        AutoConstants.kPThetaController, 0, 0, AutoConstants.kThetaControllerConstraints);
    thetaController.enableContinuousInput(-Math.PI, Math.PI);

    SwerveControllerCommand swerveControllerCommand1 = 
    new SwerveControllerCommand(
      fwdTraj,
      m_swerve::getPose,
      m_swerve.m_kinematics,
      // Position controllers
      new PIDController(AutoConstants.kPXController, 0, 0),
      new PIDController(AutoConstants.kPYController, 0, 0),
      thetaController,
      m_swerve::setModuleStates,
      m_swerve);

      SwerveControllerCommand swerveControllerCommand2 = 
      new SwerveControllerCommand(
        backTraj,
        m_swerve::getPose,
        m_swerve.m_kinematics,
        // Position controllers
        new PIDController(AutoConstants.kPXController, 0, 0),
        new PIDController(AutoConstants.kPYController, 0, 0),
        thetaController,
        m_swerve::setModuleStates,
        m_swerve);

        SwerveControllerCommand swerveControllerCommandLeft = 
        new SwerveControllerCommand(
          leftTraj,
          m_swerve::getPose,
          m_swerve.m_kinematics,
          // Position controllers
          new PIDController(AutoConstants.kPXController, 0, 0),
          new PIDController(AutoConstants.kPYController, 0, 0),
          thetaController,
          m_swerve::setModuleStates,
          m_swerve);
  
        driveStraight driveStraightCommand =
        new driveStraight(0.3, getPeriod(), m_swerve);
        
        // Reset odometry to the initial pose of the trajectory, run path following
    // command, then stop at the end.
    return Commands.sequence(
        new InstantCommand(() -> m_swerve.resetOdometry(backTraj.getInitialPose())),
        new InstantCommand(() -> System.out.println("Command 1:")),
        //driveStraightCommand,
        //new InstantCommand(() -> System.out.println("Stop & wait 3 seconds")),
        //new InstantCommand(() -> m_swerve.drive(0,0,0,false, getPeriod())).repeatedly().withTimeout(3),
        m_swerve.getPathPlannerCommand(),
        new InstantCommand(() -> System.out.println("Stop & wait  .5 seconds")),
        new InstantCommand(() -> m_swerve.drive(0,0,0,false, getPeriod())).repeatedly().withTimeout(.5),
        new InstantCommand(() -> System.out.println("Done !")));
  }
  public Command toggleJoystick() {
    return Commands.sequence(
        new InstantCommand(() -> isJoystick=!isJoystick)
    );
}

}
