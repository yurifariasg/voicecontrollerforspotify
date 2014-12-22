package com.voicecontroller;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;


public class ListenerFromMobile extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        Log.i("ListenerFromMobile", "onMessageReceived");

        String nodeId = messageEvent.getSourceNodeId();
        String path = messageEvent.getPath();

        try {
            DataMap data = DataMap.fromByteArray(messageEvent.getData());

            Log.i("ListenerFromWear", "Received Message from " + nodeId + " with topic " + path);

            if (path.equalsIgnoreCase("confirmation")) {
                Intent i = new Intent(this, ConfirmationActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra("trackName", data.getString("name"));
                i.putExtra("artistName", data.getString("artist"));
                i.putExtra("image", data.getByteArray("image"));
                i.putExtra("trackUri", data.getString("uri"));
                startActivity(i);
            }
        } catch (Exception e) {
            Log.e("ListenerFromMobile", "Error: " + e.getLocalizedMessage());
        }
    }
}
