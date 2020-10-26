package com.raidzero.sphero.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.raidzero.sphero.global.Constants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by raidzero on 2/10/18.
 */

public class ScanActivity extends AppCompatActivity implements BluetoothAdapter.LeScanCallback {
    private static final String TAG = "ScanActivity";

    private BluetoothAdapter mBtAdapter;
    private Handler mHandler;

    // stop scanning after five seconds
    private static final long SCAN_PERIOD = 5000;

    private List<String> mDiscoveredSpheroAddresses = new ArrayList<String>();

    private boolean mScanning;
    private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        prefs = getSharedPreferences("SpheroMiniDrive", Context.MODE_PRIVATE);

        if (prefs.getString("spheroAddress", null) == null) {
            checkPermission();
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        startScanningForSpheros();
    }

    private void checkPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        requestPermissions(permissions, 1000);
    }

    private void startScanningForSpheros() {
        // stop scanning after five seconds
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (mScanning) {
//                    mBtAdapter.stopLeScan(ScanActivity.this);
//
//                    setResult(RESULT_CANCELED);
//                    finish();
//                }
//            }
//        }, SCAN_PERIOD);

        mScanning = true;
        mBtAdapter.startLeScan(this);
    }

    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
        if (parseUUIDs(scanRecord).contains(Constants.UUID_SERVICE_COMMAND)) {
            if (!mDiscoveredSpheroAddresses.contains(bluetoothDevice.getAddress())) {
                mScanning = false;
                mBtAdapter.stopLeScan(this);
                Log.d(TAG, String.format("Found Sphero: %s, %d", bluetoothDevice.getAddress(), rssi));
                mDiscoveredSpheroAddresses.add(bluetoothDevice.getAddress());

                getSharedPreferences("SpheroMiniDrive", Context.MODE_PRIVATE).edit()
                        .putString("spheroAddress", bluetoothDevice.getAddress()).apply();

                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        }
    }

    // taken from https://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation
    private List<UUID> parseUUIDs(final byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        int offset = 0;
        while (offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset++];
            if (len == 0)
                break;

            int type = advertisedData[offset++];
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (len > 1) {
                        int uuid16 = advertisedData[offset++];
                        uuid16 += (advertisedData[offset++] << 8);
                        len -= 2;
                        uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                    }
                    break;
                case 0x06:// Partial list of 128-bit UUIDs
                case 0x07:// Complete list of 128-bit UUIDs
                    // Loop through the advertised 128-bit UUID's.
                    while (len >= 16) {
                        try {
                            // Wrap the advertised bits and order them.
                            ByteBuffer buffer = ByteBuffer.wrap(advertisedData, offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                            long mostSignificantBit = buffer.getLong();
                            long leastSignificantBit = buffer.getLong();
                            uuids.add(new UUID(leastSignificantBit,
                                    mostSignificantBit));
                        } catch (IndexOutOfBoundsException e) {
                            // Defensive programming.
                            //Log.e(LOG_TAG, e.toString());
                            continue;
                        } finally {
                            // Move the offset to read the next uuid.
                            offset += 15;
                            len -= 16;
                        }
                    }
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }

        return uuids;
    }
}
