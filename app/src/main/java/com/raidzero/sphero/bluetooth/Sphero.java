package com.raidzero.sphero.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.UUID;
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
    BlockingQueue<BtLeCommand> commandQueue = new LinkedBlockingQueue<BtLeCommand>();
    CommandProcessor commandProcessor = new CommandProcessor();

    public Sphero(Context context, BluetoothDevice device) {
        mName = device.getName();
        mBtLe = new BtLe(context, device);
        mBtLe.setListener(this);

        if (!commandExecutor.isShutdown()) {
            commandExecutor.execute(commandProcessor);
        }
    }


    // converts colon-separated string of hex digits (like wireshark gives) to byte array
    public static byte[] hexStringToBytes(String hexStr) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String[] hex = hexStr.split(":");

        for (String s : hex) {
            byte b = (byte) ((Character.digit(s.charAt(0), 16) << 4)
                    + Character.digit(s.charAt(1), 16));
            os.write(b);
        }

        return os.toByteArray();
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
        commandQueue.add(BtLeCommand.createWriteCommand(
                Constants.UUID_SERVICE_COMMAND, Constants.UUID_CHARACTERISTIC_HANDLE_1C,
                new byte[] { (byte) 0x8d, 0x0a, 0x13, 0x04, 0x15, (byte) 0xc9, (byte) 0xd8 }));
        commandQueue.add(BtLeCommand.createWriteCommand(
                Constants.UUID_SERVICE_COMMAND, Constants.UUID_CHARACTERISTIC_HANDLE_1C,
                new byte[] { (byte) 0x8d, 0x0a, 0x13, 0x03, 0x16, (byte) 0xc9, (byte) 0xd8 }));
        commandQueue.add(BtLeCommand.createWriteCommand(
                Constants.UUID_SERVICE_COMMAND, Constants.UUID_CHARACTERISTIC_HANDLE_1C,
                new byte[] { (byte) 0x8d, 0x0a, 0x1a, 0x0e, 0x17, 0x00, 0x01, (byte) 0xff, (byte) 0xb6, (byte) 0xd8 }));
        commandQueue.add(BtLeCommand.createWriteCommand(
                Constants.UUID_SERVICE_COMMAND, Constants.UUID_CHARACTERISTIC_HANDLE_1C,
                new byte[] { (byte) 0x8d, 0x0a, 0x1a, 0x0e, 0x18, 0x00, 0x0e, 0x00, 0x00, 0x00, (byte) 0xa7, (byte) 0xd8 }));
    }


    private void useTheForce() {
        Log.d(TAG, "useTheForce()");
        BtLeCommand command = BtLeCommand.createWriteCommand(
                Constants.UUID_SERVICE_INITIALIZE,
                Constants.UUID_CHARACTERISTIC_USETHEFORCE,
                hexStringToBytes(Constants.STR_USE_THE_FORCE_BYTES));

        commandQueue.add(command);
    }

    private void writeCommonOne() {
        commandQueue.add(BtLeCommand.createWriteCommand1c("8d:0a:13:0d:00:d5:d8"));
    }

    private void mainLedRgb(byte red, byte green, byte blue) {
        Log.d(TAG, "mainLedRgb()");
        commandQueue.add(BtLeCommand.createWriteCommand1c(SpheroCommand.createRgbString(red, green, blue)));
    }

    /*
    private void mainLedOn() {
        Log.d(TAG, "mainLedOn()");
        commandQueue.add(BtLeCommand.createWriteCommand(
                Constants.UUID_SERVICE_COMMAND, Constants.UUID_CHARACTERISTIC_HANDLE_1C,
                SpheroCommand.createTurnOnLedCommand()));
    }
    */
    private void subscribe() {
        Log.d(TAG, "subscribe()");
        BtLeCommand command = BtLeCommand.createSubscribeCommand(Constants.UUID_SERVICE_INITIALIZE, UUID.fromString("00020002-574f-4f20-5370-6865726f2121"));

        commandQueue.add(command);
    }

    private void sendRead() {
        Log.d(TAG, "sendRead()");
        commandQueue.add(BtLeCommand.createReadCommand(Constants.UUID_SERVICE_INITIALIZE, UUID.fromString("00020004-574f-4f20-5370-6865726f2121")));
    }

    public void disconnect() {
        // tell sphero to turn off
        /*
        Value: 8d 0a 16 07 5c 00 01 2a 00 51 d8 // last drive command?
        Value: 8d 0a 13 01 5d 84 d8
        Value: 8d 0a 13 01 5e 83 d8
        */

        commandQueue.add(BtLeCommand.createWriteCommand1c(SpheroCommand.createDisconnectString()));
    }

    class CommandProcessor implements Runnable {
        private boolean isRunning;
        private boolean lastCommandSentSuccessfully = true;
        @Override
        public void run() {
            isRunning = true;
            Log.d(TAG, "starting command processor.");
            try {
                while (!commandExecutor.isShutdown()) {
                    // grab command off queue
                    if (commandQueue.size() > 0 && !mBtLe.isDeviceBusy()) {
                        BtLeCommand command = commandQueue.take();
                        switch (command.commandType) {
                            case WRITE_CHARACTERISTIC:
                                Log.d(TAG, "CommandProcessor sending write request "
                                        + BtLe.bytesToString(command.data)
                                        + " to " + command.service + ": "
                                        + command.characteristic);

                                mBtLe.writeToServiceCharacteristic(command);
                                break;
                            case READ_CHARACTERISTIC:
                                Log.d(TAG, "CommandProcessor sending read request"
                                        + " to " + command.service + ": "
                                        + command.characteristic);
                                mBtLe.queryServiceCharacteristic(command);
                                break;
                            case SUBSCRIBE_CHARACTERISTIC_NOTIFICATIONS:
                                Log.d(TAG, "CommandProcessor sending subscribe request"
                                        + " to " + command.service + ": "
                                        + command.characteristic);
                                mBtLe.subscribeForNotifications(command);
                                break;
                        }
                    } else {
                        //Log.d(TAG, "Commands that did not execute: " + commandQueue.size() + ", shutting down processor");
                        //Log.d(TAG, "out of commands...");
                        //commandExecutor.shutdown();

                    }
                }
            } catch (InterruptedException e) {
                isRunning = false;
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

        sendRead();

        // wake up. turn on led
        writeCommonOne();

        mainLedRgb((byte) 0x00, (byte) 0xff, (byte) 0x00);

        //mainLedOn();

        //mainLedRgb((byte) 0xff, (byte) 0xff, (byte) 0x00);
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
