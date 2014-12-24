package com.voicecontroller.settings;


import com.orm.query.Condition;
import com.orm.query.Select;

public class Settings {

    public enum ID {
        USE_NATIVE_PLAYER(0),
        BLUR_IMAGES(1);

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

    public static boolean ENABLE_CRASHLYTICS = false;
    public static boolean USE_WAKELOCK_ON_SENDING_TRACK_TO_SPOTIFY = false;
    public static boolean USE_KEYGUARD_ON_SENDING_TRACK_TO_SPOTIFY = false;
    public static int SEARCH_TRACK_MAXIMUM_RETRIES = 5;

    public static float getBlur() {
        Setting s = Select.from(Setting.class).where(Condition.prop("settingid").eq(ID.BLUR_IMAGES.val())).first();
        if (s != null) {
            String val = SettingContent.getItems().get(s.settingid).options[s.val];
            return Integer.parseInt(val);
        } else {
            return 8f; // Default.
        }
    }

    public static boolean shouldUseNativePlayer() {
        Setting s = Select.from(Setting.class).where(Condition.prop("settingid").eq(ID.USE_NATIVE_PLAYER.val())).first();
        return s == null || s.val == 0;
    }
}
