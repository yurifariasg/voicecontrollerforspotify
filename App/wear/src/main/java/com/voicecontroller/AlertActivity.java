package com.voicecontroller;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

public class AlertActivity extends Activity {

    private PowerManager.WakeLock mWakeLock;
    private String title;
    private String description;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);

        title = getIntent().getStringExtra("title");
        description = getIntent().getStringExtra("description");

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "VoiceController");
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {

                ((TextView)stub.findViewById(R.id.alertTitleTv)).setText(title);
                ((TextView)stub.findViewById(R.id.alertDescriptionTv)).setText(description);

                mWakeLock.acquire();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mWakeLock != null && mWakeLock.isHeld()) {
                            mWakeLock.release();
                        }
                    }
                }, 3000);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }
}
