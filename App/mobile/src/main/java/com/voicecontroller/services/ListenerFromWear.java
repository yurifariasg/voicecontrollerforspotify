package com.voicecontroller.services;

import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.voicecontroller.R;
import com.voicecontroller.models.Track;
import com.voicecontroller.utils.SpotifyWebAPI;

import org.json.JSONArray;
import org.json.JSONObject;

public class ListenerFromWear extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        String nodeId = messageEvent.getSourceNodeId();
        String path = messageEvent.getPath();

        try {
            String data = new String(messageEvent.getData(), "UTF-8");

            Log.i("ListenerFromWear", "Received Message from " + nodeId + " with topic " + path + " saying: " + data);

            WearableConnection connection = new WearableConnection(nodeId, this);

            if (path.equalsIgnoreCase("query")) {
                TrackHandler.lookForTrack(data, connection, this);
                Intent mIntent = getPackageManager().getLaunchIntentForPackage("com.spotify.music");
                if (mIntent != null) {
                    try {
                        startActivity(mIntent);
                    } catch (ActivityNotFoundException err) {
                        Toast t = Toast.makeText(getApplicationContext(), "App not found", Toast.LENGTH_SHORT);
                        t.show();
                    }
                }


            } else if (path.equalsIgnoreCase("confirm_track")) {
                onConfirmationReceived(data, connection);
            }

        } catch (Exception e) {
            Log.e("ListenerFromWear", e.getLocalizedMessage());
        }


    }

    private void onConfirmationReceived(String trackUri, WearableConnection connection) {

//        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
//        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
//                | PowerManager.ACQUIRE_CAUSES_WAKEUP
//                | PowerManager.ON_AFTER_RELEASE, "INFO");
//        wl.acquire();

//        KeyguardManager km = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
//        KeyguardManager.KeyguardLock kl = km.newKeyguardLock("name");
//        kl.disableKeyguard();

        Log.i("ListenerFromWear", "Lock Acquired");
        Log.i("ListenerFromWear", trackUri);
        TrackHandler.playTrack(trackUri, connection, this);

//        try {
//            Thread.sleep(10000);
//        } catch (Exception e) { }

//        wl.release();
        Log.i("ListenerFromWear", "Lock Released");
    }

}