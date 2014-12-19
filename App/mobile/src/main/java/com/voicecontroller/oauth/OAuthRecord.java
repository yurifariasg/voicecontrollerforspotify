package com.voicecontroller.oauth;


import com.orm.SugarRecord;

public class OAuthRecord extends SugarRecord<OAuthRecord> {

    public String token;
    public long expiration;

    public OAuthRecord() {
    }

    public OAuthRecord(String token, long expiresIn) {
        this.token = token;
        this.expiration = expiresIn;
    }

    @Override
    public String toString() {
        return "<OAuthRecord> " + token + " - " + this.expiration;
    }
}
