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
import java.security.SecureRandom;
import java.util.Arrays;

public class ScryptKeyExporter {

    private EditText passwordEditText;
    private EditText passwordConfirmEditText;
    private byte [] tempPassword;
    SecureRandom random = new SecureRandom();
    private byte[] salt;
    private int N, r, p, outputLen;
    private byte [] derivedKey;

    static {
        System.loadLibrary("scrypt_jni");
    }

    private native byte[] nativeScrypt(byte[] password, byte[] salt, int N, int r, int p, int outLen);

    /**
     * TODO: Need to support more various parameter
     *
     */
    public ScryptKeyExporter(byte [] salt, int N, int r, int p, int outputLen){
        this.salt = salt;
        this.N = N;
        this.r = r;
        this.p = p;
        this.outputLen = outputLen;
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
     * Initialize KeyExporter for Scrypt.
     *
     * It initialize Scrypt with user password.
     * After that, it cleans up the password.
     *
     * TODO: For real, it should be implemented generally to support different cases.
     */
    public void init() {

        tempPassword = new byte[passwordEditText.getText().length()];
        for (int i = 0; i < passwordEditText.getText().length(); i++) {
            tempPassword[i] = (byte) (passwordEditText.getText().charAt(i));
        }

        derivedKey = nativeScrypt(tempPassword, salt, N, r, p, outputLen);


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
     * Return derived value
     *
     * @return String of hash
     */
    public String getKey() {
        return bytesToHex(derivedKey);
    }

    public byte [] getKeyBytes() {
        return derivedKey;
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
