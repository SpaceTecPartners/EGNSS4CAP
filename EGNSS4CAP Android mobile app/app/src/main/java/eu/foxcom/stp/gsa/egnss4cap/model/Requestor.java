package eu.foxcom.stp.gsa.egnss4cap.model;

import android.content.Context;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class Requestor {

    public static class Req {
        public boolean forceCancel = false;

        public Map<String, String> getParams() {
            // override
            return null;
        }

        public Map<String, String> getHeaders() {
            // override
            return null;
        }
    }

    private Context context;
    private RequestQueue queue;

    public Requestor (Context context) {
        this.context = context;
        this.queue = Volley.newRequestQueue(context);
    }

    public void request(String url, Response.Listener<String> listener, Response.ErrorListener errorListener, final Req req) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, listener, errorListener) {

            @Override
            protected java.util.Map<String, String> getParams() {
                if (req.forceCancel) {
                    this.cancel();
                }
                return req.getParams();
            }

            @Override
            public java.util.Map<String, String> getHeaders() throws AuthFailureError {
                return req.getHeaders();
            }
        };
        queue.add(stringRequest);
    }

    public void requestAuth(String url, Response.Listener<String> listener, Response.ErrorListener errorListener, final Req req) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, listener, errorListener) {

            @Override
            protected java.util.Map<String, String> getParams() {
                if (req.forceCancel) {
                    this.cancel();
                }
                return req.getParams();
            }

            @Override
            public java.util.Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/x-www-form-urlencoded");
                String creds = String.format("%s:%s", "login", "pswd");
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
                params.put("Authorization", auth);
                return params;
            }
        };
        queue.add(stringRequest);
    }

    // region get, set



    // endregion
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */