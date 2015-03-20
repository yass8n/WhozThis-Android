package com.example.yass8n.whozthis.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
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
import java.util.ArrayList;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import android.support.v4.widget.SwipeRefreshLayout;


public class MainActivity extends ActionBarActivity {
    public static Firebase firebase;
    public static Context context;
    public static ArrayList<Conversation> conversations_array = new ArrayList<Conversation>();
    //need the conversations array attached to the main activity so we can access it from other activities with "MainActivity.conversations_array"


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //https://radiant-inferno-906.firebaseio.com   this is the URL where our data will be stored
        super.onCreate(savedInstanceState);
        context = this;
        Firebase.setAndroidContext(this);
        firebase = new Firebase("https://radiant-inferno-906.firebaseio.com/");
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
        firebase.child("user_id").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
//                Log.v(snapshot.getValue().toString(), "  <<<<<<<<");
                Toast.makeText(MainActivity.this, "the value of user_id is " + snapshot.getValue().toString(), Toast.LENGTH_SHORT).show();
            }

            @Override public void onCancelled(FirebaseError error) { }

        });
//        startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
    }

    @Override
    public void onStart(){
        if (checkUserLogin()){
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

    public boolean checkUserLogin(){
        boolean result = true;
        SharedPreferences user = getSharedPreferences("user", Context.MODE_PRIVATE);
        if (user.contains("signed_in")) {
            final String signed_in = user.getString("signed_in", null);
            if (signed_in.equals("true")) {
                WelcomeActivity.setCurrentUser(user.getString("phone", null), user.getString("first", null),
                        user.getString("last", null), "", Integer.parseInt(user.getString("user_id", null)));
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
                Log.e("Problem accessing API signup", "JSONError");
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
//                startActivity(new Intent(getActivity(), ContactActivity.class));
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
                ImageView picture;
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
                    view_holder.picture = (ImageView) conversation_view.findViewById(R.id.profile_pic);
                    view_holder.last_message = (TextView) conversation_view.findViewById(R.id.last_message);
                    conversation_view.setTag(view_holder);
                }
                ConversationViewHolder holder = (ConversationViewHolder) conversation_view.getTag();
                conversation_view.setId(position);
                final Conversation conversation = conversations_array.get(position);

                holder.date.setText(conversation.getDate());
                holder.title.setText(conversation.title);
                holder.last_message.setText("Put the last message in here");
                RelativeLayout image = (RelativeLayout) conversation_view.findViewById(R.id.users_modal);
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

                return conversation_view;
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
                    LayoutInflater inflater = getActivity().getLayoutInflater();
                    View header = inflater.inflate(R.layout.modal_header, null, false);
                    builderSingle.setCustomTitle(header); //setting the "Who's in" header
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

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    LayoutInflater inflater = getActivity().getLayoutInflater();
                    View view = inflater.inflate(R.layout.users_list_fragment, parent, false);
                    TextView first_name = (TextView) view.findViewById(R.id.full_name);
                    User user = this.user_array.get(position);
                    ImageView image = (ImageView) view.findViewById(R.id.display_pic);
                    first_name.setText(user.first_name + " " +user.last_name);
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

            public class PostAPI extends AsyncTask<String, Void, JSONObject> {

                @Override
                protected void onPreExecute() {
                }

                @Override
                protected JSONObject doInBackground(String... s) {

                    JSONObject jObject = null;
                    InputStream inputStream = null;
                    String result = null;
                    try {

                        HttpClient httpClient = new DefaultHttpClient();
                        HttpContext localContext = new BasicHttpContext();
//                    HttpPost httpPost = new HttpPost("http://ec2-54-69-64-152.us-west-2.compute.amazonaws.com/whoz_rails/api/v1/users/sign_up");
                        HttpPost httpPost = new HttpPost("http://ec2-54-69-64-152.us-west-2.compute.amazonaws.com/whoz_rails/api/v1/users/friends");
//                    HttpPost httpPost = new HttpPost("http://ec2-54-69-64-152.us-west-2.compute.amazonaws.com/whoz_rails/api/v1/users/friends");

                        httpPost.setHeader("Accept", "application/json");
                        httpPost.setHeader("Content-type", "application/json");

                        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

                        StringBuilder sb = new StringBuilder();
//                    httpPost.setEntity(new StringEntity("{\"user\":{\"password\":\"aa\",\"phone\":\"aa\",\"first_name\":\"aa\",\"last_name\":\"aa\"}}"));
//                    httpPost.setEntity(new StringEntity("{\"conversation\":{\"title\":\"hey\",\"user_id\":1},\"phones\":[\"aa\",\"2097402793\"]}"));
                        httpPost.setEntity(new StringEntity("{\"phones\":[\"a\"]}"));


                        HttpResponse response = httpClient.execute(httpPost, localContext);
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
                    Toast.makeText(getActivity(), result.toString(), Toast.LENGTH_LONG).show();
                    super.onPostExecute(result);
                }
            }

        }
    }





//    private class getContactsTask extends AsyncTask<Void, String, String> {
//
//        private String resp;
//
//        @Override
//        protected String doInBackground(Void... v)  {
//            try {
//                // Run query
//                ContentResolver cr = MainActivity.this.getContentResolver();
//                Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, ContactsContract.Contacts.DISPLAY_NAME + " ASC");
//                int count = 0;
//                if (cur.getCount() > 0) {
//                    while (cur.moveToNext()) {
//                        String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
//                        String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
//                        Contact contact = new Contact();
//                        if (Integer.parseInt(cur.getString(
//                                cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
//
//                            Cursor pCur = cr.query(
//                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
//                                    null,
//                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
//                                    new String[]{id}, null);
////                            while (pCur.moveToNext()) {
//                            pCur.moveToNext();
//
//
//                            String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
//                            contact.setPhone(phoneNo);
//                            phones.add(contact.getPhone());
//                            contact.name = name;
//                            contact.first_letter = Character.toString(contact.name.charAt(0)).toUpperCase();
//                            contact.image = 0;
//                            contact.index = count;
//                            contacts_in_phone.add(contact);
//                            count++;
//
////                        }
////                            pCur.close();
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                resp = e.getMessage();
//            }
//            return resp;
//        }
//
//        @Override
//        protected void onPostExecute(String result) {
//            UpdateContactsTask task = new UpdateContactsTask();
//            task.execute(phones);
//        }
//
//        @Override
//        protected void onPreExecute() {
//            // Things to be done before execution of long running operation. For
//            // example showing ProgessDialog
//            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
//                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
//            ImageView container = (ImageView) findViewById(R.id.start_up);
//            container.setVisibility(View.VISIBLE);
//        }
//
//        @Override
//        protected void onProgressUpdate(String... text) {
//        }
//    }
}
