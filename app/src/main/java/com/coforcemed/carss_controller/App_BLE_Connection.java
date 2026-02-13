package com.coforcemed.carss_controller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

public class App_BLE_Connection {
    protected Activity mActivity;
    public App_BLE_Connection(Activity activity, App_UI appUi, App_Command appCommand) {
        mActivity = activity;
        app_ui = appUi;
        app_ble_service_nus = new App_BLE_Service_NUS(mActivity, appUi, appCommand);
    }
    private final App_UI app_ui;
    private final App_BLE_Service_NUS app_ble_service_nus;

    private BluetoothGatt mbluetoothGatt;

    private final IntentFilter bondBtFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
    private final IntentFilter pairBtFilter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);

    public void onCreate() {
        mActivity.registerReceiver(mReceiver, bondBtFilter);
        mActivity.registerReceiver(mReceiver, pairBtFilter);
        app_ble_service_nus.onCreate();
    }

    public void unregisterReceiver() {
        mActivity.unregisterReceiver(mReceiver);
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void closeGatt() {
        if (mbluetoothGatt != null) {
            mbluetoothGatt.disconnect();
            mbluetoothGatt.close();
            mbluetoothGatt = null;
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void connectDevice(BluetoothDevice bluetoothDevice) {
        closeGatt();
        mbluetoothGatt = bluetoothDevice.connectGatt(mActivity, false, bluetoothGattCallback);
        mbluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        Log.i("BLE", "Connect to Device");
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            assert action != null;
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                //BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                if (state == BluetoothDevice.BOND_NONE) {
                    Log.d("BLE", "<ACTION_BOND_STATE_CHANGED>, <BOND_NONE>");
                }
                else if (state == BluetoothDevice.BOND_BONDING) {
                    Log.d("BLE", "<ACTION_BOND_STATE_CHANGED>, <BOND_BONDING>");
                }
                else if (state == BluetoothDevice.BOND_BONDED) {
                    Log.d("BLE", "<ACTION_BOND_STATE_CHANGED>, <BOND_BONDED>");
                    mbluetoothGatt.discoverServices();
                }
            }
            else if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
                int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
                Log.d("BLE", "<ACTION_PAIRING_REQUEST>, Type " + type );
            }
        }
    };

    public final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d("BLE", "status: " + status + ", newState: " + newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_BONDED) {
                            mbluetoothGatt.setPreferredPhy(BluetoothDevice.PHY_LE_2M,BluetoothDevice.PHY_LE_2M,BluetoothDevice.PHY_OPTION_NO_PREFERRED);
                            mbluetoothGatt.discoverServices();
                        }
                        break;

                    case BluetoothProfile.STATE_DISCONNECTED:
                        app_ui.onBleStateChange(R.string.state_idle);
                        mbluetoothGatt.close();
                        mbluetoothGatt = null;
                        break;
                }
            }
            else {
                closeGatt();
                app_ui.onBleStateChange(R.string.state_idle);
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                app_ble_service_nus.onServicesDiscovered(gatt);
            }
            else {
                disconnect();
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt,
                                         @NonNull BluetoothGattCharacteristic characteristic,
                                         @NonNull byte[] value,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                app_ble_service_nus.onCharacteristicRead(characteristic, value);
            }
            else {
                disconnect();
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt,
                                            @NonNull BluetoothGattCharacteristic characteristic,
                                            @NonNull byte[] value) {
            app_ble_service_nus.onCharacteristicChanged(characteristic, value);
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                app_ble_service_nus.onCharacteristicWrite(characteristic);
            }
            else {
                disconnect();
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onDescriptorWrite (BluetoothGatt gatt,
                                       BluetoothGattDescriptor descriptor,
                                       int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                app_ble_service_nus.onDescriptorWrite(descriptor);
            }
            else {
                disconnect();
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.d("BLE", "onMtuChanged, mtu: " + mtu + ", status: " + status);
        }
    };

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void disconnect() {
        if (mbluetoothGatt != null) {
            mbluetoothGatt.disconnect();
        }
    }

    public void sendData(byte[] data) {
        if (mbluetoothGatt != null) {
            app_ble_service_nus.sendData(data);
        }
    }
}
