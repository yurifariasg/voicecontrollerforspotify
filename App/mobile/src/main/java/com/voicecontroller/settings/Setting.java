package com.voicecontroller.settings;

import com.orm.SugarRecord;


public class Setting extends SugarRecord<Setting> {

    public int settingid;
    public int val;

    public Setting() {
    }

    public Setting(int id, int val) {
        this.settingid = id;
        this.val = val;
    }
}
