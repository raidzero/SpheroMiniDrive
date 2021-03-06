package com.raidzero.sphero.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.raidzero.sphero.R;
import com.raidzero.sphero.bluetooth.Sphero;
import com.raidzero.sphero.executors.JoystickProcessor;
import com.raidzero.sphero.executors.LedProcessor;
import com.raidzero.sphero.view.RgbSeekBar;


public class MainActivity extends Activity implements
        Sphero.SpheroListener,
        SeekBar.OnSeekBarChangeListener,
        AdapterView.OnItemSelectedListener,
        JoystickProcessor.JoystickInterface,
        RgbSeekBar.RgbSeekBarListener {
    private static final String TAG = "MainActivity";

    // main players
    private BluetoothAdapter adapter;
    private Sphero sphero;

    // from prefs
    private String savedSpheroAddress;
    private int savedLedColor = Color.GREEN;
    private int savedLedMode = 0;

    private static final int SCAN_ACTIVITY_REQUEST_CODE = 1000;
    SharedPreferences prefs;

    // executor service for controlling the sphero motors
    JoystickProcessor joystickProcessor;
    JoystickProcessor.JoystickData jsData = new JoystickProcessor.JoystickData();

    // executor service for controlling sphero LED
    LedProcessor ledProcessor;

    // UI elements
    LinearLayout driveControls, connectingOverlay;
    SeekBar maxSpeedBar;
    Spinner ledMode;
    TextView maxSpeedPercentage;
    TextView battery;
    RgbSeekBar rgbBar;
    TextView colorValue;
    LinearLayout ledColorContainer;

    @Override
    public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
        if (value < 50) {
            value = 50;
            maxSpeedBar.setProgress(value);
        }

        jsData.maxSpeed = value;
        maxSpeedPercentage.setText(String.valueOf((int) ((value / 255.0) * 100)) + "%");
        prefs.edit().putInt("maxSpeed", value).apply();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // dont go to sleep and cause the BT LE connection to drop
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        prefs = getSharedPreferences("SpheroMiniDrive", Context.MODE_PRIVATE);
        savedSpheroAddress = prefs.getString("spheroAddress", null);
        savedLedColor = prefs.getInt("ledColor", Color.GREEN);
        savedLedMode = prefs.getInt("ledMode", LedProcessor.LedMode.FADE_RGB.ordinal());

        driveControls = (LinearLayout) findViewById(R.id.driveControls);
        connectingOverlay = (LinearLayout) findViewById(R.id.connectingDisplay);
        ledColorContainer = (LinearLayout) driveControls.findViewById(R.id.ledColorContainer);

        maxSpeedBar = (SeekBar) findViewById(R.id.maxSpeedBar);
        ledMode = (Spinner) findViewById(R.id.ledMode);
        rgbBar = (RgbSeekBar) findViewById(R.id.rgbSeekBar);
        colorValue = (TextView) findViewById(R.id.color_value);
        maxSpeedPercentage = (TextView) findViewById(R.id.maxSpeedPercentage);
        battery = (TextView) findViewById(R.id.battery);

        int maxSpeed = prefs.getInt("maxSpeed", 127);
        maxSpeedBar.setProgress(maxSpeed);
        maxSpeedPercentage.setText(String.valueOf((int) ((maxSpeed / 255.0) * 100)) + "%");
        jsData.maxSpeed = maxSpeed;
        adapter = BluetoothAdapter.getDefaultAdapter();

        if (hideColorSelectorForMode(LedProcessor.LedMode.values()[savedLedMode])) {
            ledColorContainer.setVisibility(View.GONE);
        } else {
            ledColorContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectingOverlay.setVisibility(View.VISIBLE);
        driveControls.setVisibility(View.GONE);

        connectToSphero();
    }

    private void connectToSphero() {
        sphero = new Sphero(this, adapter.getRemoteDevice(savedSpheroAddress), this);
    }

    private void disconnectFromSphero() {
        if (sphero != null) {
            if (ledProcessor != null) {
                ledProcessor.stop();
            }
            if (joystickProcessor != null) {
                joystickProcessor.stop();
            }

            sphero.disconnect();
            sphero = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        disconnectFromSphero();
    }

    @Override
    public void onSpheroConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,
                        getString(R.string.sphero_connected, sphero.getName()), Toast.LENGTH_SHORT)
                        .show();
                driveControls.setVisibility(View.VISIBLE);
                connectingOverlay.setVisibility(View.GONE);
            }
        });

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

    private void init() {
        // turn off motor just in case
        sphero.roll(0, 0, 0);
        updateColorPreview(savedLedColor);

        LedProcessor.LedMode ledModeValue = LedProcessor.LedMode.values()[savedLedMode];
        ledProcessor = new LedProcessor(sphero, ledModeValue, savedLedColor);
        joystickProcessor = new JoystickProcessor(sphero, JoystickProcessor.SpheroControlMode.SINGLE_STICK, this);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ledMode.setSelection(savedLedMode);
                maxSpeedBar.setOnSeekBarChangeListener(MainActivity.this);
                ledMode.setOnItemSelectedListener(MainActivity.this);

                rgbBar.setColorChangeListener(MainActivity.this);
            }
        });
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        // dpad?
        if (ev.getAxisValue(MotionEvent.AXIS_HAT_X) != 0) {
            jsData.lX = ev.getAxisValue(MotionEvent.AXIS_HAT_X);
        } else {
            jsData.lX = ev.getAxisValue(MotionEvent.AXIS_X);
        }

        if (ev.getAxisValue(MotionEvent.AXIS_HAT_Y) != 0) {
            jsData.lY = ev.getAxisValue(MotionEvent.AXIS_HAT_Y);
        } else {
            jsData.lY = ev.getAxisValue(MotionEvent.AXIS_Y);
        }

        jsData.rX = ev.getAxisValue(MotionEvent.AXIS_Z);
        jsData.rY = ev.getAxisValue(MotionEvent.AXIS_RZ);

        return true;
    }


    @Override
    public JoystickProcessor.JoystickData getJoystickData() {
        return jsData;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        if (ledProcessor != null) {
            LedProcessor.LedMode newMode = LedProcessor.LedMode.values()[position];
            prefs.edit().putInt("ledMode", position).apply();

            ledProcessor.setLedMode(newMode);

            if (hideColorSelectorForMode(newMode)) {
                ledColorContainer.setVisibility(View.GONE);
            } else {
                ledColorContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    private boolean hideColorSelectorForMode(LedProcessor.LedMode mode) {
        return mode == LedProcessor.LedMode.FADE_RGB ||
                mode == LedProcessor.LedMode.STROBE_RANDOM ||
                mode == LedProcessor.LedMode.BREATHE_RANDOM ||
                mode == LedProcessor.LedMode.PULL_OVER ||
                mode == LedProcessor.LedMode.OFF;
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public void onBackPressed() {
        disconnectFromSphero();
        finishAffinity();
    }

    @Override
    public void onColorSelected(int color) {
        updateColorPreview(color);

        prefs.edit().putInt("ledColor", color).apply();
        if (ledProcessor != null) {
            ledProcessor.setLedColor(color);
        }
    }

    private void updateColorPreview(final int color) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                colorValue.setBackgroundColor(color);
            }
        });
    }
}
