package com.tonikamitv.loginregister;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class RegisterRequest extends StringRequest {
    private static final String REGISTER_REQUEST_URL = "http://total_recall:8080/SrpRegister";
    private Map<String, String> params;

    public RegisterRequest(String name, String username, int age, String s, String v, Response.Listener<String> listener) {
        super(Method.POST, REGISTER_REQUEST_URL, listener, null);
        params = new HashMap<>();

        params.put("name", name);
        params.put("age", age + "");
        params.put("username", username);

        params.put("salt", s);
        params.put("verifier", v);
    }

    @Override
    public Map<String, String> getParams() {
        return params;
    }
}
