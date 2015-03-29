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
import android.widget.ImageView;
import android.widget.Toast;

import com.example.yass8n.whozthis.R;
import com.example.yass8n.whozthis.activities.MainActivity;
import com.example.yass8n.whozthis.activities.WelcomeActivity;

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
//EXECUTE WITH:   "new CreateConversationAPI().execute();"
//    public static class CreateConversationAPI extends AsyncTask<String, Void, JSONObject> {
//        private int status_code;
//        @Override
//        protected void onPreExecute() {
//        }
//
//        @Override
//        protected JSONObject doInBackground(String... s) {
//
//            JSONObject jObject = null;
//            InputStream inputStream = null;
//            String result = null;
//            try {
//
//                HttpClient httpClient = new DefaultHttpClient();
//                HttpContext localContext = new BasicHttpContext();
////                    HttpPost httpPost = new HttpPost("http://ec2-54-69-64-152.us-west-2.compute.amazonaws.com/whoz_rails/api/v1/users/sign_up");
//                HttpPost httpPost = new HttpPost(Global.AWS_URL + "v1/conversations");
////                    HttpPost httpPost = new HttpPost("http://ec2-54-69-64-152.us-west-2.compute.amazonaws.com/whoz_rails/api/v1/users/friends");
//
//                httpPost.setHeader("Accept", "application/json");
//                httpPost.setHeader("Content-type", "application/json");
//
////                httpPost.setEntity(new StringEntity("{\"conversation\":{\"title\":\"hey\",\"user_id\":1},\"phones\":[\"aa\",\"2097402793\"]}"));
//                StringBuilder sb = new StringBuilder();
//                sb.append("{");sb.append('"');sb.append("conversation");sb.append('"');sb.append(":");
//                sb.append("{");sb.append('"');sb.append("title");sb.append('"');sb.append(":");
//                sb.append('"');sb.append("HERE IS THE TITLE");sb.append('"');sb.append(',');
//                sb.append('"');sb.append("user_id");sb.append('"');sb.append(":");sb.append(WelcomeActivity.current_user.user_id);sb.append("}");sb.append(',');
//                sb.append('"');sb.append("phones");sb.append('"');sb.append(":");sb.append("[");
//                for (int i = 0; i < MY_PHONES_ARRAYLIST.size(); i++) {
//                    sb.append('"');sb.append(MY_PHONES_ARRAYLIST.get(i).phone);sb.append('"');
//                    if (i != MY_PHONES_ARRAYLIST.size()-1 )
//                        sb.append(",");
//                }
//                sb.append("]");sb.append('}');
//                String params = sb.toString();
//                httpPost.setEntity(new StringEntity(params));
//
//                HttpResponse response = httpClient.execute(httpPost, localContext);
//                status_code = response.getStatusLine().getStatusCode();
//                HttpEntity response_entity = response.getEntity();
//
//                inputStream = response_entity.getContent();
//
//                // json is UTF-8 by default
//                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
//                sb = new StringBuilder();
//
//                String line = null;
//                while ((line = reader.readLine()) != null) {
//                    sb.append(line + "\n");
//                }
//
//                result = sb.toString();
//
//                // write response to log
//                jObject = new JSONObject(result);
//
//
//            } catch (ClientProtocolException e) {
//                // Log exception
//                Log.v("CLIENT", "ERROR");
//
//                e.printStackTrace();
//            } catch (IOException e) {
//                // Log exception
//                Log.v("IOE", "ERROR");
//
//                e.printStackTrace();
//            } catch (JSONException e) {
//                Log.v(e.toString(), "ERROR");
//
//                e.printStackTrace();
//            }
//            return jObject;
//        }
//
//        @Override
//        protected void onPostExecute(JSONObject result) {
//            if (status_code == 200) {
//                try {
//                    JSONArray conversations = new JSONArray(result.getString("conversations"));
//                    JSONObject json_conversation = conversations.getJSONObject(0);
//                    MainActivity.conversations_array.add(MainActivity.createConversation(json_conversation));
//                    MainActivity.PlaceholderFragment.conversatons_adapter.notifyDataSetChanged();
//                } catch (JSONException e) {
//                    Log.e(e.toString(), "JSONError");
//                }
//            } else{
//                Toast.makeText(context, "Error! Please make sure you have a stable internet connection.", Toast.LENGTH_LONG).show();
//            }
//            super.onPostExecute(result);
//        }
//    }
}
