package com.raidzero.sphero.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Random;
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


    private void useTheForce() {
        Log.d(TAG, "useTheForce()");
        BtLeCommand command = BtLeCommand.createWriteCommand(
                Constants.UUID_SERVICE_INITIALIZE,
                Constants.UUID_CHARACTERISTIC_USETHEFORCE,
                hexStringToBytes(Constants.STR_USE_THE_FORCE_BYTES));
        command.duration = 50;

        commandQueue.add(command);
    }

    private void writeCommonOnes() {
        // magic numbers to initialize the sphero
        sendCommand("8d:0a:13:0d:00:d5:d8");
        sendCommand("8d:0a:13:0d:01:d4:d8");
        sendCommand("8d:0a:11:06:04:da:d8");
        sendCommand("8d:0a:13:10:05:cd:d8");
        sendCommand("8d:0a:13:04:06:ab:50:d8");
        sendCommand("8d:0a:13:1e:07:bd:d8");
        sendCommand("8d:0a:11:00:08:dc:d8");
        sendCommand("8d:0a:13:1e:09:bb:d8");
        sendCommand("8d:0a:13:1e:0a:ba:d8");
        sendCommand("8d:0a:11:06:0b:d3:d8");
        sendCommand("8d:0a:1f:27:0c:a3:d8");
        sendCommand("8d:0a:11:12:0d:c5:d8");
        sendCommand("8d:0a:11:28:0e:ae:d8");
        sendCommand("8d:0a:13:10:0f:c3:d8");
        sendCommand("8d:0a:13:04:10:ce:d8");
        sendCommand("8d:0a:13:10:11:c1:d8");
        sendCommand("8d:0a:13:04:12:cc:d8");
        sendCommand("8d:0a:13:10:13:bf:d8");
        sendCommand("8d:0a:13:04:14:ca:d8");
        sendCommand("8d:0a:13:10:15:bd:d8");
        sendCommand("8d:0a:13:04:16:c8:d8");
    }

    private void mainLedRgb(byte red, byte green, byte blue) {
        Log.d(TAG, "mainLedRgb()");
        commandQueue.add(BtLeCommand.createWriteCommand1c(SpheroCommand.createRgbString(red, green, blue)));
    }

    private void mainLedRgb(byte red, byte green, byte blue, int duration) {
        Log.d(TAG, "mainLedRgb()");
        commandQueue.add(BtLeCommand.createWriteCommand1c(SpheroCommand.createRgbString(red, green, blue), duration));
    }

    private void mainLedRgb(int color) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        mainLedRgb((byte) red, (byte) green, (byte) blue);
    }

    private void mainLedRgb(int color, int duration) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        mainLedRgb((byte) red, (byte) green, (byte) blue, duration);
    }


    private void rearLed(byte brightness, int duration) {
        Log.d(TAG, "rearLed()");
        commandQueue.add(BtLeCommand.createWriteCommand1c(SpheroCommand.createRearLedCommand(brightness), duration));
    }

    private void rearLed(byte brightness) {
        Log.d(TAG, "rearLed()");
        commandQueue.add(BtLeCommand.createWriteCommand1c(SpheroCommand.createRearLedCommand(brightness)));
    }

    // left & right can be -4095 to 4095
    private void rawMotor(int left, int right, int duration) {
        Log.d(TAG, "rawMotor()");
        commandQueue.add(BtLeCommand.createWriteCommand1c(SpheroCommand.createRawMotorCommand(left, right), duration));
    }

    private void rotate(int power, int duration) {
        Log.d(TAG, "rotate()");
        commandQueue.add(BtLeCommand.createWriteCommand1c(SpheroCommand.createRotateCommand(power), duration));
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
        // stop motors and wait quarter second
        rawMotor(0,0, 250);
        // turn off main led and wait half a sec
        mainLedRgb(Color.parseColor("#ff000000"), 500);
        // turn off rear led
        rearLed((byte) 0x0);

        // send disconnection commands. not sure what they should be
        List<String> disconnectStrings = SpheroCommand.createDisconnectStrings();
        for (String str : disconnectStrings) {
            commandQueue.add(BtLeCommand.createWriteCommand1c(str));
        }
    }

    private void sendCommand(String cmd) {
        // tshark -r 20180202-mini-connect-quit.log -2 -O btatt -R "btatt.opcode == 0x12" | grep Value | grep -o "8d0a1a0e.*$" | sed -e 's/\(..\)/\1:/g' | sed 's/:$//' | sed 's/^/sendCommand("/g' | sed 's/$/")/g'
        commandQueue.add(BtLeCommand.createWriteCommand1c(cmd));
    }

    private void sendCommand(String cmd, int duration) {
        commandQueue.add(BtLeCommand.createWriteCommand1c(cmd));
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

        // wake up
        writeCommonOnes();

        // turn off motor just in case
        rawMotor(0, 0, 50);

        // turn on rear led
        rearLed((byte) 0xff);

        // turn on main led
        mainLedRgb(Color.parseColor("#ffff0000"));
        /*
        // 100 random color light show
        for (int i = 0; i < 100; i++) {
            Random r = new Random();
            int red = r.nextInt(256);
            int green = r.nextInt(256);
            int blue = r.nextInt(256);
            String color = String.format("#ff%02x%02x%02x", red, green, blue);
            mainLedRgb(Color.parseColor(color));
        }

        // now breathe green, five times, keeping the rear led on while "inhaling"
        for (int i = 0; i < 5; i++) {
            for (int g = 0; g < 256; g += 20) {
                mainLedRgb(Color.parseColor(String.format("#ff00%02x00", g)));
            }
            rearLed((byte) 0x00);
            for (int g = 255; g > 0; g -= 20) {
                mainLedRgb(Color.parseColor(String.format("#ff00%02x00", g)));
            }
            rearLed((byte) 0xff);
        }

        // turn off rear led
        rearLed((byte) 0x00);

        // main led back to blue
        mainLedRgb(Color.parseColor("#ff0054ff"));
        */

        // motor stuff: spin left for 2 secs, stop for 1 sec, spin right for 2 secs, stop for 1s

        rawMotor(50, 50, 2000);
        rawMotor(0, 0, 1000);
        rawMotor(-50, -50, 2000);
        rawMotor(0, 0, 1000);


        // rotate for 1 second then rotate the other way for a second
        //rotate(-4095, 1000);
        //rotate(4095, 1000);
        //disconnect();
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
