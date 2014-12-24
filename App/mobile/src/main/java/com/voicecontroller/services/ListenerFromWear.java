package com.voicecontroller.services;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.voicecontroller.models.Track;
import com.voicecontroller.settings.Settings;
import com.voicecontroller.nativeplayer.NativePlayer;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;

public class ListenerFromWear extends WearableListenerService {

    private static HashMap<String, Track> tracks = new HashMap<String, Track>();

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (Settings.ENABLE_CRASHLYTICS) {
            Crashlytics.start(this);
        }

        String nodeId = messageEvent.getSourceNodeId();
        String path = messageEvent.getPath();

        try {
            DataMap data = DataMap.fromByteArray(messageEvent.getData());

            Log.i("ListenerFromWear", "Received Message from " + nodeId + " with topic " + path);

            WearableConnection connection = new WearableConnection(nodeId, this);

            if (path.equalsIgnoreCase("query")) {
                Track track = TrackHandler.lookForTrack(data.getString("query"), connection, this);
                // Add track to map
                tracks.put(track.getUri(), track);
            } else if (path.equalsIgnoreCase("confirm_track")) {
                onConfirmationReceived(data.getString("uri"), connection);
            } else if (path.equalsIgnoreCase("wear_error")) {
                if (Settings.ENABLE_CRASHLYTICS) {
                    DataMap map = DataMap.fromByteArray(messageEvent.getData());
                    ByteArrayInputStream bis = new ByteArrayInputStream(map.getByteArray("exception"));
                    try {
                        ObjectInputStream ois = new ObjectInputStream(bis);
                        Throwable ex = (Throwable) ois.readObject();

                        Crashlytics.setBool("wear_exception", true);
                        Crashlytics.setString("board", map.getString("board"));
                        Crashlytics.setString("fingerprint", map.getString("fingerprint"));
                        Crashlytics.setString("model", map.getString("model"));
                        Crashlytics.setString("manufacturer", map.getString("manufacturer"));
                        Crashlytics.setString("product", map.getString("product"));
                        Crashlytics.logException(ex);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        } catch (Exception e) {
            Log.e("ListenerFromWear", e.getLocalizedMessage());
        }
    }

    private void onConfirmationReceived(String trackUri, WearableConnection connection) {

        if (Settings.shouldUseNativePlayer()) {
            Track track = tracks.get(trackUri);
            if (track != null) {
                Intent i = new Intent(this, NativePlayer.class);
                i.setAction(NativePlayer.PLAY_CONTROL_ACTION);
                i.putExtra("track", track.toBundle());
                startService(i);
            } else {
                Log.w("ListenerFromWear", "Could not find track on track map.");
            }

        } else {
            PowerManager.WakeLock wl = null;
            if (Settings.USE_WAKELOCK_ON_SENDING_TRACK_TO_SPOTIFY) {
                Log.i("ListenerFromWear", "Lock Acquired");
                PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
                wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP
                        | PowerManager.ON_AFTER_RELEASE, Settings.APP_TAG);
                wl.acquire();
            }
            final PowerManager.WakeLock wlFinal = wl;

            if (Settings.USE_KEYGUARD_ON_SENDING_TRACK_TO_SPOTIFY) {
                KeyguardManager km = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
                KeyguardManager.KeyguardLock kl = km.newKeyguardLock(Settings.APP_TAG);
                kl.disableKeyguard();
            }

            Log.i("ListenerFromWear", "Sending to Spotify: " + trackUri);
            TrackHandler.playTrack(trackUri, connection, this);

            if (Settings.USE_WAKELOCK_ON_SENDING_TRACK_TO_SPOTIFY && wl != null) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        wlFinal.release();
                        Log.i("ListenerFromWear", "Lock Released");
                    }
                }, 10000);
            }
        }
    }

}