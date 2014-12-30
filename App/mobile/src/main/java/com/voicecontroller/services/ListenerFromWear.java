package com.voicecontroller.services;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Parcelable;
import android.os.PowerManager;
import android.speech.tts.Voice;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.voicecontroller.BuildConfig;
import com.voicecontroller.R;
import com.voicecontroller.exceptions.NoTrackFoundException;
import com.voicecontroller.models.QueryResults;
import com.voicecontroller.models.QueryType;
import com.voicecontroller.models.Track;
import com.voicecontroller.models.VoiceQuery;
import com.voicecontroller.settings.Settings;
import com.voicecontroller.nativeplayer.NativePlayer;

import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import io.fabric.sdk.android.Fabric;

public class ListenerFromWear extends WearableListenerService {

    private static HashMap<String, QueryResults> queryResults = new HashMap<>();

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Crashlytics crashlytics;
        if (BuildConfig.DEBUG) {
            crashlytics = new Crashlytics.Builder().disabled(BuildConfig.DEBUG).build();
        } else {
            crashlytics = new Crashlytics();
        }
        Fabric.with(this, crashlytics);

        String nodeId = messageEvent.getSourceNodeId();
        String path = messageEvent.getPath();

        try {
            DataMap data = DataMap.fromByteArray(messageEvent.getData());
            WearableConnection connection = new WearableConnection(nodeId, this);

            if (path.equalsIgnoreCase("query")) {
                onQueryReceived(data.getString("query"), connection);
            } else if (path.equalsIgnoreCase("confirm_track")) {
                onConfirmationReceived(data.getString("uri"), connection);
            } else if (path.equalsIgnoreCase("wear_error")) {
                DataMap map = DataMap.fromByteArray(messageEvent.getData());
                try (
                    ByteArrayInputStream bis = new ByteArrayInputStream(map.getByteArray("exception"));
                    ObjectInputStream ois = new ObjectInputStream(bis);
                ) {
                    Throwable ex = (Throwable) ois.readObject();

                    Crashlytics.setBool("wear_exception", true);
                    Crashlytics.setString("board", map.getString("board"));
                    Crashlytics.setString("fingerprint", map.getString("fingerprint"));
                    Crashlytics.setString("model", map.getString("model"));
                    Crashlytics.setString("manufacturer", map.getString("manufacturer"));
                    Crashlytics.setString("product", map.getString("product"));
                    Crashlytics.logException(ex);
                } catch (Exception e) { Crashlytics.logException(e); }
            }

        } catch (Exception e) {
            Log.e("ListenerFromWear", e.getLocalizedMessage());
            Crashlytics.logException(e);
        }
    }

    private void showTrackNotFoundNotification() {
        int notificationId = 1001;
        // Build intent for notification content
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.ic_stat_music)
                        .setContentTitle(getString(R.string.track_not_found_title))
                        .setContentText(getString(R.string.track_not_found_desc));
        notificationBuilder.extend(new NotificationCompat.WearableExtender().setHintHideIcon(true));
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_MAX);

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Build the notification and issues it with notification manager.
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    private void onQueryReceived(String query, WearableConnection connection) throws JSONException, UnsupportedEncodingException {
        try {
            VoiceQuery voiceQuery = new VoiceQuery(query);
            QueryResults results = TrackHandler.lookForTrack(voiceQuery, connection, this);
            queryResults.put(results.getId(), results);
        } catch (NoTrackFoundException e) {
            showTrackNotFoundNotification();
        }
    }

    private void onConfirmationReceived(String confirmationId, final WearableConnection connection) {
        final QueryResults results = queryResults.get(confirmationId);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    executeConfirmation(results, connection);
                } catch (Exception e) {
                    Log.e(Settings.APP_TAG, "Exception", e);
                    Crashlytics.logException(e);
                }
                return null;
            }
        }.execute();
    }
    private void executeConfirmation(QueryResults results, WearableConnection connection) throws JSONException, IOException {
        if (results != null) {
            if (Settings.shouldUseNativePlayer()) {
                results.fetchTracks();
                Intent intent = new Intent(this, NativePlayer.class);
                intent.setAction(NativePlayer.PLAY_CONTROL_ACTION);

                Track[] tracks = results.getTracks();
                Parcelable[] parcelables = new Parcelable[tracks.length];
                for (int i = 0 ; i < tracks.length ; i++) {
                    parcelables[i] = tracks[i].toBundle();
                }
                intent.putExtra("tracks", parcelables);
                if (results.getQuery() != null) {
                    intent.putExtra("enqueue", results.getQuery().shouldEnqueue());
                }
                startService(intent);
            } else {
                PowerManager.WakeLock wl = null;
                if (Settings.USE_WAKELOCK_ON_SENDING_TRACK_TO_SPOTIFY) {
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


                TrackHandler.playTrack(results.getUri(), connection, this);

                if (Settings.USE_WAKELOCK_ON_SENDING_TRACK_TO_SPOTIFY && wl != null) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            wlFinal.release();
                        }
                    }, 10000);
                }
            }
        } else {
            Log.w(Settings.APP_TAG, "Could not find track on track map.");
        }
    }
}