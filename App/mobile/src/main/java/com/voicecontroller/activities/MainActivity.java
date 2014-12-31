package com.voicecontroller.activities;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.voicecontroller.BuildConfig;
import com.voicecontroller.R;
import com.voicecontroller.fragments.HelpFragment;
import com.voicecontroller.fragments.SettingsFragment;
import com.voicecontroller.callbacks.OnProfileAcquired;
import com.voicecontroller.fragments.LoginFragment;
import com.voicecontroller.fragments.ProfileFragment;
import com.voicecontroller.models.Profile;
import com.voicecontroller.models.QueryResults;
import com.voicecontroller.models.QueryType;
import com.voicecontroller.models.Track;
import com.voicecontroller.nativeplayer.NativePlayer;
import com.voicecontroller.oauth.OAuthRecord;
import com.voicecontroller.oauth.OAuthService;
import com.voicecontroller.settings.Settings;
import com.voicecontroller.utils.SpotifyWebAPI;

import java.lang.ref.WeakReference;

import io.fabric.sdk.android.Fabric;


public class MainActivity extends FragmentActivity implements View.OnClickListener, OnProfileAcquired {

    private FrameLayout fl;
    private WeakReference<MainActivity> mainActivity = null;
    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics crashlytics;
        if (BuildConfig.DEBUG) {
            crashlytics = new Crashlytics.Builder().disabled(BuildConfig.DEBUG).build();
        } else {
            crashlytics = new Crashlytics();
        }
        Fabric.with(this, crashlytics);

        if (Settings.MOCK_WATCH_REQUEST) {
            final MainActivity me = this;
            new AsyncTask<Void,Void,Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        QueryResults results = SpotifyWebAPI.search("rise against", QueryType.DEFAULT);
//                        results.fetchTracks("US");
                        Intent intent = new Intent(me, NativePlayer.class);
                        intent.setAction(NativePlayer.PLAY_CONTROL_ACTION);

                        Track[] tracks = results.getTracks();
                        Parcelable[] parcelables = new Parcelable[tracks.length];
                        for (int i = 0; i < tracks.length; i++) {
                            parcelables[i] = tracks[i].toBundle();
                        }
                        intent.putExtra("tracks", parcelables);
                        if (results.getQuery() != null) {
                            intent.putExtra("enqueue", results.getQuery().shouldEnqueue());
                        }
                        startService(intent);
                    } catch (Exception e) {
                        Log.e(Settings.APP_TAG, "Exception", e);
                    }
                    return null;
                }
            }.execute();
        }

        mainActivity = new WeakReference<>(this);
        setContentView(R.layout.activity_main);
        fl = (FrameLayout) findViewById(R.id.main_layout);
        initializeScreen();
    }

    @Override
    protected void onStart() {
        super.onStart();
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
        if (profile != null) {
            if (profile.product == null || !profile.product.equalsIgnoreCase("premium")) {
                Toast.makeText(this, getString(R.string.not_premium_account), Toast.LENGTH_LONG).show();
                OAuthService.clean();
                LoginFragment fragment = LoginFragment.newInstance();
                switchTo(fragment);
            } else {
                ProfileFragment fragment = ProfileFragment.newInstance(profile);
                switchTo(fragment);
                if (profile.getId() == null) {
                    profile.save();
                }
            }
        } else {
            logOut();
        }
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.logoutBt) {
            logOut();
        } else if (v.getId() == R.id.signInBt) {
            if (mFragment instanceof LoginFragment) {
                ((LoginFragment)mFragment).showLoading();
            }
            SpotifyWebAPI.callOAuthWindow(this);
        }

    }

    private void logOut() {
        OAuthService.clean();

        LoginFragment fragment = LoginFragment.newInstance();
        switchTo(fragment);
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
        } else if (id == R.id.action_help) {
            HelpFragment fragment = HelpFragment.newInstance();
            switchTo(fragment);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mFragment instanceof SettingsFragment || mFragment instanceof HelpFragment) {
            initializeScreen();
            invalidateOptionsMenu();
        } else {
            finish();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return !(mFragment instanceof SettingsFragment || mFragment instanceof HelpFragment);
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

        if (mainActivity.get() != null && !mainActivity.get().isFinishing()) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();

            if (mFragment != null) {
                if (mFragment instanceof SettingsFragment || mFragment instanceof HelpFragment) {
                    ft.setCustomAnimations(R.animator.slide_in_left, R.animator.slide_out_right);
                } else if (fragment instanceof SettingsFragment || fragment instanceof HelpFragment) {
                    ft.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left);
                } else if (fragment instanceof ProfileFragment) {
                    ft.setCustomAnimations(R.animator.slide_in_bottom, R.animator.slide_out_top);
                } else if (fragment instanceof LoginFragment) {
                    ft.setCustomAnimations(R.animator.slide_in_top, R.animator.slide_out_bottom);
                }
            }

            invalidateOptionsMenu();
            ft.replace(fl.getId(), fragment).commitAllowingStateLoss();
            mFragment = fragment;

            if (mFragment instanceof LoginFragment) {
                ((LoginFragment)mFragment).showButton();
            }
        }
    }
}
