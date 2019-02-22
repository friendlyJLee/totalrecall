package edu.rice.seclab.challengeresponseclient;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class Comms {
    private final String url = "http://127.0.0.1:8080/ChallengeLogin";
    private Context ctx;
    private DefaultHttpClient client;

    public Comms(Activity act) {
        ctx = act.getApplicationContext();
        client = new DefaultHttpClient();
    }

    public String sendResponse(String hash) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("challenge", hash));
        String paramString = URLEncodedUtils.format(params, "utf-8");
        String cUrl = url + "?" + paramString;
        return doGetAsString(cUrl);
    }

    public boolean authorized(String response) {
        InputStream is = new ByteArrayInputStream(response.getBytes());
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        Document doc = null;
        String reply = "";
        try {
            db = dbFactory.newDocumentBuilder();
            doc = db.parse(is);
            NodeList nl = doc.getElementsByTagName("Response");
            reply = nl.item(0).getTextContent();
            is.close();
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return reply.matches("Authorized");
    }

    public String getChallenge() {

        InputStream challengeText = doGetAsInputStream(url);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        Document doc = null;
        String challenge = "";
        try {
            db = dbFactory.newDocumentBuilder();
            doc = db.parse(challengeText);
            NodeList nl = doc.getElementsByTagName("Challenge");
            challenge = nl.item(0).getTextContent();
            challengeText.close();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return challenge;
    }

    public String doGetAsString(String url) {
        HttpGet request = new HttpGet(url);
        String result = "";
        try {
            ClientExecute t = new ClientExecute(request);
            t.start();
            t.join();
            HttpResponse response = t.getResponse();
//            HttpResponse response = client.execute(request);
            int code = response.getStatusLine().getStatusCode();
            if (code == 200) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                Toast toast = Toast.makeText(ctx, "Status Code " + code,
                        Toast.LENGTH_SHORT);
                toast.show();
            }
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;

    }

    public InputStream doGetAsInputStream(String url) {
        HttpGet request = new HttpGet(url);
        InputStream result = null;
        try {

            ClientExecute t = new ClientExecute(request);
            t.start();
            t.join();
            HttpResponse response = t.getResponse();

            int code = response.getStatusLine().getStatusCode();
            if (code == 200) {
                result = response.getEntity().getContent();
            } else {
                Toast toast = Toast.makeText(ctx, "Status Code " + code,
                        Toast.LENGTH_SHORT);
                toast.show();
            }
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;

    }

    class ClientExecute extends Thread{
        HttpResponse response;
        HttpGet request;

        public ClientExecute(HttpGet request){
            this.request = request;
        }
        public void run() {
            try {
                response = client.execute(request);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public HttpResponse getResponse() {
            return response;
        }
    }



}
