package team492.diagnostics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.wpi.first.wpilibj.DriverStation;
import hallib.HalDashboard;
import team492.Robot;
import team492.diagnostics.DiagnosticsTest.TestResult;
import team492.diagnostics.tests.DigitalSensorUnchangedTest;
import team492.diagnostics.tests.ElevatorPositionUnchangedTest;
import team492.diagnostics.tests.EncoderUnpluggedTest;
import team492.diagnostics.tests.GyroNotConnectedTest;
import team492.diagnostics.tests.UltrasonicUnpluggedTest;
import team492.diagnostics.tests.PixyVisionTaskTerminatedTest;
import team492.diagnostics.tests.HighTalonErrorRateTest;
import team492.diagnostics.tests.MotorCurrentTest;
import team492.diagnostics.tests.PneumaticsNotPressurizingTest;
import team492.diagnostics.tests.ElevatorLimitSwitchStuckTest;
import team492.diagnostics.tests.PneumaticsCurrentlyLowTest;

public class OnBoardDiagnostics
{

    private Robot robot;
    private List<DiagnosticsTest> tests;
    
    public enum Subsystem
    {
        DRIVEBASE,
        ELEVATOR,
        SENSORS,
        GRABBER,
        PRESSURE
    }

    public OnBoardDiagnostics(Robot robot)
    {
        this.robot = robot;
        tests = new ArrayList<>();
        tests.add(new EncoderUnpluggedTest(robot.leftFrontWheel, "lf encoder", Subsystem.DRIVEBASE));
        tests.add(new EncoderUnpluggedTest(robot.rightFrontWheel, "rf encoder", Subsystem.DRIVEBASE));
        tests.add(new EncoderUnpluggedTest(robot.leftRearWheel, "lr encoder", Subsystem.DRIVEBASE));
        tests.add(new EncoderUnpluggedTest(robot.rightRearWheel, "rr encoder", Subsystem.DRIVEBASE));

        tests.add(new HighTalonErrorRateTest(robot.leftFrontWheel, "lf motor errors", Subsystem.DRIVEBASE));
        tests.add(new HighTalonErrorRateTest(robot.rightFrontWheel, "rf motor errors", Subsystem.DRIVEBASE));
        tests.add(new HighTalonErrorRateTest(robot.leftRearWheel, "lr motor errors", Subsystem.DRIVEBASE));
        tests.add(new HighTalonErrorRateTest(robot.rightRearWheel, "rr motor errors", Subsystem.DRIVEBASE));
        tests.add(new HighTalonErrorRateTest(robot.elevator.elevatorMotor, "elevator motor errors", Subsystem.ELEVATOR));

        tests.add(
            new ElevatorLimitSwitchStuckTest(robot.elevator, ElevatorLimitSwitchStuckTest.ElevatorLimitSwitch.LOWER));
        tests.add(
            new ElevatorLimitSwitchStuckTest(robot.elevator, ElevatorLimitSwitchStuckTest.ElevatorLimitSwitch.UPPER));

        if (robot.leftSonarArray != null || robot.leftSonarSensor != null)
        {
            tests.add(new UltrasonicUnpluggedTest(robot::getLeftSonarDistance, "left sonar"));
        }

        if (robot.rightSonarArray != null || robot.rightSonarSensor != null)
        {
            tests.add(new UltrasonicUnpluggedTest(robot::getRightSonarDistance, "right sonar"));
        }

        tests.add(new DigitalSensorUnchangedTest(robot.elevator.elevatorMotor::isLowerLimitSwitchActive,
            "elev L switch unchanged", Subsystem.ELEVATOR));
        tests.add(new DigitalSensorUnchangedTest(robot.elevator.elevatorMotor::isUpperLimitSwitchActive,
            "elev U switch unchanged", Subsystem.ELEVATOR));

        tests.add(new DigitalSensorUnchangedTest(robot.cubePickup::cubeInProximity, "cube proximity sensor", Subsystem.SENSORS));

        tests.add(new ElevatorPositionUnchangedTest("Elevator position", robot.elevator));

        tests.add(new PneumaticsNotPressurizingTest("Pneumatics working", robot));
        tests.add(new PneumaticsCurrentlyLowTest("Pneumatics charged", robot));

        tests.add(new MotorCurrentTest("Master grabber motor", Subsystem.GRABBER, robot.cubePickup.controlMotor));
        tests.add(new MotorCurrentTest("Slave grabber motor", Subsystem.GRABBER, robot.cubePickup.slaveMotor));
        
        if (robot.pixy != null)
        {
            tests.add(new PixyVisionTaskTerminatedTest("Pixy errors", robot.pixy));
        }

        if (robot.gyro != null)
        {
            tests.add(new GyroNotConnectedTest("Gyro connected", robot.gyro));
        }
    }

    public void doPeriodicTests()
    {
        tests.forEach(DiagnosticsTest::test);
    }
    
    public Map<String,Boolean> getAllDiagnosticResults()
    {
        Map<String,Boolean> map = new HashMap<>();
        for(DiagnosticsTest test:tests)
        {
            map.put(test.getName(), !test.getResult().faultDetected());
        }
        return map;
    }
    
    public Map<Subsystem, Boolean> getSubsystemDiagnosticResults()
    {
        return tests.stream()
                .collect(Collectors.groupingBy(DiagnosticsTest::getSubsystem,
                                               Collectors.reducing(
                                                   true, // Show green by default
                                                   test -> !test.getResult().faultDetected(), // Show green if not faulted
                                                   Boolean::logicalAnd))); // Show green only if all are OK
    }
    
    public void updateDiagnosticsAndDashboard()
    {
        doPeriodicTests();
        Map<String,Boolean> testResults = getAllDiagnosticResults();
        for(String testName:testResults.keySet())
        {
            HalDashboard.putBoolean("Diagnostics/" + testName, testResults.get(testName));
        }
        
        Map<Subsystem, Boolean> subsystemResults = getSubsystemDiagnosticResults();
        for(Map.Entry<Subsystem, Boolean> entry : subsystemResults.entrySet())
        {
            HalDashboard.putBoolean("Test/" + entry.getKey().name(), entry.getValue());
        }
        
    }

    public void printDiagnostics()
    {
        boolean faults = false;

        for (DiagnosticsTest test : tests)
        {
            TestResult result = test.getResult();

            if (result.faultDetected())
            {
                faults = true;
                DriverStation.reportError("### Diagnostics: " + result.errorMessage(), false);
            }
        }

        if (faults)
        {
            robot.ledIndicator.indicateDiagnosticError();
        }
        else
        {
            DriverStation.reportError("### Diagnostics: No faults", false);
            robot.ledIndicator.indicateNoDiagnosticError();
        }
    }
}
