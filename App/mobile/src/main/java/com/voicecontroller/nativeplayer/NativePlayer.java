package com.voicecontroller.nativeplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.playback.Config;
import com.spotify.sdk.android.playback.ConnectionStateCallback;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;
import com.spotify.sdk.android.playback.PlayerState;
import com.voicecontroller.R;
import com.voicecontroller.callbacks.OnOAuthTokenRefreshed;
import com.voicecontroller.models.Track;
import com.voicecontroller.oauth.OAuthRecord;
import com.voicecontroller.oauth.OAuthService;
import com.voicecontroller.settings.Settings;
import com.voicecontroller.utils.SpotifyWebAPI;

import java.util.LinkedList;
import java.util.Queue;


public class NativePlayer extends Service implements PlayerNotificationCallback,
        ConnectionStateCallback, Player.InitializationObserver, OnOAuthTokenRefreshed {

    public static final int NOTIFICATION_ID = 1;
    public static final int PLAY = 1;
    public static final int PAUSE = 2;
    public static final int NEXT = 3;
    public static final int CLOSE = 5;

    public static final String MEDIA_CONTROL_ACTION = "com.voicecontroller.music.cmd";
    public static final String PLAY_CONTROL_ACTION = "com.voicecontroller.music.play";

    private int state;

    private Player mPlayer;
    private Queue<Track> tracks;
    private MediaSession mySession;
    private MediaController myController;

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
        mySession = new MediaSession(this, Settings.APP_TAG);
        myController = new MediaController(this, mySession.getSessionToken());

        mySession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                resume();
                Log.i("NativePlayer.mySession", "onPlay");
            }

            @Override
            public void onPause() {
                pause();
                Log.i("NativePlayer.mySession", "onPause");
            }

            @Override
            public void onStop() {
                pause();
                Log.i("NativePlayer.mySession", "onStop");
            }
        });
        mySession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
    }

    @Override
    public void onDestroy() {
        if (mPlayer != null && !mPlayer.isShutdown()) {
            mPlayer.shutdown();
        }
        mySession.setActive(false);
        mySession.release();
        super.onDestroy();
    }

    private PendingIntent getIntentFor(int mediaCmd) {
        switch (mediaCmd) {
            case PAUSE:
                Intent pauseIntent = new Intent(this, NativePlayer.class);
                pauseIntent.setAction(MEDIA_CONTROL_ACTION);
                pauseIntent.putExtra("cmd", PAUSE);
                return PendingIntent.getService(this, PAUSE, pauseIntent, 0);
            case NEXT:
                Intent nextIntent = new Intent(this, NativePlayer.class);
                nextIntent.setAction(MEDIA_CONTROL_ACTION);
                nextIntent.putExtra("cmd", NEXT);
                return PendingIntent.getService(this, NEXT, nextIntent, 0);
            case CLOSE:
                Intent closeIntent = new Intent(this, NativePlayer.class);
                closeIntent.setAction(MEDIA_CONTROL_ACTION);
                closeIntent.putExtra("cmd", CLOSE);
                return PendingIntent.getService(this, CLOSE, closeIntent, 0);
            case PLAY:
                Intent playIntent = new Intent(this, NativePlayer.class);
                playIntent.setAction(MEDIA_CONTROL_ACTION);
                playIntent.putExtra("cmd", PLAY);
                return PendingIntent.getService(this, PLAY, playIntent, 0);
        }
        return null;
    }

    private void updateTrackMetadata() {
        byte[] artworkBytes = tracks.peek().getImage();
        Bitmap artwork = BitmapFactory.decodeByteArray(artworkBytes, 0, artworkBytes.length);

        mySession.setMetadata(new MediaMetadata.Builder()
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, tracks.peek().getArtist())
                .putString(MediaMetadata.METADATA_KEY_TITLE, tracks.peek().getName())
                .build());

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_music)
                .setContentTitle(tracks.peek().getName())
                .setContentText(tracks.peek().getArtist())
                .setLargeIcon(artwork)
                .setColor(Color.rgb(38, 50, 56))
                .setStyle(new Notification.MediaStyle()
                        .setMediaSession(mySession.getSessionToken()));

        if (state == PlaybackState.STATE_PLAYING) {
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", getIntentFor(PAUSE));
        } else {
            builder.addAction(android.R.drawable.ic_media_play, "Play", getIntentFor(PLAY));
        }

        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", getIntentFor(CLOSE));

        Notification noti = builder.build();

        PlaybackState state = new PlaybackState.Builder()
                .setActions(
                        PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_PAUSE |
                                PlaybackState.ACTION_PAUSE)
                .setState(PlaybackState.STATE_PLAYING, 0, 1, SystemClock.elapsedRealtime())
                .build();
        mySession.setPlaybackState(state);

        startForeground(NOTIFICATION_ID, noti);
    }

    public void stopNotification() {
        stopForeground(true);
        mySession.setActive(false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(MEDIA_CONTROL_ACTION)) {
                int cmd = intent.getIntExtra("cmd", 0);
                switch (cmd) {
                    case PLAY:
                        resume();
                        break;
                    case PAUSE:
                        pause();
                        break;
                    case NEXT:
                        playNext();
                        break;
                    case CLOSE:
                        close();
                        break;
                    default:
                        break;
                }

            } else if (intent.getAction().equals(PLAY_CONTROL_ACTION)) {
                Track track = Track.fromBundle(intent.getBundleExtra("track"));
                Log.i("NativePlayer", "New track received: " + track.toString());

                if (track != null) {
                    tracks.add(track);
                }

                // Initialization.
                if (mPlayer == null || !mPlayer.isInitialized() || mPlayer.isShutdown()) {
                    Log.i("NativePlayer", "Initializing player...");
                    OAuthRecord record = OAuthService.getOAuthToken();
                    if (record != null) {
                        if (!record.isValid()) {
                            SpotifyWebAPI.refreshOAuth(record, this);
                        } else {
                            initializePlayerWithToken(record);
                        }
                    } else {
                        Log.w("NativePlayer", "No valid record found.");
                    }
                } else {
                    playNext();
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onOAuthTokenRefreshed(OAuthRecord record) {
        if (record.isValid()) {
            initializePlayerWithToken(record);
        } else {
            Log.w("NativePlayer", "Token refresh failed... can't initialize player...");
        }
    }

    private void initializePlayerWithToken(OAuthRecord record) {
        // No verifications with token are done here...
        String token = record.access_token;
        Config playerConfig = new Config(this, token, SpotifyWebAPI.CLIENT_ID);
        Spotify spotify = new Spotify();
        mPlayer = spotify.getPlayer(playerConfig, this, this);
    }


    public void pause() {
        if (mPlayer != null && mPlayer.isInitialized()) {
            state = PlaybackState.STATE_PAUSED;
            mPlayer.pause();
            updateTrackMetadata();
        }
    }

    public void resume() {
        if (mPlayer != null && mPlayer.isInitialized()) {
            state = PlaybackState.STATE_PLAYING;
            mPlayer.resume();
            updateTrackMetadata();
        }
    }

    public void play() {
        if (!tracks.isEmpty() && mPlayer != null && mPlayer.isInitialized()) {
            Log.i("NativePlayer", "Playing song in the queue...");
            state = PlaybackState.STATE_PLAYING;
            mPlayer.play(tracks.peek().getUri());
            updateTrackMetadata();
        }
    }

    public void playNext() {
        if (!tracks.isEmpty()) {
            tracks.remove();
            play();
        }
    }

    public void close() {
        stopNotification();
        if (mPlayer != null && !mPlayer.isShutdown()) {
            mPlayer.shutdown();
        }
    }

    @Override
    public void onInitialized() {
        mPlayer.addConnectionStateCallback(this);
        mPlayer.addPlayerNotificationCallback(this);
        play();
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.i("NativePlayer", "onPlaybackEvent: " + eventType.name() + " - " + playerState.toString());
        if (eventType.equals(EventType.LOST_PERMISSION) || eventType.equals(EventType.END_OF_CONTEXT)) {
            stopNotification();
            mySession.setActive(false);
        } else if (eventType.equals(EventType.TRACK_START)) {
            mySession.setActive(true);
        }
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String s) {
        Log.i("NativePlayer", "onPlaybackError: " + errorType.name() + " - " + s);
        close();
    }

    @Override
    public void onError(Throwable throwable) {
        Log.e("NativePlayer", "onError Spotify Initialization Error", throwable);
        close();
    }

    @Override
    public void onLoggedIn() {
        Log.i("NativePlayer", "SpotifySDK Connection: onLoggedIn");
    }

    @Override
    public void onLoggedOut() {
        Log.i("NativePlayer", "SpotifySDK Connection: onLoggedOut");
    }

    @Override
    public void onLoginFailed(Throwable throwable) {
        Log.e("NativePlayer", "SpotifySDK Connection: onLoginFailed", throwable);
    }

    @Override
    public void onTemporaryError() {
        Log.e("NativePlayer", "SpotifySDK Connection: onTemporaryError");
    }

    @Override
    public void onNewCredentials(String s) {
        Log.i("NativePlayer", "onNewCredentials: " + s);
    }

    @Override
    public void onConnectionMessage(String s) {
        Log.i("NativePlayer", "SpotifySDK Connection: onConnectionMessage: " + s);
    }
}
