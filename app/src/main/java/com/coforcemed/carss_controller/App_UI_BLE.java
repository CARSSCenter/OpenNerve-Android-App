package com.coforcemed.carss_controller;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

public class App_UI_BLE {
    protected Activity mActivity;
    public App_UI_BLE(Activity activity) {
        mActivity = activity;
    }

    public int state_StrId = R.string.state_idle;

    private final int[] BLE_STAT_STR_ID = new int[] {
            R.string.state_idle,
            R.string.state_scaning,
            R.string.state_connecting,
            R.string.state_connected,
            R.string.state_disconnecting,
    };

    private final int[] BLE_STAT_BTN_COLOR = new int[] {
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#03DAC5"),
            Color.parseColor("#018786"),
            Color.parseColor("#BBDEFB"),
            Color.parseColor("#018786"),
    };

    private final int[] BLE_STAT_STR_COLOR = new int[] {
            Color.parseColor("#AAAAAA"),
            Color.parseColor("#0000FF"),
            Color.parseColor("#00DDFF"),
            Color.parseColor("#8BC34A"),
            Color.parseColor("#AAAAAA"),
    };

    private final int[] BLE_STAT_PROGRAMMER_BAR = new int[] {
            ProgressBar.GONE,
            ProgressBar.VISIBLE,
            ProgressBar.VISIBLE,
            ProgressBar.GONE,
            ProgressBar.VISIBLE,
    };

    public void onCreate () {

    }

    public void onBleStateChange (int stateStrId) {
        int selected = 0;
        for (int i=0;i<BLE_STAT_STR_ID.length;i++) {
            if (stateStrId == BLE_STAT_STR_ID[i])
                selected = i;
        }

        state_StrId = stateStrId;
        ColorStateList colorStateList = ColorStateList.valueOf(BLE_STAT_BTN_COLOR[selected]);
        String status = mActivity.getString(stateStrId);
        String message = "BLE " + status + "...";
        int Visibility = BLE_STAT_PROGRAMMER_BAR[selected];
        int textColor = BLE_STAT_STR_COLOR[selected];

        mActivity.runOnUiThread(() -> {
            mActivity.findViewById(R.id.fab).setBackgroundTintList(colorStateList);
            Snackbar.make(mActivity.findViewById(R.id.fab), message, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

            mActivity.findViewById(R.id.progressBarWait).setVisibility(Visibility);
            ((TextView)mActivity.findViewById(R.id.textViewBleStatus)).setText(status);
            ((TextView)mActivity.findViewById(R.id.textViewBleStatus)).setTextColor(textColor);
        });
    }
}
