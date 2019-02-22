package net.zenconsult.android.examples;

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

public class LoginDemoClient1Activity extends Activity implements
		OnClickListener {
	private final String TAG = "LoginDemo1";
	private HttpResponse response;
	private Login login;

	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

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

		response = login.execute();
		Log.i(TAG, "After login.execute()");

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
		text.setText(msg);
	}
}