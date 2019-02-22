package net.zenconsult.android.examples;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.util.Log;

public class Login {
	private final String TAG = "HttpPost";
	private String username;
	private String password;

	public Login(String user, String pass) {
		username = user;
		password = pass;
	}

	public HttpResponse execute() {
		Log.i(TAG, "Execute Called");
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost("http://logindemo1.appspot.com/logindemo");
		HttpResponse response = null;

		// Post data with number of parameters
		List<NameValuePair> nvPairs = new ArrayList<NameValuePair>(2);
		nvPairs.add(new BasicNameValuePair("username", username));
		nvPairs.add(new BasicNameValuePair("password", password));

		// Add post data to http post
		try {
			UrlEncodedFormEntity params = new UrlEncodedFormEntity(nvPairs);
			post.setEntity(params);
			response = client.execute(post);
			Log.i(TAG, "After client.execute()");

		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "Unsupported Encoding used");
		} catch (ClientProtocolException e) {
			Log.e(TAG, "Client Protocol Exception");
		} catch (IOException e) {
			Log.e(TAG, "IOException in HttpPost");
		}
		return response;
	}

}
