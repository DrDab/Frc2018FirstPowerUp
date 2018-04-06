/*
 * Copyright (c) 2018 Titan Robotics Club (http://www.titanrobotics.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package team492;

import trclib.TrcRobot;

public class FrcDisabled implements TrcRobot.RobotMode
{
    private enum State
    {
        START,
        AUTO_DONE,
        TELEOP_DONE,
        DONE
    }

    private Robot robot;
    private State state = State.START;

    public FrcDisabled(Robot robot)
    {
        this.robot = robot;
    } // FrcDisabled

    //
    // Implements TrcRobot.RunMode interface.
    //

    @Override
    public void startMode()
    {
        if (robot.ds.isFMSAttached())
        {
            //
            // We are in a competition match.
            //
            switch (state)
            {
                case START:
                    break;

                case AUTO_DONE:
                    state = State.TELEOP_DONE;
                    robot.getFMSInfo();
                    break;

                case TELEOP_DONE:
                    // TODO: Figure out what the hell is going on here
//                    try
//                    {
//                        String traceLogName = robot.globalTracer.getTraceLogName();
//                        String suffix = traceLogName.substring(traceLogName.indexOf('&'));
//                        String newFile = String.format("%s_%s%03d%s", 
//                            robot.eventName, robot.matchType, robot.matchNumber, suffix);
//                        File file = new File(traceLogName);
//                        robot.globalTracer.traceInfo("FrcDisabled", "### OldName: %s, NewName: %s",
//                            traceLogName, newFile);
//                        robot.closeTraceLog();
//                        //file.renameTo(new File(file.getParent() + "\\" + newFile));                        
//                    }
//                    catch(Exception e)
//                    {
//                        DriverStation.reportError(e.getMessage(), false);
//                        // Fail silently
//                    }
                    state = State.DONE;
                    break;

                default:
                case DONE:
                    break;
            }
        }
        else
        {
            //
            // Not in competition match, probably in test or practice mode.
            //
            robot.closeTraceLog();
        }
    } // startMode

    @Override
    public void stopMode()
    {
        if (!robot.ds.isFMSAttached())
        {
            robot.openTraceLog();
        }
    } // stopMode

    @Override
    public void runPeriodic(double elapsedTime)
    {
        if (!robot.traceLogOpened && state != State.DONE && robot.ds.isFMSAttached())
        {
            robot.openTraceLog();
            state = State.AUTO_DONE;
        }

        robot.updateDashboard();
        robot.announceIdling();
    } // runPeriodic

    @Override
    public void runContinuous(double elapsedTime)
    {
    } // runContinuous

} // class FrcDisabled
