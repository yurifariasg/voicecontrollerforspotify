package com.voicecontroller;

import android.app.Activity;
import android.app.SearchManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;
import android.widget.Toast;


public class LaunchActivity extends Activity implements MessageCallback {

    private static final long MINIMUM_QUERY_TIME_IN_MS = 3000;

    private String query;

    private long startTimestamp;
    private Handler queryFinishHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileConnection.createInstance(this);
        queryFinishHandler = new Handler();
    }

    @Override
    protected void onStart() {
        super.onStart();
        query = getIntent().getStringExtra(SearchManager.QUERY);
        if (query != null && !query.isEmpty()) {
            final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
            stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
                @Override
                public void onLayoutInflated(WatchViewStub stub) {
                    ((TextView) stub.findViewById(R.id.queryNameTv)).setText(query);
                }
            });
            startTimestamp = System.currentTimeMillis();

            MobileConnection.getInstance().sendQuery(query, this);
        } else {
            finish();
        }
    }

    @Override
    public void onMessageSent() {
        long nowTimestamp = System.currentTimeMillis();
        long diff = nowTimestamp - startTimestamp;
        if (diff > MINIMUM_QUERY_TIME_IN_MS) {
            finish();
        } else {
            queryFinishHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, MINIMUM_QUERY_TIME_IN_MS - diff);
        }
    }

    @Override
    public void onMessageFailed() {
        Toast.makeText(this, "Message Failed...", Toast.LENGTH_SHORT).show();
    }
}
