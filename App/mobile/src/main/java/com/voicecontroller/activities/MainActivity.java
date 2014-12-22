package com.voicecontroller.activities;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.crashlytics.android.Crashlytics;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.authentication.SpotifyAuthentication;
import com.voicecontroller.R;
import com.voicecontroller.Settings;
import com.voicecontroller.oauth.OAuthService;
import com.voicecontroller.utils.SpotifyWebAPI;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Settings.ACTIVATE_CRASHLYTICS) {
            Crashlytics.start(this);
        }
        setContentView(R.layout.activity_main);
        SpotifyWebAPI.setMainActivity(this);
        findViewById(R.id.playBt).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.playBt) {
            Log.i("MainActivity", "PlayBt");
            if (!SpotifyWebAPI.checkOAuth()) {
                SpotifyWebAPI.callOAuthWindow();
            } else {
                // Initialize stuff and play play play...
                EditText et = (EditText) findViewById(R.id.trackEt);
                String queryName = et.getText().toString();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri uri = intent.getData();
        if (uri != null) {
            AuthenticationResponse response = SpotifyAuthentication.parseOauthResponse(uri);
            OAuthService.setOAuthToken(response);
        }
    }
}
