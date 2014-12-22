package com.voicecontroller.nativeplayer;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;

import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.playback.Config;
import com.spotify.sdk.android.playback.ConnectionStateCallback;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;
import com.spotify.sdk.android.playback.PlayerState;
import com.voicecontroller.oauth.OAuthRecord;
import com.voicecontroller.oauth.OAuthService;
import com.voicecontroller.utils.SpotifyWebAPI;

import java.util.LinkedList;
import java.util.Queue;


public class NativePlayer extends Service implements PlayerNotificationCallback, ConnectionStateCallback, Player.InitializationObserver {

    private Player mPlayer;
    private Queue<String> tracks;

    public NativePlayer() {
        tracks = new LinkedList<>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        mPlayer.shutdown();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            String uri = intent.getStringExtra("uri");

            if (uri != null && !uri.isEmpty()) {
                tracks.add(uri);
            }

            // Initialization.
            if (mPlayer == null || !mPlayer.isInitialized()) {
                OAuthRecord record = OAuthService.getOAuthToken();
                if (record != null && record.isValid()) {
                    String token = record.token;
                    Config playerConfig = new Config(this, token, SpotifyWebAPI.CLIENT_ID);
                    Spotify spotify = new Spotify();
                    mPlayer = spotify.getPlayer(playerConfig, this, this);
                } else {
                    Log.w("NativePlayer", "No valid record found.");
                }
            } else {
                playNext();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public void playNext() {
        if (!tracks.isEmpty()) {
            mPlayer.play(tracks.poll());
        }
    }

    @Override
    public void onInitialized() {
        mPlayer.addConnectionStateCallback(this);
        mPlayer.addPlayerNotificationCallback(this);
        playNext();
    }

    @Override
    public void onError(Throwable throwable) {
        Log.e("NativePlayer", "onError", throwable);
    }

    @Override
    public void onLoggedIn() {
        Log.i("NativePlayer", "onLoggedIn");
    }

    @Override
    public void onLoggedOut() {
        Log.i("NativePlayer", "onLoggedOut");
    }

    @Override
    public void onLoginFailed(Throwable throwable) {
        Log.e("NativePlayer", "onLoginFailed", throwable);
    }

    @Override
    public void onTemporaryError() {
        Log.e("NativePlayer", "onTemporaryError");
    }

    @Override
    public void onNewCredentials(String s) {
        Log.i("NativePlayer", "onNewCredentials: " + s);
    }

    @Override
    public void onConnectionMessage(String s) {
        Log.i("NativePlayer", "onConnectionMessage: " + s);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.i("NativePlayer", "onPlaybackEvent: " + eventType.name() + " - " + playerState.toString());
        if (eventType.equals(EventType.END_OF_CONTEXT)) {
            playNext();
        }
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String s) {
        Log.i("NativePlayer", "onPlaybackError: " + errorType.name() + " - " + s);
    }
}
