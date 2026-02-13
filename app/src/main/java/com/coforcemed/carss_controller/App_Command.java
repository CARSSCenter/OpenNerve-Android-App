package com.coforcemed.carss_controller;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.widget.SwitchCompat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.PrivateKey;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;

public class App_Command {
    protected Activity mActivity;
    public App_Command(Activity activity, App_File appFile) {
        mActivity = activity;
        app_file = appFile;
        app_auth = new App_Auth(mActivity, appFile);
    }
    private final App_Auth app_auth;
    private final App_File app_file;

    private PrivateKey privateKey_admin;
    private PrivateKey privateKey_clinician;
    private PrivateKey privateKey_patient;
    public byte[] fw_digest;
    public byte[] fw_signature_admin;

    public int fw_image_offset = 0;
    public int fw_image_size = 0;
    public final int fw_image_packet_size = 128;

    public final byte opcode_Auth                           = (byte) 0xF0;
    public final byte opcode_ReadBleAdvance                 = (byte) 0xF1;
    public final byte opcode_WriteBleAdvance                = (byte) 0xF2;
    public final byte opcode_AuthFwImage               	    = (byte) 0xF3;
    public final byte opcode_DownloadFwImage				= (byte) 0xF4;
    public final byte opcode_VerifyFwImage				    = (byte) 0xF5;
    public final byte opcode_SetStartState				    = (byte) 0xF6;

    public final byte opcode_ShutdownSystem                 = (byte) 0xA0;
    public final byte opcode_RebootSystem                   = (byte) 0xA1;
    public final byte opcode_BleDisconnectRequest           = (byte) 0xA2;
    public final byte opcode_StartScheduledTherapySession   = (byte) 0xA3;
    public final byte opcode_EndScheduledTherapySession     = (byte) 0xA4;
    public final byte opcode_StartManualTherapySession      = (byte) 0xA5;
    public final byte opcode_StopManualTherapySession       = (byte) 0xA6;
    public final byte opcode_MeasureImpedance               = (byte) 0xA7;
    public final byte opcode_MeasureBatteryVoltage 	        = (byte) 0xA8;
    public final byte opcode_MeasureSensorVoltage           = (byte) 0xA9;
    public final byte opcode_ReadHardwareParameters         = (byte) 0xAA;
    public final byte opcode_WriteHardwareParameters        = (byte) 0xAB;
    public final byte opcode_ReadStimulationParameters      = (byte) 0xAC;
    public final byte opcode_WriteStimulationParameters     = (byte) 0xAD;
    public final byte opcode_ReadIPGLog                     = (byte) 0xAE;
    public final byte opcode_EraseIPGLog                    = (byte) 0xAF;
    public final byte opcode_ReadTimeDate                   = (byte) 0xB0;
    public final byte opcode_WriteTimeDate                  = (byte) 0xB1;

    public final byte opcode_StartAcc                       = (byte) 0x47;
    public final byte opcode_GetDataAcc                     = (byte) 0x48;
    public final byte opcode_StopAcc                        = (byte) 0x49;

    public final int requestHeaderLen = 2;
    public final int responseHeaderLen = requestHeaderLen + 1;
    public final int crcLen = 2;

    public final byte STATUS_SUCCESS = (byte)0x00;
    public final byte STATUS_INVALID = (byte)0x01;
    public final byte STATUS_CRC_ERR = (byte)0xF0;
    public final byte STATUS_PAYLOAD_LEN_ERR = (byte)0xF1;
    public final byte STATUS_OPCODE_ERR = (byte)0xF2;
    public final byte STATUS_USER_CLASS_ERR = (byte)0xF3;

    public final byte STATUS_GET_ACC_BUFFER_OVERFLOW = (byte)0x01;
    public final byte STATUS_GET_ACC_BUFFER_EMPTY = (byte)0x02;

    public final byte accDevID = (byte)0x18;

    public final int STATE_ACT_MODE_BLE_ACT = 0x0201;
    public final int STATE_ACT_MODE_DVT     = 0x0208;

    private final byte[] default_fw_ver = "yymmdd".getBytes();

    private final byte[] default_ble_id = new byte[] {
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
    };

    private final byte[] default_location = new byte[] {
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
    };

    private static class ParameterDef {
        public final byte[] id;
        private final double min;
        private final double max;
        private final double def;
        private final double step;
        public final String format;
        public byte[] rawData;

        public ParameterDef (String ID, double Min, double Max, double Def, double Step, String Format) {
            id = ID.getBytes();
            min = Min;
            max = Max;
            def = Def;
            step = Step;
            format = Format;
        }

        public ParameterDef (String ID, byte[] rawData) {
            id = ID.getBytes();
            min = 0;
            max = 0;
            def = 0;
            step = 0;
            format = "Data";
            this.rawData = rawData;
        }

        private String getTimeFormatStr (double val) {
            int hr = (int)(val/60);
            int mn = (int)(val%60);
            return new DecimalFormat("00").format(hr) +
                    ":" + new DecimalFormat("00").format(mn);
        }

        private String getIntFormatStr (double val) {
            return new DecimalFormat("0").format(val);
        }

        private String getDoubleFormatStr (double val) {
            return new DecimalFormat("0.00").format(val);
        }

        public String getDefStr () {
            String parameterTypeStr_data = "<Ascii / Raw Data>";
            String parameterTypeStr_value = "<Steps of Value>";
            StringBuilder definesStr = new StringBuilder(parameterTypeStr_data);
            switch (format) {
                case "Time":
                    definesStr = new StringBuilder(parameterTypeStr_value + "\n" +
                            "[MIN]\t\t\t\t\t" + getTimeFormatStr(min) + "\n" +
                            "[MAX]\t\t\t\t\t" + getTimeFormatStr(max) + "\n" +
                            "[DEFAULT]\t\t" + getTimeFormatStr(def) + "\n" +
                            "[STEP SIZE]\t" + getTimeFormatStr(step));
                    break;
                case "Value":
                    if (step >= 1) {
                        definesStr = new StringBuilder(parameterTypeStr_value + "\n" +
                                "[MIN]\t\t\t\t\t" + getIntFormatStr(min) + "\n" +
                                "[MAX]\t\t\t\t\t" + getIntFormatStr(max) + "\n" +
                                "[DEFAULT]\t\t" + getIntFormatStr(def) + "\n" +
                                "[STEP SIZE]\t" + getIntFormatStr(step));
                    } else if (step < 1) {
                        definesStr = new StringBuilder(parameterTypeStr_value + "\n" +
                                "[MIN]\t\t\t\t\t" + getDoubleFormatStr(min) + "\n" +
                                "[MAX]\t\t\t\t\t" + getDoubleFormatStr(max) + "\n" +
                                "[DEFAULT]\t\t" + getDoubleFormatStr(def) + "\n" +
                                "[STEP SIZE]\t" + getDoubleFormatStr(step));
                    }
                    break;
                case "Data":
                    definesStr = new StringBuilder(parameterTypeStr_data);
                    for (int i = 0; i < rawData.length; i++) {
                        if (i % 8 == 0)
                            definesStr.append("\n");
                        definesStr.append(byteToHexStr(rawData[i])).append("\t");
                    }
                    break;
            }
            return definesStr.toString();
        }
    }

    public static String byteToHexStr(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits).toUpperCase();
    }

    private ParameterDef[] SP_Val;
    private ParameterDef[] SP_Val_H1;
    private ParameterDef[] SP_Val_H2;
    private ParameterDef[] HP_Val;
    private ParameterDef[] HP_Val_H1;
    private ParameterDef[] HP_Val_H2;

    public void onCreate() {
        SP_Val_H1 = new ParameterDef[] {
                new ParameterDef("SP01", 1, 6, 3, 1, "Value"),
                new ParameterDef("SP02", 0.2, 5, 0.2, 0.1, "Value"),
                new ParameterDef("SP03", 100, 1000, 500, 50, "Value"),
                new ParameterDef("SP04", 1, 2000, 5, 1, "Value"),
                new ParameterDef("SP05", 0, 10, 2, 1, "Value"),
                new ParameterDef("SP06", 0, 10, 2, 1, "Value"),
                new ParameterDef("SP07", 10, 300, 10, 10, "Value"),
                new ParameterDef("SP08", 0, 300, 90, 10, "Value"),
                new ParameterDef("SP09", 1, 9, 1, 1, "Value"),
                new ParameterDef("SP10", 1, 9, 2, 1, "Value"),
                new ParameterDef("SP11", 0.5, 5, 0.5, 0.1, "Value"),
                new ParameterDef("SP12", 100, 600, 450, 10, "Value"),

                new ParameterDef("ST11", 0, 1438, 480, 1, "Time"),
                new ParameterDef("ST12", 1, 1439, 510, 1, "Time"),
                new ParameterDef("ST21", 0, 1438, 480, 1, "Time"),
                new ParameterDef("ST22", 1, 1439, 510, 1, "Time"),
                new ParameterDef("ST31", 0, 1438, 480, 1, "Time"),
                new ParameterDef("ST32", 1, 1439, 510, 1, "Time"),
                new ParameterDef("ST41", 0, 1438, 480, 1, "Time"),
                new ParameterDef("ST42", 1, 1439, 510, 1, "Time"),
                new ParameterDef("ST51", 0, 1438, 480, 1, "Time"),
                new ParameterDef("ST52", 1, 1439, 510, 1, "Time"),
                new ParameterDef("ST61", 0, 1438, 480, 1, "Time"),
                new ParameterDef("ST62", 1, 1439, 510, 1, "Time"),
        };

        SP_Val_H2 = new ParameterDef[]{
                new ParameterDef("SP01", 1, 6, 3, 1, "Value"),
                new ParameterDef("SP02", 0.2, 5, 0.2, 0.1, "Value"),
                new ParameterDef("SP03", 100, 1000, 500, 50, "Value"),
                new ParameterDef("SP04", 1, 2000, 5, 1, "Value"),
                new ParameterDef("SP05", 0, 10, 2, 1, "Value"),
                new ParameterDef("SP06", 0, 10, 2, 1, "Value"),
                new ParameterDef("SP07", 10, 300, 10, 10, "Value"),
                new ParameterDef("SP08", 0, 300, 90, 10, "Value"),
                new ParameterDef("SP09", 1, 5, 1, 1, "Value"),
                new ParameterDef("SP10", 1, 5, 2, 1, "Value"),
                new ParameterDef("SP11", 1, 5, 1, 1, "Value"),
                new ParameterDef("SP12", 1, 5, 2, 1, "Value"),
                new ParameterDef("SP13", 0.5, 5, 0.5, 0.1, "Value"),
                new ParameterDef("SP14", 100, 600, 450, 10, "Value"),
                new ParameterDef("SP15", 0.1, 2, 1, 0.05, "Value"),
                new ParameterDef("SP16", 2, 2, 2, 1, "Value"),
                new ParameterDef("SP17", 0.5, 15, 2.5, 0.1, "Value"),
                new ParameterDef("SP18", 1, 600, 10, 1, "Value"),
                new ParameterDef("SP19", 0, 600, 10, 1, "Value"),

                new ParameterDef("ST11", 0, 1438, 480, 1, "Time"),
                new ParameterDef("ST12", 1, 1439, 510, 1, "Time"),
                new ParameterDef("ST21", 0, 1438, 480, 1, "Time"),
                new ParameterDef("ST22", 1, 1439, 510, 1, "Time"),
                new ParameterDef("ST31", 0, 1438, 480, 1, "Time"),
                new ParameterDef("ST32", 1, 1439, 510, 1, "Time"),
                new ParameterDef("ST41", 0, 1438, 480, 1, "Time"),
                new ParameterDef("ST42", 1, 1439, 510, 1, "Time"),
                new ParameterDef("ST51", 0, 1438, 480, 1, "Time"),
                new ParameterDef("ST52", 1, 1439, 510, 1, "Time"),
                new ParameterDef("ST61", 0, 1438, 480, 1, "Time"),
                new ParameterDef("ST62", 1, 1439, 510, 1, "Time"),
        };

        SP_Val = SP_Val_H2;

        HP_Val_H1 = new ParameterDef[] {
                new ParameterDef("HP01", "YYLL0001".getBytes()),
                new ParameterDef("HP02", "CAR-01".getBytes()),
                new ParameterDef("HP03", default_ble_id),
                new ParameterDef("HP04", default_location),
                new ParameterDef("HP05", default_fw_ver),
                new ParameterDef("HP06", "YYYY-MM-DD".getBytes()),
                new ParameterDef("HP07", "YYYY-MM-DD".getBytes()),
                new ParameterDef("HP08", default_ble_id),
                new ParameterDef("HP09", app_auth.getPublicKeyBytes(R.raw.publickey_clinician)),
                new ParameterDef("HP10", default_fw_ver),
                new ParameterDef("HP11", default_ble_id),
                new ParameterDef("HP12", app_auth.getPublicKeyBytes(R.raw.publickey_patient)),
                new ParameterDef("HP13", default_fw_ver),
                new ParameterDef("HP14", 10, 600, 60, 10, "Value"),
                new ParameterDef("HP15", 10, 600, 300, 10, "Value"),
                new ParameterDef("HP16", 10, 600, 10, 10, "Value"),
                new ParameterDef("HP17", 1, 600, 60, 1, "Value"),
                new ParameterDef("HP18", 1, 96, 24, 1, "Value"),
                new ParameterDef("HP19", 1, 96, 24, 1, "Value"),
                //new ParameterDef("HP20", 1, 60, 24, 1, "Value"),
                //new ParameterDef("HP21", 1, 60, 40, 1, "Value"),
                new ParameterDef("HP22", 1, 60, 2, 1, "Value"),
                new ParameterDef("HP23", 1, 60, 6, 1, "Value"),
                new ParameterDef("HP24", 1, 60, 9, 1, "Value"),
                new ParameterDef("HP25", 1, 60, 13, 1, "Value"),
                new ParameterDef("HP26", 2.0, 4.0, 2.6, 0.1, "Value"),
                new ParameterDef("HP27", 2.0, 4.0, 2.5, 0.1, "Value"),
                new ParameterDef("HP28", 1, 4, 1, 1, "Value"),
                new ParameterDef("HP29", 1, 600, 60, 1, "Value"),
                new ParameterDef("HP30", 60, 60, 60, 1, "Value"),
        };

        HP_Val_H2 = new ParameterDef[] {
                new ParameterDef("HP01", "YYLL0001".getBytes()),
                new ParameterDef("HP02", "ON-01".getBytes()),
                new ParameterDef("HP03", default_ble_id),
                new ParameterDef("HP04", default_location),
                new ParameterDef("HP05", default_fw_ver),
                new ParameterDef("HP06", "YYYY-MM-DD".getBytes()),
                new ParameterDef("HP07", "YYYY-MM-DD".getBytes()),
                new ParameterDef("HP08", default_ble_id),
                new ParameterDef("HP09", app_auth.getPublicKeyBytes(R.raw.publickey_clinician)),
                new ParameterDef("HP10", default_fw_ver),
                new ParameterDef("HP11", default_ble_id),
                new ParameterDef("HP12", app_auth.getPublicKeyBytes(R.raw.publickey_patient)),
                new ParameterDef("HP13", default_fw_ver),
                new ParameterDef("HP14", 10, 600, 60, 10, "Value"),
                new ParameterDef("HP15", 10, 600, 300, 10, "Value"),
                new ParameterDef("HP16", 10, 600, 10, 10, "Value"),
                new ParameterDef("HP17", 1, 600, 60, 1, "Value"),
                new ParameterDef("HP18", 1, 96, 24, 1, "Value"),
                new ParameterDef("HP19", 1, 96, 24, 1, "Value"),
                //new ParameterDef("HP20", 1, 60, 24, 1, "Value"),
                //new ParameterDef("HP21", 1, 60, 40, 1, "Value"),
                new ParameterDef("HP22", 1, 60, 2, 1, "Value"),
                new ParameterDef("HP23", 1, 60, 6, 1, "Value"),
                new ParameterDef("HP24", 1, 60, 9, 1, "Value"),
                new ParameterDef("HP25", 1, 60, 13, 1, "Value"),
                new ParameterDef("HP26", 2.0, 4.0, 2.6, 0.1, "Value"),
                new ParameterDef("HP27", 2.0, 4.0, 2.5, 0.1, "Value"),
                new ParameterDef("HP28", 1, 4, 1, 1, "Value"),
                new ParameterDef("HP29", 1, 600, 60, 1, "Value"),
                new ParameterDef("HP30", 60, 60, 60, 1, "Value"),
        };

        HP_Val = HP_Val_H2;

        privateKey_admin     = app_auth.getPrivateKey(R.raw.privatekey_pkcs8_admin);
        privateKey_clinician = app_auth.getPrivateKey(R.raw.privatekey_pkcs8_clinician);
        privateKey_patient   = app_auth.getPrivateKey(R.raw.privatekey_pkcs8_patient);
    }

    public void setHWver(byte ver) {
        switch (ver) {
            case (byte) 15:
                SP_Val = SP_Val_H1;
                HP_Val = HP_Val_H1;
                break;

            case (byte) 21:
                SP_Val = SP_Val_H2;
                HP_Val = HP_Val_H2;
                break;

            default:
                break;
        }
    }

    public byte[] toByteLSB(int value, int bytes) {
        byte[] raw = ByteBuffer.allocate(4).putInt(value).array();
        byte[] ret = new byte[bytes];
        for (int i = 0; i< bytes; i++) {
            ret[i] = raw[4-i-1];
        }
        return ret;
    }

    private byte[] getCRC16(byte[] data, int len)
    {
        char crc = 0xFFFF;
        for (int i = 0; i < len; i++)
        {
            crc ^= (char)(data[i] << 8);
            for (int j = 0; j < 8; j++)
            {
                if ((crc & 0x8000) > 0)
                    crc = (char)((crc << 1) ^ 0x1021);
                else
                    crc <<= 1;
            }
        }
        return toByteLSB(crc,2);
    }

    public byte[] byteMerger(byte[] bt1, byte[] bt2){
        byte[] bt3 = new byte[bt1.length+bt2.length];
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);
        return bt3;
    }

    public byte[] authCommand(int userClassStrID) {
        byte[] authData;
        byte[] message = new byte[100];
        new Random().nextBytes(message);

        try {
            byte[] digest               = app_auth.getDigest(message);
            byte[] signature_admin      = app_auth.getSignatureRS(privateKey_admin, message);
            byte[] signature_clinician  = app_auth.getSignatureRS(privateKey_clinician, message);
            byte[] signature_patient    = app_auth.getSignatureRS(privateKey_patient, message);
            if (userClassStrID == R.string.action_selectAdmin) {
                authData = byteMerger(signature_admin, digest);
                authData = byteMerger(authData, default_fw_ver);
                authData = byteMerger(authData, default_ble_id);
                return generateRequestCommand(opcode_Auth, authData);
            }
            else if (userClassStrID == R.string.action_selectClinician) {
                authData = byteMerger(signature_clinician, digest);
                authData = byteMerger(authData, default_fw_ver);
                authData = byteMerger(authData, default_ble_id);
                return generateRequestCommand(opcode_Auth, authData);
            }
            else if (userClassStrID == R.string.action_selectPatient) {
                authData = byteMerger(signature_patient, digest);
                authData = byteMerger(authData, default_fw_ver);
                authData = byteMerger(authData, default_ble_id);
                return generateRequestCommand(opcode_Auth, authData);
            }
        } catch (Exception ignored) {

        }
        return null;
    }

    public byte[] generateRequestCommand(byte opcode, byte[] payload) {
        int payloadLen = 0;
        if (payload != null)
            payloadLen = payload.length;

        byte[] cmd = new byte[requestHeaderLen + payloadLen + crcLen];
        cmd[0] = opcode;
        cmd[1] = (byte) payloadLen;
        if (payload != null) {
            System.arraycopy(payload, 0, cmd, requestHeaderLen, payloadLen);
        }
        byte[] crc = getCRC16(cmd, cmd.length - crcLen);
        System.arraycopy(crc, 0, cmd, cmd.length - crcLen, crcLen);
        return cmd;
    }

    public byte[] parseResponseCommand(byte[] cmd, App_UI app_ui) {
        byte[] ret = null;
        byte[] crc = getCRC16(cmd, cmd.length - crcLen);
        if (cmd[cmd.length-2] == crc[0] && cmd[cmd.length-1] == crc[1]) {
            byte opcode = cmd[0];
            int payloadLen = Byte.toUnsignedInt(cmd[1]);
            byte status = cmd[2];
            int payloadOffset = responseHeaderLen;
            byte[] payload = Arrays.copyOfRange(cmd, payloadOffset, payloadOffset + payloadLen);
            String msg;
            switch (status) {
                case STATUS_SUCCESS:
                    msg = "SUCCESS";
                    break;

                case STATUS_INVALID:
                    msg = "INVALID";
                    break;

                case STATUS_CRC_ERR:
                    msg = "CRC_ERR";
                    break;

                case STATUS_PAYLOAD_LEN_ERR:
                    msg = "PAYLOAD_LEN_ERR";
                    break;

                case STATUS_OPCODE_ERR:
                    msg = "OPCODE_ERR";
                    break;

                case STATUS_USER_CLASS_ERR:
                    msg = "USER_CLASS_ERR";
                    break;

                default:
                    msg = "UNKNOWN";
                    break;
            }

            switch (opcode) {
                case opcode_Auth:
                    if (status == STATUS_SUCCESS) {
                        if (payload[0] == (byte)0xFF)
                            ret = generateRequestCommand(opcode_ReadBleAdvance, null);
                    }
                    break;

                case opcode_ReadBleAdvance:
                    if (status == STATUS_SUCCESS)
                        app_ui.app_ui_user_class.onReadBleAdvance(payload);
                    break;

                case opcode_ReadStimulationParameters:
                    if (status == STATUS_SUCCESS)
                        app_ui.app_ui_user_class.onReadSP(payload);
                    break;

                case opcode_ReadHardwareParameters:
                    if (status == STATUS_SUCCESS)
                        app_ui.app_ui_user_class.onReadHP(payload);
                    break;

                case opcode_MeasureImpedance:
                    if (status == STATUS_SUCCESS)
                        app_ui.app_ui_user_class.onMeasureImpedance(payload);
                    break;

                case opcode_MeasureBatteryVoltage:
                    if (status == STATUS_SUCCESS)
                        app_ui.app_ui_user_class.onMeasureBatteryVoltage(payload);
                    break;

                case opcode_MeasureSensorVoltage:
                    if (status == STATUS_SUCCESS)
                        app_ui.app_ui_user_class.onMeasureSensorVoltage(payload);
                    break;

                case opcode_ReadIPGLog:
                    if (status == STATUS_SUCCESS)
                        app_ui.app_ui_user_class.onReadLog(payload);
                    break;

                case opcode_EraseIPGLog:
                    if (status == STATUS_SUCCESS)
                        app_ui.app_ui_user_class.onEraseIPGLog();
                    break;

                case opcode_ReadTimeDate:
                    if (status == STATUS_SUCCESS)
                        app_ui.app_ui_user_class.onReadTimeDate(payload);
                    break;

                case opcode_AuthFwImage:
                    app_ui.app_ui_user_class.onAuthFwImage(status);
                    break;

                case opcode_DownloadFwImage:
                    if (status == STATUS_SUCCESS) {
                        fw_image_offset = Math.min(fw_image_offset + fw_image_packet_size, app_file.fw_image.length);
                    }
                    if (fw_image_offset < app_file.fw_image.length) {
                        byte[] offsetBytes = toByteLSB(fw_image_offset, 4);
                        byte[] fw_packet = new byte[fw_image_packet_size];
                        System.arraycopy(app_file.fw_image,fw_image_offset, fw_packet, 0, Math.min(fw_image_packet_size, app_file.fw_image.length - fw_image_offset));
                        payload = byteMerger(offsetBytes, fw_packet);
                        ret = generateRequestCommand(opcode, payload);
                    }
                    break;

                case opcode_VerifyFwImage:
                    app_ui.app_ui_user_class.onVerifyFwImage(status);
                    break;

                case opcode_SetStartState:
                    
                    break;

                case opcode_StartAcc:
                    if (status == STATUS_SUCCESS) {
                        app_ui.app_ui_user_class.onStartAcc(payload);
                        if (((SwitchCompat) mActivity.findViewById(R.id.switchAutoGetAccelerometer)).isChecked()) {
                            byte[] devID_start = {payload[0]};
                            ret = generateRequestCommand(opcode_GetDataAcc, devID_start);
                        }
                    }

                    String[] strSTATUS_START_ACC = {
                            "SUCCESS",
                            "START_ACC_BUFFER_OVERFLOW",
                            "START_ACC_COMMUNICTION_ERR",
                            "START_ACC_DEVICE_NOT_READY",
                            "START_ACC_INVALID_CONFIGURATION",
                    };
                    msg = strSTATUS_START_ACC[status];

                    break;

                case opcode_GetDataAcc:
                    if (    status == STATUS_SUCCESS ||
                            status == STATUS_GET_ACC_BUFFER_OVERFLOW ||
                            status == STATUS_GET_ACC_BUFFER_EMPTY   ) {
                        app_ui.app_ui_user_class.onGetDataAcc(payload);
                        if (((SwitchCompat) mActivity.findViewById(R.id.switchAutoGetAccelerometer)).isChecked()) {
                            byte[] devID_get = {payload[0]};
                            ret = generateRequestCommand(opcode_GetDataAcc, devID_get);
                        }
                    }

                    String[] STATUS_GET_ACC = {
                            "SUCCESS",
                            "GET_ACC_BUFFER_OVERFLOW",
                            "GET_ACC_BUFFER_EMPTY",
                            "GET_ACC_STARTUP_NOT_PERFORMED",
                            "GET_ACC_COMMUNICTION_ERR",
                    };
                    msg = STATUS_GET_ACC[status];

                    break;

                case opcode_StopAcc:

                    break;
            }
            if (status == STATUS_SUCCESS) {
                if (    opcode != opcode_DownloadFwImage &&
                        opcode != opcode_MeasureSensorVoltage &&
                        opcode != opcode_GetDataAcc ) {
                    app_ui.app_ui_user_class.onStatus(status, msg);
                }
            }
            else {
                app_ui.app_ui_user_class.onStatus(status, msg);
            }
        }

        return ret;
    }

    public String getSPDefineStr (int position) {
        return SP_Val[position].getDefStr();
    }

    public String getHPDefineStr (int position) {
        return HP_Val[position].getDefStr();
    }

    public String getSPDataFormat (int position) {
        return SP_Val[position].format;
    }

    public String getHPDataFormat (int position) {
        return HP_Val[position].format;
    }

    private byte[] floatToBytes(float value) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putFloat(value);
        return buffer.array();
    }

    @SuppressLint("NonConstantResourceId")
    public byte[] onClick(View view) {
        int viewID = view.getId();

        byte opcode;
        byte[] payload = null;

        int position;
        byte[] ID;
        String val_str;
        int val;
        byte[] val_byte;

        if (viewID == R.id.imageButtonBleAdvanceSave) {
            opcode = opcode_WriteBleAdvance;
            String pairing_code_str = ((EditText) mActivity.findViewById(R.id.editTextBlePairingCode)).getText().toString();
            if (pairing_code_str.isEmpty()) {
                pairing_code_str = ((EditText) mActivity.findViewById(R.id.editTextBlePairingCode)).getHint().toString();
            }

            byte[] pairing_code = pairing_code_str.getBytes();
            byte whitelist = ((SwitchCompat) mActivity.findViewById(R.id.switchBleWhitelist)).isChecked() ? (byte) 0x01 : (byte) 0x00;
            payload = new byte[pairing_code.length + 1];
            System.arraycopy(pairing_code, 0, payload, 0, pairing_code.length);
            payload[pairing_code.length] = whitelist;

        } else if (viewID == R.id.imageButtonScheduledTherapyStart) {
            opcode = opcode_StartScheduledTherapySession;
            if (mActivity.findViewById(R.id.switchVNSb).isEnabled()) {
                payload = new byte[1];
                payload[0] = ((SwitchCompat) mActivity.findViewById(R.id.switchVNSb)).isChecked() ? (byte) 0x01 : (byte) 0x00;
            }
        } else if (viewID == R.id.imageButtonScheduledTherapyStop) {
            opcode = opcode_EndScheduledTherapySession;

        } else if (viewID == R.id.imageButtonManualTherapyStart) {
            opcode = opcode_StartManualTherapySession;
            if (mActivity.findViewById(R.id.switchVNSb).isEnabled()) {
                payload = new byte[1];
                payload[0] = ((SwitchCompat) mActivity.findViewById(R.id.switchVNSb)).isChecked() ? (byte) 0x01 : (byte) 0x00;
            }
        } else if (viewID == R.id.imageButtonManualTherapyStop) {
            opcode = opcode_StopManualTherapySession;

        } else if (viewID == R.id.buttonReadSP) {
            opcode = opcode_ReadStimulationParameters;
            position = ((Spinner) mActivity.findViewById(R.id.spinnerStimulationParametersID)).getSelectedItemPosition();
            ID = SP_Val[position].id;
            payload = ID;

        } else if (viewID == R.id.buttonWriteSP) {
            opcode = opcode_WriteStimulationParameters;
            position = ((Spinner) mActivity.findViewById(R.id.spinnerStimulationParametersID)).getSelectedItemPosition();
            ID = SP_Val[position].id;

            if (!getSPDataFormat(position).equals("Data")) {
                val_str = ((EditText) mActivity.findViewById(R.id.editTextStimulationParameters)).getText().toString();
                if (!val_str.isEmpty()) {
                    val = Integer.parseInt(val_str);
                    val_byte = toByteLSB(val, 2);
                    payload = new byte[ID.length + val_byte.length];
                    System.arraycopy(ID, 0, payload, 0, ID.length);
                    System.arraycopy(val_byte, 0, payload, ID.length, val_byte.length);
                } else {
                    payload = new byte[ID.length];
                    System.arraycopy(ID, 0, payload, 0, ID.length);
                }
            }

        } else if (viewID == R.id.buttonReadHP) {
            opcode = opcode_ReadHardwareParameters;
            position = ((Spinner) mActivity.findViewById(R.id.spinnerHardwareParametersID)).getSelectedItemPosition();
            ID = HP_Val[position].id;
            payload = ID;

        } else if (viewID == R.id.buttonWriteHP) {
            opcode = opcode_WriteHardwareParameters;
            position = ((Spinner) mActivity.findViewById(R.id.spinnerHardwareParametersID)).getSelectedItemPosition();
            ID = HP_Val[position].id;

            if (!getHPDataFormat(position).equals("Data")) {
                val_str = ((EditText) mActivity.findViewById(R.id.editTextHardwareParameters)).getText().toString();
                if (!val_str.isEmpty()) {
                    val = Integer.parseInt(val_str);
                    val_byte = toByteLSB(val, 2);
                    payload = new byte[ID.length + val_byte.length];
                    System.arraycopy(ID, 0, payload, 0, ID.length);
                    System.arraycopy(val_byte, 0, payload, ID.length, val_byte.length);
                } else {
                    payload = new byte[ID.length];
                    System.arraycopy(ID, 0, payload, 0, ID.length);
                }
            } else {
                String str = ((Spinner) mActivity.findViewById(R.id.spinnerHardwareParametersRawData)).getSelectedItem().toString();
                byte[] RawData = new byte[str.length() / 2];
                for (int i = 0; i < str.length(); i += 2) {
                    RawData[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
                }
                payload = new byte[ID.length + RawData.length];
                System.arraycopy(ID, 0, payload, 0, ID.length);
                System.arraycopy(RawData, 0, payload, ID.length, RawData.length);
            }

        } else if (viewID == R.id.buttonShutdownSystem) {
            opcode = opcode_ShutdownSystem;

        } else if (viewID == R.id.buttonRebootSystem) {
            opcode = opcode_RebootSystem;

        } else if (viewID == R.id.buttonBleDisconnectRequest) {
            opcode = opcode_BleDisconnectRequest;

        } else if (viewID == R.id.buttonMeasureImpedance) {
            opcode = opcode_MeasureImpedance;

        } else if (viewID == R.id.buttonMeasureBatteryVoltage) {
            opcode = opcode_MeasureBatteryVoltage;

        } else if (viewID == R.id.toggleButtonMeasureSensorVoltage) {
            ToggleButton toggleButton = (ToggleButton) view;
            boolean isChecked = toggleButton.isChecked();
            opcode = opcode_MeasureSensorVoltage;
            int senID = ((Spinner) mActivity.findViewById(R.id.spinnerMeasureSensorID)).getSelectedItemPosition() + 1;
            float freq = ((SeekBar)mActivity.findViewById(R.id.seekBarMeasureSensorFreq)).getProgress() * 0.1f;
            byte[] byteFreq = floatToBytes(freq);
            if (isChecked) {
                payload = new byte[5];
                payload[0] = (byte) senID;
                payload[1] = byteFreq[0];
                payload[2] = byteFreq[1];
                payload[3] = byteFreq[2];
                payload[4] = byteFreq[3];
            }
            else {
                payload = new byte[1];
            }
        } else if (viewID == R.id.buttonReadLog) {
            opcode = opcode_ReadIPGLog;
            String strDateTime = ((TextView) mActivity.findViewById(R.id.textViewLogTimeStamp)).getText().toString();
            String[] str_date = strDateTime.split("T")[0].split("-");
            String[] str_time = strDateTime.split("T")[1].split(":");
            payload = new byte[7];
            payload[0] = Integer.valueOf(str_date[0].substring(2)).byteValue();
            payload[1] = Integer.valueOf(str_date[1]).byteValue();
            payload[2] = Integer.valueOf(str_date[2]).byteValue();
            payload[3] = Integer.valueOf(str_time[0]).byteValue();
            payload[4] = Integer.valueOf(str_time[1]).byteValue();
            payload[5] = Integer.valueOf(str_time[2].substring(0, 2)).byteValue();
            payload[6] = Integer.valueOf(str_time[2].substring(4, 7)).byteValue();

        } else if (viewID == R.id.buttonEraseLog) {
            opcode = opcode_EraseIPGLog;

        } else if (viewID == R.id.buttonReadTimeDate) {
            opcode = opcode_ReadTimeDate;

        } else if (viewID == R.id.buttonWriteTimeDate) {
            opcode = opcode_WriteTimeDate;
            byte Year = (byte) (Calendar.getInstance().get(Calendar.YEAR) - 2000);
            byte Month = (byte) (Calendar.getInstance().get(Calendar.MONTH) + 1);
            byte Day = (byte) Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            byte Hour = (byte) Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            byte Minute = (byte) Calendar.getInstance().get(Calendar.MINUTE);
            byte Second = (byte) Calendar.getInstance().get(Calendar.SECOND);
            payload = new byte[]{Year, Month, Day, Hour, Minute, Second};

        } else if (viewID == R.id.imageButtonAuthFwImage) {
            opcode = opcode_AuthFwImage;
            try {
                fw_image_size = app_file.fw_image.length;
                fw_digest = app_auth.getDigest(app_file.fw_image);
                fw_signature_admin = app_auth.getSignatureRS(privateKey_admin, app_file.fw_image);
            } catch (Exception ignored) {

            }
            payload = byteMerger(fw_signature_admin, fw_digest);

        } else if (viewID == R.id.imageButtonDownloadFwImage) {
            opcode = opcode_DownloadFwImage;
            fw_image_offset = 0;
            byte[] offsetBytes = toByteLSB(fw_image_offset, 4);
            payload = byteMerger(offsetBytes, Arrays.copyOfRange(app_file.fw_image, fw_image_offset, fw_image_offset + fw_image_packet_size));

        } else if (viewID == R.id.imageButtonVerifyFwImage) {
            opcode = opcode_VerifyFwImage;
            payload = toByteLSB(app_file.fw_image.length, 4);

        } else if (viewID == R.id.switchDvtTitle) {
            opcode = opcode_SetStartState;
            if (((SwitchCompat)view).isChecked()) {
                payload = toByteLSB(STATE_ACT_MODE_DVT, 2);
            } else {
                payload = toByteLSB(STATE_ACT_MODE_BLE_ACT, 2);
            }

        } else if (viewID == R.id.buttonStartAccelerometer) {
            opcode = opcode_StartAcc;
            String freqStr = ((Spinner) mActivity.findViewById(R.id.spinnerAccelerometerFrequency)).getSelectedItem().toString();
            String numStr = freqStr.replaceAll("[^0-9]", "");
            byte freq = (byte) Integer.parseInt(numStr);
            byte mode = (byte) (((Spinner) mActivity.findViewById(R.id.spinnerAccelerometerMode)).getSelectedItemPosition() * 0x80);
            byte devID = (byte) (((Spinner) mActivity.findViewById(R.id.spinnerAccelerometerDeviceID)).getSelectedItemPosition() + accDevID);
            payload = new byte[]{ freq, (byte) (mode + devID)};

        } else if (viewID == R.id.buttonGetAccelerometer) {
            opcode = opcode_GetDataAcc;
            byte devID = (byte) (((Spinner) mActivity.findViewById(R.id.spinnerAccelerometerDeviceID)).getSelectedItemPosition() + accDevID);
            payload = new byte[]{ devID };

        } else if (viewID == R.id.buttonStopAccelerometer) {
            opcode = opcode_StopAcc;
            byte devID = (byte) (((Spinner) mActivity.findViewById(R.id.spinnerAccelerometerDeviceID)).getSelectedItemPosition() + accDevID);
            payload = new byte[]{ devID };
            ((SwitchCompat) mActivity.findViewById(R.id.switchAutoGetAccelerometer)).setChecked(false);

        } else {
            return null;
        }


        return generateRequestCommand(opcode, payload);
    }
}
