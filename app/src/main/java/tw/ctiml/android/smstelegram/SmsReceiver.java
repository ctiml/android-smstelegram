package tw.ctiml.android.smstelegram;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

public class SmsReceiver extends BroadcastReceiver {

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle pdusBundle = intent.getExtras();
        Object[] pdus = (Object[]) pdusBundle.get("pdus");
        String format = (String) pdusBundle.get("format");

        QueueService.start(context);

        if (pdus.length > 0) {
            Intent service_intent = new Intent(context, SmsIntentService.class);
            for (int i = 0; i < pdus.length; i++) {
                service_intent.putExtra("sms" + i, (byte[]) pdus[i]);
            }
            service_intent.putExtra("length", pdus.length);
            service_intent.putExtra("format", format);
            context.startService(service_intent);
        }
    }
}
