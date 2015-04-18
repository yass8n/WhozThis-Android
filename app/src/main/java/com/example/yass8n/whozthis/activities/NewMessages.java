package com.example.yass8n.whozthis.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yass8n.whozthis.R;
import com.example.yass8n.whozthis.objects.Global;
import com.example.yass8n.whozthis.objects.User;
import com.firebase.client.Firebase;
import com.squareup.picasso.Picasso;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


public class NewMessages extends ActionBarActivity {
    private TextView title;
    public static EditText first_text_mess;
    private static LinearLayout whoz_it_to;
    private ImageView plus;
    private static Activity activity;
    public static Set selected_people;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_new_message);
        initializeVariables();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_messages, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void initializeVariables(){
       whoz_it_to = (LinearLayout) findViewById(R.id.profile_pics);
       first_text_mess = (EditText) findViewById(R.id.text_mess);
       plus = (ImageView) findViewById(R.id.plus);
       title = (TextView) findViewById(R.id.convo_title);
       selected_people = new LinkedHashSet();
    }

    public void sendConvo(View view) {
        if (first_text_mess.getText().length() == 0){
            Toast.makeText(activity, "You must write something in your message.", Toast.LENGTH_SHORT).show();
        } else if (selected_people.size() == 0){
            Toast.makeText(activity, "Please select someone to send the message to.", Toast.LENGTH_SHORT).show();
        } else {
            hideKeyboard();
            CreateConversationAPI convAPI = new CreateConversationAPI();
            convAPI.execute(first_text_mess.getText().toString());
            first_text_mess.setText("");
        }
    }

    public void inviteMore(View view) {
        Intent intent = new Intent(activity, ContactActivity.class);
        startActivity(intent);
    }
    public static void setProfilePics() {
        LayoutInflater inflater = activity.getLayoutInflater();
        Iterator<User> itr = selected_people.iterator();
        User person;
        while (itr.hasNext()) {
            person = itr.next();
            View profile_pic_view = inflater.inflate(R.layout.profile_rounded_fragment, whoz_it_to, false);
            TextView profile_pic_text = (TextView) profile_pic_view.findViewById(R.id.profile_pic_text);
            ImageView profile_pic = (ImageView) profile_pic_view.findViewById(R.id.profile_pic);
            profile_pic_text.setText(Character.toString(person.first_name.charAt(0)).toUpperCase());
            if (person.user_id == 0) {
                profile_pic_text.setText(Character.toString(person.first_name.charAt(0)).toUpperCase());
                profile_pic.setImageResource(person.color);
            } else if (!Global.empty(person.filename)) {
                Picasso.with(activity)
                        .load(person.filename)
                        .into(profile_pic);
            } else {
                profile_pic.setImageResource(R.drawable.single_pic);
            }
            whoz_it_to.addView(profile_pic_view);
        }
    }
    public void sendText(View view) {

    }
    private void hideKeyboard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private class CreateConversationAPI extends AsyncTask<String, Void, JSONObject> {
        private int status_code;
        ImageView faded_screen = (ImageView) findViewById(R.id.faded);
        ProgressBar progress_bar = (ProgressBar) findViewById(R.id.progress_bar);
        String text_message;
        @Override
        protected void onPreExecute() {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            faded_screen.setVisibility(View.VISIBLE);
            progress_bar.setVisibility(View.VISIBLE);
        }

        @Override
        protected JSONObject doInBackground(String... s) {

            JSONObject jObject = null;
            InputStream inputStream = null;
            String result = null;
            this.text_message = s[0];
            try {

                HttpClient httpClient = new DefaultHttpClient();
                HttpContext localContext = new BasicHttpContext();
//                    HttpPost httpPost = new HttpPost("http://ec2-54-69-64-152.us-west-2.compute.amazonaws.com/whoz_rails/api/v1/users/sign_up");
                HttpPost httpPost = new HttpPost(Global.AWS_URL + "v1/conversations");
//                    HttpPost httpPost = new HttpPost("http://ec2-54-69-64-152.us-west-2.compute.amazonaws.com/whoz_rails/api/v1/users/friends");

                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");

//                httpPost.setEntity(new StringEntity("{\"conversation\":{\"title\":\"hey\",\"user_id\":1},\"phones\":[\"aa\",\"2097402793\"]}"));
                StringBuilder sb = new StringBuilder();
                sb.append("{");sb.append('"');sb.append("conversation");sb.append('"');sb.append(":");
                sb.append("{");sb.append('"');sb.append("title");sb.append('"');sb.append(":");
                sb.append('"');sb.append(title.getText().toString());sb.append('"');sb.append(',');
                sb.append('"');sb.append("user_id");sb.append('"');sb.append(":");sb.append(WelcomeActivity.current_user.user_id);sb.append("}");sb.append(',');
                sb.append('"');sb.append("phones");sb.append('"');sb.append(":");sb.append("[");
                Iterator<User> itr = selected_people.iterator();
                User chosen_user = null;
                while(itr.hasNext()) {
                    chosen_user = itr.next();
                    sb.append('"');sb.append(chosen_user.phone);sb.append('"');
                    if (itr.hasNext())
                        sb.append(",");
                }
                sb.append("]");sb.append('}');
                String params = sb.toString();
                Log.v(params.toString(), " <PARAMS");
                httpPost.setEntity(new StringEntity(params));

                HttpResponse response = httpClient.execute(httpPost, localContext);
                status_code = response.getStatusLine().getStatusCode();
                HttpEntity response_entity = response.getEntity();

                inputStream = response_entity.getContent();

                // json is UTF-8 by default
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                sb = new StringBuilder();

                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }

                result = sb.toString();

                // write response to log
                jObject = new JSONObject(result);


            } catch (ClientProtocolException e) {
                // Log exception
                Log.v("CLIENT", "ERROR");

                e.printStackTrace();
            } catch (IOException e) {
                // Log exception
                Log.v("IOE", "ERROR");

                e.printStackTrace();
            } catch (JSONException e) {
                Log.v(e.toString(), "ERROR");

                e.printStackTrace();
            }
            return jObject;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (status_code == 200) {
                try {
                    JSONArray conversations = new JSONArray(result.getString("conversations"));
                    JSONObject json_conversation = conversations.getJSONObject(0);
                    MainActivity.conversations_array.add(MainActivity.createConversation(json_conversation));
                    MainActivity.PlaceholderFragment.conversatons_adapter.notifyDataSetChanged();
                    MainActivity.current_conversation = MainActivity.conversations_array.get(MainActivity.conversations_array.size()-1);
                } catch (JSONException e) {
                    Log.e(e.toString(), "JSONError");
                }
            } else{
                Toast.makeText(activity, "Error! Please make sure you have a stable internet connection.", Toast.LENGTH_LONG).show();
            }
            faded_screen.setVisibility(View.GONE);
            progress_bar.setVisibility(View.GONE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            Global.SendFireBaseMessage fbaseAPI = new Global.SendFireBaseMessage();
            fbaseAPI.execute(this.text_message);
            Intent intent = new Intent(activity, MessagingActivity.class);
            startActivity(intent);
            super.onPostExecute(result);
        }
    }
}

