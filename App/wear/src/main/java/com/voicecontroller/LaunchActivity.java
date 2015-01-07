package com.voicecontroller;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;


public class LaunchActivity extends Activity implements MessageCallback {

    private static final long MINIMUM_QUERY_TIME_IN_MS = 3000;
    private static final int SPEECH_REQUEST_CODE = 0;
    private static final boolean SHOULD_FAKE_QUERY = false;

    private String query;

    private long startTimestamp;
    private Handler queryFinishHandler;

    private View mainLayout;
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
                mainLayout = stub.findViewById(R.id.mainLayout);

                if (query != null && !query.isEmpty()) {
                    queryTv.setText(query);
                    mainLayout.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        query = getIntent().getStringExtra(SearchManager.QUERY);
        handleQuery(true);
    }

    private void handleQuery(boolean cameFromSystemVoiceAction) {
        if (query != null && !query.isEmpty()) {
            if (cameFromSystemVoiceAction && query.toLowerCase().equals("music")) {
                displaySpeechRecognizer();
            } else {
                if (query.startsWith("play ")) {
                    query = query.substring(5).trim(); // Ignore play
                }

                if (queryTv != null && mainLayout != null) {
                    queryTv.setText(query);
                    mainLayout.setVisibility(View.VISIBLE);
                }

                startTimestamp = System.currentTimeMillis();
                MobileConnection.getInstance().sendQuery(query, this);
            }
        } else if (SHOULD_FAKE_QUERY) {
            query = "Anavae World in a Bottle next";
            handleQuery(false);
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
            query = results.get(0);
            handleQuery(false);
        } else if (requestCode == SPEECH_REQUEST_CODE) {
            finish();
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
