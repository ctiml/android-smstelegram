package tw.ctiml.android.smstelegram;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.telephony.SmsMessage;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class SmsIntentService extends IntentService {
    private String api_url = "https://api.telegram.org/bot%s/sendMessage";

    private URL url;
    private HttpURLConnection conn;
    private Map<String, Object> params;
    private StringBuilder postData, responseData;
    private byte[] postDataBytes;
    private Reader in;

    public SmsIntentService() {
        super("SmsIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);
        String chat_id = sharedPref.getString(getString(R.string.et_chat_id_hint), "");
        String token = sharedPref.getString(getString(R.string.et_token_hint), "");
        Log.d("SMS_RECEIVE", chat_id);
        Log.d("SMS_RECEIVE", token);

        if (intent != null && chat_id.length() > 0 && token.length() > 0) {
            SmsMessage message = getSmsMessage(intent);

            params = new LinkedHashMap<>();
            params.put("chat_id", chat_id);
            params.put("text", message.getMessageBody());

            postData = new StringBuilder();
            try {
                for (Map.Entry<String, Object> param : params.entrySet()) {
                    if (postData.length() != 0)
                        postData.append('&');
                    postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                    postData.append('=');
                    postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
                }
                postDataBytes = postData.toString().getBytes("UTF-8");

                url = new URL(String.format(api_url, token));
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                conn.setDoOutput(true);
                conn.getOutputStream().write(postDataBytes);

                in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                responseData = new StringBuilder();
                int c;
                while ((c = in.read()) >= 0) {
                    responseData.append((char) c);
                }
                Log.i("SMS_RECEIVE", responseData.toString());
            } catch (Exception e) {
                Log.w("SMS_RECEIVE", e.getMessage());
            }
        }
    }

    private SmsMessage getSmsMessage(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return SmsMessage.createFromPdu(intent.getByteArrayExtra("sms"), intent.getStringExtra("format"));
        } else {
            return SmsMessage.createFromPdu(intent.getByteArrayExtra("sms"));
        }
    }
}
