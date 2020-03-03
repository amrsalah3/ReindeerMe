package com.amr.mineapps.reindeerme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedIntentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // For security to ensure that this class is received only from the system intent action
        if(intent.getAction().equals("android.intent.action.BOOT_COMPLETED")){
            context.startService(new Intent(context, NotificationService.class));
        }
    }
}
