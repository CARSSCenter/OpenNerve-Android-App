package com.coforcemed.carss_controller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class App_File {
    protected Activity mActivity;
    public App_File(Activity activity) {
        mActivity = activity;
    }

    public byte[] fw_image = null;
    public String fw_image_name = "-----";

    public static final int OPEN_FW_IMAGE_FILE = 1;

    public final int fw_image_max_size = 0x40000;

    private byte[] readData(Uri uri) {
        try {
            ParcelFileDescriptor pfd = mActivity.getContentResolver().
                    openFileDescriptor(uri, "r");
            assert pfd != null;
            FileInputStream fileInputStream =
                    new FileInputStream(pfd.getFileDescriptor());
            byte[] buff = new byte[fileInputStream.available()];
            int len = fileInputStream.read(buff);
            Log.d("FILE", "Read data length = " + len);
            fileInputStream.close();
            pfd.close();
            return buff;
        } catch (IOException e) {
            return null;
        }
    }

    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        Log.d("FILE", "requestCode: " + requestCode + ", resultCode: " + resultCode);
        if (requestCode == OPEN_FW_IMAGE_FILE && resultCode == Activity.RESULT_OK) {
            Log.d("FILE", "OPEN_FW_IMAGE_FILE");
            if (resultData != null) {
                Uri fileUri = resultData.getData();
                fw_image = readData(fileUri);
                assert fw_image != null;
                if (fw_image.length > fw_image_max_size) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                    builder.setMessage("Image size is too large.")
                            .setPositiveButton("OK", null)
                            .show();
                }
                assert fileUri != null;
                DocumentFile documentFile = DocumentFile.fromSingleUri(mActivity, fileUri);
                assert documentFile != null;
                fw_image_name = documentFile.getName();
                Log.d("FILE", "fw_image_name: " + fw_image_name);
            }
        }
    }

    public void searchFwImageFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        //intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.setType("application/octet-stream");

        mActivity.startActivityForResult(intent, OPEN_FW_IMAGE_FILE);
    }

    public byte[] getRawFileBytes(int id) throws Exception {
        InputStream inStream = mActivity.getResources().openRawResource(id);
        BufferedInputStream bis = new BufferedInputStream(inStream, inStream.available());
        DataInputStream dis = new DataInputStream(bis);
        byte[] rawFileByteArray = new byte[inStream.available()];
        int i = 0;
        while (dis.available() > 0) {
            rawFileByteArray[i]=dis.readByte();
            i++;
        }
        return rawFileByteArray;
    }
}
