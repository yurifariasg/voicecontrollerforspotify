package com.voicecontroller.services;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.voicecontroller.BuildConfig;
import com.voicecontroller.R;
import com.voicecontroller.models.MediaCommandType;
import com.voicecontroller.models.QueryResults;
import com.voicecontroller.models.VoiceQuery;
import com.voicecontroller.oauth.OAuthRecord;
import com.voicecontroller.oauth.OAuthService;
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
            Log.e("ListenerFromWear", "Exception", e);
            Crashlytics.logException(e);
        }
    }

    private void onQueryReceived(String query, WearableConnection connection) throws JSONException, UnsupportedEncodingException {
        OAuthRecord record = OAuthService.getOAuthToken();
        if (record == null) {
            // No record found. User needs to log in into the app.
            connection.sendAlert(getString(R.string.login_app_alert_title), getString(R.string.login_app_alert_description));
        } else {
            VoiceQuery voiceQuery = new VoiceQuery(query);
            if (voiceQuery.isMediaCommand()) {
                sendMediaCommand(voiceQuery.getMediaCommand());
            } else {
                QueryResults results = TrackHandler.lookForTrack(voiceQuery, connection, this);
                if (results != null) {
                    queryResults.put(results.getId(), results);
                }
            }
        }
    }

    private void sendMediaCommand(MediaCommandType type) {
        Intent i = new Intent(this, NativePlayer.class);
        i.setAction(NativePlayer.MEDIA_CONTROL_ACTION);
        switch (type) {
            case RESUME:
                i.putExtra("cmd", NativePlayer.PLAY);
                break;
            case PAUSE:
                i.putExtra("cmd", NativePlayer.PAUSE);
                break;
            case SKIP:
                i.putExtra("cmd", NativePlayer.NEXT);
                break;
            case PREVIOUS:
                i.putExtra("cmd", NativePlayer.PREVIOUS_FORCE);
                break;
            default:
                break;
        }
        startService(i);
    }

    private void onConfirmationReceived(String confirmationId, final WearableConnection connection) {
        final QueryResults results = queryResults.get(confirmationId);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    executeConfirmationAsync(results, connection);
                } catch (Exception e) {
                    Log.e(Settings.APP_TAG, "Exception", e);
                    Crashlytics.logException(e);
                }
                return null;
            }
        }.execute();
    }
    private void executeConfirmationAsync(QueryResults results, WearableConnection connection) throws JSONException, IOException {
        if (results != null) {
            if (Settings.shouldUseNativePlayer()) {

                Intent intent = new Intent(this, NativePlayer.class);
                intent.setAction(NativePlayer.PLAY_CONTROL_ACTION);
                intent.putExtra("result", results.toBundle());
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