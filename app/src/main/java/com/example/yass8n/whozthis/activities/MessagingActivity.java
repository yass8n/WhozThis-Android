package com.example.yass8n.whozthis.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;
import android.widget.Toast;

import com.example.yass8n.whozthis.R;
import com.example.yass8n.whozthis.objects.Conversation;
import com.example.yass8n.whozthis.objects.Global;
import com.example.yass8n.whozthis.objects.Message;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;

public class MessagingActivity extends ActionBarActivity {
    private static ChatAdapter chat_adapter;
    public static Activity activity;
    public static boolean is_in_front;
    private static EditText message_view;
    private static HashMap<Integer, ChildEventListener> conversation_chats_set = new HashMap<>();

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
        chat_adapter.notifyDataSetChanged();
    }

    public void sendText(View view) {
        EditText text_message = (EditText) findViewById(R.id.text_mess);
        Global.SendFireBaseMessage fbaseAPI = new Global.SendFireBaseMessage();
        fbaseAPI.execute(text_message.getText().toString());
        text_message.setText("");
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
