package com.coforcemed.carss_controller;

import android.app.Activity;
import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.View;

public class App_UI {
    protected Activity mActivity;

    public App_UI(Activity activity, App_Command appCommand, App_File appFile) {
        mActivity = activity;
        app_ui_ble = new App_UI_BLE(mActivity);
        app_ui_user_class = new App_UI_User_Class(mActivity, appCommand, appFile);
    }

    public App_UI_BLE app_ui_ble;
    public App_UI_User_Class app_ui_user_class;

    public void onCreate () {
        app_ui_ble.onCreate();
        app_ui_user_class.onCreate();
    }

    public void onBleStateChange (int stateStrId) {
        vibration();
        app_ui_ble.onBleStateChange(stateStrId);
        app_ui_user_class.onBleStateChange(stateStrId);
    }

    private void vibration () {
        VibratorManager vibratorManager = (VibratorManager) mActivity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
        Vibrator vibrator = vibratorManager.getDefaultVibrator();
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    public void onClick(View view) {
        app_ui_user_class.onClick(view);
    }
}