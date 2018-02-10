package com.raidzero.sphero.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.os.Handler;

/**
 * Created by raidzero on 2/10/18.
 */

public class ScanActivity extends Activity {
    private BluetoothAdapter mBtAdapter;
    private boolean mScanning;
    private Handler mHandler;

    // stop scanning after ten seconds
    private static final long SCAN_PERIOD = 10000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }
}
