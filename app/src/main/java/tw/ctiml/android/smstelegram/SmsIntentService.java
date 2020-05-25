package tw.ctiml.android.smstelegram;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.telephony.SmsMessage;

import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class SmsIntentService extends IntentService {
    public SmsIntentService() {
        super("SmsIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);
        String chat_id = sharedPref.getString(getString(R.string.et_chat_id_hint), "");
        String token = sharedPref.getString(getString(R.string.et_token_hint), "");
        String text = null;

        if (intent != null && chat_id.length() > 0 && token.length() > 0) {
            text = getTextFromSmsMessage(intent);

            try {
                QueueService.queue.putLast(text);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String getTextFromSmsMessage(Intent intent) {
        StringBuilder sb = new StringBuilder("");
        int len = intent.getIntExtra("length", 0);
        for (int i = 0; i < len; i++) {
            SmsMessage message = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                message = SmsMessage.createFromPdu(intent.getByteArrayExtra("sms" + i), intent.getStringExtra("format"));
            } else {
                message = SmsMessage.createFromPdu(intent.getByteArrayExtra("sms" + i));
            }
            if (i == 0) {
                sb.append(String.format("☎ %s\n", message.getOriginatingAddress()));
            }
            sb.append(message.getMessageBody());
        }
        return sb.toString();
    }
}
