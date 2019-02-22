package com.tonikamitv.loginregister;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

class SRPStep1Request extends StringRequest {
    private static final String LOGIN_REQUEST_URL = "http://total_recall:8080/SrpLogin1";
    private Map<String, String> params;

    public SRPStep1Request(String userName, String A, Response.Listener<String> listener) {
        super(Method.POST, LOGIN_REQUEST_URL, listener, null);
        params = new HashMap<>();
        params.put("username", userName);
        params.put("A", A);
    }
    public Map<String, String> getParams() {
        return params;
    }
}

class SRPStep2Request extends StringRequest {
    private static final String LOGIN_REQUEST_URL = "http://total_recall:8080/SrpLogin2";
    private Map<String, String> params;

    public SRPStep2Request(String m1, Response.Listener<String> listener) {
        super(Method.POST, LOGIN_REQUEST_URL, listener, null);
        params = new HashMap<>();
        params.put("M1", m1);

    }
    public Map<String, String> getParams() {
        return params;
    }
}
