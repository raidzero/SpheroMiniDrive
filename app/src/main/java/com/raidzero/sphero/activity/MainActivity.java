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
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.raidzero.sphero.R;
import com.raidzero.sphero.bluetooth.Sphero;
import com.raidzero.sphero.executors.JoystickProcessor;
import com.raidzero.sphero.executors.LedProcessor;


public class MainActivity extends Activity implements
        Sphero.SpheroListener,
        SeekBar.OnSeekBarChangeListener,
        AdapterView.OnItemSelectedListener,
        JoystickProcessor.JoystickInterface {
    private static final String TAG = "MainActivity";

    // main players
    private BluetoothAdapter adapter;
    private Sphero sphero;

    SharedPreferences prefs;

    // executor service for controlling the sphero motors
    JoystickProcessor joystickProcessor;
    JoystickProcessor.JoystickData jsData = new JoystickProcessor.JoystickData();

    // executor service for controlling sphero LED
    LedProcessor ledProcessor;

    // UI elements
    SeekBar maxSpeedBar;
    Spinner ledMode;
    TextView maxSpeedPercentage;
    TextView battery;

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

        maxSpeedBar = (SeekBar) findViewById(R.id.maxSpeedBar);
        ledMode = (Spinner) findViewById(R.id.ledMode);
        maxSpeedPercentage = (TextView) findViewById(R.id.maxSpeedPercentage);
        battery = (TextView) findViewById(R.id.battery);

        maxSpeedBar.setProgress(prefs.getInt("maxSpeed", 127));
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
            ledProcessor.stop();
            joystickProcessor.stop();
            sphero.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sphero != null) {
            ledProcessor.stop();
            joystickProcessor.stop();
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

    private void init() {
        // turn off motor just in case
        sphero.roll(0, 0, 0);
        final int prefLedMode = prefs.getInt("ledMode", 0);
        LedProcessor.LedMode savedLedMode = LedProcessor.LedMode.values()[prefLedMode];
        ledProcessor = new LedProcessor(sphero, savedLedMode, Color.GREEN);
        joystickProcessor = new JoystickProcessor(sphero, JoystickProcessor.SpheroControlMode.SINGLE_STICK, this);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                maxSpeedBar.setOnSeekBarChangeListener(MainActivity.this);
                ledMode.setSelection(prefLedMode);
                ledMode.setOnItemSelectedListener(MainActivity.this);
            }
        });
    }

    @Override
    public boolean dispatchGenericMotionEvent (MotionEvent ev) {
        jsData.lX = ev.getAxisValue(MotionEvent.AXIS_X);
        jsData.lY = ev.getAxisValue(MotionEvent.AXIS_Y);

        jsData.rX = ev.getAxisValue(MotionEvent.AXIS_Z);
        jsData.rY = ev.getAxisValue(MotionEvent.AXIS_RZ);

        Log.d(TAG, String.format("joystick activity: lX: %f rX: %f lY: %f rY: %f",
                jsData.lX, jsData.lY, jsData.rX, jsData.rY));

        return true;
    }


    @Override
    public JoystickProcessor.JoystickData getJoystickData() {
        return jsData;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        if (ledProcessor != null) {
            LedProcessor.LedMode[] values = LedProcessor.LedMode.values();
            LedProcessor.LedMode newMode = LedProcessor.LedMode.values()[position];

            prefs.edit().putInt("ledMode", position).apply();

            ledProcessor.setLedMode(newMode);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
