package com.example.yass8n.whozthis.activities;
/**
 * Created by yass8n on 3/08/15.
 */
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.BaseAdapter;
import android.widget.Button;
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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import android.support.v4.widget.SwipeRefreshLayout;


public class MainActivity extends ActionBarActivity implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {
    public static Context context;
    public static Set blocked_people = new LinkedHashSet();
    public static ArrayList<Conversation> conversations_array = new ArrayList<Conversation>();
    public static ArrayList<User> friends_array = new ArrayList<>();
    public static int HEADER_ID = -1;
    public static Conversation current_conversation = new Conversation();
    public static ArrayList<String> phones = new ArrayList<String>();
    public static ArrayList<User> contacts_in_phone = new ArrayList<>();
    private static HashMap<Integer, ChildEventListener> conversation_chats_set = new HashMap<>();
    public static ListView conversations_list;
    public static ConversationsAdapter conversatons_adapter;
    private SwipeRefreshLayout refreshLayout;
    private static boolean is_in_front;
    private static int initial_number_of_contacts;
    public static ChildEventListener current_conversation_listener;
    private static boolean need_to_load_info;
    @Override
    public void onResume() {
        super.onResume();
        is_in_front = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        is_in_front = false;
    }

    //need the conversations array attached to the main activity so we can access it from other activities with "MainActivity.conversations_array"


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //https://radiant-inferno-906.firebaseio.com   this is the URL where our data will be stored
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initializeVariables();
    }
    @Override
    public void onBackPressed(){
        //override back button when main activity fragment is showing
            super.onBackPressed();
    }


    @Override
    public void onStart(){
        if (checkUserLogin()){
            loadContacts();
            refreshConversations();
        }
        notifyAdapter();
        super.onStart();
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
            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else if (id == R.id.edit_profile){
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }
    public static void notifyAdapter() {
        if (is_in_front) {
            conversatons_adapter.notifyDataSetChanged();
        }
    }
    private void initializeVariables() {
        context = this;
        need_to_load_info = true;
        setNumberOfContacts();
        Firebase.setAndroidContext(this);
        RelativeLayout start_up = (RelativeLayout)findViewById(R.id.start_up);
        start_up.setVisibility(View.VISIBLE);
        RelativeLayout create_message = (RelativeLayout) findViewById(R.id.create_message);
        create_message.setOnClickListener(MainActivity.this);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        refreshLayout.setOnRefreshListener(this);
        conversatons_adapter = new ConversationsAdapter();
        conversations_list = (ListView) findViewById(R.id.conversations_scroll);
        conversations_list.setAdapter(conversatons_adapter);
    }
    private void setNumberOfContacts(){
        Cursor cursor =  managedQuery(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        initial_number_of_contacts = cursor.getCount();
    }
    private  int getNumberOfContacts(){
        Cursor cursor =  managedQuery(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        return cursor.getCount();
    }
    public void loadContacts(){
        int current_number_of_contacts = getNumberOfContacts();
        if (initial_number_of_contacts != current_number_of_contacts || need_to_load_info) {
            getContactsTask runner = new getContactsTask();
            runner.execute();
        }
    }
    private class getContactsTask extends AsyncTask<Void, String, String> {
        private int status_code;
        private String resp;

        @Override
        protected String doInBackground(Void... v)  {
            try {
                // Run query
                contacts_in_phone.clear();
                ContentResolver cr = context.getContentResolver();
                Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, ContactsContract.Contacts.DISPLAY_NAME + " ASC");
                if (cur.getCount() > 0) {
                    while (cur.moveToNext()) {
                        String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                        String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                        User contact = new User();
                        if (Integer.parseInt(cur.getString(
                                cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {

                            Cursor pCur = cr.query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                    new String[]{id}, null);
//                            while (pCur.moveToNext()) {
                            pCur.moveToNext();


                            String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            contact.phone = User.stripPhone(phoneNo);
                            phones.add(contact.phone);
                            contact.first_name = name;
                            contact.first_letter = Character.toString(contact.first_name.charAt(0)).toUpperCase();
                            contact.filename = "";
                            contacts_in_phone.add(contact);
//                        }
//                            pCur.close();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                resp = e.getMessage();
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String result) {
                UpdateFriendsAPI task = new UpdateFriendsAPI();
                task.execute(phones);
        }
        private class UpdateFriendsAPI extends AsyncTask<ArrayList<String>, Void, JSONObject> {

            @Override
            protected void onPreExecute() {
            }

            @Override
            protected JSONObject doInBackground(ArrayList<String>... phones) {

                JSONObject jObject = null;
                InputStream inputStream = null;
                String result = null;
                try {

                    HttpClient httpClient = new DefaultHttpClient();
                    HttpContext localContext = new BasicHttpContext();
                    HttpPost httpPost = new HttpPost(Global.AWS_URL + "v1/users/friends");

                    httpPost.setHeader("Accept", "application/json");
                    httpPost.setHeader("Content-type", "application/json");

                    StringBuilder sb = new StringBuilder();
                    sb.append("{");sb.append('"');sb.append("phones");sb.append('"');sb.append(":");sb.append("[");
                    for (int i = 0; i < contacts_in_phone.size(); i++) {
                        sb.append('"');sb.append(contacts_in_phone.get(i).phone);sb.append('"');
                        if (i != contacts_in_phone.size()-1 )
                            sb.append(",");
                    }
                    sb.append("]");sb.append('}');
                    String params = sb.toString();
                    httpPost.setEntity(new StringEntity(params));

                    HttpResponse response = httpClient.execute(httpPost, localContext);
                    status_code = response.getStatusLine().getStatusCode();
                    HttpEntity response_entity = response.getEntity();

                    inputStream = response_entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                    sb = new StringBuilder();

                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line + "\n");
                    }

                    result = sb.toString();

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
                        JSONArray friends = new JSONArray(result.getString("friends"));
                        friends_array.clear();
                        for (int i = 0; i < friends.length(); i ++){
                            JSONObject json_user = friends.getJSONObject(i);
                            friends_array.add(createUser(json_user.getJSONObject("user")));
                        }
                    } catch (JSONException e) {
                        Log.e(e.toString(), "JSONError");
                    }
                }
                else {
                    Toast.makeText(context, "Error! Please make sure you have a stable internet connection.", Toast.LENGTH_LONG).show();
                }
                GetBlockedUsersAPI task = new GetBlockedUsersAPI();
                task.execute();
                super.onPostExecute(result);
            }
        }

        @Override
        protected void onPreExecute() {
        }

    }
    public static User createUser(JSONObject json_user){
        User temp_user  = new User();
        try {
            temp_user.first_name = json_user.getString("first_name");
            temp_user.last_name = json_user.getString("last_name");
            temp_user.filename = "http://ec2-54-69-64-152.us-west-2.compute.amazonaws.com/whoz_rails/images/" + json_user.getString("filename");
            temp_user.phone = json_user.getString("phone");
            temp_user.user_id = json_user.getInt("id");
        } catch (JSONException e) {
            Log.v(e.toString(), "JSON ERROR");
        }
        return temp_user;
    }
    public boolean checkUserLogin(){
        boolean result = true;
        SharedPreferences user = getSharedPreferences("user", Context.MODE_PRIVATE);
        if (user.contains("signed_in")) {
            final String signed_in = user.getString("signed_in", null);
            if (signed_in.equals("true")) {
                String filename = user.getString("filename", null);
                boolean filename_exists = true;
                if (Global.empty(filename)){
                    filename_exists = false;
                }
                WelcomeActivity.setCurrentUser(user.getString("phone", null), user.getString("first", null),
                        user.getString("last", null), "http://ec2-54-69-64-152.us-west-2.compute.amazonaws.com/whoz_rails/images/"+ filename,
                        filename_exists, Integer.parseInt(user.getString("user_id", null)));
                //also need to make sure the modal removes the current user
                result =  true;
            }
        } else {
            startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
            result =  false;
        }
        return result;
    }

    public void refreshConversations(){
        GetStreamAPI get_stream = new GetStreamAPI();
        get_stream.execute();
    }

    private class GetStreamAPI extends AsyncTask<String, Void, JSONObject> {
        private int status_code;
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
                HttpGet httpGet = new HttpGet(Global.AWS_URL + "v1/users/stream/" + Integer.toString(WelcomeActivity.current_user.user_id));

                HttpResponse response = httpClient.execute(httpGet, localContext);
                status_code = status_code = response.getStatusLine().getStatusCode();
                HttpEntity response_entity = response.getEntity();

                inputStream = response_entity.getContent();

                // json is UTF-8 by default
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                StringBuilder sb = new StringBuilder();

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
                try {
                    JSONArray conversations = new JSONArray(result.getString("conversations"));
                    conversations_array.clear();
                    for (int i = 0; i < conversations.length(); i++) {
                        JSONObject json_conversation = conversations.getJSONObject(i);
                        conversations_array.add(createConversation(json_conversation));
                    }
                    setFireBaseLastMessage();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            notifyAdapter();
                            RelativeLayout start_up = (RelativeLayout) findViewById(R.id.start_up);
                            start_up.setVisibility(View.GONE);
                        }
                    }, 1000);
                } catch (JSONException e) {
                    Log.e(e.toString(), "JSONError");
                }
            } else{
                Toast.makeText(context, "Error! Please make sure you have a stable internet connection.", Toast.LENGTH_LONG).show();
            }
            super.onPostExecute(result);
        }
    }
    public static Conversation createConversation(JSONObject json_conversation){
        Conversation temp_conversation = new Conversation();
        try {
            temp_conversation.title = json_conversation.getString("title");
            temp_conversation.id = json_conversation.getInt("id");
            temp_conversation.setDate(json_conversation.getString("created_at"));
            temp_conversation.users = createUserList(new JSONArray(json_conversation.getString("users")));
        } catch (JSONException e) {
            Log.v(e.toString(), "JSON ERROR");
        }
        return temp_conversation;
    }
    public static ArrayList<User> createUserList(JSONArray users) {
        ArrayList<User> user_list = new ArrayList<User>();
        try {
            for (int i = 0; i < users.length(); i++) {
                JSONObject json_user = users.getJSONObject(i);
                User user = new User();
                user.user_id = json_user.getInt("id");
                user.filename = json_user.getString("filename");
                user.first_name = json_user.getString("first_name");
                user.last_name = json_user.getString("last_name");
                user.fake_id = json_user.getInt("fake_id");
                user.convo_color = json_user.getString("color");
                user.phone = json_user.getString("phone");
                user_list.add(user);
            }

        } catch (Exception e){
            Log.e(e.toString(), "EXCEPTION");
        }
        return user_list;
    }
    public static class GetBlockedUsersAPI extends AsyncTask<String, Void, JSONObject> {
        private int status_code;

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
                HttpGet httpGet = new HttpGet(Global.AWS_URL + "v1/users/blocked/" + Integer.toString(WelcomeActivity.current_user.user_id));

                HttpResponse response = httpClient.execute(httpGet, localContext);
                status_code = status_code = response.getStatusLine().getStatusCode();
                HttpEntity response_entity = response.getEntity();

                inputStream = response_entity.getContent();

                // json is UTF-8 by default
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                StringBuilder sb = new StringBuilder();

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
                try {
                    JSONArray blocked = new JSONArray(result.getString("blocked"));
                    blocked_people.clear();
                    for (int i = 0; i < blocked.length(); i ++){
                        JSONObject json_user = blocked.getJSONObject(i);
                        blocked_people.add(createUser(json_user.getJSONObject("user")));
                    }
                } catch (JSONException e) {
                    Log.e(e.toString(), "JSONError");
                }
            } else {
                Toast.makeText(context, "Error! Please make sure you have a stable internet connection.", Toast.LENGTH_LONG).show();
            }
            need_to_load_info = false;
            super.onPostExecute(result);
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */


        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.create_message ) {
                startActivity(new Intent(this, NewMessages.class));
            }
        }

        // on refresh of page by pull down
        @Override public void onRefresh() {
            this.refreshConversations();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshLayout.setRefreshing(false);
                }
            }, 2000);
        }

        public class ConversationsAdapter extends BaseAdapter {

            ConversationsAdapter() {
            }

            class ConversationViewHolder {
                TextView date;
                TextView title;
                TextView last_message;
            }

            @Override
            public int getCount() {
                return conversations_array.size();
            }

            @Override
            public Object getItem(int position) {
                return conversations_array.get(position);

            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View conversation_view = convertView;

                if (conversation_view == null) {
                    LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                    ConversationViewHolder view_holder = new ConversationViewHolder();
                    conversation_view = inflater.inflate(R.layout.conversation_fragment, parent, false);
                    view_holder.date = (TextView) conversation_view.findViewById(R.id.date);
                    view_holder.title = (TextView) conversation_view.findViewById(R.id.title);
                    view_holder.last_message = (TextView) conversation_view.findViewById(R.id.last_message);
                    conversation_view.setTag(view_holder);
                }
                ConversationViewHolder holder = (ConversationViewHolder) conversation_view.getTag();
                conversation_view.setId(position);
                final Conversation conversation = conversations_array.get(position);

                holder.date.setText(conversation.getDate());
                holder.title.setText(conversation.title);
                try {
                    holder.last_message.setText(conversation.last_message.comment);
                }catch(Exception e){
                    holder.last_message.setText("");
                }
                final RelativeLayout image = (RelativeLayout) conversation_view.findViewById(R.id.users_modal);
                ImageView modal_pic = (ImageView) image.findViewById(R.id.modal_pic);
                if (conversation.users.size() > 2){
                    modal_pic.setImageResource(R.drawable.group_icon);
                    image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ShowUsersModal task = new ShowUsersModal();
                            task.execute(conversation);
                        }
                    });
                } else {
                    //if only 2 ppl in conversation, dont show the modal on click
                    //take to the new conversation instead
                    image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            current_conversation = conversation;
                            current_conversation_listener = conversation_chats_set.get(conversation.id);
                            startActivity(new Intent(MainActivity.this, MessagingActivity.class));
                        }
                    });
                    modal_pic.setImageResource(R.drawable.single_icon);
                }
                conversation_view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        current_conversation = conversation;
                        current_conversation_listener = conversation_chats_set.get(conversation.id);
                        startActivity(new Intent(MainActivity.this, MessagingActivity.class));
                    }
                });
                ImageView trash = (ImageView) conversation_view.findViewById(R.id.delete);
                final View[] conversation_view_arr= new View[1];
                conversation_view_arr[0] = conversation_view; //putting the view in an array so I can access it in the function below without declaring it as final
                trash.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle("Delete Conversation")
                                .setMessage("Are you sure you want to delete this conversation?")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ArrayList<Object> params = new ArrayList<>();
                                        DeleteApi delete_api = new DeleteApi();
                                        params.add(conversation);
                                        params.add(conversation_view_arr[0]);
                                        params.add(image);
                                        delete_api.execute(params);
                                    }
                                })
                                .setNegativeButton("No", null)
                                .show();
                    }
                });

                return conversation_view;
            }
            private void animateAndDelete(final Conversation conversation, View convo, View pic) {
                final int initialHeightConvo = convo.getMeasuredHeight();
                final int initialHeightPic = pic.getMeasuredHeight();
                Animation.AnimationListener al = new Animation.AnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation arg0) {
                        conversations_array.remove(conversation);
                        notifyAdapter();
                    }
                    @Override public void onAnimationRepeat(Animation animation) {}
                    @Override public void onAnimationStart(Animation animation) {
                    }
                };

                collapse(initialHeightConvo, initialHeightPic, convo, al, pic);
            }

            private void collapse(final int initialHeightConvo, final int initialHeightPic, final View convo, Animation.AnimationListener al, final View pic) {
                Animation anim = new Animation() {
                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        if (interpolatedTime == 1) {
                            pic.getLayoutParams().height = initialHeightPic;
                            pic.requestLayout();
                            convo.getLayoutParams().height = initialHeightConvo;
                            convo.requestLayout();
                        }
                        else {
                            pic.getLayoutParams().height = initialHeightPic - (int)(initialHeightPic * interpolatedTime) - 1;
                            pic.requestLayout();
                            convo.getLayoutParams().height = initialHeightConvo - (int)(initialHeightConvo * interpolatedTime) - 1;
                            convo.requestLayout();
                        }
                    }

                    @Override
                    public boolean willChangeBounds() {
                        return true;
                    }
                };

                if (al!=null) {
                    anim.setAnimationListener(al);
                }
                anim.setDuration(200);
                convo.startAnimation(anim);
            }
            private class DeleteApi extends AsyncTask<ArrayList<Object>, Void, JSONObject> {
                private int status_code;
                private Conversation conversation;
                private View conversation_view;
                private View pic_view;


                @Override
                protected void onPreExecute() {
                }

                @Override
                protected JSONObject doInBackground(ArrayList<Object>... objects) {
                    this.conversation = ((Conversation)objects[0].get(0));
                    this.conversation_view = ((View)objects[0].get(1));
                    this.pic_view = ((View)objects[0].get(2));
                    JSONObject jObject = null;
                    InputStream inputStream = null;
                    String result = null;
                    try {
                        HttpClient httpClient = new DefaultHttpClient();
                        HttpContext localContext = new BasicHttpContext();
                        HttpPut httpPut = new HttpPut(Global.AWS_URL + "v1/conversation_users/delete");

                        httpPut.setHeader("Accept", "application/json");
                        httpPut.setHeader("Content-type", "application/json");

                        JSONObject jsonBody = new JSONObject("{\"conversation_user\":{\"user_id\":\"" + WelcomeActivity.current_user.user_id + "\",\"conversation_id\":\"" + this.conversation.id + "\"}}");
                        httpPut.setEntity(new StringEntity(jsonBody.toString()));

                        HttpResponse response = httpClient.execute(httpPut, localContext);
                        status_code = response.getStatusLine().getStatusCode();
                        HttpEntity response_entity = response.getEntity();

                        inputStream = response_entity.getContent();

                        // json is UTF-8 by default
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                        StringBuilder sb = new StringBuilder();

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
                        animateAndDelete(this.conversation, this.conversation_view, this.pic_view);
                        super.onPostExecute(result);
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Error! Please make sure you have a stable internet connection.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }

        public class ShowUsersModal extends AsyncTask<Conversation, String, ArrayList<User>> {
            private float user_height;
            private  AlertDialog.Builder builderSingle;
            private AlertDialog alertDialog;
            private UsersAdapter adapter;

            @Override
            protected void onPreExecute() {
                builderSingle = new AlertDialog.Builder(MainActivity.this, R.style.DialogSlideAnim);
                user_height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
            }

            @Override
            protected ArrayList<User> doInBackground(Conversation... c) {
                ArrayList<User> users_in_convo = new ArrayList<>();
                User header = new User();
                header.user_id = HEADER_ID;
                header.first_name = "People in the conversation";
                users_in_convo.add(header);
                for (int i = 0; i < c[0].users.size(); i++) {
                    users_in_convo.add(c[0].users.get(i));
                }
                users_in_convo.remove(WelcomeActivity.current_user);//dont show the current user in the modal
                return users_in_convo;
            }

            @Override
            protected void onPostExecute(ArrayList<User> users_in_convo) {
                if( alertDialog != null && alertDialog.isShowing() ) return;
                adapter = new UsersAdapter(users_in_convo);
                builderSingle.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                Display display = MainActivity.this.getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                double window_width = (double) size.x;
                window_width = window_width /1.5;
                alertDialog = builderSingle.create();
                alertDialog.show();
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.copyFrom(alertDialog.getWindow().getAttributes());
                lp.height = (int) (users_in_convo.size() >= 4 ? user_height * 5 : (user_height * (users_in_convo.size() + 1)));//111 is height of one list item..80 is height of header
                lp.width = (int) window_width;
                lp.dimAmount = 0.5f;
                lp.x = 0;
                alertDialog.getWindow().setAttributes(lp);
            }
        }

        public class UsersAdapter extends BaseAdapter{
            private LayoutInflater inflater;
            ArrayList<User> user_array = new ArrayList<>();

            UsersAdapter(ArrayList<User> user_array) {
                this.inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                this.user_array = (ArrayList<User>) user_array.clone();
            }

            @Override
            public int getCount() {
                return this.user_array.size();
            }

            @Override
            public Object getItem(int position) {
                return this.user_array.get(position);

            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            class UserListViewHolder {
                TextView full_name;
                ImageView display_pic;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (position == 0) { //This is the HEADER
                    View view = this.inflater.inflate(R.layout.modal_header, parent, false);
                    TextView textView = (TextView) view.findViewById(R.id.header);
                    textView.setText("People in the conversation");
                    return view;
                } else {
                    LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                    View view = inflater.inflate(R.layout.users_list_fragment, parent, false);
                    TextView first_name = (TextView) view.findViewById(R.id.full_name);
                    User user = this.user_array.get(position);
                    ImageView image = (ImageView) view.findViewById(R.id.display_pic);
                    first_name.setText(user.first_name + " " + user.last_name);
                    if (!Global.empty(user.filename)) {
                        Picasso.with(MainActivity.this)
                                .cancelRequest(image);
                        Picasso.with(MainActivity.this)
                                .load(user.filename)
                                .into(image);
                    } else {
                        image.setImageResource(R.drawable.single_icon);
                    }
                    return view;
                }
            }

        }

    public static void setFireBaseLastMessage() {
        for (int i = 0;i<conversations_array.size();i++) {
            final Conversation conversation = conversations_array.get(i);

            final Firebase firebase = new Firebase(Global.FBASE_URL + "messages/" + conversation.id);
            Query queryRef = firebase.limitToLast(1);
            queryRef.addChildEventListener(new ChildEventListener() {
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
                        conversation.last_message = message;
                        notifyAdapter();
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

}
