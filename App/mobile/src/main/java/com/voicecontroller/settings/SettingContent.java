package com.voicecontroller.settings;

import android.util.SparseArray;

import com.google.android.gms.internal.id;
import com.orm.query.Condition;
import com.orm.query.Select;
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

    /**
     * An array of sample (dummy) items.
     */
    public static List<SettingItem> ITEMS = new ArrayList<>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static SparseArray<SettingItem> ITEM_MAP = new SparseArray<>();

    static {
        // Add 3 sample items.
        addItem(new SettingItem(Settings.ID.USE_NATIVE_PLAYER, "Use native player", "(EXPERIMENTAL) Native player is more reliable than Spotify App.", new String[] {"YES", "NO"}));
        addItem(new SettingItem(Settings.ID.BLUR_IMAGES, "Blur album images", "Increase or reduce the blur on album artwork.", new String[] {"8", "14", "0", "4"}));
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
     * A dummy item representing a piece of content.
     */
    public static class SettingItem {
        public Settings.ID id;
        public String name;
        public String description;
        public int value;
        public String[] options;

        public SettingItem(Settings.ID id, String name, String description, String[] options) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.value = 0;
            this.options = options;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
