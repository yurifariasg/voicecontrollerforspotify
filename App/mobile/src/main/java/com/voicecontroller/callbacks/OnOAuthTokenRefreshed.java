package com.voicecontroller.callbacks;

import com.voicecontroller.oauth.OAuthRecord;


public interface OnOAuthTokenRefreshed {

    public void onOAuthTokenRefreshed(OAuthRecord record);
}
