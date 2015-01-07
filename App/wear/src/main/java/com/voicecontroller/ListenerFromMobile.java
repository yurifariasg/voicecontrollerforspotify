package com.voicecontroller;

import android.content.Intent;
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
            } else if (path.equalsIgnoreCase("alert") && data != null) {
                Intent i = new Intent(this, AlertActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra("title", data.getString("title"));
                i.putExtra("description", data.getString("description"));
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
