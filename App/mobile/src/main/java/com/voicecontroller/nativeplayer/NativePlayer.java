package com.voicecontroller.nativeplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.crashlytics.android.Crashlytics;
import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.playback.Config;
import com.spotify.sdk.android.playback.ConnectionStateCallback;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;
import com.spotify.sdk.android.playback.PlayerState;
import com.voicecontroller.R;
import com.voicecontroller.activities.MainActivity;
import com.voicecontroller.callbacks.OnOAuthTokenRefreshed;
import com.voicecontroller.models.Track;
import com.voicecontroller.oauth.OAuthRecord;
import com.voicecontroller.oauth.OAuthService;
import com.voicecontroller.settings.Settings;
import com.voicecontroller.utils.SpotifyWebAPI;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;


public class NativePlayer extends Service implements PlayerNotificationCallback,
        ConnectionStateCallback, Player.InitializationObserver, OnOAuthTokenRefreshed {

    public static final int NOTIFICATION_ID = 1;
    public static final int ERROR_NOTIFICATION_ID = 2;
    public static final int PLAY = 1;
    public static final int PAUSE = 2;
    public static final int NEXT = 3;
    public static final int CLOSE = 5;

    public static final String MEDIA_CONTROL_ACTION = "com.voicecontroller.music.cmd";
    public static final String PLAY_CONTROL_ACTION = "com.voicecontroller.music.play";

    private int state;

    private Player mPlayer;
    private Queue<Track> tracks;
    private MediaSessionCompat mySession;

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
        mySession = new MediaSessionCompat(this, Settings.APP_TAG);

        mySession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                resume();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onStop() {
                pause();
            }
        });
        mySession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
    }

    @Override
    public void onDestroy() {
        if (mPlayer != null && !mPlayer.isShutdown()) {
            mPlayer.shutdown();
            Spotify.destroyPlayer(this);
            try {
                mPlayer.awaitTermination(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                Log.e("NativePlayer", "Raised exceptiong when awaiting termination...", e);
                Crashlytics.logException(e);
            }
            mPlayer = null;
        }
        stopNotification();
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

        mySession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, tracks.peek().getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, tracks.peek().getName())
                .build());

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_music);
        builder.setContentTitle(tracks.peek().getName())
                .setContentText(tracks.peek().getArtist())
                .setLargeIcon(artwork);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(Color.rgb(38, 50, 56));
            builder.setStyle(new Notification.MediaStyle()
                    .setMediaSession((MediaSession.Token) mySession.getSessionToken().getToken()));

            if (state == PlaybackStateCompat.STATE_PLAYING) {
                builder.addAction(android.R.drawable.ic_media_pause, "Pause", getIntentFor(PAUSE));
            } else {
                builder.addAction(android.R.drawable.ic_media_play, "Play", getIntentFor(PLAY));
            }

            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", getIntentFor(CLOSE));
        }

        Notification noti = builder.build();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);

            contentView.setImageViewBitmap(R.id.thumbnail_notification_tv, artwork);
            contentView.setTextViewText(R.id.trackname_notification_tv, tracks.peek().getName());
            contentView.setTextViewText(R.id.artistname_notification_tv, tracks.peek().getArtist());

            if (state == PlaybackStateCompat.STATE_PLAYING) {
                contentView.setOnClickPendingIntent(R.id.play_pause_notification_bt, getIntentFor(PAUSE));
                contentView.setImageViewResource(R.id.play_pause_notification_bt, R.drawable.pause);
            } else {
                contentView.setOnClickPendingIntent(R.id.play_pause_notification_bt, getIntentFor(PLAY));
                contentView.setImageViewResource(R.id.play_pause_notification_bt, R.drawable.play);
            }

            contentView.setOnClickPendingIntent(R.id.close_notification_bt, getIntentFor(CLOSE));

            noti.bigContentView = contentView;
        }

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_PAUSE);
        stateBuilder.setState(state, 0, 1);

        PlaybackStateCompat pbackState = stateBuilder.build();
        mySession.setPlaybackState(pbackState);

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

                if (track != null) {
                    tracks.add(track);
                }

                // Initialization.
                if ((mPlayer == null || !mPlayer.isInitialized() || mPlayer.isShutdown()) && !Settings.MOCK_SPOTIFY_PLAYER) {
                    OAuthRecord record = OAuthService.getOAuthToken();
                    if (record != null) {
                        if (!record.isValid()) {
                            SpotifyWebAPI.refreshOAuth(record, this);
                        } else {
                            initializePlayerWithToken(record);
                        }
                    } else {
                        Log.w("NativePlayer", "No valid record found.");
                        createOAuthErrorNotification();
                    }
                } else if (Settings.MOCK_SPOTIFY_PLAYER) {
                    play();
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
            createOAuthErrorNotification();
        }
    }

    private void createOAuthErrorNotification() {
        // Build intent for notification content
        Intent viewIntent = new Intent(this, MainActivity.class);
        PendingIntent viewPendingIntent = PendingIntent.getActivity(this, 0, viewIntent, 0);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_music)
                        .setContentTitle(getString(R.string.could_not_play_title))
                        .setContentText(getString(R.string.login_to_play_songs))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(getString(R.string.login_to_play_songs)))
                        .setContentIntent(viewPendingIntent);

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);

        // Build the notification and issues it with notification manager.
        notificationManager.notify(ERROR_NOTIFICATION_ID, notificationBuilder.build());
    }

    private void initializePlayerWithToken(OAuthRecord record) {
        // No verifications with token are done here...
        String token = record.access_token;
        Config playerConfig = new Config(this, token, SpotifyWebAPI.CLIENT_ID);
        Spotify spotify = new Spotify();
        mPlayer = spotify.getPlayer(playerConfig, this, this);
    }


    public void pause() {
        if (Settings.MOCK_SPOTIFY_PLAYER || (mPlayer != null && mPlayer.isInitialized())) {
            state = PlaybackStateCompat.STATE_PAUSED;
            if (mPlayer != null) {
                mPlayer.pause();
            }
            updateTrackMetadata();
        }
    }

    public void resume() {
        if (Settings.MOCK_SPOTIFY_PLAYER || (mPlayer != null && mPlayer.isInitialized())) {
            state = PlaybackStateCompat.STATE_PLAYING;
            if (mPlayer != null) {
                mPlayer.resume();
            }
            updateTrackMetadata();
        }
    }

    public void play() {
        if (!tracks.isEmpty() && (Settings.MOCK_SPOTIFY_PLAYER || (mPlayer != null && mPlayer.isInitialized()))) {
            state = PlaybackStateCompat.STATE_PLAYING;

            if (mPlayer != null) {
                mPlayer.play(tracks.peek().getUri());
            }
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
        stopSelf();
    }

    @Override
    public void onInitialized() {
        mPlayer.addConnectionStateCallback(this);
        mPlayer.addPlayerNotificationCallback(this);
        play();
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        if (eventType.equals(EventType.LOST_PERMISSION) || eventType.equals(EventType.END_OF_CONTEXT)) {
            stopNotification();
            mySession.setActive(false);
        } else if (eventType.equals(EventType.TRACK_START)) {
            mySession.setActive(true);
        }
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String s) {
        Log.e("NativePlayer", "onPlaybackError: " + errorType.name() + " - " + s);
        close();
    }

    @Override
    public void onError(Throwable throwable) {
        Log.e("NativePlayer", "onError Spotify Initialization Error", throwable);
        Crashlytics.logException(throwable);
        close();
    }

    @Override
    public void onLoggedIn() {
    }

    @Override
    public void onLoggedOut() {

    }

    @Override
    public void onLoginFailed(Throwable throwable) {
        Log.e("NativePlayer", "SpotifySDK Connection: onLoginFailed", throwable);
        Crashlytics.logException(throwable);
    }

    @Override
    public void onTemporaryError() {
    }

    @Override
    public void onNewCredentials(String s) {
    }

    @Override
    public void onConnectionMessage(String s) {
    }
}
