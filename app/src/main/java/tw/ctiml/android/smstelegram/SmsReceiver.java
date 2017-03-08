package tw.ctiml.android.smstelegram;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle pudsBundle = intent.getExtras();
        Object[] pdus = (Object[]) pudsBundle.get("pdus");

        if (pdus.length > 0) {
            Intent service_intent = new Intent(context, SmsIntentService.class);
            service_intent.putExtra("sms", (byte[]) pdus[0]);
            context.startService(service_intent);
        }
    }
}
