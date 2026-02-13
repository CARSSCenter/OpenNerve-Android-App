package com.coforcemed.carss_controller;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Switch;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.widget.SwitchCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class App_BLE {
    protected Activity mActivity;
    public App_BLE(Activity activity, App_UI appUi, App_Command appCommand) {
        mActivity = activity;
        app_ui = appUi;
        app_command = appCommand;
        app_ble_connection = new App_BLE_Connection(mActivity, appUi, appCommand);
    }
    private final App_UI app_ui;
    private final App_BLE_Connection app_ble_connection;
    private final App_Command app_command;

    private BluetoothAdapter mbluetoothAdapter;
    private BluetoothLeScanner mbluetoothLeScanner;

    private static final int REQUEST_ENABLE_BT = 0;

    private static final int COMPANY_ID_SRS = 0xF0F0;
    private static final int COMPANY_ID_DVT = 0xFFFF;

    private final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    private final IntentFilter stateBtFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);

    private final ScanSettings scanSettings = new ScanSettings.Builder()
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build();

    private final List<ScanFilter> scanFilters = new ArrayList<>();

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void onCreate() {
        BluetoothManager mbluetoothManager = (BluetoothManager) mActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        mbluetoothAdapter = mbluetoothManager.getAdapter();
        mbluetoothLeScanner = mbluetoothAdapter.getBluetoothLeScanner();

        mActivity.registerReceiver(mReceiver, stateBtFilter);
        if (    mbluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF ||
                mbluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_OFF  ) {
            Log.i("BLE", "Bluetooth is off" );
            mActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        app_ble_connection.onCreate();
        app_ui.onBleStateChange(R.string.state_idle);
    }

    public void onPause() {

    }

    public void onStop() {

    }

    public void onDestroy() {
        unregisterReceiver();
    }

    public void onResume() {

    }

    private void unregisterReceiver() {
        mActivity.unregisterReceiver(mReceiver);
        app_ble_connection.unregisterReceiver();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            assert action != null;
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (bluetoothState) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.i("BLE", "Bluetooth is off" );
                        app_ui.onBleStateChange(R.string.state_idle);
                        mActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.i("BLE", "Bluetooth turning off" );
                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.i("BLE", "Bluetooth turning on" );
                        break;

                    case BluetoothAdapter.STATE_ON:
                        Log.i("BLE", "Bluetooth is on" );
                        break;

                    default:
                        Log.i("BLE", "BluetoothAdapter:" +  bluetoothState);
                        break;
                }
            }
        }
    };

    private String byteToHexStr(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    private final ScanCallback scanCallback = new ScanCallback() {

        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice bluetoothDevice = result.getDevice();
            ScanRecord scanRecord = result.getScanRecord();
            assert scanRecord != null;
            SparseArray<byte[]> manufacturerData = scanRecord.getManufacturerSpecificData();
            int companyId = manufacturerData.keyAt(0);
            String companyIdHex = String.format("0x%04X", companyId);
            byte[] data = manufacturerData.valueAt(0);
            StringBuilder hex_str = new StringBuilder();
            for (byte datum : data) {
                hex_str.append(byteToHexStr(datum).toUpperCase()).append(" ");
            }
            Log.d("BLE", "onScanResult, name: " + bluetoothDevice.getName() +
                    ", address: " + bluetoothDevice.getAddress() +
                    ", rssi: " + result.getRssi() +
                    ", Company ID: " + companyIdHex +
                    ", Manufacturer Data: " + hex_str);

            if (companyId == COMPANY_ID_SRS &&
                    app_ui.app_ui_user_class.user_class_StrId != R.string.action_selectDVT) {
                mbluetoothLeScanner.stopScan(scanCallback);
                Log.d("BLE","StopScan");
                Log.d("BLE","Bond state: " + bluetoothDevice.getBondState());
                byte hwVer = data[data.length - 1];
                app_command.setHWver(hwVer);
                app_ui.app_ui_user_class.setHWver(hwVer);
                app_ble_connection.connectDevice(bluetoothDevice);
                app_ui.onBleStateChange(R.string.state_connecting);
            }
            else if (companyId == COMPANY_ID_DVT &&
                    app_ui.app_ui_user_class.user_class_StrId == R.string.action_selectDVT) {
                app_ui.app_ui_user_class.onScanResult(bluetoothDevice.getAddress(), data);
                if (((SwitchCompat)mActivity.findViewById(R.id.switchAutoConnect)).isChecked()) {
                    mbluetoothLeScanner.stopScan(scanCallback);
                    Log.d("BLE","StopScan");
                    Log.d("BLE","Bond state: " + bluetoothDevice.getBondState());
                    app_ble_connection.connectDevice(bluetoothDevice);
                    app_ui.onBleStateChange(R.string.state_connecting);
                }
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d("BLE", "onBatchScanResults");
            mbluetoothLeScanner.stopScan(this);
            Log.d("BLE","StopScan");
            app_ui.onBleStateChange(R.string.state_idle);
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d("BLE", "onScanFailed, errorCode: " + errorCode);
            mbluetoothLeScanner.stopScan(this);
            Log.d("BLE","StopScan");
            app_ui.onBleStateChange(R.string.state_idle);
        }
    };

    private class runnableStartScan implements Runnable {
        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        @Override
        public void run() {
            Log.d("BLE","StartScan");
            try {
                mbluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback);
            }
            catch (Exception e) {
                Log.e("fail", Objects.requireNonNull(e.getMessage()));
                app_ui.onBleStateChange(R.string.state_idle);
            }
        }
    }

    private void setScanFilters () {
        ScanFilter scanFilter;
        scanFilter = new ScanFilter.Builder()
                .setDeviceName(mActivity.getString(R.string.device_name_of_scanfilter))
                .build();

        scanFilters.clear();
        scanFilters.add(scanFilter);
    }


    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    public void nextBleStat () {
        if (mbluetoothAdapter.isEnabled()) {
            int currID = app_ui.app_ui_ble.state_StrId;
            int nextID = currID;

            if (currID == R.string.state_idle) {
                setScanFilters();
                new Thread(new runnableStartScan()).start();
                nextID = R.string.state_scaning;
            } else if (currID == R.string.state_scaning) {
                mbluetoothLeScanner.stopScan(scanCallback);
                nextID = R.string.state_idle;
            } else if (currID == R.string.state_connected) {
                app_ble_connection.disconnect();
                nextID = R.string.state_disconnecting;
            }

            app_ui.onBleStateChange(nextID);
        }
    }

    public void onClick(View view) {
        byte[] cmd = app_command.onClick(view);
        app_ble_connection.sendData(cmd);
    }
}
