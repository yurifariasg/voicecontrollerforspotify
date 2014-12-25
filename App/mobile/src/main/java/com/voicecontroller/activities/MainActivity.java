package com.voicecontroller.activities;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.orm.query.Condition;
import com.orm.query.Select;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.authentication.SpotifyAuthentication;
import com.voicecontroller.R;
import com.voicecontroller.fragments.SettingsFragment;
import com.voicecontroller.settings.Settings;
import com.voicecontroller.callbacks.OnProfileAcquired;
import com.voicecontroller.fragments.LoginFragment;
import com.voicecontroller.fragments.ProfileFragment;
import com.voicecontroller.models.Profile;
import com.voicecontroller.oauth.OAuthRecord;
import com.voicecontroller.oauth.OAuthService;
import com.voicecontroller.utils.SpotifyWebAPI;


public class MainActivity extends FragmentActivity implements View.OnClickListener, OnProfileAcquired {

    private FrameLayout fl;
    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Settings.ENABLE_CRASHLYTICS) {
            Crashlytics.start(this);
        }

        setContentView(R.layout.activity_main);
        fl = (FrameLayout) findViewById(R.id.main_layout);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeScreen();
    }

    private void initializeScreen() {
        OAuthRecord record = OAuthService.getOAuthToken();
        if (record != null && record.isValid()) {
            SpotifyWebAPI.getProfileAsync(record, this);
        } else if (record != null) {
            SpotifyWebAPI.refreshOAuth(record, this);
        } else {
            LoginFragment fragment = LoginFragment.newInstance();
            switchTo(fragment);
        }
    }

    @Override
    public void onProfileAcquired(Profile profile) {
        if (profile.product == null || !profile.product.equalsIgnoreCase("premium")) {
            Toast.makeText(this, getString(R.string.not_premium_account), Toast.LENGTH_LONG).show();
            OAuthRecord.deleteAll(OAuthRecord.class);
            LoginFragment fragment = LoginFragment.newInstance();
            switchTo(fragment);
        } else {
            ProfileFragment fragment = ProfileFragment.newInstance(profile);
            switchTo(fragment);
            if (profile.getId() == null) {
                profile.save();
            }
        }
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.logoutBt) {
            Profile.deleteAll(Profile.class);
            OAuthRecord.deleteAll(OAuthRecord.class);

            LoginFragment fragment = LoginFragment.newInstance();
            switchTo(fragment);
        } else if (v.getId() == R.id.signInBt) {
            SpotifyWebAPI.callOAuthWindow(this);
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
            SettingsFragment fragment = SettingsFragment.newInstance();
            switchTo(fragment);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mFragment instanceof SettingsFragment) {
            initializeScreen();
            invalidateOptionsMenu();
        } else {
            finish();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return !(mFragment instanceof SettingsFragment);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri uri = intent.getData();
        if (uri != null) {
            SpotifyWebAPI.onOAuthCallback(uri, this);
        }
    }

    private void switchTo(Fragment fragment) {

        if (mFragment != null && mFragment.getClass() == fragment.getClass())
            return; // Cancel if they are from the same class

        FragmentTransaction ft = getFragmentManager().beginTransaction();

        if (mFragment != null) {
            if (mFragment instanceof SettingsFragment) {
                ft.setCustomAnimations(R.animator.slide_in_left, R.animator.slide_out_right);
            } else if (fragment instanceof SettingsFragment) {
                ft.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left);
            } else if (fragment instanceof ProfileFragment) {
                ft.setCustomAnimations(R.animator.slide_in_bottom, R.animator.slide_out_top);
            } else if (fragment instanceof LoginFragment) {
                ft.setCustomAnimations(R.animator.slide_in_top, R.animator.slide_out_bottom);
            }
        }

        invalidateOptionsMenu();
        ft.replace(fl.getId(), fragment).commit();
        mFragment = fragment;
    }
}
