package com.voicecontroller;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Random;


public class ConfirmationActivity extends Activity
        implements DelayedConfirmationView.DelayedConfirmationListener, MessageCallback {

    private static final int CONFIRMATION_ACTIVITY_CODE = 5;
    private static final int[] DEFAULT_BACKGROUNDS = new int[] {R.drawable.disco, R.drawable.guitar, R.drawable.lights, R.drawable.dj};

    private DelayedConfirmationView mDelayedView;

    private String trackName;
    private String artistName;
    private String trackUri;
    private Bitmap image = null;
    private boolean trackConfirmed = true;

    private PowerManager.WakeLock mWakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        trackName = getIntent().getStringExtra("trackName");
        artistName = getIntent().getStringExtra("artistName");
        trackUri = getIntent().getStringExtra("trackUri");
        byte[] imgBytes = getIntent().getByteArrayExtra("image");
        final int confirmationTime = getIntent().getIntExtra("confirmation_time", 4); // Default

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "VoiceController");

        if (imgBytes != null && imgBytes.length > 0) {
            image = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
        } else {
            Random r = new Random();
            int randomIndex = r.nextInt(DEFAULT_BACKGROUNDS.length);
            image = BitmapFactory.decodeResource(getResources(), DEFAULT_BACKGROUNDS[randomIndex]);
        }

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                TextView trackNameTv = (TextView) stub.findViewById(R.id.trackNameTv);
                TextView artistNameTv = (TextView) stub.findViewById(R.id.artistNameTv);

                trackNameTv.setText(trackName);
                artistNameTv.setText(artistName);

                mWakeLock.acquire();

                mDelayedView = (DelayedConfirmationView) stub.findViewById(R.id.delay_view);
                mDelayedView.setTotalTimeMs(confirmationTime * 1000);
                mDelayedView.setListener(getActivity());
                mDelayedView.start();

                if (image != null) {
                    ((ImageView) stub.findViewById(R.id.trackImage)).setImageBitmap(image);
                }
            }
        });
    }

    public ConfirmationActivity getActivity() {
        return this;
    }

    @Override
    public void onTimerFinished(View view) {
        if (trackConfirmed) {
            MobileConnection.getInstance(this).confirmTrack(trackUri, this);
        }

        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    @Override
    public void onMessageSent() {
        Intent intent = new Intent(this, android.support.wearable.activity.ConfirmationActivity.class);
        intent.putExtra(android.support.wearable.activity.ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                android.support.wearable.activity.ConfirmationActivity.SUCCESS_ANIMATION);
        startActivityForResult(intent, CONFIRMATION_ACTIVITY_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONFIRMATION_ACTIVITY_CODE) {
            finish();
        }
    }

    @Override
    public void onMessageFailed() {
        Intent intent = new Intent(this, android.support.wearable.activity.ConfirmationActivity.class);
        intent.putExtra(android.support.wearable.activity.ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                android.support.wearable.activity.ConfirmationActivity.FAILURE_ANIMATION);
        startActivityForResult(intent, CONFIRMATION_ACTIVITY_CODE);
    }

    @Override
    protected void onDestroy() {
        if (image != null) {
            new AsyncTask<Bitmap, Void, Void>() {
                @Override
                protected Void doInBackground(Bitmap... params) {
                    if (params[0] != null && !params[0].isRecycled()) {
                        params[0].recycle();
                    }
                    return null;
                }
            }.execute(image);
        }
        super.onDestroy();
    }

    @Override
    public void onTimerSelected(View view) {
        trackConfirmed = false;
        finish();

        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }
}
