package com.example.yass8n.whozthis.activities;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.example.yass8n.whozthis.objects.User;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
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
import java.util.Objects;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import android.support.v4.widget.SwipeRefreshLayout;


public class MainActivity extends ActionBarActivity {
    public static Firebase firebase;
    public static Context context;
    public static ArrayList<Conversation> conversations_array = new ArrayList<Conversation>();
    public static ArrayList<User> friends_array = new ArrayList<>();
    public static int HEADER_ID = -1;
    public static ArrayList<String> phones = new ArrayList<String>();
    public static ArrayList<User> contacts_in_phone = new ArrayList<>();
    //need the conversations array attached to the main activity so we can access it from other activities with "MainActivity.conversations_array"


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //https://radiant-inferno-906.firebaseio.com   this is the URL where our data will be stored
        super.onCreate(savedInstanceState);
        context = this;
        Firebase.setAndroidContext(this);
        firebase = new Firebase("https://radiant-inferno-906.firebaseio.com/");
        setContentView(R.layout.activity_holder);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment(), "MAIN")
                    .commit();
        }
        firebase.child("user_id").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
//                Log.v(snapshot.getValue().toString(), "  <<<<<<<<");
//                Toast.makeText(MainActivity.this, "the value of user_id is " + snapshot.getValue().toString(), Toast.LENGTH_SHORT).show();
            }

            @Override public void onCancelled(FirebaseError error) { }

        });
//        startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
    }
    @Override
    public void onBackPressed(){
        //override back button when main activity fragment is showing
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("MAIN");
        if (fragment.isVisible()) {
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public void onStart(){
        if (checkUserLogin()){
            loadContacts();
            refreshConversations();
        }
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
            startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
        } else if (id == R.id.edit_profile){
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }
    public void loadContacts(){
//            if (loaded_info == false) {
        getContactsTask runner = new getContactsTask();
        runner.execute();
//            }
    }
    private class getContactsTask extends AsyncTask<Void, String, String> {
        private int status_code;
        private String resp;

        @Override
        protected String doInBackground(Void... v)  {
            try {
                // Run query
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
                            Log.v(contact.phone,  " phone");
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
                UpdateFriendsTask task = new UpdateFriendsTask();
                task.execute(phones);
        }
        public class UpdateFriendsTask extends AsyncTask<ArrayList<String>, Void, JSONObject> {

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
//                    HttpPost httpPost = new HttpPost("http://ec2-54-69-64-152.us-west-2.compute.amazonaws.com/whoz_rails/api/v1/users/sign_up");
                    HttpPost httpPost = new HttpPost(Global.AWS_URL + "v1/users/friends");
//                    HttpPost httpPost = new HttpPost("http://ec2-54-69-64-152.us-west-2.compute.amazonaws.com/whoz_rails/api/v1/users/friends");

                    httpPost.setHeader("Accept", "application/json");
                    httpPost.setHeader("Content-type", "application/json");

                    MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

//                    httpPost.setEntity(new StringEntity("{\"user\":{\"password\":\"aa\",\"phone\":\"aa\",\"first_name\":\"aa\",\"last_name\":\"aa\"}}"));
//                    httpPost.setEntity(new StringEntity("{\"conversation\":{\"title\":\"hey\",\"user_id\":1},\"phones\":[\"aa\",\"2097402793\"]}"));
//                                                    :["aa","2097402793"]}"
//                    httpPost.setEntity(new StringEntity("{\"phones\":[\"a\"]}"));

                    StringBuilder sb = new StringBuilder();
                    sb.append("{");sb.append('"');sb.append("phones");sb.append('"');sb.append(":");sb.append("[");
                    for (int i = 0; i < contacts_in_phone.size(); i++) {
                        sb.append('"');sb.append(contacts_in_phone.get(i).phone);sb.append('"');
                        if (i != contacts_in_phone.size()-1 )
                            sb.append(",");
                    }
                    sb.append("]");sb.append('}');
                    String params = sb.toString();
                    Global.log(params, " <<<<<<<<");
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
                        JSONArray friends = new JSONArray(result.getString("friends"));
                        conversations_array.clear();
                        for (int i = 0; i < friends.length(); i ++){
                            JSONObject json_user = friends.getJSONObject(i);
                            friends_array.add(createFriend(json_user));
                        }
                    } catch (JSONException e) {
                        Log.e(e.toString(), "JSONError");
                    }
                    Toast.makeText(context, result.toString(), Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(context, "Error! Please make sure you have a stable internet connection.", Toast.LENGTH_LONG).show();
                }
                super.onPostExecute(result);
            }
        }
        private User createFriend(JSONObject json_user){
            User temp_user  = new User();
            try {
                temp_user.first_name = json_user.getString("first_name");
                temp_user.last_name = json_user.getString("last_name");
                temp_user.filename = json_user.getString("filename");
                temp_user.user_id = json_user.getInt("id");
            } catch (JSONException e) {
                Log.v(e.toString(), "JSON ERROR");
            }
            return temp_user;
        }

        @Override
        protected void onPreExecute() {
            // Things to be done before execution of long running operation. For
            // example showing ProgessDialog
//                getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
//                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
//                ImageView container = (ImageView) findViewById(R.id.start_up);
//                container.setVisibility(View.VISIBLE);
        }

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

    public static class GetStreamAPI extends AsyncTask<String, Void, JSONObject> {

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
            try {
                JSONArray conversations = new JSONArray(result.getString("conversations"));
//                    Toast.makeText(getActivity(), conversations.toString(), Toast.LENGTH_LONG).show();
                conversations_array.clear();
                for (int i = 0; i < conversations.length(); i ++){
                    JSONObject json_conversation = conversations.getJSONObject(i);
                    conversations_array.add(createConversation(json_conversation));
                }
//                    Toast.makeText(context, Integer.toString(conversations_array.size()), Toast.LENGTH_LONG).show();
                super.onPostExecute(result);
            } catch (JSONException e) {
                Log.e(e.toString(), "JSONError");
            }
            PlaceholderFragment.conversatons_adapter.notifyDataSetChanged();
        }
        private Conversation createConversation(JSONObject json_conversation){
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
        private ArrayList<User> createUserList(JSONArray users) {
            ArrayList<User> user_list = new ArrayList<User>();
            try {
                for (int i = 0; i < users.length(); i++) {
                    JSONObject json_user = users.getJSONObject(i);
                    User user = new User();
                    user.user_id = json_user.getInt("id");
                    user.filename = json_user.getString("filename");
                    user.first_name = json_user.getString("first_name");
                    user.last_name = json_user.getString("last_name");
                    user.phone = json_user.getString("phone");
                    user_list.add(user);
                }

            } catch (Exception e){
                Log.e(e.toString(), "EXCEPTION");
            }
            return user_list;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {
        public ListView conversations_list;
        public static ConversationsAdapter conversatons_adapter;
        private SwipeRefreshLayout refreshLayout;


        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            conversatons_adapter = new ConversationsAdapter();
            RelativeLayout create_message = (RelativeLayout) rootView.findViewById(R.id.create_message);
            create_message.setOnClickListener(this);
            refreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
            refreshLayout.setOnRefreshListener(this);
            conversations_list = (ListView) rootView.findViewById(R.id.conversations_scroll);
            conversations_list.setAdapter(conversatons_adapter);
            conversations_list.requestLayout();
            return rootView;
        }

        @Override
        public void onClick(View v) {
//                firebase.child("user_id").setValue("5");
//                firebase = new Firebase("https://radiant-inferno-906.firebaseio.com/conversation/1");
//                firebase.child("first_maessage").setValue("Setting message.");
//                GetStreamAPI task = new GetStreamAPI();
//                task.execute();
            Log.v(Integer.toString(v.getId()), " <<<<<<<<");
            if (v.getId() == R.id.create_message ) {
                Toast.makeText(context, "Go to Contact Activity", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getActivity(), ContactActivity.class));
            }
        }

        // on refresh of page by pull down
        @Override public void onRefresh() {
            MainActivity activity = (MainActivity) getActivity();
            activity.refreshConversations();

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
                    LayoutInflater inflater = getActivity().getLayoutInflater();
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
                holder.last_message.setText("Put the last message in here");
                final RelativeLayout image = (RelativeLayout) conversation_view.findViewById(R.id.users_modal);
                ImageView modal_pic = (ImageView) image.findViewById(R.id.modal_pic);
                if (conversation.users.size() > 2){
                    modal_pic.setImageResource(R.drawable.group_pic);
                    image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ShowUsersModal task = new ShowUsersModal();
                            task.execute(conversation);
                        }
                    });
                } else {
                    //if only 2 ppl in conversation, dont show the modal on click
                    modal_pic.setImageResource(R.drawable.single_pic);
                }
                conversation_view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getActivity(), "LEON: This should open up a new activity called 'MessageActivity' that will show all the messages for this conversation", Toast.LENGTH_LONG).show();
                    }
                });
                ImageView trash = (ImageView) conversation_view.findViewById(R.id.delete);
                final View[] conversation_view_arr= new View[1];
                conversation_view_arr[0] = conversation_view; //putting the view in an array so I can access it in the function below without declaring it as final
                trash.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        new AlertDialog.Builder(getActivity())
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
//                                        animateAndDelete(conversation, conversation_view_arr[0], image);
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
//                        updateIndicies(index);
                        conversatons_adapter.notifyDataSetChanged();
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
                anim.setDuration(600);
                convo.startAnimation(anim);
            }
            public class DeleteApi extends AsyncTask<ArrayList<Object>, Void, JSONObject> {
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
                        Toast.makeText(getActivity(), "Error! Please make sure you have a stable internet connection.", Toast.LENGTH_LONG).show();
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
                builderSingle = new AlertDialog.Builder(getActivity(), R.style.DialogSlideAnim);
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
                Display display = getActivity().getWindowManager().getDefaultDisplay();
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
                this.inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
//            View view = convertView;
//            final UserListViewHolder view_holder = new UserListViewHolder();
//            if (view == null) {
//                view = this.inflater.inflate(R.layout.users_list_fragment, parent, false);
//                setViewHolder(view, view_holder);
//            }
//            UserListViewHolder holder = (UserListViewHolder) view.getTag();
//            User user = this.user_array.get(position);
//            Log.v(Integer.toString(user.user_id), "<<<<<<<USER_ID<<<<<<<<<");
//            Log.v(Integer.toString(position), "<<<<<<<POSITION<<<<<<<<<");
//
//            if (position == 0){
//                Log.v("HERRRRRRRRRRR", "<<<<<<<<<");
//                view = this.inflater.inflate(R.layout.modal_header, parent, false);
//                TextView textView = (TextView) view.findViewById(R.id.header);
//                textView.setText("People in the conversation");
//                view.setTag(view_holder);
//                return view;
//
//            } else {
//                holder.full_name.setText(user.first_name + " " + user.last_name);
//                if (!Global.empty(user.filename)) {
//                    Picasso.with(getActivity())
//                            .cancelRequest(holder.display_pic);
//                    Picasso.with(getActivity())
//                            .load(user.filename)
//                            .into(holder.display_pic);
//                } else {
//                    holder.display_pic.setImageResource(R.drawable.single_pic);
//                }
//            }
//            return view;
                if (position == 0){ //This is the HEADER
                    View view = this.inflater.inflate(R.layout.modal_header, parent, false);
                    TextView textView = (TextView) view.findViewById(R.id.header);
                    textView.setText("People in the conversation");
                    return view;
                } else {
                    LayoutInflater inflater = getActivity().getLayoutInflater();
                    View view = inflater.inflate(R.layout.users_list_fragment, parent, false);
                    TextView first_name = (TextView) view.findViewById(R.id.full_name);
                    User user = this.user_array.get(position);
                    ImageView image = (ImageView) view.findViewById(R.id.display_pic);
                    first_name.setText(user.first_name + " " + user.last_name);
                    if (!Global.empty(user.filename)) {
                        Picasso.with(getActivity())
                                .cancelRequest(image);
                        Picasso.with(getActivity())
                                .load(user.filename)
                                .into(image);
                    } else {
                        image.setImageResource(R.drawable.single_pic);
                    }
                    return view;
                }
            }
            private void setViewHolder(View view, UserListViewHolder view_holder){
                view_holder.full_name = (TextView) view.findViewById(R.id.full_name);
                view_holder.display_pic = (ImageView) view.findViewById(R.id.display_pic);
                view.setTag(view_holder);
            }

        }

    }
}
