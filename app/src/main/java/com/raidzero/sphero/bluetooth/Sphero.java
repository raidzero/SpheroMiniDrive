package com.raidzero.sphero.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by posborn on 1/17/18.
 */

public class Sphero implements BtLe.BtLeListener {
    private static final String TAG = "Sphero";
    private BtLe mBtLe;
    private String mName;
    private boolean mServicesReadyForUse;

    ExecutorService commandExecutor = Executors.newSingleThreadExecutor();
    BlockingQueue<SpheroCommand> commandQueue = new LinkedBlockingQueue<SpheroCommand>();
    CommandProcessor commandProcessor = new CommandProcessor();

    public Sphero(Context context, BluetoothDevice device) {
        mName = device.getName();
        mBtLe = new BtLe(context, device);
        mBtLe.setListener(this);

        if (!commandExecutor.isShutdown()) {
            commandExecutor.execute(commandProcessor);
        }
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
        commandQueue.add(SpheroCommand.createWriteCommand(
                Constants.UUID_SERVICE_COMMAND, Constants.UUID_CHARACTERISTIC_HANDLE_1C,
                new byte[] { (byte) 0x8d, 0x0a, 0x13, 0x04, 0x15, (byte) 0xc9, (byte) 0xd8 }));
        commandQueue.add(SpheroCommand.createWriteCommand(
                Constants.UUID_SERVICE_COMMAND, Constants.UUID_CHARACTERISTIC_HANDLE_1C,
                new byte[] { (byte) 0x8d, 0x0a, 0x13, 0x03, 0x16, (byte) 0xc9, (byte) 0xd8 }));
        commandQueue.add(SpheroCommand.createWriteCommand(
                Constants.UUID_SERVICE_COMMAND, Constants.UUID_CHARACTERISTIC_HANDLE_1C,
                new byte[] { (byte) 0x8d, 0x0a, 0x1a, 0x0e, 0x17, 0x00, 0x01, (byte) 0xff, (byte) 0xb6, (byte) 0xd8 }));
        commandQueue.add(SpheroCommand.createWriteCommand(
                Constants.UUID_SERVICE_COMMAND, Constants.UUID_CHARACTERISTIC_HANDLE_1C,
                new byte[] { (byte) 0x8d, 0x0a, 0x1a, 0x0e, 0x18, 0x00, 0x0e, 0x00, 0x00, 0x00, (byte) 0xa7, (byte) 0xd8 }));
    }


    private void useTheForce() {
        Log.d(TAG, "useTheForce()");
        SpheroCommand command = SpheroCommand.createWriteCommand(
                Constants.UUID_SERVICE_INITIALIZE,
                Constants.UUID_CHARACTERISTIC_USETHEFORCE,
                Constants.USE_THE_FORCE_BYTES);

        commandQueue.add(command);
    }

    private void subscribe() {
        Log.d(TAG, "subscribe()");
        SpheroCommand command = SpheroCommand.createSubscribeCommand(Constants.UUID_SERVICE_COMMAND, Constants.UUID_CHARACTERISTIC_HANDLE_1C);

        commandQueue.add(command);
    }

    public void disconnect() {
        mBtLe.disconnect();
    }

    class CommandProcessor implements Runnable {
        private boolean isRunning;
        private boolean lastCommandSentSuccessfully = true;
        @Override
        public void run() {
            isRunning = true;

            try {
                while (!commandExecutor.isShutdown()) {
                    // grab command off queue
                    if (commandQueue.size() > 0 && lastCommandSentSuccessfully && !mBtLe.isDeviceBusy()) {
                        SpheroCommand command = commandQueue.take();
                        Log.d(TAG, "CommandProcessor sending command to service: " + command.service.toString());
                        switch (command.commandType) {
                            case WRITE_CHARACTERISTIC:
                                lastCommandSentSuccessfully = mBtLe.writeToServiceCharacteristic(command);
                                if (!lastCommandSentSuccessfully) {
                                    Log.d(TAG, "command did not get sent properly: " + command.data.length + " bytes");
                                } else {
                                    Log.d(TAG, "command sent properly: " + command.data.length + " bytes");
                                }
                                break;
                            case READ_CHARACTERISTIC:
                                lastCommandSentSuccessfully = mBtLe.queryServiceCharacteristic(command);
                                break;
                            case SUBSCRIBE_CHARACTERISTIC_NOTIFICATIONS:
                                lastCommandSentSuccessfully = mBtLe.subscribeForNotifications(command);
                                if (!lastCommandSentSuccessfully) {
                                    Log.d(TAG, "Subscription command fail");
                                } else {
                                    Log.d(TAG, "Subscription command success");
                                }
                                break;
                        }
                    } else {
                        //Log.d(TAG, "Commands that did not execute: " + commandQueue.size() + ", shutting down processor");
                        //commandExecutor.shutdown();
                    }
                }
            } catch (InterruptedException e) {

            }
        }
    }

    /**
     * BT LE CALLBACK METHODS FOLLOW
     */

    @Override
    public void onServicesDiscoverySuccess() {
        mServicesReadyForUse = true;

        useTheForce();

        subscribe();

        turnOnBackLed();
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
