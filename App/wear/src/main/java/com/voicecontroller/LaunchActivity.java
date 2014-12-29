package com.voicecontroller;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;


public class LaunchActivity extends Activity implements MessageCallback {

    private static final long MINIMUM_QUERY_TIME_IN_MS = 3000;
    private static final int SPEECH_REQUEST_CODE = 0;

    private String query;

    private long startTimestamp;
    private Handler queryFinishHandler;

    private TextView queryTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        MobileConnection.createInstance(this);
        queryFinishHandler = new Handler();
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                queryTv = (TextView) stub.findViewById(R.id.queryNameTv);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        query = getIntent().getStringExtra(SearchManager.QUERY);
        handleQuery(query);
    }

    private void handleQuery(String query) {
        if (query != null && !query.isEmpty()) {
            if (query.startsWith("play")) {
                query = query.substring(4).trim(); // Ignore play
            }

            if (queryTv != null) {
                queryTv.setText(query);
            } else {
                final String queryText = query;
                final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
                stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
                    @Override
                    public void onLayoutInflated(WatchViewStub stub) {
                        queryTv = (TextView) stub.findViewById(R.id.queryNameTv);
                        queryTv.setText(queryText);
                    }
                });
            }

            startTimestamp = System.currentTimeMillis();

            MobileConnection.getInstance().sendQuery(query, this);
        } else {
            displaySpeechRecognizer();
        }
    }


    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            handleQuery(spokenText);
        }
        super.onActivityResult(requestCode, resultCode, data);
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
        Intent intent = new Intent(this, android.support.wearable.activity.ConfirmationActivity.class);
        intent.putExtra(android.support.wearable.activity.ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                android.support.wearable.activity.ConfirmationActivity.FAILURE_ANIMATION);
        intent.putExtra(android.support.wearable.activity.ConfirmationActivity.EXTRA_MESSAGE,
                getString(R.string.query_failed));
        startActivity(intent);
        finish();
    }
}
