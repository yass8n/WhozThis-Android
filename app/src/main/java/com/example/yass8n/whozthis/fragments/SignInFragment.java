package com.example.yass8n.whozthis.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;
import android.widget.Button;
import android.widget.Toast;

import com.example.yass8n.whozthis.R;

/**
 * Created by yass8n on 3/16/15.
 */
public class SignInFragment extends Fragment implements View.OnClickListener {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sign_in, container, false);
        Button sign_up_button = (Button)rootView.findViewById(R.id.sign_up);
        sign_up_button.setOnClickListener(SignInFragment.this);
        return rootView;
    }
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.sign_in){
            Toast.makeText(getActivity(), "Sign in clicked", Toast.LENGTH_SHORT).show();
        }
    }

}