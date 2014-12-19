package com.voicecontroller.oauth;


import android.util.Log;
import com.orm.query.Select;
import com.spotify.sdk.android.authentication.AuthenticationResponse;


public class OAuthService {

    public static OAuthRecord getOAuthToken() {

        OAuthRecord record = Select.from(OAuthRecord.class).orderBy("expiration DESC").first();

        long currentTime = System.currentTimeMillis() / 1000;
        if (record != null) {

            Log.i("OAuthService", "Found OAuthToken: " + currentTime + " expiresIn: " +
                    record.expiration + " now is " + currentTime);

            if (record.expiration > currentTime) {
                return record;
            }
        } else {
            Log.w("OAuthService", "No OAuth Record found.");
        }

        return null;
    }

    public static void setOAuthToken(AuthenticationResponse response) {
        long expirationTime = System.currentTimeMillis() / 1000 + (long) response.getExpiresIn();
        OAuthRecord record = new OAuthRecord(response.getAccessToken(), expirationTime);
        record.save();

        Log.i("OAuthService", "New Token Saved:" + record.toString());
    }

}
