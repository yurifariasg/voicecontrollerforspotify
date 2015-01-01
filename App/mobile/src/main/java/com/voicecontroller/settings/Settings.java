package com.voicecontroller.settings;


import com.orm.query.Condition;
import com.orm.query.Select;
import com.voicecontroller.BuildConfig;

public class Settings {

    public enum ID {
        USE_NATIVE_PLAYER(0),
        BLUR_IMAGES(1),
        CONFIRMATION_TIME(2),
        SIMILARITY_PERCENTAGE(3);

        private int id;
        ID(int id){
            this.id = id;
        }

        public int val() {
            return this.id;
        }
    }

    // These are real constants
    public static final String APP_TAG = "VoiceControllerForSpotify";

    public static final int MINIMUM_TIME_BETWEEN_PLAYLIST_TRACKS_REFRESH = 3600 * 24; // one day
    public static final int MINIMUM_TIME_BETWEEN_PLAYLIST_REFRESH = 3600; // 1hr
    public static final int NOTIFICATION_ID = 1;
    public static final int ERROR_NOTIFICATION_ID = 2;
    public static boolean ENABLE_CRASHLYTICS = true && !BuildConfig.DEBUG;
    public static boolean MOCK_WATCH_REQUEST = false && BuildConfig.DEBUG;
    public static boolean EMULATOR_DEBUGGING_ACTIVE = false && BuildConfig.DEBUG;
    public static boolean MOCK_SPOTIFY_PLAYER = false && BuildConfig.DEBUG;
    public static boolean USE_WAKELOCK_ON_SENDING_TRACK_TO_SPOTIFY = false;
    public static boolean USE_KEYGUARD_ON_SENDING_TRACK_TO_SPOTIFY = false;
    public static int SEARCH_TRACK_MAXIMUM_RETRIES = 5;
    public static String[] SPOTIFY_USER_PERMISSIONS = new String[]{"user-read-private", "streaming", "playlist-read-private"};

    public static float getBlur() {
        Setting s = Select.from(Setting.class).where(Condition.prop("settingid").eq(ID.BLUR_IMAGES.val())).first();
        if (s != null) {
            String val = SettingContent.getItems().get(s.settingid).options[s.val];
            return Integer.parseInt(val);
        } else {
            return 8f; // Default.
        }
    }

    public static int getConfirmationTime() {
        Setting s = Select.from(Setting.class).where(Condition.prop("settingid").eq(ID.CONFIRMATION_TIME.val())).first();
        if (s != null) {
            String val = SettingContent.getItems().get(s.settingid).options[s.val];
            return Integer.parseInt(val);
        } else {
            return 4; // Default.
        }
    }

    public static boolean shouldUseNativePlayer() {
        Setting s = Select.from(Setting.class).where(Condition.prop("settingid").eq(ID.USE_NATIVE_PLAYER.val())).first();
        return s == null || s.val == 0;
    }

    public static float getSimilarityValue() {
        Setting s = Select.from(Setting.class).where(Condition.prop("settingid").eq(ID.SIMILARITY_PERCENTAGE.val())).first();
        if (s != null) {
            String val = SettingContent.getItems().get(s.settingid).options[s.val];
            return Integer.parseInt(val.substring(0, val.length()-1)) / 100f;  // Exclude the # sign
        } else {
            return .7f; // Default.
        }
    }
}
