package com.voicecontroller.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.app.ListFragment;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.voicecontroller.settings.SettingContent;
import com.voicecontroller.views.SettingsAdapter;


public class SettingsFragment extends ListFragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    // TODO: Rename and change types of parameters
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        SettingContent.SettingItem settingSelected = (SettingContent.SettingItem) getListView().getItemAtPosition(position);
        settingSelected.value++;
        if (settingSelected.value >= settingSelected.options.length) {
            settingSelected.value = 0;
        }

        SettingContent.saveValuesToDatabase();

        ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
    }

}
