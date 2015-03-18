package com.example.yass8n.whozthis.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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
import android.widget.Toast;

import com.example.yass8n.whozthis.R;
import com.example.yass8n.whozthis.objects.Conversation;
import com.example.yass8n.whozthis.objects.Global;
import com.example.yass8n.whozthis.objects.User;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

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


public class MainActivity extends ActionBarActivity {
    public static Firebase firebase;
    public static ArrayList<Conversation> conversations_array = new ArrayList<Conversation>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //https://radiant-inferno-906.firebaseio.com   this is the URL where our data will be stored
        super.onCreate(savedInstanceState);
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
                Toast.makeText(MainActivity.this, "the value of user_id is " + snapshot.getValue().toString(), Toast.LENGTH_LONG).show();
            }

            @Override public void onCancelled(FirebaseError error) { }

        });
//        startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
        checkUserLogin();

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
            Toast.makeText(this, "Editing Profile!", Toast.LENGTH_SHORT).show();
        }

        return super.onOptionsItemSelected(item);
    }
    public boolean checkUserLogin(){
        boolean result = true;
        SharedPreferences user = getSharedPreferences("user", Context.MODE_PRIVATE);
        if (user.contains("signed_in")) {
            final String signed_in = user.getString("signed_in", null);
            if (signed_in.equals("true")) {
                result =  true;
            }
        } else {
            startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
            result =  false;
        }
        return result;
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
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            Button api_call = (Button) rootView.findViewById(R.id.api_call);
            api_call.setOnClickListener(this);
            return rootView;
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.api_call){
//                firebase.child("user_id").setValue("5");
                firebase = new Firebase("https://radiant-inferno-906.firebaseio.com/conversation/1");
                firebase.child("first_maessage").setValue("Setting message.");



//                PostAPI task = new PostAPI();
//                task.execute();

                GetAPI task = new GetAPI();
                task.execute();
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
        public class GetAPI extends AsyncTask<String, Void, JSONObject> {

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
                    HttpGet httpGet = new HttpGet(Global.AWS_URL + "v1/users/stream/1");


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
                    for (int i = 0; i < conversations.length(); i ++){
                        JSONObject json_conversation = conversations.getJSONObject(i);
                        conversations_array.add(createConversation(json_conversation));
                        Conversation c = conversations_array.get(i);
                    }
//                    Toast.makeText(getActivity(), Integer.toString(conversations_array.size()), Toast.LENGTH_LONG).show();
                    super.onPostExecute(result);
                } catch (JSONException e) {
                    Log.e("Problem accessing API signup", "JSONError");
                    Log.e(e.toString(), "JSONError");
                }
            }
            private Conversation createConversation(JSONObject json_conversation){
                Conversation temp_conversation = new Conversation();
                try {
                    temp_conversation.title = json_conversation.getString("title");
                    temp_conversation.id = json_conversation.getInt("id");
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
                        user.filename = json_user.getString("first_name");
                        user.last_name = json_user.getString("last_name");
                        user.filename = json_user.getString("filename");
                        user.phone = json_user.getString("phone");
                        user_list.add(user);
                    }

                } catch (Exception e){
                    Log.e(e.toString(), "EXCEPTION");
                }
                return user_list;
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
}
