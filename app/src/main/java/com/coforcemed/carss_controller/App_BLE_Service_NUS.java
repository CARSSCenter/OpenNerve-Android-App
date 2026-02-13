package com.coforcemed.carss_controller;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.util.UUID;

public class App_BLE_Service_NUS {
    protected Activity mActivity;
    public App_BLE_Service_NUS(Activity activity, App_UI appUi, App_Command appCommand) {
        mActivity = activity;
        app_ui = appUi;
        app_command = appCommand;
    }
    private final App_UI app_ui;
    private final App_Command app_command;

    private BluetoothGatt app_gatt;

    private static UUID UUID_ClientCharacteristic_Config;

    private static UUID UUID_NUS_Service;
    private static UUID UUID_NUS_Rx_Characteristic;
    private static UUID UUID_NUS_Tx_Characteristic;

    private BluetoothGattCharacteristic NUS_Rx_Characteristic;
    private BluetoothGattCharacteristic NUS_Tx_Characteristic;

    public void onCreate() {
        UUID_ClientCharacteristic_Config = UUID.fromString(mActivity.getString(R.string.UUID_Client_Characteristic_Config));

        UUID_NUS_Service = UUID.fromString(mActivity.getString(R.string.UUID_Nordic_UART_Service));
        UUID_NUS_Rx_Characteristic = UUID.fromString(mActivity.getString(R.string.UUID_Nordic_UART_RX_Characteristic));
        UUID_NUS_Tx_Characteristic = UUID.fromString(mActivity.getString(R.string.UUID_Nordic_UART_TX_Characteristic));
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void enableNusTxNotification() {
        BluetoothGattDescriptor descriptor = NUS_Tx_Characteristic.getDescriptor(UUID_ClientCharacteristic_Config);
        if (descriptor != null) {
            app_gatt.setCharacteristicNotification(NUS_Tx_Characteristic, true);
            app_gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void onServicesDiscovered(BluetoothGatt gatt) {
        app_gatt = gatt;
        BluetoothGattService NUS_Service = app_gatt.getService(UUID_NUS_Service);
        if (NUS_Service != null) {
            Log.d("NUS", "Number of UART Characteristic Discovered: " + NUS_Service.getCharacteristics().size());
            NUS_Rx_Characteristic = NUS_Service.getCharacteristic(UUID_NUS_Rx_Characteristic);
            NUS_Tx_Characteristic = NUS_Service.getCharacteristic(UUID_NUS_Tx_Characteristic);
            app_ui.onBleStateChange(R.string.state_connected);
            enableNusTxNotification();
        }
    }

    public void onCharacteristicRead(BluetoothGattCharacteristic characteristic, byte[] value) {
        onCharacteristicChanged(characteristic, value);
    }

    public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (characteristic == NUS_Tx_Characteristic) {
            StringBuilder hex_str = new StringBuilder();
            for (byte datum : value) {
                hex_str.append(byteToHexStr(datum).toUpperCase()).append(" ");
            }
            Log.d("NUS", "RX(" + value.length + "): " + hex_str);
            sendData(app_command.parseResponseCommand(value, app_ui));
        }
    }

    public void onCharacteristicWrite(BluetoothGattCharacteristic characteristic) {
        if (characteristic == NUS_Rx_Characteristic) {
            StringBuilder hex_str = new StringBuilder();
            for (byte datum : writeData) {
                hex_str.append(byteToHexStr(datum).toUpperCase()).append(" ");
            }
            Log.d("NUS", "TX(" + writeData.length + "): " + hex_str);
        }
    }

    public void onDescriptorWrite (BluetoothGattDescriptor descriptor) {
        if (descriptor.getCharacteristic() == NUS_Tx_Characteristic) {
            Log.d("NUS", "Tx Notification Enabled");
            sendData(app_command.authCommand(app_ui.app_ui_user_class.user_class_StrId));
        }
    }

    private String byteToHexStr(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    private byte[] writeData;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void sendToRemote() {
        int res = app_gatt.writeCharacteristic(NUS_Rx_Characteristic, writeData, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        if (res == BluetoothGatt.GATT_SUCCESS) {
            Log.d("NUS", "Send Data(" + writeData.length + ")");
        }
        else {
            Log.e("NUS", "Send Data Failed(" + res + ")");
            Log.d("NUS", "Busy, Retry after 500 ms");
            new Handler(Looper.getMainLooper()).postDelayed(this::sendToRemote, 500);
        }
    }

    public void sendData(byte[] data) {
        if (data == null)
            return;

        writeData = data;
        new Handler(Looper.getMainLooper()).postDelayed(this::sendToRemote, 100);
    }
}
