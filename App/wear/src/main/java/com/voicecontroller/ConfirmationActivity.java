package com.voicecontroller;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.WatchViewStub;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class ConfirmationActivity extends Activity
        implements DelayedConfirmationView.DelayedConfirmationListener{

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
            MobileConnection.getInstance().confirmTrack(trackUri);
        }
        finish();

        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    @Override
    protected void onDestroy() {
        image.recycle();
        image = null;
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
