package com.raidzero.sphero;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.raidzero.sphero.bluetooth.Constants;
import com.raidzero.sphero.bluetooth.Sphero;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends Activity {

    // UI elements
    private TextView messages;

    // BTLE state
    private BluetoothAdapter adapter;

    private Sphero sphero;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Grab references to UI elements.
        messages = (TextView) findViewById(R.id.messages);
        adapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sphero != null) {
            sphero.disconnect();
        }
        sphero = new Sphero(this, adapter.getRemoteDevice("E5:67:61:BA:3D:57"));
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
        sphero.disconnect();
    }
}
