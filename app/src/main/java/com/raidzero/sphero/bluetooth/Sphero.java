package com.raidzero.sphero.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.raidzero.sphero.global.Constants;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import static com.raidzero.sphero.global.ByteUtils.hexStringToBytes;

/**
 * Created by posborn on 1/17/18.
 */

public class Sphero implements BtLe.BtLeListener {
    private static final String TAG = "Sphero";
    private BtLe mBtLe;
    private String mName;
    private boolean mServicesReadyForUse;
    private SpheroListener mListener;

    private int batteryLevel = -1;

    public interface SpheroListener {
        void onSpheroConnected();
        void onSpheroDisconnected();
        void onBatteryLevelChange(int newLevel);
    }

    public Sphero(Context context, BluetoothDevice device, SpheroListener listener) {
        mName = device.getName();
        mListener = listener;
        mBtLe = new BtLe(context, device);
        mBtLe.setListener(this);
    }

    public String getName() {
        return mName;
    }

    private void useTheForce() {
        Log.d(TAG, "useTheForce()");
        BtLeCommand command = BtLeCommand.createWriteCommand(
                Constants.UUID_SERVICE_INITIALIZE,
                Constants.UUID_CHARACTERISTIC_USETHEFORCE,
                hexStringToBytes(Constants.STR_USE_THE_FORCE_BYTES));
        mBtLe.addCommandToQueue(command);
    }

    private void subscribe() {
        Log.d(TAG, "subscribe()");
        BtLeCommand command = BtLeCommand.createSubscribeCommand(Constants.UUID_SERVICE_INITIALIZE, UUID.fromString("00020002-574f-4f20-5370-6865726f2121"));

        mBtLe.addCommandToQueue(command);
    }

    private void subscribeForBatteryNotifications() {
        BtLeCommand command = BtLeCommand.createSubscribeCommand(Constants.UUID_SERVICE_BATTERY, Constants.UUID_CHARACTERISTIC_BATTERY);
        mBtLe.addCommandToQueue(command);
    }

    private void sendRead() {
        Log.d(TAG, "sendRead()");
        mBtLe.addCommandToQueue(BtLeCommand.createReadCommand(Constants.UUID_SERVICE_INITIALIZE, UUID.fromString("00020004-574f-4f20-5370-6865726f2121")));
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
        mBtLe.addCommandToQueue(BtLeCommand.createWriteCommand1c(SpheroCommand.createRgbCommand(red, green, blue)));
    }

    private void mainLedRgb(byte red, byte green, byte blue, int duration) {
        Log.d(TAG, "mainLedRgb()");
        mBtLe.addCommandToQueue(BtLeCommand.createWriteCommand1c(SpheroCommand.createRgbCommand(red, green, blue), duration));
    }

    public void mainLedRgb(int color) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        mainLedRgb((byte) red, (byte) green, (byte) blue);
    }

    public void mainLedRgb(int color, int duration) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        mainLedRgb((byte) red, (byte) green, (byte) blue, duration);
    }


    public void rearLed(boolean on, int duration) {
        Log.d(TAG, "rearLed()");
        mBtLe.addCommandToQueue(BtLeCommand.createWriteCommand1c(SpheroCommand.createRearLedCommand(on), duration));
    }

    public void rearLed(boolean on) {
        Log.d(TAG, "rearLed()");
        mBtLe.addCommandToQueue(BtLeCommand.createWriteCommand1c(SpheroCommand.createRearLedCommand(on)));
    }

    // left & right can be -4095 to 4095
    public void rawMotor(int left, int right, int duration) {
        Log.d(TAG, String.format("rawMotor(%d, %d, %d)", left, right, duration));
        mBtLe.addCommandToQueue(BtLeCommand.createWriteCommand1c(SpheroCommand.createRawMotorCommand(left, right), duration));
    }

    public void rotate(int power, int duration) {
        Log.d(TAG, "rotate()");
        mBtLe.addCommandToQueue(BtLeCommand.createWriteCommand1c(SpheroCommand.createRotateCommand(power), duration));
    }

    public void roll(int speed, int heading, int aim) {
        Log.d(TAG, "roll()");
        mBtLe.addCommandToQueue(BtLeCommand.createWriteCommand1c(SpheroCommand.createRollCommand(speed, heading, aim), 0));
    }

    public void disconnect() {
        // stop motors
        rawMotor(0,0, 0);

        mBtLe.clearCommandQueue();
        mBtLe.prepareToShutDown();

        // send disconnection commands. not sure what they should be
        List<String> disconnectStrings = SpheroCommand.createDisconnectStrings();
        for (String str : disconnectStrings) {
            mBtLe.addCommandToQueue(BtLeCommand.createWriteCommand1c(str, 0));
        }
    }

    private void sendCommand(String cmd) {
        // tshark -r 20180202-mini-connect-quit.log -2 -O btatt -R "btatt.opcode == 0x12" | grep Value | grep -o "8d0a1a0e.*$" | sed -e 's/\(..\)/\1:/g' | sed 's/:$//' | sed 's/^/sendCommand("/g' | sed 's/$/")/g'
        mBtLe.addCommandToQueue(BtLeCommand.createWriteCommand1c(cmd, 0));
    }

    private void sendCommand(String cmd, int duration) {
        mBtLe.addCommandToQueue(BtLeCommand.createWriteCommand1c(cmd));
    }

    public void clearCommands() {
        mBtLe.clearCommandQueue();
    }

    /**
     * BT LE CALLBACK METHODS FOLLOW
     */

    @Override
    public void onServicesDiscoverySuccess() {
        mServicesReadyForUse = true;

        useTheForce();
        subscribe();
        subscribeForBatteryNotifications();
        sendRead();

        // wake up
        writeCommonOnes();

        mListener.onSpheroConnected();
    }

    @Override
    public void onServicesDiscoveryFail() {
        mServicesReadyForUse = false;
    }

    @Override
    public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().toString().equals(Constants.UUID_CHARACTERISTIC_BATTERY.toString())) {
            int batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            Log.d(TAG, (String.format(Locale.US, "Battery level: %d%%", batteryLevel)));
            this.batteryLevel = batteryLevel;
            mListener.onBatteryLevelChange(batteryLevel);
        }
    }

    @Override
    public void onCommandProcessorFinish() {
        mListener.onSpheroDisconnected();
    }
}
