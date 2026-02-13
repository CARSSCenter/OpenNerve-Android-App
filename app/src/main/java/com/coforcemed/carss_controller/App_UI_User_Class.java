package com.coforcemed.carss_controller;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.widget.SwitchCompat;

import com.androidplot.Plot;
import com.androidplot.PlotListener;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PanZoom;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.google.android.material.snackbar.Snackbar;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Objects;

public class App_UI_User_Class {
    protected Activity mActivity;
    public App_UI_User_Class(Activity activity, App_Command appCommand, App_File appFile) {
        mActivity = activity;
        app_command = appCommand;
        app_file = appFile;
    }
    private final App_Command app_command;
    private final App_File app_file;

    public int user_class_StrId = R.string.action_selectAdmin;

    private double downloadTime = 0;

    private final int[] connectedEnableIDs = new int[] {
            R.id.editTextBlePairingCode,
            R.id.switchBleWhitelist,
            R.id.imageButtonBleAdvanceSave,
            R.id.switchDvtTitle,
            R.id.imageButtonAuthFwImage,
            R.id.imageButtonDownloadFwImage,
            R.id.imageButtonVerifyFwImage,
            R.id.imageButtonScheduledTherapyStart,
            R.id.imageButtonScheduledTherapyStop,
            R.id.imageButtonManualTherapyStart,
            R.id.imageButtonManualTherapyStop,
            R.id.buttonReadSP,
            R.id.buttonWriteSP,
            R.id.buttonReadHP,
            R.id.buttonWriteHP,
            R.id.buttonShutdownSystem,
            R.id.buttonRebootSystem,
            R.id.buttonBleDisconnectRequest,
            R.id.buttonMeasureImpedance,
            R.id.buttonMeasureBatteryVoltage,
            R.id.spinnerMeasureSensorID,
            R.id.toggleButtonMeasureSensorVoltage,
            R.id.buttonReadLog,
            R.id.buttonEraseLog,
            R.id.buttonReadTimeDate,
            R.id.buttonWriteTimeDate,
            R.id.buttonStartAccelerometer,
            R.id.buttonGetAccelerometer,
            R.id.buttonStopAccelerometer,
    };

    private final int[] userClassLayoutID_All = new int[] {
            R.id.layout_ble_dvt,
            R.id.layout_ble_advance,
            R.id.layout_dvt,
            R.id.layout_oad,
            R.id.layout_therapy_session,
            R.id.layout_stimulation_parameters,
            R.id.layout_hardware_parameters,
            R.id.layout_system,
            R.id.layout_measurement,
            R.id.layout_log,
            R.id.layout_time_date,
            R.id.layout_acc,
    };

    private final int[] userClassLayoutID_Admin = new int[] {
            R.id.layout_ble_advance,
            R.id.layout_dvt,
            R.id.layout_oad,
            R.id.layout_therapy_session,
            R.id.layout_stimulation_parameters,
            R.id.layout_hardware_parameters,
            R.id.layout_system,
            R.id.layout_measurement,
            R.id.layout_log,
            R.id.layout_time_date,
            R.id.layout_acc,
    };

    private final int[] userClassLayoutID_Clinician = new int[] {
            R.id.layout_therapy_session,
            R.id.layout_stimulation_parameters,
    };

    private final int[] userClassLayoutID_Patient = new int[] {
            R.id.layout_therapy_session,
    };

    private final int[] userClassLayoutID_DVT = new int[] {
            R.id.layout_ble_dvt,
            R.id.layout_dvt,
    };

    private String getTimeDateStr () {
        String strYear = "20" + new DecimalFormat("00-").
                format(Calendar.getInstance().get(Calendar.YEAR) - 2000);
        String strMonth = new DecimalFormat("00-").
                format(Calendar.getInstance().get(Calendar.MONTH) + 1);
        String strDay = new DecimalFormat("00T").
                format(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        String strHour = new DecimalFormat("00:").
                format(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        String strMinute = new DecimalFormat("00:").
                format(Calendar.getInstance().get(Calendar.MINUTE));
        String strSecond = new DecimalFormat("00Z").
                format(Calendar.getInstance().get(Calendar.SECOND));
        return (strYear + strMonth + strDay +
                strHour + strMinute + strSecond);
    }

    private void reloadTimeDate() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (mActivity.findViewById(R.id.textViewTimeDateHost) != null) {
                mActivity.runOnUiThread(() -> {
                    String str = "[Host]\t\t\t" + getTimeDateStr();
                    ((TextView) mActivity.findViewById(R.id.textViewTimeDateHost)).setText(str);
                });
                reloadTimeDate();
            }
        }, 1000);
    }

    private String strLogTimeStamp;
    private void reloadLogTimeStamp() {
        strLogTimeStamp = "2023-01-01T00:00:00Z(000)";
        mActivity.runOnUiThread(() -> ((TextView)mActivity.findViewById(R.id.textViewLogTimeStamp)).setText(strLogTimeStamp));
    }

    public void reloadFwIMGName() {
        mActivity.runOnUiThread(() -> {
            ((TextView)mActivity.findViewById(R.id.textViewImgFileName)).setText(app_file.fw_image_name);
            if (app_file.fw_image_name.equals("-----"))
                mActivity.findViewById(R.id.layoutOadControl).setVisibility(LinearLayout.GONE);
            else
                mActivity.findViewById(R.id.layoutOadControl).setVisibility(LinearLayout.VISIBLE);
        });
    }

    private TextView textViewFreq;
    private SeekBar seekBarFreq;

    private XYPlot plot;
    private ArrayList<Number> dataPoints;

    private static final int maxPlotPoints = 5000;
    private boolean isPlotting = false;

    @SuppressLint("DefaultLocale")
    public void onCreate () {
        reloadTimeDate();
        reloadLogTimeStamp();
        setLogWait(false);
        reloadFwIMGName();

        textViewFreq = mActivity.findViewById(R.id.textViewMeasureSensorFreq);
        seekBarFreq = mActivity.findViewById(R.id.seekBarMeasureSensorFreq);

        float initValue = seekBarFreq.getProgress() * 0.1f;
        textViewFreq.setText(String.format("%01.1f", initValue));

        seekBarFreq.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress * 0.1f;
                textViewFreq.setText(String.format("%01.1f", value));
                //Log.i("OnSeekBarChange", "Freq: " + value + "Hz");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 開始拖動
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 停止拖動
            }
        });

        plot = mActivity.findViewById(R.id.plot);
        PanZoom.attach(plot, PanZoom.Pan.HORIZONTAL, PanZoom.Zoom.STRETCH_HORIZONTAL);

        plot.setDomainBoundaries(0, maxPlotPoints - 1,  BoundaryMode.FIXED);
        plot.setRangeBoundaries(0, 3300, BoundaryMode.FIXED);

        plot.getGraph().getGridBackgroundPaint().setColor(Color.WHITE);
        plot.getGraph().getDomainGridLinePaint().setColor(Color.GRAY);
        plot.getGraph().getRangeGridLinePaint().setColor(Color.GRAY);

        plot.addListener(new PlotListener() {
            @Override
            public void onBeforeDraw(Plot source, Canvas canvas) {
                isPlotting = true;
            }

            @Override
            public void onAfterDraw(Plot source, Canvas canvas) {
                isPlotting = false;
            }
        });

        dataPoints = new ArrayList<>(Collections.nCopies(maxPlotPoints, 0));
        drawPlot(dataPoints);

        ((Spinner)mActivity.findViewById(R.id.spinnerStimulationParametersID)).
                setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        mActivity.runOnUiThread(() -> {
                            ((TextView)mActivity.findViewById(R.id.textViewStimulationParametersDefine)).
                                    setText(app_command.getSPDefineStr(position));

                            EditText editText = mActivity.findViewById(R.id.editTextStimulationParameters);
                            if (app_command.getSPDataFormat(position).equals("Data")) {
                                editText.setText("");
                                editText.setVisibility(View.GONE);
                            }
                            else {
                                editText.setVisibility(View.VISIBLE);
                            }
                        });
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

        ((Spinner)mActivity.findViewById(R.id.spinnerHardwareParametersID)).
                setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        mActivity.runOnUiThread(() -> {
                            String str = app_command.getHPDefineStr(position);
                            ((TextView)mActivity.findViewById(R.id.textViewHardwareParametersDefine)).
                                    setText(str);

                            EditText editText = mActivity.findViewById(R.id.editTextHardwareParameters);
                            Spinner spinner = mActivity.findViewById(R.id.spinnerHardwareParametersRawData);
                            if (app_command.getHPDataFormat(position).equals("Data")) {
                                editText.setText("");
                                editText.setVisibility(View.GONE);
                                spinner.setVisibility(View.VISIBLE);

                                ArrayList<String> str_list = new ArrayList<>();
                                String str_def = str.split(">\n")[1].replace("\n", "").replace("\t", "");
                                str_list.add(str_def);

                                String str_hex = "0123456789ABCDEF";
                                StringBuilder str_rng = new StringBuilder();
                                for (int i=0;i<str_def.length();i++) {
                                    str_rng.append(str_hex.charAt((int) (Math.random() * 16)));
                                }
                                str_list.add(str_rng.toString());
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_item, str_list);
                                spinner.setAdapter(adapter);
                            }
                            else {
                                editText.setVisibility(View.VISIBLE);
                                spinner.setVisibility(View.GONE);
                            }
                        });
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
    }

    public void onBleStateChange (int stateStrId) {
        mActivity.runOnUiThread(() -> {
            boolean enableView;
            if (stateStrId == R.string.state_connected) {
                enableView = true;
                ((SwitchCompat)mActivity.findViewById(R.id.switchDvtTitle)).
                        setChecked(user_class_StrId == R.string.action_selectDVT);
            }
            else {
                enableView = false;
                if (stateStrId == R.string.state_idle) {
                    setLogWait(false);
                    ((CheckedTextView) mActivity.findViewById(R.id.checkedTextViewOadTitle)).setChecked(false);
                    ((TextView) mActivity.findViewById(R.id.textViewAuthFwImageStatus)).setText("");
                    ((TextView) mActivity.findViewById(R.id.textViewDownloadFwImageStatus)).setText("");
                    ((TextView) mActivity.findViewById(R.id.textViewVerifyFwImageStatus)).setText("");
                    ((ToggleButton) mActivity.findViewById(R.id.toggleButtonMeasureSensorVoltage)).setChecked(false);
                }
            }

            if (stateStrId != R.string.state_scaning) {
                ((TextView) mActivity.findViewById(R.id.textViewManufacturerData)).setText("");
            }

            for (int id : connectedEnableIDs) {
                mActivity.findViewById(id).setEnabled(enableView);
            }
        });
    }

    public void setHWver(byte ver) {
        Spinner spinnerSPID = mActivity.findViewById(R.id.spinnerStimulationParametersID);
        switch (ver) {
            case (byte) 15:
                ((SwitchCompat) mActivity.findViewById(R.id.switchVNSb)).setChecked(false);
                mActivity.findViewById(R.id.switchVNSb).setEnabled(false);
                ArrayAdapter<CharSequence> adapterH1 = ArrayAdapter.createFromResource(
                        mActivity,
                        R.array.spinnerCommandPayloadSPIDsH1,
                        android.R.layout.simple_spinner_item
                );
                adapterH1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerSPID.setAdapter(adapterH1);
                break;

            case (byte) 21:
                ((SwitchCompat) mActivity.findViewById(R.id.switchVNSb)).setChecked(false);
                mActivity.findViewById(R.id.switchVNSb).setEnabled(true);
                ArrayAdapter<CharSequence> adapterH2 = ArrayAdapter.createFromResource(
                        mActivity,
                        R.array.spinnerCommandPayloadSPIDsH2,
                        android.R.layout.simple_spinner_item
                );
                adapterH2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerSPID.setAdapter(adapterH2);
                break;

            default:
                break;
        }
    }

    public void setUserClass (int userClassStrId) {
        user_class_StrId = userClassStrId;
        String userClassStr = mActivity.getString(userClassStrId);
        String message = "Current User Class: " + userClassStr;
        mActivity.runOnUiThread(() -> {
            for (int ID : userClassLayoutID_All) {
                mActivity.findViewById(ID).setVisibility(View.GONE);
            }

            if (userClassStrId == R.string.action_selectAdmin) {
                for (int ID : userClassLayoutID_Admin) {
                    mActivity.findViewById(ID).setVisibility(View.VISIBLE);
                }
            }
            else if (userClassStrId == R.string.action_selectClinician) {
                for (int ID : userClassLayoutID_Clinician) {
                    mActivity.findViewById(ID).setVisibility(View.VISIBLE);
                }
            }
            else if (userClassStrId == R.string.action_selectPatient) {
                for (int ID : userClassLayoutID_Patient) {
                    mActivity.findViewById(ID).setVisibility(View.VISIBLE);
                }
            }
            else if (userClassStrId == R.string.action_selectDVT) {
                for (int ID : userClassLayoutID_DVT) {
                    mActivity.findViewById(ID).setVisibility(View.VISIBLE);
                }
            }

            Snackbar.make(mActivity.findViewById(R.id.fab), message, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        });
    }

    public void onClick(View view) {
        int viewID = view.getId();
        mActivity.runOnUiThread(() -> {
            if (viewID == R.id.buttonWriteSP) {
                ((TextView)mActivity.findViewById(R.id.textViewStimulationParametersData)).setText("");
            } else if (viewID == R.id.buttonWriteHP) {
                ((TextView)mActivity.findViewById(R.id.textViewHardwareParametersData)).setText("");
            } else if (viewID == R.id.buttonReadTimeDate || viewID == R.id.buttonWriteTimeDate) {
                ((TextView)mActivity.findViewById(R.id.textViewTimeDateRemote)).setText(R.string.side_remote);
            } else if (viewID == R.id.buttonReadLog || viewID == R.id.buttonEraseLog) {
                setLogWait(true);
            } else if (viewID == R.id.imageButtonDownloadFwImage) {
                onDownloadImage(true);
            } else if (viewID == R.id.imageButtonSelectFwImage) {
                app_file.searchFwImageFile();
            } else if (viewID == R.id.textViewManufacturerData) {
                copyTextView();
            } else if (viewID == R.id.toggleButtonMeasureSensorVoltage) {
                boolean isChecked = ((ToggleButton)mActivity.findViewById(viewID)).isChecked();
                mActivity.findViewById(R.id.spinnerMeasureSensorID)
                        .setEnabled(!isChecked);
                if (isChecked) {
                    resetProcessPacket();
                    plot.clear();
                    ((TextView) mActivity.findViewById(R.id.textViewSensorVoltage))
                            .setText("");
                }
            } else {
                Log.w("OnClick", "Unhandled view ID: " + viewID);
            }
        });
    }

    private void vibration () {
        VibratorManager vibratorManager = (VibratorManager) mActivity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
        Vibrator vibrator = vibratorManager.getDefaultVibrator();
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void copyTextView () {
        TextView textView = mActivity.findViewById(R.id.textViewManufacturerData);
        String textToCopy = textView.getText().toString();

        ClipboardManager clipboard = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied Text", textToCopy);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(mActivity, mActivity.getText(R.string.copied_text), Toast.LENGTH_SHORT).show();
        vibration();
    }

    public void onStatus (byte status, String msg) {
        String str = "Status(0x" + App_Command.byteToHexStr(status) + "): ";
        String finalStr = str + msg;
        mActivity.runOnUiThread(() ->
                Snackbar.make(mActivity.findViewById(R.id.fab), finalStr, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());
    }

    public static String[] byteToBitStringArray(byte[] byteArray) {
        // 計算字串陣列的大小（每個 byte 包含 8 個位元）
        String[] result = new String[byteArray.length * 8];

        int index = 0;
        for (byte b : byteArray) {
            for (int j = 0; j <= 7; j++) { // 從低位到高位逐一檢查
                result[index++] = ((b >> j) & 1) == 1 ? "H" : "L";
            }
        }

        return result;
    }

    public void onScanResult (String address, byte[] data) {
        mActivity.runOnUiThread(() -> {
            int[] volt = new int[] {
                    data[0] * 100,
                    data[1] * 100,
                    data[2] * 100,
                    data[3] * 10,
                    data[4] * 10,
                    data[5] * 10,
                    data[6] * 10,
                    data[7] * 10,
            };

            byte[] byteGPIO = new byte[2];
            System.arraycopy(data, 8, byteGPIO, 0, byteGPIO.length);
            String[] bitStrings = byteToBitStringArray(byteGPIO);

            String str = "[" + mActivity.getString(R.string.address)+ "] " + address + "\n\n" +
                    "[" + mActivity.getString(R.string.payload01)+ "] " + volt[0] + " mV\n" +
                    "[" + mActivity.getString(R.string.payload02)+ "] " + volt[1] + " mV\n" +
                    "[" + mActivity.getString(R.string.payload03)+ "] " + volt[2] + " mV\n" +
                    "[" + mActivity.getString(R.string.payload04)+ "] " + volt[3] + " mV\n" +
                    "[" + mActivity.getString(R.string.payload05)+ "] " + volt[4] + " mV\n" +
                    "[" + mActivity.getString(R.string.payload06)+ "] " + volt[5] + " mV\n" +
                    "[" + mActivity.getString(R.string.payload07)+ "] " + volt[6] + " mV\n" +
                    "[" + mActivity.getString(R.string.payload08)+ "] " + volt[7] + " mV\n" +
                    "[" + mActivity.getString(R.string.payload09)+ "] " + bitStrings[0] + "\n" +
                    "[" + mActivity.getString(R.string.payload10)+ "] " + bitStrings[1] + "\n" +
                    "[" + mActivity.getString(R.string.payload11)+ "] " + bitStrings[2] + "\n" +
                    "[" + mActivity.getString(R.string.payload12)+ "] " + bitStrings[3] + "\n" +
                    "[" + mActivity.getString(R.string.payload13)+ "] " + bitStrings[4] + "\n" +
                    "[" + mActivity.getString(R.string.payload14)+ "] " + bitStrings[5] + "\n" +
                    "[" + mActivity.getString(R.string.payload15)+ "] " + bitStrings[6] + "\n" +
                    "[" + mActivity.getString(R.string.payload16)+ "] " + bitStrings[7] + "\n" +
                    "[" + mActivity.getString(R.string.payload17)+ "] " + bitStrings[8] + "\n" +
                    "[" + mActivity.getString(R.string.payload18)+ "] " + bitStrings[9] + "\n" +
                    "[" + mActivity.getString(R.string.payload19)+ "] " + bitStrings[10] + "\n" +
                    "[" + mActivity.getString(R.string.payload20)+ "] " + bitStrings[11] + "\n" +
                    "[" + mActivity.getString(R.string.payload21)+ "] " + bitStrings[12] + "\n" +
                    "[" + mActivity.getString(R.string.payload22)+ "] " + bitStrings[13];

            ((TextView) mActivity.findViewById(R.id.textViewManufacturerData)).setText(str);
        });
    }

    public void onReadBleAdvance (byte[] payload) {
        byte[] pairing_code = new byte[6];
        System.arraycopy(payload, 0, pairing_code, 0, pairing_code.length);
        boolean whitelist = payload[pairing_code.length] > 0;
        mActivity.runOnUiThread(() -> {
            ((EditText)mActivity.findViewById(R.id.editTextBlePairingCode)).setText(new String(pairing_code));
            ((SwitchCompat)mActivity.findViewById(R.id.switchBleWhitelist)).setChecked(whitelist);
        });
    }

    public void onReadSP (byte[] payload) {
        StringBuilder payloadStr = new StringBuilder();
        for(int i=4;i<payload.length;i++) {
            if((i-4)/8 > 0 && (i-4)%8 == 0)
                payloadStr.append("\n");

            payloadStr.append(App_Command.byteToHexStr(payload[i])).append("\t");
        }
        mActivity.runOnUiThread(() ->
                ((TextView)mActivity.findViewById(R.id.textViewStimulationParametersData))
                        .setText(payloadStr.toString()));
    }

    public void onReadHP (byte[] payload) {
        StringBuilder payloadStr = new StringBuilder();
        for(int i=4;i<payload.length;i++) {
            if((i-4)/8 > 0 && (i-4)%8 == 0)
                payloadStr.append("\n");

            payloadStr.append(App_Command.byteToHexStr(payload[i])).append("\t");
        }
        mActivity.runOnUiThread(() ->
                ((TextView)mActivity.findViewById(R.id.textViewHardwareParametersData))
                        .setText(payloadStr.toString()));
    }

    public void onMeasureBatteryVoltage (byte[] payload) {
        int vbatA = Byte.toUnsignedInt(payload[0]) + Byte.toUnsignedInt(payload[1]) * 0x100;
        int vbatB = Byte.toUnsignedInt(payload[2]) + Byte.toUnsignedInt(payload[3]) * 0x100;
        String str =    "VbatA = " + new DecimalFormat("0 mV").format(vbatA) + "\n" +
                        "VbatB = " + new DecimalFormat("0 mV").format(vbatB);
        mActivity.runOnUiThread(() ->
                ((TextView)mActivity.findViewById(R.id.textViewMeasureBatteryVoltage))
                        .setText(str));
    }

    public void onReadLog (byte[] payload) {
        setLogWait(false);
        if (payload.length == 0)
            return;
        String log = new String(payload);
        mActivity.runOnUiThread(() ->
                ((TextView)mActivity.findViewById(R.id.textViewLog)).append(log));
        int indexA = log.indexOf("[") + 1;
        int indexB = log.indexOf("]");
        strLogTimeStamp = log.substring(indexA, indexB);
        mActivity.runOnUiThread(() -> ((TextView)mActivity.findViewById(R.id.textViewLogTimeStamp)).setText(strLogTimeStamp));
    }
    public void onEraseIPGLog () {
        setLogWait(false);
        reloadLogTimeStamp();
        mActivity.runOnUiThread(() -> {
            ((TextView)mActivity.findViewById(R.id.textViewLog)).setText("");
            ((TextView)mActivity.findViewById(R.id.textViewLog)).setLines(10);
        });
    }

    public void onReadTimeDate (byte[] payload) {
            String strYear = "20" + new DecimalFormat("00-").
                    format(payload[0]);
            String strMonth = new DecimalFormat("00-").
                    format(payload[1]);
            String strDay = new DecimalFormat("00T").
                    format(payload[2]);
            String strHour = new DecimalFormat("00:").
                    format(payload[3]);
            String strMinute = new DecimalFormat("00:").
                    format(payload[4]);
            String strSecond = new DecimalFormat("00Z").
                    format(payload[5]);
            String str = "[Remote]\t" + strYear + strMonth + strDay + strHour + strMinute + strSecond;
        mActivity.runOnUiThread(() ->
                ((TextView)mActivity.findViewById(R.id.textViewTimeDateRemote)).setText(str));
    }

    public void onAuthFwImage (byte status) {
        mActivity.runOnUiThread(() -> {
            if (status == app_command.STATUS_SUCCESS) {
                ((TextView) mActivity.findViewById(R.id.textViewAuthFwImageStatus)).setText(R.string.status_success);
                ((CheckedTextView) mActivity.findViewById(R.id.checkedTextViewOadTitle)).setChecked(true);
                Log.d("Image", "Authentication: Pass");
            }
            else {
                ((TextView) mActivity.findViewById(R.id.textViewAuthFwImageStatus)).setText(R.string.status_error);
                Log.d("Image", "Authentication: Fail");
            }
        });
    }

    public void onDownloadImage (boolean reset) {
        if (reset)
            downloadTime = 0;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            downloadTime += 0.5;
            String str = app_command.fw_image_offset + " / " + app_command.fw_image_size;
            mActivity.runOnUiThread(() -> ((TextView)mActivity.findViewById(R.id.textViewDownloadFwImageStatus)).setText(str));
            if (app_command.fw_image_offset < app_command.fw_image_size) {
                onDownloadImage(false);
            }
            else {
                double speed = app_command.fw_image_size / downloadTime;
                Log.d("Image", "Download completed, Time: " + downloadTime + " sec, Speed: " + speed + " byte/sec");
                AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                builder.setMessage("Image download completed.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        }, 500);
    }

    public void onVerifyFwImage (byte status) {
        mActivity.runOnUiThread(() -> {
            if (status == app_command.STATUS_SUCCESS) {
                ((TextView) mActivity.findViewById(R.id.textViewVerifyFwImageStatus)).setText(R.string.status_success);
                Log.d("Image", "Verify: Pass");
            }
            else {
                ((TextView) mActivity.findViewById(R.id.textViewVerifyFwImageStatus)).setText(R.string.status_error);
                Log.d("Image", "Verify: Fail");
            }
        });
    }

    public void setLogWait (boolean enable) {
        mActivity.runOnUiThread(() -> {
            if (enable) {
                (mActivity.findViewById(R.id.progressBarLogWait)).setVisibility(ProgressBar.VISIBLE);
                (mActivity.findViewById(R.id.buttonReadLog)).setEnabled(false);
                (mActivity.findViewById(R.id.buttonEraseLog)).setEnabled(false);
            }
            else {
                (mActivity.findViewById(R.id.progressBarLogWait)).setVisibility(ProgressBar.INVISIBLE);
                (mActivity.findViewById(R.id.buttonReadLog)).setEnabled(true);
                (mActivity.findViewById(R.id.buttonEraseLog)).setEnabled(true);
            }
        });
    }

    public void onMeasureImpedance(byte[] payload) {
        int impedance = Byte.toUnsignedInt(payload[0]) + Byte.toUnsignedInt(payload[1]) * 0x100;
        String str =  new DecimalFormat("0 ohm").format(impedance);
        mActivity.runOnUiThread(() ->
                ((TextView)mActivity.findViewById(R.id.textViewMeasureImpedance))
                        .setText(str));
    }

    public void onStartAcc(byte[] payload) {
        int xAxis = Byte.toUnsignedInt(payload[1]) + Byte.toUnsignedInt(payload[2]) * 0x100;
        int yAxis = Byte.toUnsignedInt(payload[3]) + Byte.toUnsignedInt(payload[4]) * 0x100;
        int zAxis = Byte.toUnsignedInt(payload[5]) + Byte.toUnsignedInt(payload[6]) * 0x100;
        String str = "[X]: " + xAxis + "\n[Y]: " + yAxis + "\n[Z]: " + zAxis;
        mActivity.runOnUiThread(() ->
                ((TextView)mActivity.findViewById(R.id.textViewAccelerometer))
                        .setText(str));
    }

    public void onGetDataAcc(byte[] payload) {
        if (payload.length >= 13) {
            int xAxis = Byte.toUnsignedInt(payload[7]) + Byte.toUnsignedInt(payload[8]) * 0x100;
            int yAxis = Byte.toUnsignedInt(payload[9]) + Byte.toUnsignedInt(payload[10]) * 0x100;
            int zAxis = Byte.toUnsignedInt(payload[11]) + Byte.toUnsignedInt(payload[12]) * 0x100;
            String str = "[X]: " + xAxis + "\n[Y]: " + yAxis + "\n[Z]: " + zAxis;
            mActivity.runOnUiThread(() ->
                    ((TextView) mActivity.findViewById(R.id.textViewAccelerometer))
                            .setText(str));
        }
    }

    private byte lastPacketNum = -1;
    private int lossPackets = 0;
    private int sampleFreq = 0;
    private int packetCount = 0;
    private final boolean[] packetNumExist = new boolean[100];
    private void resetProcessPacket() {
        lastPacketNum = -1;
        lossPackets = 0;
        packetCount = 0;
        sampleFreq = (int)(seekBarFreq.getProgress() * 0.1f);
        Arrays.fill(packetNumExist, false);
    }
    private long lastTimestamp = -1;

    private void processPacket(byte packetNum) {
        long currentTimestamp = System.currentTimeMillis();

        if (lastPacketNum == -1) {
            lastPacketNum = packetNum;
            lastTimestamp = currentTimestamp;
            return;
        }

        packetNumExist[packetNum] = true;
        packetCount++;

        if (packetCount >= 100 || packetNum < lastPacketNum) {
            lossPackets = 0;
            for (boolean value : packetNumExist) {
                if (!value) lossPackets++;
            }

            Arrays.fill(packetNumExist, false);
            packetCount = 0;

            long timeInterval = currentTimestamp - lastTimestamp;
            float freq = 1f / (timeInterval / 1000f / 10000f);
            sampleFreq = (int) freq;
            lastTimestamp = currentTimestamp;

            Log.d("processPacket", "Loss per 100 Packets: " + lossPackets);
            Log.d("processPacket", "Sample frequency: " + sampleFreq + " Hz");
        }
        byte expectedPacketNum = (byte) ((lastPacketNum + 1) % 100);
        if (packetNum != expectedPacketNum) {
            int lost = ((packetNum - expectedPacketNum) + 100) % 100;
            Log.d("processPacket", "Loss packets detected: " + lost);
        }
        lastPacketNum = packetNum;
    }

    private int fps = 0;
    public void onMeasureSensorVoltage(byte[] payload) {
        mActivity.runOnUiThread(() -> {
            if (payload.length == 0) {
                return;
            }

            byte packetNum = payload[0];
            processPacket(packetNum);

            byte[] packetVoltages = Arrays.copyOfRange(payload, 1, payload.length);
            int[] voltages = convertByteArrayToIntArray(packetVoltages);
            int average = (int) Arrays.stream(voltages).average().orElse(0);
            String strVolt = "Current voltage: " + average + " mV\n"
                    + "Loss per 100 Packets: " + lossPackets + "\n"
                    + "Sample Frequency: " + sampleFreq + " Hz";

            for (int voltage : voltages) {
                while (dataPoints.size() >= maxPlotPoints) {
                    dataPoints.remove(0);
                }
                dataPoints.add(voltage);
            }
            fps = (fps + 1) % 2;
            if (fps == 0 && !isPlotting) {
                drawPlot(dataPoints);
                ((TextView) mActivity.findViewById(R.id.textViewSensorVoltage))
                        .setText(strVolt);
            }
        });
    }

    private XYSeries series1;
    private void drawPlot(ArrayList<Number> poltPoints) {
        mActivity.runOnUiThread(() -> {
             series1 = new XYSeries() {
                @Override
                public String getTitle() {
                    return "";
                }

                @Override
                public int size() {
                    return poltPoints.size();
                }

                @Override
                public Number getX(int index) {
                    return index;
                }

                @Override
                public Number getY(int index) {
                    return poltPoints.get(index);
                }
            };
            plot.clear();
            plot.addSeries(series1, new LineAndPointFormatter(Color.BLUE, null, null, null));
            plot.redraw();
        });
    }

    private static int[] convertByteArrayToIntArray(byte[] byteArray) {
        int numElements = byteArray.length / 2;
        int[] intArray = new int[numElements];

        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < numElements; i++) {
            intArray[i] = byteBuffer.getShort() & 0xFFFF;
        }

        return intArray;
    }

}
