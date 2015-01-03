package com.voicecontroller.fragments;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.orm.query.Condition;
import com.orm.query.Select;
import com.voicecontroller.R;
import com.voicecontroller.models.MediaCommand;
import com.voicecontroller.models.MediaCommandType;
import com.voicecontroller.models.Playlist;
import com.voicecontroller.models.Profile;
import com.voicecontroller.oauth.OAuthRecord;
import com.voicecontroller.oauth.OAuthService;
import com.voicecontroller.utils.GeneralUtils;
import com.voicecontroller.views.CommandRenameAdapter;
import com.voicecontroller.views.PlaylistRenameAdapter;

import java.util.ArrayList;
import java.util.List;


public class MediaCommandRenameFragment extends ListFragment {

    public static MediaCommandRenameFragment newInstance() {
        MediaCommandRenameFragment fragment = new MediaCommandRenameFragment();
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MediaCommandRenameFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ArrayList<MediaCommand> mediaCommandArrayList = new ArrayList<>();

        for (MediaCommandType type : MediaCommandType.values()) {
            MediaCommand command = queryCommand(type);
            if (command == null) {
                command = createCommand(type);
            }
            mediaCommandArrayList.add(command);
        }

        setListAdapter(new CommandRenameAdapter(getActivity(), mediaCommandArrayList));
    }

    private MediaCommand createCommand(MediaCommandType type) {
        MediaCommand command = new MediaCommand();
        command.name = GeneralUtils.toCapitalize(type.toString());
        command.type = type.toString();
        command.save();
        return command;
    }

    private MediaCommand queryCommand(MediaCommandType type) {
        return Select.from(MediaCommand.class).where(Condition.prop("TYPE").eq(type.toString())).first();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setFocusable(true);

        final MediaCommand command = (MediaCommand) getListView().getItemAtPosition(position);

        input.setText(command.name == null ?  "" : command.name);
        if (command.name != null) {
            input.setSelection(command.name.length());
        }

        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
        alertDialog.setTitle(getString(R.string.rename_command_dialog_title));
        alertDialog.setMessage(getString(R.string.rename_command_dialog_desc));
        alertDialog.setView(input, 40, 0, 40, 0); // 40 spacing, left and right
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.rename), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Clicked
                if (input.getText() != null && !input.getText().toString().isEmpty()) {
                    command.name= input.getText().toString();
                } else {
                    command.name = GeneralUtils.toCapitalize(command.type);
                }
                command.save();

                ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
            }
        });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        alertDialog.setCancelable(true);
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
    }

}
