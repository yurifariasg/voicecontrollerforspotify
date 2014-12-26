package com.voicecontroller.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.voicecontroller.R;
import com.voicecontroller.settings.SettingContent;

import java.util.List;


public class SettingsAdapter extends ArrayAdapter<SettingContent.SettingItem> {

    public SettingsAdapter(Context context, List<SettingContent.SettingItem> list) {
        super(context, 0, list);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        SettingContent.SettingItem setting = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.setting_view, parent, false);
        }

        ((TextView)convertView.findViewById(R.id.settingNameTv)).setText(getContext().getString(setting.nameResId));
        ((TextView)convertView.findViewById(R.id.settingDescriptionTv)).setText(getContext().getString(setting.descriptionResId));

        if (setting.optionsResId == null) {
            ((TextView) convertView.findViewById(R.id.settingValueTv)).setText(setting.options[setting.value]);
        } else {
            ((TextView) convertView.findViewById(R.id.settingValueTv)).setText(getContext().getString(setting.optionsResId[setting.value]));
        }

        return convertView;
    }
}
