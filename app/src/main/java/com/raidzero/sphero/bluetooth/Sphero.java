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
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x13, (byte) 0x0d, (byte) 0x00, (byte) 0xd5, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x13, (byte) 0x0d, (byte) 0x01, (byte) 0xd4, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x11, (byte) 0x06, (byte) 0x04, (byte) 0xda, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x13, (byte) 0x10, (byte) 0x05, (byte) 0xcd, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x13, (byte) 0x04, (byte) 0x06, (byte) 0xab, (byte) 0x50, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x13, (byte) 0x1e, (byte) 0x07, (byte) 0xbd, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x11, (byte) 0x00, (byte) 0x08, (byte) 0xdc, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x13, (byte) 0x1e, (byte) 0x09, (byte) 0xbb, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x13, (byte) 0x1e, (byte) 0x0a, (byte) 0xba, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x11, (byte) 0x06, (byte) 0x0b, (byte) 0xd3, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x1f, (byte) 0x27, (byte) 0x0c, (byte) 0xa3, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x11, (byte) 0x12, (byte) 0x0d, (byte) 0xc5, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x11, (byte) 0x28, (byte) 0x0e, (byte) 0xae, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x13, (byte) 0x10, (byte) 0x0f, (byte) 0xc3, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x13, (byte) 0x04, (byte) 0x10, (byte) 0xce, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x13, (byte) 0x10, (byte) 0x11, (byte) 0xc1, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x13, (byte) 0x04, (byte) 0x12, (byte) 0xcc, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x13, (byte) 0x10, (byte) 0x13, (byte) 0xbf, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x13, (byte) 0x04, (byte) 0x14, (byte) 0xca, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x13, (byte) 0x10, (byte) 0x15, (byte) 0xbd, (byte) 0xd8});
        sendCommand(new byte[] {(byte) 0x8d, (byte) 0x0a, (byte) 0x13, (byte) 0x04, (byte) 0x16, (byte) 0xc8, (byte) 0xd8});
    }

    private void mainLedRgb(byte red, byte green, byte blue) {
        Log.d(TAG, "mainLedRgb()");
        mBtLe.addCommandToQueue(BtLeCommand.createWriteCommand1c(SpheroCommand.createRgbCommand(red, green, blue), 0));
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
        mBtLe.addCommandToQueue(BtLeCommand.createWriteCommand1c(SpheroCommand.createRearLedCommand(on), 0));
    }

    // left & right can be -4095 to 4095
    public void rawMotor(int left, int right, int duration) {
        Log.d(TAG, String.format("rawMotor(%d, %d, %d)", left, right, duration));
        mBtLe.addCommandToQueue(BtLeCommand.createWriteCommand1c(SpheroCommand.createRawMotorCommand(left, right), duration));
    }

    public void roll(int speed, int heading, int aim) {
        Log.d(TAG, "roll()");
        mBtLe.addCommandToQueue(BtLeCommand.createWriteCommand1c(SpheroCommand.createRollCommand(speed, heading, aim), 0));
    }

    public void disconnect() {
        // stop motors
        rawMotor(0,0, 0);

        clearCommands();

        List<byte[]> disconnectCommands = SpheroCommand.createDisconnectCommands();
        for (byte[] cmd : disconnectCommands) {
            mBtLe.addCommandToQueue(BtLeCommand.createWriteCommand1c(cmd, 0));
        }

        mBtLe.prepareToShutDown();
    }

    private void sendCommand(String cmd) {
        // tshark -r 20180202-mini-connect-quit.log -2 -O btatt -R "btatt.opcode == 0x12" | grep Value | grep -o "8d0a1a0e.*$" | sed -e 's/\(..\)/\1:/g' | sed 's/:$//' | sed 's/^/sendCommand("/g' | sed 's/$/")/g'
        mBtLe.addCommandToQueue(BtLeCommand.createWriteCommand1c(cmd, 0));
    }

    private void sendCommand(byte[] cmd) {
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
