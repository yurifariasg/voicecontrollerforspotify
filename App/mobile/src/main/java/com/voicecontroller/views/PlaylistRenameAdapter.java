package com.voicecontroller.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.voicecontroller.R;
import com.voicecontroller.models.Playlist;
import com.voicecontroller.settings.SettingContent;

import java.util.List;


public class PlaylistRenameAdapter extends ArrayAdapter<Playlist> {

    public PlaylistRenameAdapter(Context context, List<Playlist> list) {
        super(context, 0, list);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Playlist playlist = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.playlist_renaming_view, parent, false);
        }

        String queryName = playlist.nameForQuery == null ? playlist.name : playlist.nameForQuery;

        ((TextView)convertView.findViewById(R.id.playlistQueryNameTv)).setText(queryName);
        ((TextView)convertView.findViewById(R.id.playlistRealNameTv)).setText(playlist.name);

        return convertView;
    }
}
