package edu.rice.seclab.challengeresponseclient;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import android.app.Activity;
import android.util.Base64;
import android.widget.TextView;

public class CRAM {
    private Activity activity;

    public CRAM(Activity act) {
        activity = act;
    }

    public String generate(String serverChallenge) {
        String algo = "HmacSHA1";
        TextView pass = (TextView) activity.findViewById(R.id.editText2);
        byte[] server = Base64.decode(serverChallenge, Base64.DEFAULT);

        Mac mac = null;
        try {
            mac = Mac.getInstance(algo);
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String keyText = pass.getText().toString();
        SecretKey key = new SecretKeySpec(keyText.getBytes(), algo);
        try {
            mac.init(key);
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        byte[] tmpHash = mac.doFinal(server);
        TextView user = (TextView) activity.findViewById(R.id.editText1);
        String username = user.getText().toString();
        String concat = username + " " + Hex.toHex(tmpHash);
        String hash = Base64.encodeToString(concat.getBytes(), Base64.URL_SAFE);
        return hash;
    }
}