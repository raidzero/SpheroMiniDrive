package com.raidzero.sphero.executors;

import android.util.Log;

import com.raidzero.sphero.bluetooth.Sphero;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Math.abs;
import static java.lang.Math.max;

/**
 * Created by posborn on 2/9/18.
 */

public class JoystickProcessor {
    private static final String TAG = "JoystickProcessor";

    private ExecutorService service = Executors.newSingleThreadExecutor();
    private JoystickThread thread = new JoystickThread();
    private Sphero sphero;
    private JoystickInterface callback;

    public enum SpheroControlMode {
        DUAL_STICK,
        SINGLE_STICK
    }

    // container class for holding a joystick coordinates
    public static class JoystickData {
        public float lX, lY, rX, rY;
        public int maxSpeed;
    }

    private double aim;

    public interface JoystickInterface {
        JoystickData getJoystickData();
    }

    public JoystickProcessor(Sphero sphero, SpheroControlMode mode, JoystickInterface callback) {
        this.sphero = sphero;
        this.callback = callback;

        thread.setControlMode(mode);

        if (!service.isShutdown()) {
            service.execute(thread);
        }
    }

    class JoystickThread implements Runnable {
        boolean running;
        boolean movementStopped;
        boolean rearLedOff = true;

        SpheroControlMode mode;

        @Override
        public void run() {
            running = true;
            Log.d(TAG, "starting joystick processor");
            while (running) {
                switch (mode) {
                    case DUAL_STICK:
                        if (rearLedOff) {
                            sphero.rearLed(true);
                            rearLedOff = false;
                        }

                        if (leftStickMoving() || rightStickMoving()) {
                            Log.d(TAG, "joystick motion. sending motor command");
                            sphero.rawMotor(getLeftPower(), getRightPower(), 0);
                            movementStopped = false;
                        } else {
                            // stop the sphero motors, but only if they are not already stopped
                            if (!movementStopped) {
                                sphero.rawMotor(0, 0, 0);
                                movementStopped = true;
                            }
                        }
                        break;
                    case SINGLE_STICK:
                        int aimInt = (int) aim;
                        int headingInt = (int) getHeading();
                        int speedInt = (int) (getSpeed() * getMaxSpeed()); // restrict to half speed
                        if (leftStickMoving()) {
                            sphero.roll(speedInt, headingInt, aimInt);
                            Log.d(TAG, String.format("rolling: speed: %d, heading: %d, aim: %d",
                                    speedInt, headingInt, aimInt));
                            movementStopped = false;
                        } else {
                            if (!movementStopped) {
                                sphero.roll(0, headingInt, aimInt);
                                movementStopped = true;
                            }
                        }
                        if (rightStickAiming()) {
                            aim = getAim();
                            sphero.rearLed(true, 0);
                            sphero.roll(0, aimInt, 0);

                            rearLedOff = false;
                        } else {
                            if (!rearLedOff) {
                                sphero.rearLed(false, 0);
                                rearLedOff = true;
                            }
                        }

                        break;
                }

                try {
                    // only poll for joystick activity 20 times per second
                    Thread.sleep(50);
                } catch (Exception e) {
                    //
                }
            }
        }

        public void interrupt() {
            running = false;
        }

        public boolean isRunning() {
            return running;
        }

        public void setControlMode(SpheroControlMode mode) {
            this.mode = mode;
        }
    }

    private boolean leftStickMoving() {
        float lX = callback.getJoystickData().lX;
        float lY = callback.getJoystickData().lY;

        return abs(lX) > 0.005 || abs(lY) > 0.005;
    }

    private boolean rightStickMoving() {
        float rX = callback.getJoystickData().rX;
        float rY = callback.getJoystickData().rY;

        return abs(rX) > 0.005 || abs(rY) > 0.005;
    }

    private boolean rightStickAiming() {
        // aiming is done when the right stick is all the way in both axes
        float rX = callback.getJoystickData().rX;
        float rY = callback.getJoystickData().rY;

        return abs(rX) == 1 || abs(rY) == 1;
    }

    private int getLeftPower() {
        return (int) (callback.getJoystickData().lY * 50);
    }

    private int getRightPower() {
        return (int) (callback.getJoystickData().rY * 50);
    }

    private double getAim() {
        return getAngle(
                (double) callback.getJoystickData().rX,
                (double) callback.getJoystickData().rY);
    }

    private double getHeading() {
        return getAngle(
                (double) callback.getJoystickData().lX,
                (double) callback.getJoystickData().lY);
    }

    private double getSpeed() {
        return max(
                abs(callback.getJoystickData().lX),
                abs(callback.getJoystickData().lY)
        );
    }

    private int getMaxSpeed() {
        return callback.getJoystickData().maxSpeed;
    }

    private double getAngle(double x, double y) {
        double radians = Math.atan2(y, x);
        radians += Math.PI/2.0; // rotate 180 degrees
        double angle = Math.toDegrees(radians);

        if (angle < 0) {
            angle += 360;
        }

        return angle;
    }

    public void stop() {
        thread.interrupt();
        service.shutdownNow();
    }
}
