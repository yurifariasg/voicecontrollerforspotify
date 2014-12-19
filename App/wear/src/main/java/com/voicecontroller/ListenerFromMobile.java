package com.voicecontroller;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONObject;


public class ListenerFromMobile extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        Log.i("ListenerFromMobile", "onMessageReceived");

        String nodeId = messageEvent.getSourceNodeId();
        String path = messageEvent.getPath();

        try {
            String data = new String(messageEvent.getData(), "UTF-8");

            Log.i("ListenerFromWear", "Received Message from " + nodeId + " with topic " + path);

            if (path.equalsIgnoreCase("confirmation")) {

                JSONObject json = new JSONObject(data);

                Intent i = new Intent(this, ConfirmationActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra("trackName", json.getString("name"));
                i.putExtra("artistName", json.getString("artist"));
                i.putExtra("image", json.getString("image"));
                i.putExtra("trackUri", json.getString("uri"));
                startActivity(i);
            }
        } catch (Exception e) {
            Log.e("ListenerFromMobile", "Error: " + e.getLocalizedMessage());
        }
    }
}
