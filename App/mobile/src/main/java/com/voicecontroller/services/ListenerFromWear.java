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
import com.voicecontroller.Settings;
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

            Log.i("ListenerFromWear", "Received Message from " + nodeId + " with topic " + path);

            WearableConnection connection = new WearableConnection(nodeId, this);

            if (path.equalsIgnoreCase("query")) {
                TrackHandler.lookForTrack(data, connection, this);

                if (Settings.START_SPOTIFY_ON_QUERY) {
                    Intent mIntent = getPackageManager().getLaunchIntentForPackage("com.spotify.music");
                    if (mIntent != null) {
                        try {
                            startActivity(mIntent);
                        } catch (ActivityNotFoundException err) {
                            Toast t = Toast.makeText(getApplicationContext(), "App not found", Toast.LENGTH_SHORT);
                            t.show();
                        }
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

        PowerManager.WakeLock wl = null;
        if (Settings.USE_WAKELOCK_ON_SENDING_TRACK_TO_SPOTIFY) {
            Log.i("ListenerFromWear", "Lock Acquired");
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.ON_AFTER_RELEASE, Settings.APP_TAG);
            wl.acquire();

            KeyguardManager km = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
            KeyguardManager.KeyguardLock kl = km.newKeyguardLock(Settings.APP_TAG);
            kl.disableKeyguard();
        }

        Log.i("ListenerFromWear", "Sending to Spotify: " + trackUri);
        TrackHandler.playTrack(trackUri, connection, this);

        if (Settings.USE_WAKELOCK_ON_SENDING_TRACK_TO_SPOTIFY && wl != null) {
            wl.release();
            Log.i("ListenerFromWear", "Lock Released");
        }
    }

}