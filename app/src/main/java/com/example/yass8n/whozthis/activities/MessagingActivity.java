package com.example.yass8n.whozthis.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
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
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yass8n.whozthis.R;
import com.example.yass8n.whozthis.objects.Conversation;
import com.example.yass8n.whozthis.objects.Global;
import com.example.yass8n.whozthis.objects.Message;
import com.example.yass8n.whozthis.objects.User;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MessagingActivity extends ActionBarActivity {
    private static ChatAdapter chat_adapter;
    public static Activity activity;
    public static boolean is_in_front;
    private static EditText message_view;
    private static HashMap<Integer, ChildEventListener> conversation_chats_set = new HashMap<>();
    private ListView chat_list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        initializeVariables();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.sign_out) {
            getSharedPreferences("user", Context.MODE_PRIVATE).edit().clear().commit();
            startActivity(new Intent(MessagingActivity.this, WelcomeActivity.class));
        } else if (id == R.id.edit_profile){
            startActivity(new Intent(MessagingActivity.this, ProfileActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        setFireBaseChats();
        Global.setAsRead(Global.FBASE_URL + "messages/" + MainActivity.current_conversation.id, true);
        is_in_front = true;
        if (message_view != null){
            message_view.requestFocus();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        is_in_front = false;
    }

    public static void notifyAdapter() {
        if (is_in_front) {
            chat_adapter.notifyDataSetChanged();
        }
    }

    private void initializeVariables() {
        activity = this;
        message_view = (EditText) findViewById(R.id.text_mess);
        chat_adapter = new ChatAdapter();
        ListView messages_list_view = (ListView) findViewById(R.id.event_chat_list);
        messages_list_view.setAdapter(chat_adapter);
        chat_list = (ListView) findViewById(R.id.event_chat_list);
        chat_adapter.notifyDataSetChanged();
    }

    public void sendText(View view) {
        EditText text_message = (EditText) findViewById(R.id.text_mess);
        new UpdateConversationTask(text_message.getText().toString()).execute();
        text_message.setText("");
    }
    private class UpdateConversationTask extends AsyncTask<String, Void, JSONObject> {
        private int status_code;
        String text_message;
        UpdateConversationTask(String string){
            this.text_message = string;
        }
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected JSONObject doInBackground(String... s) {
            InputStream inputStream = null;
            String result = null;
            JSONObject jObject = null;

            try {

                HttpClient httpClient = new DefaultHttpClient();
                HttpContext localContext = new BasicHttpContext();
                HttpPut httpPut = new HttpPut(Global.AWS_URL + "v1/conversations/" + MainActivity.current_conversation.id);

                httpPut.setHeader("Accept", "application/json");
                httpPut.setHeader("Content-type", "application/json");

                StringBuilder sb = new StringBuilder();
                sb.append("{");sb.append('"');sb.append("conversation");sb.append('"');sb.append(":");
                sb.append("{");sb.append('"');sb.append("title");sb.append('"');sb.append(":");
                sb.append('"');sb.append(MainActivity.current_conversation.title);sb.append('"');sb.append('}');sb.append('}');

                String params = sb.toString();
                httpPut.setEntity(new StringEntity(params));

                HttpResponse response = httpClient.execute(httpPut, localContext);
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
                Log.v("JSON", "ERROR");

                e.printStackTrace();
            }
            return jObject;
        }
        @Override
        protected void onPostExecute(JSONObject result) {
            if (status_code == 200) {
                Global.SendFireBaseMessage fbaseAPI = new Global.SendFireBaseMessage();
                fbaseAPI.execute(this.text_message);
            } else{
                Toast.makeText(MessagingActivity.activity, "Error! Please make sure you have a stable internet connection.", Toast.LENGTH_LONG).show();
            }
            super.onPostExecute(result);
        }
    }

    private void setFireBaseChats() {
        Firebase firebase = new Firebase(Global.FBASE_URL + "messages/" + MainActivity.current_conversation.id);
        ChildEventListener listener = conversation_chats_set.get(MainActivity.current_conversation.id);
        if (listener != null) {
            firebase.removeEventListener(listener);
        }
        MainActivity.current_conversation.messages.clear();
        ChildEventListener firebase_listener = new ChildEventListener() {
            // Retrieve new posts as they are added to Firebase
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {
                Map<String, Object> newPost = (Map<String, Object>) snapshot.getValue();
                Message message = new Message();
                message.comment = newPost.get("comment").toString();
                message.timestamp = newPost.get("timestamp").toString();
                message.fname = newPost.get("fname").toString();
                message.user_id = newPost.get("user_id").toString();
                message.fake_id = newPost.get("fake_id").toString();
                message.color = newPost.get("color").toString();
                message.seen = true;
                MainActivity.current_conversation.messages.add(message);
                chat_adapter.notifyDataSetChanged();
                chat_list.smoothScrollToPosition(chat_adapter.getCount());
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
        };

        firebase.addChildEventListener(firebase_listener);
        conversation_chats_set.put(MainActivity.current_conversation.id, firebase_listener);
    }
}
class ChatAdapter extends BaseAdapter {
    ChatAdapter() {
    }

    class ChatViewHolder {
        TextView eventListTextMessage;
        ImageView event_list_text_image;
        TextView event_list_time;
        TextView number;
    }

    @Override
    public int getCount() {
        return MainActivity.current_conversation.messages.size();
    }

    @Override
    public Object getItem(int position) {
        return MainActivity.current_conversation.messages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View chat = convertView;
        // inject views
        if (chat == null) {
            LayoutInflater inflater = MessagingActivity.activity.getLayoutInflater();
            chat = inflater.inflate(R.layout.event_chat_list_fragment, parent, false);
            ChatViewHolder viewHolder = new ChatViewHolder();

            // setting correct data
            viewHolder.eventListTextMessage = (TextView) chat.findViewById(R.id.event_list_text_message);
            viewHolder.event_list_text_image = (ImageView) chat.findViewById(R.id.event_list_text_image);
            viewHolder.event_list_time = (TextView) chat.findViewById(R.id.event_list_time);
            viewHolder.number = (TextView) chat.findViewById(R.id.number);
            chat.setTag(viewHolder);
        }

        ChatViewHolder holder = (ChatViewHolder) chat.getTag();
        Message message = MainActivity.current_conversation.messages.get(position);

        holder.eventListTextMessage.setText(message.comment);

        String chat_time = convertChatDate(message.timestamp);

        holder.event_list_time.setText(chat_time);

        holder.number.setText(message.fake_id);

        holder.event_list_text_image.setImageResource(Global.colorsMap.get(message.color));

        return chat;
    }

    private String convertChatDate(String timestamp) {

        // Remove the try catch after all old chats are gone
        try {
            Date date = new Date((long) Integer.parseInt(timestamp) * 1000);
            Date today = new Date();
            SimpleDateFormat compare_df = new SimpleDateFormat("M/d/yy");
            Date date_compare = new Date(compare_df.format(date));
            today = new Date(compare_df.format(today));

            if (today.equals(date_compare)) {
                SimpleDateFormat today_df = new SimpleDateFormat("h:mm a");
                return today_df.format(date);
            } else {
                SimpleDateFormat df = new SimpleDateFormat("h:mm a M/d/yy");
                return df.format(date);
            }

        } catch (Exception e) {
            Log.v(e.toString(), "Error");
            return timestamp;
        }
    }
}
