package com.voicecontroller.oauth;

import com.orm.query.Select;


public class OAuthService {

    public static OAuthRecord getOAuthToken() {
        OAuthRecord record = Select.from(OAuthRecord.class).orderBy("expiration DESC").first();
        return record;
    }

}
