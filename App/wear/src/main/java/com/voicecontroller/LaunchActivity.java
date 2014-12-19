package com.voicecontroller;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;


public class LaunchActivity extends Activity implements MessageCallback {

    private String query;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileConnection.createInstance(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        query = getIntent().getStringExtra(SearchManager.QUERY);
        query = query == null ? "Dragonette Let It Go" : query;

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                ((TextView) stub.findViewById(R.id.queryNameTv)).setText(query);
            }
        });

        MobileConnection.getInstance().sendQuery(query, this);
    }

    @Override
    public void onMessageSent() {
//        Intent intent = new Intent(this, android.support.wearable.activity.ConfirmationActivity.class);
//        intent.putExtra(android.support.wearable.activity.ConfirmationActivity.EXTRA_ANIMATION_TYPE,
//                android.support.wearable.activity.ConfirmationActivity.SUCCESS_ANIMATION);
//        intent.putExtra(android.support.wearable.activity.ConfirmationActivity.EXTRA_MESSAGE,
//                "Query Sent");
//        startActivity(intent);
        finish();
    }

    @Override
    public void onMessageFailed() {
        Toast.makeText(this, "Message Failed...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
