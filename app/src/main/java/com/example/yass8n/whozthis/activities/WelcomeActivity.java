package com.example.yass8n.whozthis.activities;

import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.Button;

import com.example.yass8n.whozthis.R;
import com.example.yass8n.whozthis.fragments.SignInFragment;
import com.example.yass8n.whozthis.fragments.SignUpFragment;
import com.example.yass8n.whozthis.objects.User;

public class WelcomeActivity extends ActionBarActivity {
    public static User current_user = new User();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_welcome, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }
    //this is called whenever main activity is created so we can access the static variable "currennt_user"
    //from anywhere in the app
    public static void setCurrentUser(String phone, String first, String last, String filename, int user_id){
        current_user.phone = phone;
        current_user.filename = filename;
        current_user.first_name = first;
        current_user.last_name = last;
        current_user.user_id = user_id;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements View.OnClickListener {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_welcome, container, false);
            Button sign_up_button = (Button)rootView.findViewById(R.id.sign_up);
            Button sign_in_button = (Button)rootView.findViewById(R.id.sign_in);
            sign_up_button.setOnClickListener(this);
            sign_in_button.setOnClickListener(this);
            return rootView;
        }

        @Override
        public void onClick(View v) {
            Fragment fragment = new Fragment();
            if (v.getId() == R.id.sign_up){
                fragment = new SignUpFragment();
            } else if (v.getId() == R.id.sign_in){
                fragment = new SignInFragment();
            }
            // Insert the fragment by replacing any existing fragment
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}
