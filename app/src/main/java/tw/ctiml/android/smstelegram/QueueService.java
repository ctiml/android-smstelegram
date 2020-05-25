package tw.ctiml.android.smstelegram;

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

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void checkAndSend() {
        while (true) {
            String text = null;
            try {
                text = queue.takeFirst();
                if (isInternetAvailable()) {
                    send(text);
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

    private void send(String text) {

        params = new LinkedHashMap<>();
        params.put("parse_mode", "MarkdownV2");
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

    public boolean isInternetAvailable() {
        try {
            InetAddress ipAddr = InetAddress.getByName("google.com");
            return !ipAddr.equals("");

        } catch (Exception e) {
            return false;
        }
    }
}