package com.voicecontroller.oauth;


import com.orm.SugarRecord;

public class OAuthRecord extends SugarRecord<OAuthRecord> {

    public String access_token;
    public String refresh_token;
    public long expiration;

    public OAuthRecord() {
    }

    public OAuthRecord(String access_token, String refresh_token, long expiresIn) {
        this.access_token = access_token;
        this.refresh_token = refresh_token;
        this.expiration = expiresIn;
    }

    public boolean isValid() {
        long now = System.currentTimeMillis() / 1000;
        return now < expiration;
    }

    @Override
    public String toString() {
        return "<OAuthRecord> " + access_token + " - " + refresh_token + " - " + this.expiration;
    }
}
