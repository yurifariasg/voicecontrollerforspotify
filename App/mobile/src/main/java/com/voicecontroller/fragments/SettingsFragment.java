package com.voicecontroller.fragments;

import android.os.Bundle;
import android.app.ListFragment;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.voicecontroller.settings.SettingContent;
import com.voicecontroller.views.SettingsAdapter;


public class SettingsFragment extends ListFragment {

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SettingsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingContent.updateValuesFromDatabase();
        setListAdapter(new SettingsAdapter(getActivity(), SettingContent.getItems()));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        SettingContent.SettingItem settingSelected = (SettingContent.SettingItem) getListView().getItemAtPosition(position);
        settingSelected.value++;

        if (settingSelected.optionsResId == null) {
            if (settingSelected.value >= settingSelected.options.length) {
                settingSelected.value = 0;
            }
        } else {
            if (settingSelected.value >= settingSelected.optionsResId.length) {
                settingSelected.value = 0;
            }
        }

        SettingContent.saveValuesToDatabase();

        ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
    }

}
