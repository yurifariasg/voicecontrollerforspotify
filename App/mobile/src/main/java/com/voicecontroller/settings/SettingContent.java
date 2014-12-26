package com.voicecontroller.settings;

import android.util.SparseArray;

import com.google.android.gms.internal.id;
import com.orm.query.Condition;
import com.orm.query.Select;
import com.voicecontroller.R;
import com.voicecontroller.settings.Setting;
import com.voicecontroller.settings.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 */
public class SettingContent {

    public static List<SettingItem> ITEMS = new ArrayList<>();

    /**
     * A map of confirmations to show on fragment.
     */
    public static SparseArray<SettingItem> ITEM_MAP = new SparseArray<>();

    static {
        addItem(new SettingItem(Settings.ID.USE_NATIVE_PLAYER, R.string.use_native_player, R.string.use_native_player_desc, new int[] {R.string.yes, R.string.no}));
        addItem(new SettingItem(Settings.ID.BLUR_IMAGES, R.string.blur_images, R.string.blur_images_desc, new String[] {"8", "14", "0", "4"}));
        addItem(new SettingItem(Settings.ID.CONFIRMATION_TIME, R.string.confirmation_time, R.string.confirmation_time_desc, new String[] {"4", "5", "6", "7", "2", "3"}));
    }

    public static List<SettingItem> getItems() {
        return ITEMS;
    }

    private static void addItem(SettingItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id.val(), item);
    }

    public static SettingItem getItem(int id) {
        return ITEM_MAP.get(id);
    }

    public static void updateValuesFromDatabase() {
        Iterator<Setting> settingIt = Select.from(Setting.class).iterator();
        while (settingIt.hasNext()) {
            Setting s = settingIt.next();
            ITEM_MAP.get(s.settingid).value = s.val;
        }
    }

    public static void saveValuesToDatabase() {
        for (SettingItem s : getItems()) {
            List<Setting> savedSettings = Setting.find(Setting.class, "settingid = ?", String.valueOf(s.id.val()));
            if (!savedSettings.isEmpty()) {
                Setting saved = savedSettings.get(0);
                saved.val = s.value;
                saved.save();
            } else {
                Setting newSetting = new Setting();
                newSetting.settingid = s.id.val();
                newSetting.val = s.value;
                newSetting.save();
            }
        }
    }

    /**
     * The settings item to show on fragment.
     */
    public static class SettingItem {
        public Settings.ID id;
        public int nameResId;
        public int descriptionResId;
        public int value;
        public int[] optionsResId = null;
        public String[] options;

        public SettingItem(Settings.ID id, int nameResId, int descriptionResId, int[] optionsResId) {
            this.id = id;
            this.nameResId = nameResId;
            this.descriptionResId = descriptionResId;
            this.value = 0;
            this.optionsResId = optionsResId;
        }

        public SettingItem(Settings.ID id, int nameResId, int descriptionResId, String[] options) {
            this.id = id;
            this.nameResId = nameResId;
            this.descriptionResId = descriptionResId;
            this.value = 0;
            this.options = options;
        }

    }
}
