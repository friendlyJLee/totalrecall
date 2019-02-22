package com.tonikamitv.loginregister;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

import edu.rice.seclab.keyexporter.SRPKeyExporter;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText etUsername = (EditText) findViewById(R.id.etUsername);
        final EditText etPassword = (EditText) findViewById(R.id.etPassword);
        final TextView tvRegisterLink = (TextView) findViewById(R.id.tvRegisterLink);
        final Button bLogin = (Button) findViewById(R.id.bSignIn);

        final RequestQueue queue = Volley.newRequestQueue(this);

        final SRPKeyExporter auth = new SRPKeyExporter();
        auth.bind(etUsername, etPassword);


        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
                LoginActivity.this.startActivity(registerIntent);
            }
        });

        bLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                auth.init();

                // Response received from the server
                final Response.Listener<String> step2responseListener = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            String M2 = jsonResponse.getString("M2");

                            Log.d("LoginAcitivity", "M2: " + M2);

                            boolean success = auth.verifyServerEvidence(M2);
                            if (success) {
                                String name = jsonResponse.getString("name");
                                int age = jsonResponse.getInt("age");

                                Intent intent = new Intent(LoginActivity.this, UserAreaActivity.class);
                                intent.putExtra("name", name);
                                intent.putExtra("age", age);
                                intent.putExtra("username", auth.getUsername());
                                LoginActivity.this.startActivity(intent);
                            } else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                                builder.setMessage("Login Failed")
                                        .setNegativeButton("Retry", null)
                                        .create()
                                        .show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };


                Response.Listener<String> step1responseListener = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            String salt = jsonResponse.getString("salt");
                            String B = jsonResponse.getString("B");

                            Log.d("LoginAcitivity", "salt: " + salt);
                            Log.d("LoginAcitivity", "B: " + B);

                            auth.setSaltFromHexString(salt);
                            auth.setBFromHexString(B);

                            SRPStep2Request step2Request = new SRPStep2Request(auth.getM1(), step2responseListener);
                            queue.add(step2Request);

                                //Error

//                                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
//                                builder.setMessage("Login Failed")
//                                        .setNegativeButton("Retry", null)
//                                        .create()
//                                        .show();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };

                SRPStep1Request step1Request = new SRPStep1Request(auth.getUsername(), auth.getA(), step1responseListener);
                queue.add(step1Request);

//                // Instantiate the cache
//                Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap
//
//                // Set up the network to use HttpURLConnection as the HTTP client.
//                Network network = new BasicNetwork(new HurlStack());

                //RequestQueue queue = Volley.newRequestQueue(LoginActivity.this);
//                RequestQueue queue = new RequestQueue(cache, network);
//                queue.start();


            }
        });
        Debug.startMethodTracing("test.trace" + 0);
    }
}
