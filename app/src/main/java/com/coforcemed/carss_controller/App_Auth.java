package com.coforcemed.carss_controller;

import android.app.Activity;
import android.util.Log;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

public class App_Auth {
    protected Activity mActivity;
    public App_Auth(Activity activity, App_File appFile) {
        mActivity = activity;
        app_file = appFile;
    }
    private final App_File app_file;

    private PrivateKey readPKCS8PrivateKey(byte[] privateKeyBytes)
            throws Exception {

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePrivate(spec);
    }

    public PrivateKey getPrivateKey (int id) {
        PrivateKey privateKey = null;
        try {
            privateKey = readPKCS8PrivateKey(app_file.getRawFileBytes(id));
        } catch (Exception ignored) {

        }
        return privateKey;
    }

    public byte[] getPublicKeyBytes (int id) {
        byte[] publicKeyBytes = null;
        int offset = 27;
        try {
            publicKeyBytes = Arrays.copyOfRange(app_file.getRawFileBytes(id), offset, offset + 64);
        } catch (Exception ignored) {

        }
        return publicKeyBytes;
    }

    public byte[] getDigest(byte[] message) throws Exception {
        if (message == null)
            return null;

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] Digest = md.digest(message);

        StringBuilder hex_str = new StringBuilder();
        for (byte data : Digest) {
            hex_str.append(byteToHexStr(data).toUpperCase()).append(" ");
        }
        Log.i("Auth", "Digest: " + hex_str);
        return Digest;
    }

    public byte[] getSignatureRS(PrivateKey privateKey, byte[] message) throws Exception {
        if (message == null)
            return null;

        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initSign(privateKey);
        s.update(message);
        byte[] Signature = s.sign();

        int offsetR = 2 + 2 + (Signature[3] - 32);
        int offsetS = offsetR + 32 + 2 + (Signature[offsetR + 33] - 32);
        byte[] signatureR = Arrays.copyOfRange(Signature, offsetR, offsetR + 32);
        byte[] signatureS = Arrays.copyOfRange(Signature, offsetS, offsetS + 32);
        Signature = new byte[64];
        for(int i=0;i<32;i++) {
            Signature[i] = signatureR[i];
            Signature[i + 32] = signatureS[i];
        }

        StringBuilder hex_str = new StringBuilder();
        for (byte data : Signature) {
            hex_str.append(byteToHexStr(data).toUpperCase()).append(" ");
        }
        Log.i("Auth", "Signature: " + hex_str);
        return Signature;
    }

    private String byteToHexStr(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }
}
