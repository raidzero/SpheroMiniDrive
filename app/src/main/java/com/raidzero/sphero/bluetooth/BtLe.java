package com.raidzero.sphero.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import com.raidzero.sphero.global.Constants;

import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import static com.raidzero.sphero.global.ByteUtils.bytesToString;

/**
 * Created by raidzero on 1/21/18.
 */

public class BtLe {
    private static final String TAG = "BtLe";

    private Context mContext;
    private BluetoothDevice mDevice;
    private BluetoothGatt mGatt;
    private BluetoothGattCallback mGattCallback = new LeGattCallback();

    private boolean mServicesDiscovered;
    private boolean mConnected;
    private boolean mTxBusy = false;
    private boolean shutDownWhenDone = false;

    private ExecutorService commandExecutor = Executors.newSingleThreadExecutor();
    private BlockingDeque<BtLeCommand> commandQueue = new LinkedBlockingDeque<BtLeCommand>();
    private CommandProcessor commandProcessor = new CommandProcessor();

    private BtLeListener mListener;

    public interface BtLeListener {
        void onServicesDiscoverySuccess();
        void onServicesDiscoveryFail();
        void onCharacteristicChanged(BluetoothGattCharacteristic characteristic);
        void onCommandProcessorFinish();
    }

    BtLe(Context context, BluetoothDevice device) {
        mContext = context;
        mDevice = device;

        Log.d(TAG, "connecting gatt...");
        mGatt = mDevice.connectGatt(mContext, false, mGattCallback);
    }

    public void addCommandToQueue(BtLeCommand command) {
        try {
            commandQueue.put(command);
        } catch (InterruptedException e) {
            Log.e(TAG, "commandQueue insertion interrupted");
        }
    }

    public void addCommandToQueueHead(BtLeCommand command) {
        try {
            commandQueue.putFirst(command);
        } catch (InterruptedException e) {
            Log.e(TAG, "commandQueue insertion interrupted");
        }
    }

    void setListener(BtLeListener listener) {
        mListener = listener;
    }

    boolean queryServiceCharacteristic(UUID service, UUID characteristic) {
        if (mServicesDiscovered) {
            BluetoothGattService s = mGatt.getService(service);
            if (s == null) {
                Log.e(TAG, "Service not found: " + service);
                return false;
            }

            BluetoothGattCharacteristic c = s.getCharacteristic(characteristic);
            if (c == null) {
                Log.e(TAG, "Characteristic not found: " + characteristic);
                return false;
            }

            mTxBusy = true;
            return mGatt.readCharacteristic(c);
        }

        return false;
    }

    boolean queryServiceCharacteristic(BtLeCommand command) {
        return queryServiceCharacteristic(command.service, command.characteristic);
    }

    boolean subscribeForNotifications(UUID service, UUID characteristic) {
        if (mServicesDiscovered) {
            BluetoothGattService s = mGatt.getService(service);
            if (s == null) {
                Log.d(TAG, "Service not found: " + service);
                return false;
            }

            BluetoothGattCharacteristic c = s.getCharacteristic(characteristic);
            if (c == null) {
                Log.d(TAG, "Characteristic not found: " + characteristic);
                return false;
            }

            BluetoothGattDescriptor d = c.getDescriptor(Constants.UUID_DESCRIPTOR_NOTIFY);
            if (d == null) {
                Log.d(TAG, "Descriptor not found for characteristic: " + characteristic);
                return false;
            }

            mTxBusy = true;
            if (mGatt.setCharacteristicNotification(c, true)) {
                d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                return mGatt.writeDescriptor(d);
            } else {
                return false;
            }
        }

        return false;
    }

    boolean subscribeForNotifications(BtLeCommand command) {
        return subscribeForNotifications(command.service, command.characteristic);
    }

    private boolean writeToServiceCharacteristic(UUID service, UUID characteristic, byte[] data) {
        if (mServicesDiscovered) {
            BluetoothGattService s = mGatt.getService(service);
            if (s == null) {
                Log.e(TAG, "Service not found: " + service);
                return false;
            }

            BluetoothGattCharacteristic c = s.getCharacteristic(characteristic);
            if (c == null) {
                Log.e(TAG, "Characteristic not found: " + characteristic);
                return false;
            }

            c.setValue(data);

            Log.d(TAG, "writeData: " + bytesToString(data));

            mTxBusy = true;
            boolean success = mGatt.writeCharacteristic(c);

            return success;
        }

        return false;
    }

    boolean writeToServiceCharacteristic(BtLeCommand command) {
        return writeToServiceCharacteristic(
                command.service,
                command.characteristic,
                command.data
        );
    }

    public void disconnect() {
        mGatt.disconnect();
        mGatt.close();
        mGatt = null;
    }

    boolean isDeviceBusy() {
        return mTxBusy;
    }

    // Gatt Callback
    private class LeGattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            mConnected = newState == BluetoothGatt.STATE_CONNECTED;
            Log.d(TAG, "gatt connection state change. connected: " + mConnected);

            if (mConnected) {
                // Discover services.
                try {
                    // wait 600 ms
                    Thread.sleep(600);
                } catch (Exception e) {

                }
                gatt.discoverServices();
            } else {

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            mServicesDiscovered = status == BluetoothGatt.GATT_SUCCESS;

            if (mServicesDiscovered) {
                mListener.onServicesDiscoverySuccess();

                if (!commandExecutor.isShutdown()) {
                    commandExecutor.execute(commandProcessor);
                }
            } else {
                mListener.onServicesDiscoveryFail();
            }

            mTxBusy = false;
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicRead(): " + bytesToString(characteristic.getValue()));
            mTxBusy = false;
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            try {
                Thread.sleep(5); // wait a bit before next command can be processed
            } catch (Exception e) {
                // ignored
            }
            mTxBusy = false;
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "onCharacteristicChanged(): " + bytesToString(characteristic.getValue()));
            mListener.onCharacteristicChanged(characteristic);
            mTxBusy = false;
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorRead(): " + bytesToString(descriptor.getValue()));
            mTxBusy = false;
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite()");
            mTxBusy = false;
        }
    }

    class CommandProcessor implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "starting command processor.");
            try {
                while (!commandExecutor.isShutdown()) {
                    // grab command off queue
                    if (!commandQueue.isEmpty() && !isDeviceBusy()) {
                        BtLeCommand command = commandQueue.take();
                        switch (command.commandType) {
                            case WRITE_CHARACTERISTIC:
                                Log.d(TAG, "CommandProcessor sending write request "
                                        + bytesToString(command.data)
                                        + " to " + command.service + ": "
                                        + command.characteristic
                                        + " wait time: " + command.duration);

                                writeToServiceCharacteristic(command);
                                break;
                            case READ_CHARACTERISTIC:
                                Log.d(TAG, "CommandProcessor sending read request"
                                        + " to " + command.service + ": "
                                        + command.characteristic);
                                queryServiceCharacteristic(command);
                                break;
                            case SUBSCRIBE_CHARACTERISTIC_NOTIFICATIONS:
                                Log.d(TAG, "CommandProcessor sending subscribe request"
                                        + " to " + command.service + ": "
                                        + command.characteristic);
                                subscribeForNotifications(command);
                                break;
                        }
                    } else {
                        if (commandQueue.isEmpty() && shutDownWhenDone) {
                            // give the BT commands time to get sent
                            try { Thread.sleep(200); } catch (Exception e) {}
                            stop();
                        }
                    }

                    // wait a bit before processing the next command in the queue
                    try {
                        Thread.sleep(5);
                    } catch (Exception e) {

                    }
                }
            } catch (InterruptedException e) {
            }
        }
    }

    public void clearCommandQueue() {
        commandQueue.clear();
    }

    public void stop() {
        commandExecutor.shutdownNow();
        mListener.onCommandProcessorFinish();
    }

    public void prepareToShutDown() {
        shutDownWhenDone = true;
    }
}
