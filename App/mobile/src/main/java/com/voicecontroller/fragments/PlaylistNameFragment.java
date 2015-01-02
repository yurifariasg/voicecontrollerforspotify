package com.voicecontroller.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.ListFragment;
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

import com.voicecontroller.models.Playlist;
import com.voicecontroller.models.Profile;
import com.voicecontroller.oauth.OAuthRecord;
import com.voicecontroller.oauth.OAuthService;
import com.voicecontroller.views.PlaylistRenameAdapter;

import java.util.List;


public class PlaylistNameFragment extends ListFragment {

    private static final String BLOCK_CHARACTER_SET = "~#^|$%&*!";

    private static InputFilter filter = new InputFilter() {

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

            if (source != null && BLOCK_CHARACTER_SET.contains(("" + source))) {
                return "";
            }
            for (int i = start; i < end; i++) {
                if (!Character.isLetterOrDigit(source.charAt(i))) {
                    return "";
                }
            }
            return null;
        }
    };

    public static PlaylistNameFragment newInstance() {
        PlaylistNameFragment fragment = new PlaylistNameFragment();
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PlaylistNameFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OAuthRecord oauth = OAuthService.getOAuthToken();
        Profile profile = null;
        List<Playlist> playlists = null;
        if (oauth != null && oauth.isValid()) {
            profile = Select.from(Profile.class).where(Condition.prop("OAUTH").eq(oauth.getId())).first();
            playlists = Select.from(Playlist.class).where(Condition.prop("PROFILE").eq(profile.getId())).list();
        }

        if (playlists != null) {
            setListAdapter(new PlaylistRenameAdapter(getActivity(), playlists));
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setFocusable(true);

        final Playlist p = (Playlist) getListView().getItemAtPosition(position);

        input.setText(p.nameForQuery);
        input.setSelection(p.nameForQuery.length());

        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
        alertDialog.setTitle(getString(R.string.rename_playlist_dialog_title));
        alertDialog.setMessage(getString(R.string.rename_playlist_dialog_desc));
        alertDialog.setView(input, 40, 0, 40, 0); // 40 spacing, left and right
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.rename), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Clicked
                if (input.getText() != null) {
                    p.nameForQuery = input.getText().toString();
                } else {
                    p.nameForQuery = "";
                }
                p.save();

                ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
            }
        });
        alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.reset), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                p.nameForQuery = p.name;
                p.save();
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
