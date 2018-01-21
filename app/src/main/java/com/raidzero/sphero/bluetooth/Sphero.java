package com.raidzero.sphero.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import java.util.Locale;

/**
 * Created by posborn on 1/17/18.
 */

public class Sphero implements BtLe.BtLeListener {
    private static final String TAG = "Sphero";
    private BtLe mBtLe;
    private String mName;
    private boolean mServicesReadyForUse;


    public Sphero(Context context, BluetoothDevice device) {
        mName = device.getName();
        mBtLe = new BtLe(context, device);
        mBtLe.setListener(this);
    }

    public String getName() {
        return mName;
    }

    // send battery level to listener
    public void queryBatteryLevel() {
        // no need to queue this
        mBtLe.queryServiceCharacteristic(Constants.UUID_SERVICE_BATTERY, Constants.UUID_CHARACTERISTIC_BATTERY);
    }

    public void turnOnBackLed() {
        byte[] data = new byte[] { (byte) 0x8d, 0x0a, 0x13, 0x04, 0x15, (byte) 0xc9, (byte) 0xd8 };
        byte[] data2 = new byte[] { (byte) 0x8d, 0x0a, 0x13, 0x03, 0x16, (byte) 0xc9, (byte) 0xd8 };
        byte[] data3 = new byte[] { (byte) 0x8d, 0x0a, 0x1a, 0x0e, 0x17, 0x00, 0x01, (byte) 0xff, (byte) 0xb6, (byte) 0xd8 };
        byte[] data4 = new byte[] { (byte) 0x8d, 0x0a, 0x1a, 0x0e, 0x18, 0x00, 0x0e, 0x00, 0x00, 0x00, (byte) 0xa7, (byte) 0xd8 };
    }


    private boolean useTheForce() {
        Log.d(TAG, "useTheForce()");
        SpheroCommand command = SpheroCommand.createCommand(
                Constants.UUID_SERVICE_INITIALIZE,
                Constants.UUID_CHARACTERISTIC_USETHEFORCE,
                Constants.USE_THE_FORCE_BYTES);

        return mBtLe.writeToServiceCharacteristic(command);
    }

    private boolean subscribe() {
        Log.d(TAG, "subscribe()");
        return mBtLe.subscribeForNotifications(Constants.UUID_SERVICE_COMMAND, Constants.UUID_CHARACTERISTIC_HANDLE_1C);
    }

    public void disconnect() {
        mBtLe.disconnect();
    }


    /**
     * BT LE CALLBACK METHODS FOLLOW
     */

    @Override
    public void onServicesDiscoverySuccess() {
        mServicesReadyForUse = true;

        if (useTheForce()) {
            subscribe();

            turnOnBackLed();
        }

        queryBatteryLevel();
    }

    @Override
    public void onServicesDiscoveryFail() {
        mServicesReadyForUse = false;
    }

    @Override
    public void onCharacteristicRead(BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().toString().equals(Constants.UUID_CHARACTERISTIC_BATTERY.toString())) {
            int batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            Log.d(TAG, (String.format(Locale.US, "Battery level: %d%%", batteryLevel)));
        }

        //disconnect();
    }
}
