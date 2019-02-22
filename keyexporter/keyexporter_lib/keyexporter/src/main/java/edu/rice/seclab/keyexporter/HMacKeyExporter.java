/**
 *
 * The prototype of KeyExporter APIs.
 *
 * WARNING: This implementation is experimental and shows the prototype of our concept.
 * Thus, it provides the functionality in the basic scenario without error handling.
 * In order to use for real cryptographic purposes beyond testing,
 * additional engineer effort will be required such as supporting various parameters,
 * and error handling.
 *
 *
 *   Copyright (c) 2018,
 *     Jaeho Lee <jaeho.lee@rice.edu>
 *     Ang Chen <angchen@rice.edu>
 *     Dan S. Wallach <dwallach@rice.edu>
 *   All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * It is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 */

package edu.rice.seclab.keyexporter;

import android.widget.EditText;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;

/**
 * KeyExporter prototype for PBKDF2
 *
 *
 */
public class HMacKeyExporter {

    private EditText passwordEditText;
    private EditText passwordConfirmEditText;
    private byte [] tempPassword;
    SecureRandom random = new SecureRandom();
    private static final String defaultAlgorithm = "HmacSHA1";
    private String algorithm;
    Mac mac = null;


    public HMacKeyExporter(){
        this.algorithm = defaultAlgorithm;
    }

    /**
     * TODO: Need to support more various parameter
     *
     */
    public HMacKeyExporter(String algo){
        this.algorithm = algo;
    }

    /**
     * Wrapping the password widget with KeyExporter
     *
     * @param passwordWidget
     */
    public void bind(EditText passwordWidget) {
        passwordEditText = passwordWidget;
    }

    /**
     * Wrapping two password widgets with KeyExporter
     * This is for registration step where UI has two password inputs for confirmation.
     * TODO: This feature is not tested.
     *
     * @param passwordWidget
     * @param passwordWidget2
     */
    public void bind(EditText passwordWidget, EditText passwordWidget2) {
        passwordEditText = passwordWidget;
        passwordConfirmEditText = passwordWidget2;
    }

    /**
     * Initialize KeyExporter for HMac.
     *
     * It initialize Mac with user password.
     * After that, it cleans up the password.
     *
     * TODO: It only supports specific case of HMac usage.
     * For real, it should be implemented generally to support different cases.
     */
    public void init() {

        tempPassword = new byte[passwordEditText.getText().length()];
        for (int i = 0; i < passwordEditText.getText().length(); i++) {
            tempPassword[i] = (byte) (passwordEditText.getText().charAt(i));
        }

        try {
            mac = Mac.getInstance(this.algorithm);
            SecretKey key = new SecretKeySpec(tempPassword, this.algorithm);
            mac.init(key);
            key.destroy();
            key = null;
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeyException e1) {
            e1.printStackTrace();
        } catch (DestroyFailedException e) {
            e.printStackTrace();
        }

        // Clean up password
        passwordEditText.getText().clear();
        passwordEditText.setText("");

        if (passwordConfirmEditText != null){
            passwordConfirmEditText.getText().clear();
            passwordConfirmEditText.setText("");
        }

        // Three way overwriting
        Arrays.fill(tempPassword, (byte) 0xA5);
        Arrays.fill(tempPassword, (byte) 0x5A);
        Arrays.fill(tempPassword, (byte) 0);
        for (int i = 0; i < tempPassword.length; i++) {
            tempPassword[i] = (byte) random.nextInt();      // prevent optimization
        }
    }

    /**
     *
     * Update mac with data
     *
     * @param data data to update
     */
    public void update(byte [] data){
        mac.update(data);
    }

    /**
     *
     * Finalize mac and get value
     *
     * @return byte array of hash
     */
    public byte [] getKeyBytes() {
        Arrays.fill(tempPassword, (byte) 0xA5);
        Arrays.fill(tempPassword, (byte) 0x5A);
        for (int i = 0; i < tempPassword.length; i++) {
            tempPassword[i] = (byte) random.nextInt();      // prevent optimization
        }
        byte [] ret = mac.doFinal();
        mac.reset();
        mac = null;
        return ret;
    }


    /**
     *
     * Finalize mac and get value
     *
     * @return String of hash
     */
    public String getKey() {
        Arrays.fill(tempPassword, (byte) 0xA5);
        Arrays.fill(tempPassword, (byte) 0x5A);
        for (int i = 0; i < tempPassword.length; i++) {
            tempPassword[i] = (byte) random.nextInt();      // prevent optimization
        }

        byte [] ret = mac.doFinal();
        mac.reset();
        mac = null;
        return bytesToHex(ret);
    }

    /**
     *
     * Utility method.
     *
     */
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}