package com.voicecontroller.fragments;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.voicecontroller.R;
import com.voicecontroller.models.Profile;


public class ProfileFragment extends Fragment {
    private static final String ARG_NAME = "name";
    private static final String ARG_IMAGE = "imageUrl";

    private String name;
    private byte[] image;

    private View.OnClickListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param profile Parameter 1.
     * @return A new instance of fragment ProfileFragment.
     */
    public static ProfileFragment newInstance(Profile profile) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        if (profile != null) {
            args.putString(ARG_NAME, profile.name);
            args.putByteArray(ARG_IMAGE, profile.getImage());
        }
        fragment.setArguments(args);
        return fragment;
    }

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            name = getArguments().getString(ARG_NAME);
            image = getArguments().getByteArray(ARG_IMAGE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v =  inflater.inflate(R.layout.fragment_profile, container, false);
        v.findViewById(R.id.logoutBt).setOnClickListener(mListener);

        if (name != null && !name.isEmpty()) {
            ((TextView) v.findViewById(R.id.userNameTv)).setText(name);
        }

        if (image != null && image.length > 0) {
            ImageView profileIv = (ImageView) v.findViewById(R.id.profileIv);

            Bitmap profilePicture = BitmapFactory.decodeByteArray(image, 0, image.length);
            if (profilePicture != null) {
                profileIv.setImageDrawable(new BitmapDrawable(getResources(), profilePicture));
            }
        }
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (View.OnClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

}
