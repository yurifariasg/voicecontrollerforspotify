package com.voicecontroller.oauth;


import com.orm.StringUtil;
import com.orm.SugarRecord;
import com.voicecontroller.settings.Settings;

import java.util.Arrays;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.MongeElkan;

public class OAuthRecord extends SugarRecord<OAuthRecord> {

    public String access_token;
    public String refresh_token;
    public long expiration;
    public String permissions;

    public OAuthRecord() {
    }

    public OAuthRecord(String access_token, String refresh_token, long expiresIn) {
        this.access_token = access_token;
        this.refresh_token = refresh_token;
        this.expiration = expiresIn;
    }

    public boolean isValid() {
        boolean permissionsOk =  (Arrays.equals(getPermissions(), Settings.SPOTIFY_USER_PERMISSIONS));
        long now = System.currentTimeMillis() / 1000;
        return permissionsOk && now < expiration;
    }

    public void setPermissions(String... permissionsArray) {
        permissions = "";
        for (int i = 0 ; i < permissionsArray.length ; i++) {
            if (i != 0) {
                permissions += "," + permissionsArray[i];
            } else {
                permissions += permissionsArray[i];
            }
        }
    }

    public String[] getPermissions() {
        if (permissions != null && !permissions.isEmpty()) {
            return permissions.split(",");
        } else {
            return new String[] {};
        }
    }

    @Override
    public String toString() {
        return "<OAuthRecord> " + access_token + " - " + refresh_token + " - " + this.expiration;
    }
}
