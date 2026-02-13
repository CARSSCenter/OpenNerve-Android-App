package com.coforcemed.carss_controller;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.Menu;
import android.view.MenuItem;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private final App_File app_file = new App_File(this);
    private final App_Command app_command = new App_Command(this, app_file);
    private final App_UI app_ui = new App_UI(this, app_command, app_file);
    private final App_BLE app_ble = new App_BLE(this, app_ui, app_command);

    private static final int REQUEST_PERMISSION_CODE = 1001;

    private CollapsingToolbarLayout toolBarLayout;

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        toolBarLayout = findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle(getResources().getString(R.string.action_selectAdmin));

        findViewById(R.id.fab).setOnClickListener(view -> app_ble.nextBleStat());

        toolBarLayout.setTitle(getResources().getString(R.string.action_selectAdmin));
        app_ui.app_ui_user_class.setUserClass(R.string.action_selectAdmin);

        String[] permissions = new String[]{
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,

                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.BLUETOOTH_CONNECT,

                android.Manifest.permission.VIBRATE,
        };

        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CODE);
        }
        else {
            continueAppInitialization();
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void continueAppInitialization() {
        Log.d("APP", "Continue App Initialization\n" );
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            app_command.onCreate();
            app_ui.onCreate();
            app_ble.onCreate();
        });
    }

    private boolean hasPermissions(String[] permissions) {
        Log.d("APP", "Permission has:\n" );
        boolean res = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                res = false;
            }
            else {
                Log.d("APP", permission);
            }
        }
        return res;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @RequiresPermission(allOf = {Manifest.permission. BLUETOOTH_SCAN,Manifest.permission. BLUETOOTH_CONNECT})
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (app_ui.app_ui_ble.state_StrId == R.string.state_connected) {
            app_ble.nextBleStat();
        }

        if (id == R.id.action_setUserClass) {
            return true;

        } else if (id == R.id.action_selectAdmin) {
            toolBarLayout.setTitle(getResources().getString(R.string.action_selectAdmin));
            app_ui.app_ui_user_class.setUserClass(R.string.action_selectAdmin);
            return true;

        } else if (id == R.id.action_selectClinician) {
            toolBarLayout.setTitle(getResources().getString(R.string.action_selectClinician));
            app_ui.app_ui_user_class.setUserClass(R.string.action_selectClinician);
            return true;

        } else if (id == R.id.action_selectPatient) {
            toolBarLayout.setTitle(getResources().getString(R.string.action_selectPatient));
            app_ui.app_ui_user_class.setUserClass(R.string.action_selectPatient);
            return true;

        } else if (id == R.id.action_selectDVT) {
            toolBarLayout.setTitle(getResources().getString(R.string.action_selectDVT));
            app_ui.app_ui_user_class.setUserClass(R.string.action_selectDVT);
            return true;

        } else if (id == R.id.action_checkBond) {
            Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        app_ble.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        app_ble.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        app_ble.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        app_ble.onResume();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            Log.d("APP", "Permission request:\n" );
            boolean res = true;
            for (int i=0;i<grantResults.length;i++) {
                String resStr = "(UNKNOWN)";
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    resStr = "(PERMISSION_GRANTED)";
                }
                else if(grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    resStr = "(PERMISSION_DENIED)";
                    res = false;
                }
                Log.d("APP", permissions[i] + resStr);
            }

            if (res) {
                continueAppInitialization();
            }
            else {
                finishAffinity();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        app_file.onActivityResult(requestCode, resultCode, data);
        app_ui.app_ui_user_class.reloadFwIMGName();
    }

    public void onClick(View view) {
        app_ble.onClick(view);
        app_ui.onClick(view);
    }
}