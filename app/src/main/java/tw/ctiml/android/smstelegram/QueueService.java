package tw.ctiml.android.smstelegram;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class QueueService extends Service {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    public static final BlockingDeque<String> queue = new LinkedBlockingDeque<>();
    private Thread worker = null;

    private String chat_id = null;
    private String token = null;

    private String api_url = "https://api.telegram.org/bot%s/sendMessage";
    private URL url;
    private HttpURLConnection conn;
    private Map<String, Object> params;
    private StringBuilder postData, responseData;
    private byte[] postDataBytes;
    private Reader in;

    public static void start(Context context) {
        createNotificationChannel(context);

        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if(QueueService.class.getName().equals(service.service.getClassName())) {
                return;
            }
        }
        Intent serviceIntent = new Intent(context, QueueService.class);
        ContextCompat.startForegroundService(context, serviceIntent);
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            NotificationChannel serviceChannel = manager.getNotificationChannel(CHANNEL_ID);
            if (serviceChannel == null) {
                serviceChannel = new NotificationChannel(
                        CHANNEL_ID,
                        "Foreground Service Channel",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sms to Telegram Service")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);
        chat_id = sharedPref.getString(getString(R.string.et_chat_id_hint), "");
        token = sharedPref.getString(getString(R.string.et_token_hint), "");

        worker = new Thread()
        {
            public void run() {
                checkAndSend();
            }
        };
        worker.start();

        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        if (worker != null) {
            worker.interrupt();
        }
        this.stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void checkAndSend() {
        while (true) {
            String text = null;
            try {
                text = queue.takeFirst();
                if (isInternetAvailable()) {
                    boolean ok = send(text);
                    if (!ok) {
                        Log.w("SMS_RECEIVE", "Telegram API unavailable, retry");
                        queue.putFirst(text);
                        Thread.sleep(10000);
                    }
                } else {
                    Log.w("SMS_RECEIVE", "Internet unavailable, requeue and wait");
                    queue.putFirst(text);
                    Thread.sleep(10000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean send(String text) {

        params = new LinkedHashMap<>();
        //params.put("parse_mode", "MarkdownV2");
        params.put("chat_id", chat_id);
        params.put("text", text);

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
            int code = conn.getResponseCode();
            if (code != 200) {
                return false;
            }

            responseData = new StringBuilder();
            int c;
            while ((c = in.read()) >= 0) {
                responseData.append((char) c);
            }
            String resJson = responseData.toString();
            Log.i("SMS_RECEIVE", resJson);
            JSONObject o = new JSONObject(resJson);
            return o.getBoolean("ok");
        } catch (Exception e) {
            Log.w("SMS_RECEIVE", e.getMessage());
            return false;
        }
    }

    public boolean isInternetAvailable() {
        try {
            InetAddress ipAddr = InetAddress.getByName("google.com");
            return !ipAddr.equals("");

        } catch (Exception e) {
            return false;
        }
    }
}
