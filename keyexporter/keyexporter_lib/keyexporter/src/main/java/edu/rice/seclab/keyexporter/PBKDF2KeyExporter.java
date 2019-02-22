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
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * KeyExporter prototype for PBKDF2
 *
 *
 */
public class PBKDF2KeyExporter {

    private String derivedKey = null;
    private EditText passwordEditText;
    private EditText passwordConfirmEditText;
    private char [] tempPassword;
    SecureRandom random = new SecureRandom();

    // Default salt
    byte[] defalutSalt = new byte [] {'s', 'a', 'l', 't'};

    /**
     * TODO: iterations number is fixed in this code.
     * For the real usage, this value can be configurable.
     */
    static final int iterations = 1000;

    /**
     * Wrapping the password widget with KeyExporter
     *
     * @param passwordWidget
     */
    public void bind(EditText passwordWidget) {
        // TODO: Check etUsername
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
     * Initialize KeyExporter with the default salt.
     *
     * It generates the hash value of <iterations + ":" + salt + ":" + hash>
     * After that, it cleans up the password.
     *
     * TODO: It only supports specific string type of PBKDF2 hash for experimental purpose.
     * For real, it should be implemented generally to support different types.
     */
    public void init() {

        tempPassword = new char[passwordEditText.getText().length()];
        for (int i = 0; i < passwordEditText.getText().length(); i++) {
            tempPassword[i] = (passwordEditText.getText().charAt(i));
        }

        try {

            PBEKeySpec spec = new PBEKeySpec(tempPassword, defalutSalt, iterations, 64 * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            spec.clearPassword();
            derivedKey = iterations + ":" + toHex(defalutSalt) + ":" + toHex(hash);
        } catch (Exception e){
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
        Arrays.fill(tempPassword, (char) 0xA5A5);
        Arrays.fill(tempPassword, (char) 0x5A5A);
        Arrays.fill(tempPassword, (char) 0);
        for (int i = 0; i < tempPassword.length; i++) {
            tempPassword[i] = (char) random.nextInt();      // prevent optimization
        }
    }

    /**
     * Initialize KeyExporter with a salt.
     * It generates the hash value of <iterations + ":" + salt + ":" + hash>
     * After that, it cleans up the password.
     *
     * TODO: It only supports specific string type of PBKDF2 hash for experimental purpose.
     * For real, it should be implemented generally to support different cases.
     *
     * @param salt
     */
    public void initWithSalt(byte [] salt) {

        tempPassword = new char[passwordEditText.getText().length()];
        for (int i = 0; i < passwordEditText.getText().length(); i++) {
            tempPassword[i] = (passwordEditText.getText().charAt(i));
        }

        try {
            PBEKeySpec spec = new PBEKeySpec(tempPassword, salt, iterations, 64 * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            derivedKey = iterations + ":" + toHex(salt) + ":" + toHex(hash);
        } catch (Exception e){
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
        Arrays.fill(tempPassword, (char) 0xA5A5);
        Arrays.fill(tempPassword, (char) 0x5A5A);
        Arrays.fill(tempPassword, (char) 0);
        for (int i = 0; i < tempPassword.length; i++) {
            tempPassword[i] = (char) random.nextInt();      // prevent optimization
        }
    }

    /**
     * Return hash value as String
     *
     * @return hash value
     */
    public CharSequence getKey() {
        return derivedKey;
    }


    /**
     * Utility method. return random salt value for SHA1PRNG
     *
     * @return Random salt
     * @throws NoSuchAlgorithmException
     */
    public static byte[] getSalt() throws NoSuchAlgorithmException
    {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt;
    }

    /**
     * Utility method. byte array to String
     * TODO: This method should be depreciated in the future to avoid String usage
     *
     * @param array
     * @return
     */
    private static String toHex(byte[] array)
    {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if(paddingLength > 0)
        {
            return String.format("%0"  +paddingLength + "d", 0) + hex;
        }else{
            return hex;
        }
    }

    /**
     * Utility method.
     *
     * @param hex
     * @return
     */
    private static byte[] fromHex(String hex)
    {
        byte[] bytes = new byte[hex.length() / 2];
        for(int i = 0; i<bytes.length ;i++)
        {
            bytes[i] = (byte)Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

    /**
     * Utility method. To validate password.
     * TODO: This method is not tested
     *
     * @param originalPassword
     * @param storedPassword
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private static boolean validatePassword(String originalPassword, String storedPassword) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        String[] parts = storedPassword.split(":");
        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = fromHex(parts[1]);
        byte[] hash = fromHex(parts[2]);

        PBEKeySpec spec = new PBEKeySpec(originalPassword.toCharArray(), salt, iterations, hash.length * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] testHash = skf.generateSecret(spec).getEncoded();

        int diff = hash.length ^ testHash.length;
        for(int i = 0; i < hash.length && i < testHash.length; i++)
        {
            diff |= hash[i] ^ testHash[i];
        }
        return diff == 0;
    }


}
