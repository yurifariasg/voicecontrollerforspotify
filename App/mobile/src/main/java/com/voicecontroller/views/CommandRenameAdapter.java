package com.voicecontroller.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.voicecontroller.R;
import com.voicecontroller.models.MediaCommand;
import com.voicecontroller.models.Playlist;

import java.util.List;


public class CommandRenameAdapter extends ArrayAdapter<MediaCommand> {

    public CommandRenameAdapter(Context context, List<MediaCommand> list) {
        super(context, 0, list);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        MediaCommand command = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.playlist_renaming_view, parent, false);
        }

        ((TextView)convertView.findViewById(R.id.playlistQueryNameTv)).setText(command.name);
        ((TextView)convertView.findViewById(R.id.playlistRealNameTv)).setText(command.type); // Some conversion here...

        return convertView;
    }
}
