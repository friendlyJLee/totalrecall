package edu.rice.seclab.challengeresponseclient;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class ChallengeResponseClientActivity extends AppCompatActivity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge_response_client);
        final Activity activity = this;

        final Button button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Comms c = new Comms(activity);
                String challenge = c.getChallenge();
                CRAM cram = new CRAM(activity);
                String hash = cram.generate(challenge);
                String reply = c.sendResponse(hash);
                if (c.authorized(reply)) {
                    Toast toast = Toast.makeText(
                            activity.getApplicationContext(), "Login success",
                            Toast.LENGTH_LONG);
                    toast.show();
                    Intent intent = new Intent(ChallengeResponseClientActivity.this, UserAreaActivity.class);
                    intent.putExtra("name", "testname");
                    intent.putExtra("age", 20);
                    intent.putExtra("username", "testusername");
                    ChallengeResponseClientActivity.this.startActivity(intent);
                } else {
                    Toast toast = Toast.makeText(
                            activity.getApplicationContext(), "Login failed",
                            Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
    }
}
