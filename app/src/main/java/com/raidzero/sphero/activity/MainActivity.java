package com.raidzero.sphero.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.SeekBar;
import android.widget.TextView;

import com.raidzero.sphero.R;
import com.raidzero.sphero.bluetooth.Sphero;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Math.abs;


public class MainActivity extends Activity implements Sphero.SpheroListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "MainActivity";

    private BluetoothAdapter adapter;
    private Sphero sphero;

    // for raw mode
    private int leftPower, rightPower;

    // for single stick mode
    private double heading, speed, aim;

    private boolean leftJoystickMoving, rightJoystickMoving;

    // executor service for controlling the sphero motors
    private ExecutorService joystickService = Executors.newSingleThreadExecutor();
    private JoystickProcessor joystickProcessor = new JoystickProcessor();

    // executor service for controlling sphero LED
    private LedProcessor ledProcessor = new LedProcessor();
    private ExecutorService ledService = Executors.newSingleThreadExecutor();

    // UI elements
    SeekBar maxSpeedBar;
    TextView maxSpeedPercentage;
    TextView battery;

    @Override
    public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
        joystickProcessor.setMaxSpeed(value);

        if (value < 50) {
            value = 50;
            maxSpeedBar.setProgress(value);
        }
        maxSpeedPercentage.setText(String.valueOf((int) ((value / 255.0) * 100)) + "%");
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    enum SpheroControlMode {
        DUAL_STICK,
        SINGLE_STICK
    }

    SpheroControlMode controlMode = SpheroControlMode.SINGLE_STICK; // default to single stick mode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        maxSpeedBar = (SeekBar) findViewById(R.id.maxSpeedBar);
        maxSpeedPercentage = (TextView) findViewById(R.id.maxSpeedPercentage);
        battery = (TextView) findViewById(R.id.battery);

        maxSpeedBar.setOnSeekBarChangeListener(this);
        adapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sphero != null) {
            sphero.disconnect();
        }

        sphero = new Sphero(this, adapter.getRemoteDevice("E5:67:61:BA:3D:57"), this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sphero != null) {
            sphero.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sphero != null) {
            sphero.disconnect();
        }
    }

    @Override
    public void onSpheroConnected() {
        init();
    }

    @Override
    public void onSpheroDisconnected() {

    }

    @Override
    public void onBatteryLevelChange(final int newLevel) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                battery.setText(getString(R.string.label_battery) + String.format("%d%%", newLevel));
            }
        });
    }

    private void startJoystickService() {
        if (!joystickService.isShutdown()) {
            joystickProcessor.setControlMode(controlMode);
            joystickService.execute(joystickProcessor);
        }
    }

    private void startLedService() {
        if (!ledService.isShutdown()) {
            ledService.execute(ledProcessor);
        }
    }

    private void init() {
        // turn off motor just in case
        sphero.rawMotor(0, 0, 50);

        joystickProcessor.setMaxSpeed(127); // default to half speed
        startJoystickService();

        ledProcessor.setMode(LedMode.SOLID);
        ledProcessor.setColor(Color.parseColor("#ff00ff00"));

        startLedService();
    }

    @Override
    public boolean dispatchGenericMotionEvent (MotionEvent ev) {
        float leftStickX, leftStickY;
        float rightStickX, rightStickY;

        switch (controlMode) {
            case DUAL_STICK:
                leftStickY = ev.getAxisValue(MotionEvent.AXIS_Y) * -1;
                rightStickY = ev.getAxisValue(MotionEvent.AXIS_RZ) * -1;

                leftPower = (int) (leftStickY * 50);
                rightPower = (int) (rightStickY * 50);

                leftJoystickMoving = leftPower != 0 || rightPower != 0;

                Log.d(TAG, "left joystick: " + leftPower);
                Log.d(TAG, "right joystick: " + rightPower);
                break;
            case SINGLE_STICK:
                leftStickX = ev.getAxisValue(MotionEvent.AXIS_X);
                leftStickY = ev.getAxisValue(MotionEvent.AXIS_Y);

                rightStickX = ev.getAxisValue(MotionEvent.AXIS_Z);
                rightStickY = ev.getAxisValue(MotionEvent.AXIS_RZ);

                Log.d(TAG, String.format("joystick activity: lX: %f lY: %f rX: %f rY: %f",
                        leftStickX, leftStickY, rightStickX, rightStickY));

                // if either axis of the left stick is not center, it is moving
                leftJoystickMoving = abs(leftStickX) > .005 || abs(leftStickY) > 0.005;

                // if either axis of the right stick is maxed out, it is moving
                rightJoystickMoving = abs(rightStickX) == 1.0 || abs(rightStickY) == 1.0;

                if (leftJoystickMoving) {
                    heading = getAngle((double) leftStickX, (double) leftStickY);
                    speed = Math.max(abs(leftStickX), abs(leftStickY));
                    Log.d(TAG, "left joystick: speed: " + speed + " heading: " + heading);
                }
                if (rightJoystickMoving) {
                    Log.d(TAG, "right joystick: aim: " + aim);
                    aim = getAngle((double) rightStickX, (double) rightStickY);
                }

                break;
        }

        return false;
    }

    /*
    @Override
    public boolean dispatchKeyEvent(KeyEvent ev) {
        Log.d(TAG, "joystick key: " + ev.getKeyCode());

        LedMode currentMode = ledProcessor.getMode();
        Log.d(TAG, "currentMode: " + currentMode.name());
        LedMode nextMode = LedMode.values()[(currentMode.ordinal() + 1) % 4];
        Log.d(TAG, "nextMode: " + nextMode.name());

        ledProcessor.setMode(nextMode);

        return false;
    }
    */

    class JoystickProcessor implements Runnable {
        boolean running;
        boolean movementStopped;
        boolean rearLedOff;

        SpheroControlMode mode;
        int maxSpeed = 127; // restrict to ~half speed by default

        @Override
        public void run() {
            running = true;
            Log.d(TAG, "starting joystick processor");
            while (running) {
                switch (mode) {
                    case DUAL_STICK:
                        if (leftJoystickMoving) {
                            Log.d(TAG, "joystick motion. sending motor command");
                            sphero.rawMotor(leftPower, rightPower, 0);
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
                        int headingInt = (int) heading;
                        int speedInt = (int) (speed * maxSpeed); // restrict to half speed
                        if (leftJoystickMoving) {
                            sphero.roll(speedInt, headingInt, aimInt);
                            movementStopped = false;
                        } else {
                            if (!movementStopped) {
                                sphero.roll(0, headingInt, aimInt);
                                movementStopped = true;
                            }
                        }
                        if (rightJoystickMoving) {
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

        public void setMaxSpeed(int maxSpeed) {
            this.maxSpeed = maxSpeed;
        }


    }

    public enum LedMode {
        STROBE,
        STROBE_RANDOM,
        BREATHE,
        SOLID,

        // experimental:
        FADE_RGB,
    }

    class LedProcessor implements Runnable {
        boolean running;
        LedMode mode;
        int ledColor;
        boolean solidColorSet;

        @Override
        public void run() {
            running = true;
            while (running) {
                switch (mode) {
                    case STROBE:
                    case STROBE_RANDOM:
                        solidColorSet = false;
                        int color = ledColor;
                        if (mode == LedMode.STROBE_RANDOM) {
                            color = getRandomColor();
                        }

                        sphero.mainLedRgb(color, 0);
                        try {Thread.sleep(100);} catch (Exception e) {}

                        sphero.mainLedRgb(Color.parseColor("#ff000000"), 0);
                        try {Thread.sleep(100);} catch (Exception e) {}

                        break;
                    case BREATHE:
                        solidColorSet = false;
                        for (int g = 0; g < 256; g += 20) {
                            sphero.mainLedRgb(Color.parseColor(String.format("#ff00%02x00", g)), 0);
                            try { Thread.sleep(100); } catch (Exception e) {}
                        }
                        for (int g = 255; g > 0; g -= 20) {
                            sphero.mainLedRgb(Color.parseColor(String.format("#ff00%02x00", g)), 0);
                            try { Thread.sleep(100); } catch (Exception e) {}
                        }
                        break;
                    case FADE_RGB:
                        solidColorSet = false;

                        int [] rgbColor = new int[3];
                        rgbColor[0] = 255;
                        rgbColor[1] = 0;
                        rgbColor[2] = 0;

                        for (int decColor = 0; decColor < 3; decColor += 1) {
                            int incColour = decColor == 2 ? 0 : decColor + 1;

                            // cross-fade the two colors.
                            for(int i = 0; i < 255; i += 1) {
                                rgbColor[decColor] -= 1;
                                rgbColor[incColour] += 1;

                                sphero.mainLedRgb(Color.parseColor(String.format("#ff%02x%02x%02x",
                                        rgbColor[0], rgbColor[1], rgbColor[2])), 0);
                                try { Thread.sleep(5); } catch (Exception e) {}
                            }
                        }
                        break;
                    case SOLID:
                        if (!solidColorSet) {
                            sphero.mainLedRgb(ledColor, 0);
                            solidColorSet = true;
                        }

                        break;
                }
            }
        }

        public void setColor(int color) {
            this.ledColor = color;
        }

        public void setMode(LedMode mode) {
            this.mode = mode;
        }

        public LedMode getMode() {
            return mode;
        }

        public void interrupt() {
            running = false;
        }

        public boolean isRunning() {
            return running;
        }

        private int getRandomColor() {
            Random r = new Random();
            int red = r.nextInt(256);
            int green = r.nextInt(256);
            int blue = r.nextInt(256);
            String colorStr = String.format("#ff%02x%02x%02x", red, green, blue);
            return Color.parseColor(colorStr);
        }
    }

    public static double getAngle(double x, double y) {
        double radians = Math.atan2(y, x);
        radians += Math.PI/2.0; // rotate 180 degrees
        double angle = Math.toDegrees(radians);

        if (angle < 0) {
            angle += 360;
        }

        return angle;
    }
}
