package com.voicecontroller.nativeplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.session.MediaSession;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import com.crashlytics.android.Crashlytics;
import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.playback.Config;
import com.spotify.sdk.android.playback.ConnectionStateCallback;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;
import com.spotify.sdk.android.playback.PlayerState;
import com.spotify.sdk.android.playback.PlayerStateCallback;
import com.voicecontroller.R;
import com.voicecontroller.activities.MainActivity;
import com.voicecontroller.callbacks.OnOAuthTokenRefreshed;
import com.voicecontroller.models.Profile;
import com.voicecontroller.models.QueryResults;
import com.voicecontroller.models.Track;
import com.voicecontroller.models.TrackQueue;
import com.voicecontroller.oauth.OAuthRecord;
import com.voicecontroller.oauth.OAuthService;
import com.voicecontroller.settings.Settings;
import com.voicecontroller.utils.SpotifyWebAPI;

import org.json.JSONException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;


public class NativePlayer extends Service implements PlayerNotificationCallback,
        ConnectionStateCallback, Player.InitializationObserver, OnOAuthTokenRefreshed {

    public static final int PLAY = 1;
    public static final int PAUSE = 2;
    public static final int NEXT = 3;
    public static final int CLOSE = 5;
    public static final int MAIN = 6;
    public static final int PREVIOUS = 7;
    public static final int PREVIOUS_FORCE = 8;

    public static final String MEDIA_CONTROL_ACTION = "com.voicecontroller.music.cmd";
    public static final String PLAY_CONTROL_ACTION = "com.voicecontroller.music.play";

    private int state;

    private Player mPlayer;
    private TrackQueue tracks;
    private MediaSessionCompat mySession;

    private Notification ongoingNotification;
    private boolean isInRepeatMode = false;
    private boolean isInShuffleMode = false;

    public NativePlayer() {
        tracks = new TrackQueue();
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

            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                if (Intent.ACTION_MEDIA_BUTTON.equals(mediaButtonEvent.getAction())) {
                    KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (KeyEvent.KEYCODE_MEDIA_PLAY == event.getKeyCode()) {
                            resume();
                            return true;
                        } else if (KeyEvent.KEYCODE_MEDIA_PAUSE == event.getKeyCode()) {
                            pause();
                            return true;
                        } else if (KeyEvent.KEYCODE_MEDIA_STOP == event.getKeyCode()) {
                            pause();
                            return true;
                        } else if (KeyEvent.KEYCODE_MEDIA_NEXT == event.getKeyCode()) {
                            next();
                            return true;
                        } else if (KeyEvent.KEYCODE_MEDIA_PREVIOUS == event.getKeyCode()) {
                            previous(false);
                            return true;
                        }
                    }
                }
                return super.onMediaButtonEvent(mediaButtonEvent);
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
            } catch (InterruptedException e) {
                Log.e("NativePlayer", "Raised exceptiong when awaiting termination...", e);
                Crashlytics.logException(e);
            }
            mPlayer = null;
        }
        stopNotification(true);
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
            case PREVIOUS:
                Intent previousIntent = new Intent(this, NativePlayer.class);
                previousIntent.setAction(MEDIA_CONTROL_ACTION);
                previousIntent.putExtra("cmd", PREVIOUS);
                return PendingIntent.getService(this, PREVIOUS, previousIntent, 0);
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
            case MAIN:
                Intent resultIntent = new Intent(this, MainActivity.class);
                return PendingIntent.getActivity(this, 0, resultIntent, 0);
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

        Notification notification = ongoingNotification;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

            Notification.Builder builder = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_stat_music);
            builder.setContentTitle(tracks.peek().getName())
                    .setContentText(tracks.peek().getArtist())
                    .setLargeIcon(artwork)
                    .setDeleteIntent(getIntentFor(CLOSE));
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);

            builder.setContentIntent(getIntentFor(MAIN));

            builder.setColor(Color.rgb(38, 50, 56));
            builder.setStyle(new Notification.MediaStyle().setShowActionsInCompactView(1, 2)
                    .setMediaSession((MediaSession.Token) mySession.getSessionToken().getToken()));

            builder.addAction(android.R.drawable.ic_media_previous, "Previous", getIntentFor(PREVIOUS));
            if (state == PlaybackStateCompat.STATE_PLAYING) {
                builder.addAction(android.R.drawable.ic_media_pause, "Pause", getIntentFor(PAUSE));
            } else {
                builder.addAction(android.R.drawable.ic_media_play, "Play", getIntentFor(PLAY));
            }

            builder.addAction(android.R.drawable.ic_media_next, "Next", getIntentFor(NEXT));

            notification = builder.build();

        } else if (notification == null) {
            Notification.Builder builder = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_stat_music);
            builder.setContentTitle(tracks.peek().getName())
                    .setContentText(tracks.peek().getArtist())
                    .setLargeIcon(artwork)
                    .setDeleteIntent(getIntentFor(CLOSE));

            builder.setContentIntent(getIntentFor(MAIN));
            notification = builder.build();
            notification.bigContentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
        }

        if (notification != null) {
            notification.bigContentView.setImageViewBitmap(R.id.thumbnail_notification_tv, artwork);
            notification.bigContentView.setTextViewText(R.id.trackname_notification_tv, tracks.peek().getName());
            notification.bigContentView.setTextViewText(R.id.artistname_notification_tv, tracks.peek().getArtist());

            if (state == PlaybackStateCompat.STATE_PLAYING) {
                notification.bigContentView.setOnClickPendingIntent(R.id.play_pause_notification_bt, getIntentFor(PAUSE));
                notification.bigContentView.setImageViewResource(R.id.play_pause_notification_bt, android.R.drawable.ic_media_pause);
            } else {
                notification.bigContentView.setOnClickPendingIntent(R.id.play_pause_notification_bt, getIntentFor(PLAY));
                notification.bigContentView.setImageViewResource(R.id.play_pause_notification_bt, android.R.drawable.ic_media_play);
            }

            notification.bigContentView.setOnClickPendingIntent(R.id.previous_notification_bt, getIntentFor(PREVIOUS));
            notification.bigContentView.setOnClickPendingIntent(R.id.next_notification_bt, getIntentFor(NEXT));
            notification.bigContentView.setOnClickPendingIntent(R.id.close_notification_bt, getIntentFor(CLOSE));
        }

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        stateBuilder.setState(state, 0, 1);

        PlaybackStateCompat pbackState = stateBuilder.build();
        mySession.setPlaybackState(pbackState);

        ongoingNotification = notification;
        if (state != PlaybackStateCompat.STATE_PAUSED) {
            startForeground(Settings.NOTIFICATION_ID, ongoingNotification);
        } else {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(Settings.NOTIFICATION_ID, ongoingNotification);
        }
    }

    public void stopNotification(boolean clearNotification) {
        stopForeground(clearNotification);
        mySession.setActive(false);
        if (clearNotification) {
            ongoingNotification = null;
        }
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
                        next();
                        break;
                    case PREVIOUS:
                        previous(false);
                        break;
                    case PREVIOUS_FORCE:
                        previous(true);
                        break;
                    case CLOSE:
                        close();
                        break;
                    default:
                        break;
                }
            } else if (intent.getAction().equals(PLAY_CONTROL_ACTION)) {

                QueryResults results = QueryResults.fromBundle(intent.getBundleExtra("result"));
                handlePlayControlActionAsync(results);

            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void handlePlayControlActionAsync(final QueryResults results) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    handlePlayControlAction(results);
                } catch (Exception e) {
                    Log.e(Settings.APP_TAG, "Exception", e);
                    Crashlytics.logException(e);
                }
                return null;
            }
        }.execute();
    }

    private void handlePlayControlAction(QueryResults results) throws JSONException, IOException {

        OAuthRecord record = OAuthService.getOAuthToken();
        if (record == null) {
            createOAuthErrorNotification();
            return;
        } else if (!record.isValid()) {
            SpotifyWebAPI.refreshOAuth(record);
        }
        Profile profile = SpotifyWebAPI.getUserProfile(record, false);
        results.fetchTracks(profile);

        boolean shouldEnqueue = results.getQuery().shouldEnqueue();
        boolean shouldRepeat = results.getQuery().shouldRepeat();
        boolean shouldShuffle = results.getQuery().shouldShuffle();

        isInRepeatMode = shouldRepeat;
        isInShuffleMode = shouldShuffle;

        if (shouldShuffle) {
            Collections.shuffle(Arrays.asList(results.getTracks()));
        }

        if (!shouldEnqueue) {
            tracks.clear();
        }

        if (results.getTracks() != null) {
            tracks.addAll(Arrays.asList(results.getTracks()));

            if ((mPlayer == null || !mPlayer.isInitialized() || mPlayer.isShutdown()) && !Settings.MOCK_SPOTIFY_PLAYER) {
                initializePlayerWithToken(record);
            } else if (Settings.MOCK_SPOTIFY_PLAYER) {
                play();
            } else if (!shouldEnqueue) {
                play();
            }
        }
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
        notificationManager.notify(Settings.ERROR_NOTIFICATION_ID, notificationBuilder.build());
    }

    private void initializePlayerWithToken(OAuthRecord record) {
        // No verifications with token are done here...
        String token = record.access_token;
        Config playerConfig = new Config(this, token, SpotifyWebAPI.CLIENT_ID);
        Spotify spotify = new Spotify();
        mPlayer = spotify.getPlayer(playerConfig, this, this);
    }


    public void pause() {
        if (Settings.MOCK_SPOTIFY_PLAYER || (mPlayer != null && mPlayer.isInitialized()) &&
                state != PlaybackStateCompat.STATE_PAUSED) {
            state = PlaybackStateCompat.STATE_PAUSED;
            if (mPlayer != null) {
                mPlayer.pause();
                stopNotification(false);
            }
            updateTrackMetadata();
        }
    }

    public void next() {
        if (tracks.size() <= 1 && !isInRepeatMode) {
            pause();
        } else {
            playNext();
        }
    }

    public void previous(final boolean force) {
        if (mPlayer != null && mPlayer.isInitialized()) {
            mPlayer.getPlayerState(new PlayerStateCallback() {
                @Override
                public void onPlayerState(PlayerState playerState) {
                    int positionMs = playerState.positionInMs;
                    if (positionMs < 3000 || force) {
                        playPrevious();
                    } else {
                        mPlayer.seekToPosition(0);
                    }

                }
            });
        }
    }

    private void playPrevious() {
        Track previousSong = tracks.previous();
        if (previousSong != null) {
            play();
        } else {
            pause();
        }
    }

    public void resume() {
        if (Settings.MOCK_SPOTIFY_PLAYER || (mPlayer != null && mPlayer.isInitialized()) &&
                state != PlaybackStateCompat.STATE_PLAYING) {
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
            if (tracks.size() == 1) {
                if (isInRepeatMode) {
                    tracks.poll();
                    tracks.refill();
                    if (isInShuffleMode) {
                        tracks.shuffle();
                    }
                    play();
                } else {
                    pause();
                    stopNotification(false);
                }
            } else {
                tracks.remove();
                play();
            }
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
        if (eventType.equals(EventType.LOST_PERMISSION)) {
            stopNotification(true);
        } else if (eventType.equals(EventType.END_OF_CONTEXT)) {
            playNext();
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
