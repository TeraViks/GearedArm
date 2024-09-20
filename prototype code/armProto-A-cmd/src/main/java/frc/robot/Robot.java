// armProto-A-cmd               Robot.j  cmd/subsys limited format
/* for testing arm prototype, learning coding of PID 
* position control of a single motor controlled arm with various motor 
* controller hardware and feedback encoders

* POV button press+hold L/R gives continuous angle control rev/fwd;
* button A, B, X, Y press + hold move arm to specific angles w/ PID controller
*/

package frc.robot;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.PIDCommand;
import edu.wpi.first.wpilibj2.command.PrintCommand;
import edu.wpi.first.wpilibj2.command.button.POVButton;
import edu.wpi.first.wpilibj2.command.button.Trigger;

import frc.robot.subsystems.ArmSubsys;
import static frc.robot.subsystems.ArmSubsys.*;

/**
 * The VM is configured to automatically run this class, and call the
 * functions corresponding to each mode, as described in TimedRobot doc.
 * -- in limited cmd/subsys format, no RC, all init done in roboInit.
 */
public class Robot extends TimedRobot {

  // instance joystick --assumes xbox-like pad plugged into port 0
  final XboxController myStick = new XboxController(0);

  final ArmSubsys myArmProto = new ArmSubsys();

  // button to reset encoder, set various arm angle
  Trigger leftBump;
  Trigger buttonA;
  Trigger buttonB;
  Trigger buttonX;
  Trigger buttonY;

  int angleGoal;

  @Override
  public void robotInit() {
    // declares, instances, configs this robot's specific components;
    // their functionality (methods) in subsys (when they exist).
    // things done in RobotContainer() in more complex program

    // configuring button binding -- here rI vs. rC for simplicity
    // define button triggers, used in rP and tP

    // will reset encoder to show distance of 0 at current position
    leftBump = new Trigger(myStick::getLeftBumperPressed);

    buttonA = new Trigger(myStick::getAButton);
    buttonB = new Trigger(myStick::getBButton);
    buttonX = new Trigger(myStick::getXButton);
    buttonY = new Trigger(myStick::getYButton);

    Trigger armRevrs = new POVButton(myStick, 270); // left POV
    Trigger armForwd = new POVButton(myStick, 90); // rt POV

    // oddly, trigger work here (rI) to move arm, but only when Enabled
    armForwd.onTrue(new InstantCommand(() -> myArmProto.armMotorSpark.set(0.5)));
    armForwd.onFalse(new InstantCommand(() -> myArmProto.armMotorSpark.set(0.0)));
    // both started movement onTrue, didn't stop until .onFalse added,
    armRevrs.onTrue(new InstantCommand(() -> myArmProto.armMotorSpark.set(-0.4)));
    armRevrs.onFalse(new InstantCommand(() -> myArmProto.armMotorSpark.set(0.0)));

    // method of encoder superclass; works here only when Enabled; 
    leftBump.onTrue(new InstantCommand(() -> myArmProto.absolArmEncod.reset()));

    // display PID coefficients on SmartDashboard
    SmartDashboard.putNumber("P Gain", kP);
    SmartDashboard.putNumber("I Gain", kI);
    SmartDashboard.putNumber("D Gain", kD);
    // SmartDashboard.putNumber("I Zone", kIz);
    // SmartDashboard.putNumber("Feed Forward", kFF);
    // SmartDashboard.putNumber("Max Output", kMaxOutput);
    // SmartDashboard.putNumber("Min Output", kMinOutput);

    // init SmtDsh field for desired angle setpoint and present angle
    SmartDashboard.putNumber("angleGoal", 0);
    // to show present encoder value
    SmartDashboard.putNumber("armAngle deg.", 0);
    // to put text on SmtDsh
    // SmartDashboard.putString("GTPcmd fin?", "???");

  } // end robotInit()

  // This function is called every robot packet, no matter the mode.
  // Does things you want run in all modes, like diagnostics.
  // This runs after the mode specific periodic functions, but before
  // LiveWindow and SmartDashboard integrated updating.
  @Override
  public void robotPeriodic() {
    // Calls the Scheduler <-- this is for polling buttons, adding
    // newly-scheduled commands, running now-scheduled commands,
    // removing finished commands, and running subsystem periodics.
    // Need CS.run for anything in the Cmd/Subsys framework to work

    // display selected angle (setpoint) on SmtDash
    if (myStick.getAButtonPressed())
      angleGoal = 0;
    if (myStick.getBButtonPressed())
      angleGoal = 60;
    if (myStick.getXButtonPressed())
      angleGoal = 90;
    if (myStick.getYButtonPressed())
      angleGoal = 120;

    SmartDashboard.putNumber("angleGoal", angleGoal);
    // display current arm angle
    SmartDashboard.putNumber("armAngle deg.",
        myArmProto.getAngle());
    // class method will return Math.round(absolArmEncod.getDistance()

    CommandScheduler.getInstance().run();
  } // end robotPeriodic

  // autoInit runs the autonomous command set in {RobotContainer}
  @Override
  public void autonomousInit() {

  } // end autoInit

  /** This function is called periodically during autonomous. */
  @Override
  public void autonomousPeriodic() {
  }

  @Override
  public void teleopInit() {
    // This confirms that the autonomous code has stopped,. If you want
    // auto cmd to continue until interrupted, remove this line.
    // if (m_autonomousCommand != null) {
    // m_autonomousCommand.cancel();
    // }
    // m_robotContainer.m_drivetrain.resetEncoders();
    // m_robotContainer.m_drivetrain.resetGyro();
  } // end teleInit

  /* This function is called periodically when bot Enabled */
  @Override
  public void teleopPeriodic() {

    // press + hold returns arm to home (full reverse) angle, slowly
    buttonA.onTrue(new PrintCommand("buttonA press"))
        .whileTrue(new PIDCommand(new PIDController(kP, kI, kD),
            // Close the loop by reading present angle
            myArmProto::getAngle,
            // Setpoint is 0 for full reverse, allow for overshoot
            setpointA, // how to .setTol for this inline PIDcontrol?
            // Pipe output to arm subsys method
            output -> myArmProto.armMotorSpark.set(output * 0.07),
            // Require the subsys instance
            myArmProto));

    buttonB.onTrue(new PrintCommand("buttonB press"))
        .whileTrue(new PIDCommand(new PIDController(kP, kI, kD),
            // Close the loop by reading present angle
            myArmProto::getAngle,
            // Setpoint = class var from subsys
            setpointB, // how to .setTol for this PIDcontrol?
            // Pipe output to arm subsys motor's method
            output -> myArmProto.armMotorSpark.set(output * 0.1),
            // Require the subsys
            myArmProto));

    // go to 90° on buttonX press+hold
    buttonX.onTrue(new PrintCommand("buttonX press"))
        .whileTrue(new PIDCommand(new PIDController(kP, kI, kD),
            // Close the loop on present angle
            myArmProto::getAngle,
            // Setpoint 90
            setpointX,
            // Pipe output to armMotor method
            output -> myArmProto.armMotorSpark.set(output * 0.15),
            // Require the subsys
            myArmProto));

    buttonY.onTrue(new PrintCommand("buttonY press"))
        .whileTrue(new PIDCommand(new PIDController(kP, kI, kD),
            // Close the loop by reading present angle
            myArmProto::getAngle,
            // Setpoint 120
            setpointY, // how to .setTol for this PIDcontrol?
            // Pipe output to arm subsys motor's method
            output -> myArmProto.armMotorSpark.set(output * 0.15),
            // Require the subsys
            myArmProto));

  } // end telePeri

  // function is called once each time robot enters Disabled mode.
  @Override
  public void disabledInit() {
  }

  @Override
  public void disabledPeriodic() {
  }

  @Override
  public void testInit() {
    // Cancels all running commands at the start of test mode.
    // CommandScheduler.getInstance().cancelAll();
  }

  /** This function is called periodically during test mode. */
  @Override
  public void testPeriodic() {
  }
} // end class