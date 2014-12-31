package com.voicecontroller.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.voicecontroller.R;


public class LoginFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    private View.OnClickListener mListener;
    private View mainView;
    private boolean shouldShowLoading;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LoginFragment.
     */
    public static LoginFragment newInstance() {
        LoginFragment fragment = new LoginFragment();
        return fragment;
    }

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        mainView = inflater.inflate(R.layout.fragment_login, container, false);
        mainView.findViewById(R.id.signInBt).setOnClickListener(mListener);

        if (shouldShowLoading) {
            showLoading();
        } else {
            showButton();
        }

        return mainView;
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

    public void showLoading() {
        if (mainView != null) {
            mainView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            mainView.findViewById(R.id.signInBt).setVisibility(View.GONE);
        }
        shouldShowLoading = true;
    }

    public void showButton() {
        if (mainView != null) {
            mainView.findViewById(R.id.progressBar).setVisibility(View.GONE);
            mainView.findViewById(R.id.signInBt).setVisibility(View.VISIBLE);
        }
        shouldShowLoading = false;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

}
