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

import android.util.Log;
import android.widget.EditText;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * KeyExporter prototype for supporting SRP protocol
 *
 * WARNNING: This source is for demo and do not provide any error handling.
 * For example, it assumes all inputs are correct, and all methods are called as proper order.
 * This experimental code will be crashed when unexpected parameter or incorrect order of method calls.
 *
 * Inspired by Nimbus-SRP library and pseudo code in http://srp.stanford.edu/design.html
 *
 *
 */
public class SRPKeyExporter {

    /**
     * TODO: Currently only support fixed value of N, g, H.
     * Need to engineer to support these value dynamically.
     */

    // Predefined N with safe 2048-bit prime 'N', as decimal. Origin RFC 5054, appendix A.
    private static final BigInteger N = new BigInteger("21766174458617435773191008891802753781907668374255538511144643224689886235383840957210909013086056401571399717235807266581649606472148410291413364152197364477180887395655483738115072677402235101762521901569820740293149529620419333266262073471054548368736039519702486226506248861060256971802984953561121442680157668000761429988222457090413873973970171927093992114751765168063614761119615476233422096442783117971236371647333871414335895773474667308967050807005509320424799678417036867928316761272274230314067548291133582479583061439577559347101961771406173684378522703483495337037655006751328447510550299250924469288819");

    // Predefined g
    private static final BigInteger g = BigInteger.valueOf(2);

    // H function
    private static final String H = "SHA-256";
    private static final int saltLength = 16;

    private static final String TAG = "SRPAuthenticatorLib";

    // salt value
    private BigInteger salt;

    // client random value for generating A
    private BigInteger a;
    private BigInteger A;
    private BigInteger B;
    private BigInteger M1;
    private BigInteger S;

    private EditText usernameEditText;
    private EditText passwordEditText;

    private SecureRandom random = new SecureRandom();

    // In SRP, derivedKey is hashed value from password which is used in generating X later with salt
    private byte [] derivedKey = null;

    byte [] tempPassword = null;


    public SRPKeyExporter()
    {
        // TODO: Support various options
    }


    /**
     *
     * Wrapping the password widget with KeyExporter.
     * For SRP, username is required, so it also binds with TextView for username
     *
     * @param usernameWidget
     * @param passwordWidget
     */
    public void bind(EditText usernameWidget, EditText passwordWidget) {
        usernameEditText = usernameWidget;
        passwordEditText = passwordWidget;
    }



    /**
     * Initialize KeyExporter.
     *
     * It performs all computation related with the password in advance.
     * After that, it cleans up the password.
     *
     */
    public void init() {

        tempPassword = new byte [ passwordEditText.getText().length() ];
        for (int i = 0; i < passwordEditText.getText().length(); i++){
            tempPassword[i] = (byte) (passwordEditText.getText().charAt(i));
        }
        MessageDigest digest = getMessageDigestInstance();
        derivedKey = digest.digest(tempPassword);

        // Clean up password
        passwordEditText.getText().clear();
        passwordEditText.setText("");

        // TODO: Other way
        Arrays.fill(tempPassword, (byte)0xA5);
        Arrays.fill(tempPassword, (byte)0x5A);
        Arrays.fill(tempPassword, (byte)0);
        for (int i = 0; i < tempPassword.length;i++){
            tempPassword[i] = (byte) random.nextInt();      // prevent optimization
        }
        for (int i = 0; i < derivedKey.length; i++) {
            Log.d(TAG, "derivedKey[" + i + "]: "  + ((byte)derivedKey[i]));
        }
    }

    /**
     * Generating Salt.
     *
     * This is for supporting the registration step.
     *
     */
    public void generateSalt()
    {
        if (salt != null) {
            throw new IllegalStateException("Salt is already set.");
        }
        byte [] saltBytes = new byte[saltLength];
        random.nextBytes(saltBytes);
        salt = bigIntegerFromBytes(saltBytes);
        Log.d(TAG, "salt generated: " +  salt.toString(16));

    }

    /**
     *
     * Calculating a verifier, and return it with String value.
     *
     *
     * @return
     */
    public String getVerifier() {

        if (derivedKey == null){
            throw new IllegalStateException("Before calculating M1, you should generate a derived key first using derivateKey().");
        }

        if (salt == null) {
            throw new IllegalStateException("Salt is not set. Please call generateSalt() first.");
        }

        // calculate x = H(s, H(password))
        for (int i = 0; i < bigIntegerToBytes(this.salt).length; i++) {
            Log.d(TAG, "salt[" + i + "]: "  + ((byte)bigIntegerToBytes(this.salt)[i]));
        }
        MessageDigest digest = getMessageDigestInstance();
        digest.update(bigIntegerToBytes(this.salt));
        digest.update(this.derivedKey);
        BigInteger x = bigIntegerFromBytes(digest.digest());
        Log.d(TAG, "x: " +  x.toString());

        // compute verifier v = g^x mod N
        Log.d(TAG, "v: " +  g.modPow(x, N).toString(16));
        return g.modPow(x, N).toString(16);
    }

    /**
     *
     * return salt
     *
     * @return
     */
    public String getSalt() {
        return salt.toString(16);
    }


    /**
     *
     * return A
     *
     * @return
     */
    public String getA() {

        if (A != null) return A.toString(16);

        // generate Private value
        if (a == null){
            final int minBits = Math.max(256, N.bitLength());
            a = BigInteger.ZERO;
            while (BigInteger.ZERO.equals(a)){
                a = (new BigInteger(minBits, random)).mod(N);
            }
        }

        Log.d(TAG, "a: " +  a.toString(16));

        A = g.modPow(a, N);

        Log.d(TAG, "A: " +  A.toString(16));
        return A.toString(16);
    }

    /**
     *
     * return M1
     *
     * @return
     */
    public String getM1() {
        if (this.salt == null) {
            throw new IllegalStateException("The salt is not set. To calculate M1, salt value is needed.");
        }

        if (this.A == null) {
            throw new IllegalStateException("The private client value 'A' is not set. To calculate M1, the value 'A' is needed.");
        }

        if (this.B == null) {
            throw new IllegalStateException("The public server value 'B' is not set. To calculate M1, the value 'B' is needed.");
        }

        // Check one more B value
        if (this.B.mod(N).equals(BigInteger.ZERO)){
            throw new IllegalArgumentException("The public server value 'B' should not be zero mod by N.");
        };

        if (derivedKey == null){
            throw new IllegalStateException("Before calculating M1, you should generate a derived key first using derivateKey().");
        }

        ////// Calculate u = H(A,B)
        // TODO: H(A,B)
        final int padLength = (N.bitLength() + 7) / 8;
//        byte[] Abytes = getPadded(A, padLength);
//        byte[] Bbytes = getPadded(B, padLength);
        MessageDigest digest = getMessageDigestInstance();
        digest.update(bigIntegerToBytes(A)); // Abytes);
        digest.update(bigIntegerToBytes(B)); // Bbytes);
        BigInteger u = bigIntegerFromBytes(digest.digest());
        digest.reset();
        Log.d(TAG, "u: " +  u.toString(16));

        // calculate k = H(N,g) in SRP-6a
//        Abytes = getPadded(N, padLength);
//        Bbytes = getPadded(g, padLength);
        digest.update(bigIntegerToBytes(N)); // Abytes);
        digest.update(bigIntegerToBytes(g)); // Bbytes);
        BigInteger k = bigIntegerFromBytes(digest.digest());
        digest.reset();
        Log.d(TAG, "k: " +  k.toString(16));

        // calculate x = H(s, H(password))
        digest.update(bigIntegerToBytes(this.salt));
        digest.update(this.derivedKey);
        BigInteger x = bigIntegerFromBytes(digest.digest());
        digest.reset();
        Log.d(TAG, "x: " +  x.toString(16));

        // calculate S = (B-kg^x)^(a+ux) (mod N)
        final BigInteger exp = u.multiply(x).add(a);
        final BigInteger tmp = g.modPow(x, N).multiply(k);
        S = B.subtract(tmp).modPow(exp, N);
        Log.d(TAG, "S: " +  S.toString(16));

        // Calcualte M1 = H (A, B, S)
        digest.update(bigIntegerToBytes(A));
        digest.update(bigIntegerToBytes(B));
        digest.update(bigIntegerToBytes(S));
        M1 = bigIntegerFromBytes(digest.digest());
        digest.reset();
        Log.d(TAG, "M1: " +  M1.toString(16));

        return M1.toString(16);
    }

    /**
     *
     * Verify the server evidence
     *
     * @param m2Str
     * @return
     */
    public boolean verifyServerEvidence(String m2Str) {
        if (m2Str == null)
            throw new IllegalArgumentException("The server evidence message 'M2' must not be null");

        BigInteger M2 = new BigInteger(m2Str, 16);

        // Check current state
        if (A == null)
            throw new IllegalStateException("state error: A should be generated in advance.");
        if (S == null)
            throw new IllegalStateException("state error: S should be generated in advance.");
        if (M1 == null)
            throw new IllegalStateException("state error: M1 should be generated in advance.");


        MessageDigest digest = getMessageDigestInstance();
        digest.update(bigIntegerToBytes(A));
        digest.update(bigIntegerToBytes(M1));
        digest.update(bigIntegerToBytes(S));
        BigInteger calculatedM2 = bigIntegerFromBytes(digest.digest());
        Log.d(TAG, "calculatedM2: " +  calculatedM2.toString(16));
        return calculatedM2.equals(M2);

    }


    /**
     * Get username from the username widget
     *
     * @return
     */
    public String getUsername() {

        return usernameEditText.getText().toString();

    }

    /**
     * Set B value
     * @param bStr
     */
    public void setBFromHexString(String bStr) {
        Log.d(TAG, "B: " +  bStr);
        B = new BigInteger(bStr, 16);
        Log.d(TAG, "B: " +  B.toString(16));

    }

    /**
     * Set Salt value
     * @param saltStr
     */
    public void setSaltFromHexString(String saltStr) {
        salt = new BigInteger(saltStr, 16);
    }


    public static MessageDigest getMessageDigestInstance() {

        try {
            return MessageDigest.getInstance(H);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }


    /**
     * Utility method from Nimbus-SRP::BigIntegerUtils
     * Converts a byte array to a positive BigInteger
     *
     * @param bytes byte array in big endian unsigned RFC2945 format
     * @return positive BigInteger containing the data of the supplied byte array
     */
    public static BigInteger bigIntegerFromBytes(final byte[] bytes) {
        return new BigInteger(1, bytes);
    }

    /**
     * Utility method from Nimbus-SRP::BigIntegerUtils
     * Converts a BigInteger into a byte array ignoring the sign of the BigInteger, according to RFC2945 specification     *
     *
     * @param bigInteger BigInteger, must not be null, should not be negative
     * @return byte array (leading byte is always != 0), empty array if BigInteger is zero.
     */
    public static byte[] bigIntegerToBytes(final BigInteger bigInteger) {
        byte[] bytes = bigInteger.toByteArray();
        if (bytes[0] == 0) {
            return Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return bytes;
    }

}
