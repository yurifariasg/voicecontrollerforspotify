package com.voicecontroller;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;


public class ListenerFromMobile extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String nodeId = messageEvent.getSourceNodeId();
        String path = messageEvent.getPath();

        try {
            DataMap data = null;
            if (messageEvent.getData() != null) {
                data = DataMap.fromByteArray(messageEvent.getData());
            }

            if (path.equalsIgnoreCase("confirmation") && data != null && data.getString("id") != null) {
                Intent i = new Intent(this, ConfirmationActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra("trackName", data.getString("title"));
                i.putExtra("artistName", data.getString("subtitle"));
                i.putExtra("image", data.getByteArray("image"));
                i.putExtra("trackUri", data.getString("id"));
                i.putExtra("confirmation_time", data.getInt("confirmation_time"));
                startActivity(i);
            } else if (path.equalsIgnoreCase("no_results")) {
                Intent i = new Intent(this, NoResultsActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        } catch (Exception e) {
            Log.e("ListenerFromMobile", "Error: " + e.getLocalizedMessage());
            // Pass the exception to a Service which will send the data upstream to your Smartphone/Tablet
            Intent errorIntent = new Intent(this, ErrorService.class);
            errorIntent.putExtra("exception", e);
            startService(errorIntent);

        }
    }
}
