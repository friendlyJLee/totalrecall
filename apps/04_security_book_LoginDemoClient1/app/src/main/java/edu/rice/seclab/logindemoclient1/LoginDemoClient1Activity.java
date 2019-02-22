package edu.rice.seclab.logindemoclient1;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class LoginDemoClient1Activity extends AppCompatActivity implements OnClickListener {

    private final String TAG = "LoginDemo1";
    private HttpResponse response;
    private Login login;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.login);
        button.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        String u = ((EditText) findViewById(R.id.username)).toString();
        String p = ((EditText) findViewById(R.id.password)).toString();

        login = new Login(u, p);

        String msg = "";
        EditText text = (EditText) findViewById(R.id.editText1);
        text.setText(msg);


        Thread t = new Thread(new Runnable() {
            public void run() {
                response = login.execute();
                Log.i(TAG, "After login.execute()");
            }
        });

        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        if (response != null) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getEntity()
                                    .getContent()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    msg = sb.toString();
                } catch (IOException e) {
                    Log.e(TAG, "IO Exception in reading from stream.");
                }

            } else {
                msg = "Status code other than HTTP 200 received";
            }
        } else {
            msg = "Response is null";
        }

        if (msg.endsWith("Login success!!")) {
            Intent intent = new Intent(LoginDemoClient1Activity.this, UserAreaActivity.class);
            intent.putExtra("name", "testname");
            intent.putExtra("age", 20);
            intent.putExtra("username", "testusername");
            LoginDemoClient1Activity.this.startActivity(intent);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginDemoClient1Activity.this);
            builder.setMessage("Login Failed")
                    .setNegativeButton("Retry", null)
                    .create()
                    .show();
        }
        text.setText(msg);
    }

}
