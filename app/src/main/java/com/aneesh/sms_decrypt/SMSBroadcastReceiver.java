package com.aneesh.sms_decrypt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.aneesh.sms_decrypt.app.AppController;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SMSBroadcastReceiver extends BroadcastReceiver {

    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final String TAG = "SMSBroadcastReceiver";
    private Context myContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        myContext = context;
        Log.i(TAG, "Intent recieved: " + intent.getAction());

        if (intent.getAction().equals(SMS_RECEIVED)) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                final SmsMessage[] messages = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                }
                if (messages.length > -1) {
                    String load[] = messages[0].getMessageBody().split("\\s+");
                    if (load[0].equalsIgnoreCase("BLOOD")) {
                        try {
                            final String bloodGroup = load[1];
                            String address = load[2];
                            for (int i = 3; i < load.length; i++) {
                                address = address + " " + load[i];
                            }
                            final String phone = messages[0].getOriginatingAddress();

                            callMeFunction(bloodGroup, phone, address);


                        } catch (Exception e) {
                            Toast.makeText(context, "All fields not provided!", Toast.LENGTH_LONG).show();
                        }
                    }
                    Toast.makeText(context, "Message recieved: " + messages[0].getMessageBody(), Toast.LENGTH_LONG).show();
                    Toast.makeText(context, "Sender: " + messages[0].getOriginatingAddress(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void callMeFunction(final String bloodGroup, final String phone, final String address) {
        String tag_string_req = "req_blood_request";
        final SmsManager sm = SmsManager.getDefault();
        StringRequest strReq = new StringRequest(Request.Method.POST,
                "http://www.aneeshbhatnagar.com/blood/api/request.php", new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Blood Request Response: " + response.toString());

                try {
                    JSONObject jObj = new JSONObject(response);
                    String error = jObj.getString("error");
                    // Check for error node in json
                    if (error.equalsIgnoreCase("0")) {
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(myContext, errorMsg + " SMS SENT SUCCESSFULLY", Toast.LENGTH_LONG).show();
                        sm.sendTextMessage(phone, null, "We have notified all our users. Someone will contact you shortly.", null, null);
                    } else {
                        // Error in login. Get the error message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(myContext,
                                errorMsg, Toast.LENGTH_LONG).show();
                        sm.sendTextMessage(phone, null, "We're sorry. There seems to be some error with our servers right now. Try again later.", null, null);
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Request Blood Error: " + error.getMessage());
                Toast.makeText(myContext,
                        error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "request");
                params.put("bgroup", bloodGroup);
                params.put("sender", phone);
                params.put("location", address);
                params.put("contact", phone);

                return params;
            }

        };

        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }
}