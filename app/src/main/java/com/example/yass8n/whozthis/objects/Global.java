package com.example.yass8n.whozthis.objects;

/**
 * Created by yass8n on 3/16/15.
 */

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.yass8n.whozthis.R;
import com.example.yass8n.whozthis.activities.MainActivity;
import com.example.yass8n.whozthis.activities.MessagingActivity;
import com.example.yass8n.whozthis.activities.WelcomeActivity;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;

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
import java.util.Map;

public class Global extends Application {
    public static final String AWS_URL = "http://ec2-54-69-64-152.us-west-2.compute.amazonaws.com/whoz_rails/api/";
    public static final String LOCAL_URL = "http://10.0.2.2:3000/api/";
    public static final String FBASE_URL = "https://radiant-inferno-906.firebaseio.com/";

    public static int[] colorArray = {R.color.orangee,
            R.color.reddish, R.color.dark_orange, R.color.red,
            R.color.greenish_blue, R.color.green, R.color.dark_green,
            R.color.blue, R.color.light_purple, R.color.purplee,
            R.color.invite_dark_blue,R.color.light_gray, R.color.grayish};

    public static String[] hex_array = {"#f39c12",
            "#e67e22", "#d35400", "#c0392b",
            "#1abc9c", "#27ae60", "#16a085",
            "#3498db", "#9b59b6", "#8e44ad",
            "#34495e", "#A2A2A2", "#7f8c8d"};

    public static HashMap<String, Integer > colorsMap = new HashMap<String, Integer>(){{
        put("#f39c12",R.color.orangee);
        put("#e67e22",R.color.reddish);
        put("#d35400",R.color.dark_orange);
        put("#c0392b",R.color.red);
        put("#1abc9c",R.color.greenish_blue);
        put("#27ae60",R.color.green);
        put("#16a085",R.color.dark_green);
        put("#3498db",R.color.blue);
        put("#9b59b6",R.color.light_purple);
        put("#8e44ad",R.color.purplee);
        put("#34495e",R.color.invite_dark_blue);
        put("#A2A2A2",R.color.light_gray);
        put("#7f8c8d",R.color.grayish);
    }};

    public static Boolean empty(String string) {
        if (string == null || string.equals("") || string.contains("null"))
            return true;
        else
            return false;
    }

    public static void log(Object s, Object m) {
        String string = s.toString();
        String message = m.toString();
        int maxLogSize = 1000;
        for (int i = 0; i <= string.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = (i + 1) * maxLogSize;
            end = end > string.length() ? string.length() : end;
            Log.v("--->" + message + "<--", string.substring(start, end));
        }
    }

    public static void saveUserToPhone(JSONObject result, Activity activity) {
        try {
            JSONObject user = new JSONObject(result.getString("user"));

            String user_id = user.getString("id");
            String first = user.getString("first_name");
            String last = user.getString("last_name");
            String phone = user.getString("phone");
            String filename = user.getString("filename");

            SharedPreferences sharedpreferences = activity.getSharedPreferences("user", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString("signed_in", "true");
            editor.putString("user_id", user_id);
            editor.putString("first", first);
            editor.putString("last", last);
            editor.putString("phone", phone);
            editor.putString("filename", filename);

            editor.commit();
        } catch (Exception e) {
            Log.e(e.toString(), " Exception in Global ");
        }
    }
    public static class SendFireBaseMessage extends AsyncTask<String, Void, String> {
        public SendFireBaseMessage(){
        }
        @Override
        public void onPreExecute() {
        }

        @Override
        public String doInBackground(String... s) {
            String user_comment = s[0];

            Map<String, Object> post = new HashMap<>();
            Calendar calendar = Calendar.getInstance();
            java.util.Date now = calendar.getTime();
            long unixTime = System.currentTimeMillis() / 1000L;
            int i = 0;
            while(i < MainActivity.current_conversation.users.size()){
                if (WelcomeActivity.current_user.user_id == MainActivity.current_conversation.users.get(i).user_id)
                    break;
                i++;
            }

            post.put("user_id", Integer.toString(WelcomeActivity.current_user.user_id));
            post.put("fname", WelcomeActivity.current_user.first_name);
            post.put("lname", WelcomeActivity.current_user.last_name);
            post.put("timestamp", String.valueOf(unixTime));
            post.put("color", MainActivity.current_conversation.users.get(i).convo_color);
            post.put("fake_id", Integer.toString(MainActivity.current_conversation.users.get(i).fake_id));
            post.put("title", MainActivity.current_conversation.title);
            post.put("comment", user_comment);
            for (int j = 0; j < MainActivity.current_conversation.users.size(); j ++){
                //if false, indicates that they have not seen the message yet
                User user = MainActivity.current_conversation.users.get(j);
                if (user.user_id == WelcomeActivity.current_user.user_id) {
                    post.put(Integer.toString(MainActivity.current_conversation.users.get(j).user_id), true);
                }else {
                    post.put(Integer.toString(MainActivity.current_conversation.users.get(j).user_id), false);
                }
            }
            Firebase firebase = new Firebase(FBASE_URL + "messages/" + MainActivity.current_conversation.id);
            firebase.push().setValue(post);
            return "";
        }

        @Override
        public void onPostExecute(String result) {
            MessagingActivity.notifyAdapter();
            MainActivity.notifyAdapter();
        }
    }
    public static void setAsRead(final String url, final boolean bool){
        final Firebase firebase = new Firebase(url);
        Query queryRef = firebase.limitToLast(1);
        queryRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {
                Map<String, Object> newPost = (Map<String, Object>) snapshot.getValue();
                Message message = new Message();
                message.key = snapshot.getKey();
                final Firebase last_message = new Firebase(url + "/" + message.key);
                firebase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Map<String, Object> updates = new HashMap<String, Object>();
                        updates.put(Integer.toString(WelcomeActivity.current_user.user_id), bool);
                        last_message.updateChildren(updates);
                    }
                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                    }
                });
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
            // Retrieve new posts as they are added to Firebase
        });
    }
}