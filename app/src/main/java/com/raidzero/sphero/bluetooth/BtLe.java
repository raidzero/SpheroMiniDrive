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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static com.raidzero.sphero.global.ByteUtils.bytesToString;
import static com.raidzero.sphero.global.ByteUtils.hexStringToBytes;

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
    private int mWaitLength;

    ExecutorService commandExecutor = Executors.newSingleThreadExecutor();
    BlockingQueue<BtLeCommand> commandQueue = new LinkedBlockingQueue<BtLeCommand>();
    CommandProcessor commandProcessor = new CommandProcessor();

    private BtLeListener mListener;
    public interface BtLeListener {
        void onServicesDiscoverySuccess();
        void onServicesDiscoveryFail();
        void onCharacteristicChanged(BluetoothGattCharacteristic characteristic);
    }

    BtLe(Context context, BluetoothDevice device) {
        mContext = context;
        mDevice = device;

        Log.d(TAG, "connecting gatt...");
        mGatt = mDevice.connectGatt(mContext, false, mGattCallback);
    }

    public void addCommandToQueue(BtLeCommand command) {
        commandQueue.add(command);
    }

    void setListener(BtLeListener listener) {
        mListener = listener;
    }

    boolean queryServiceCharacteristic(UUID service, UUID characteristic) {
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
                Log.d(TAG, "Service not found: " + service);
                return false;
            }

            BluetoothGattCharacteristic c = s.getCharacteristic(characteristic);
            if (c == null) {
                Log.d(TAG, "Characteristic not found: " + characteristic);
                return false;
            }

            c.setValue(data);

            Log.d(TAG, "writeToServiceCharacteristic(): " + service + ": " + characteristic);
            Log.d(TAG, "writeData: " + bytesToString(data));

            mTxBusy = true;
            boolean success = mGatt.writeCharacteristic(c);

            Log.d(TAG, "success: " + success);
            return success;
        }

        return false;
    }

    boolean writeToServiceCharacteristic(BtLeCommand command) {
        mWaitLength = command.duration;
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
            Log.d(TAG, "onCharacteristicWrite(): " + bytesToString(characteristic.getValue()));
            if (mWaitLength > 0) {
                try {
                    Thread.sleep(mWaitLength); // wait however long before allowing the next command to be sent
                } catch (Exception e) {
                    // ignored
                }
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
                    if (commandQueue.size() > 0 && !isDeviceBusy()) {
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
                    }
                }
            } catch (InterruptedException e) {
            }
        }
    }
}
