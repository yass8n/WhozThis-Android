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
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.yass8n.whozthis.R;

import org.json.JSONObject;

import java.io.InputStream;

public class Global extends Application {
    public static final String AWS_URL = "http://ec2-54-69-64-152.us-west-2.compute.amazonaws.com/whoz_rails/api/";
    public static final String LOCAL_URL = "http://10.0.2.2:3000/api/";
    public static final String FBASE_URL = "https://radiant-inferno-906.firebaseio.com/";

    public static int[] colorArray = {  R.color.orangee, R.color.reddish, R.color.dark_orange, R.color.red,
            R.color.greenish_blue, R.color.green, R.color.dark_green,
            R.color.blue, R.color.light_purple,R.color.purplee,R.color.invite_dark_blue,
            R.color.light_gray, R.color.grayish};

    public static String[] hex_array = {"#f39c12","#e67e22","#d35400","#c0392b",
            "#1abc9c","#27ae60","#16a085",
            "#3498db","#9b59b6","#8e44ad", "#34495e",
            "#A2A2A2", "#7f8c8d"};

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
    public static void saveUserToPhone(JSONObject result, Activity activity){
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
        }catch (Exception e){
            Log.e(e.toString(), " Exception in Global ");
        }
    }
}
