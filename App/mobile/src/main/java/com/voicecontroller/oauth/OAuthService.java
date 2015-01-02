package com.voicecontroller.oauth;

import com.orm.query.Select;
import com.voicecontroller.models.Playlist;
import com.voicecontroller.models.Profile;
import com.voicecontroller.models.Track;


public class OAuthService {

    public static OAuthRecord getOAuthToken() {
        OAuthRecord record = Select.from(OAuthRecord.class).orderBy("expiration DESC").first();
        return record;
    }

    public static void clean() {
        Profile.deleteAll(Profile.class);
        OAuthRecord.deleteAll(OAuthRecord.class);
        Playlist.deleteAll(Playlist.class);
        Track.deleteAll(Track.class);
    }

}
