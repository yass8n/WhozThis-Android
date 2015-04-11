package com.example.yass8n.whozthis.activities;
/**
 * Created by yass8n on 3/10/15.
 */
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yass8n.whozthis.R;
import com.example.yass8n.whozthis.objects.Global;
import com.example.yass8n.whozthis.objects.User;
import com.squareup.picasso.Picasso;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

public class ProfileActivity extends ActionBarActivity {
    public static Context context;
    public static Activity activity;
    public String base64Bitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_holder);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        context = this;
        activity = this;
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

//        Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
//                R.drawable.ic_launcher);
//        MediaStore.Images.Media.insertImage(getContentResolver(), icon, "ic_launcher" , "testing");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.save) {
                PlaceholderFragment.SaveProfileTask save_profile = new PlaceholderFragment.SaveProfileTask();
                save_profile.execute();
                return true;
        } else if (id == R.id.sign_out) {
            getSharedPreferences("user", Context.MODE_PRIVATE).edit().clear().commit();
            startActivity(new Intent(activity, WelcomeActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements View.OnClickListener {
        private static final int SELECT_IMAGE = 1;
        private static ImageView profile_pic;
        private static Bitmap profile_pic_bitmap;
        private static EditText f_name;
        private static EditText l_name;
        private static TextView name;
        private TextView block;
        private static TextView change_pic;
        private static ProgressBar spinner;
        private static ImageView faded_screen;
        private static boolean image_was_uploaded = false;
        private LinearLayout profile_pics;
        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_profile, container, false);
            f_name = (EditText)rootView.findViewById(R.id.first_name);
            l_name = (EditText)rootView.findViewById(R.id.last_name);
            faded_screen = (ImageView) rootView.findViewById(R.id.faded);
            profile_pics = (LinearLayout) rootView.findViewById(R.id.profile_pics);
            block = (TextView) rootView.findViewById(R.id.block);
            spinner = (ProgressBar)rootView.findViewById(R.id.profile_progress);
            spinner.bringToFront();
            profile_pic = (ImageView) rootView.findViewById(R.id.profile_pic);
            change_pic = (TextView) rootView.findViewById(R.id.change_pic);
            profile_pic.setOnClickListener(this);
            change_pic.setOnClickListener(this);
            block.setOnClickListener(this);
            loadData();
            setBlockedProfilePics();
            return rootView;
        }
        @Override
        public void onDestroy(){
            image_was_uploaded = false;
            super.onDestroy();
        }
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.change_pic || v.getId() == R.id.profile_pic){
                //this intent accesses the users pictures
                startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI), SELECT_IMAGE);
            } else if (v.getId() == R.id.block) {
                Intent intent = new Intent(getActivity(), ContactActivity.class);
                intent.putExtra("block", true);
                startActivity(intent);
            }
        }
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == SELECT_IMAGE) {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        Uri selectedImage = data.getData();
                        profile_pic.setImageURI(selectedImage); //sets the image so the user can see what it looks like on the imageView
                        InputStream image_stream = getActivity().getContentResolver().openInputStream(selectedImage);
                        profile_pic_bitmap = BitmapFactory.decodeStream(image_stream );
                        profile_pic.setImageBitmap(profile_pic_bitmap);
                        image_was_uploaded = true;
                    } catch (Exception e){
                        Toast.makeText(getActivity(), "Failed to upload photo.", Toast.LENGTH_SHORT).show();
                        Log.e(e.toString(), " EXCEPTION");
                    }
                }
            }
        }

        public void loadData() {
            SharedPreferences sharedpreferences = getActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
            String first = sharedpreferences.getString("first", "user"); //to get the data back from the phone
            String last = sharedpreferences.getString("last", "user"); //to get the data back from the phone
            if (!first.equals("")) {
                String fname = first.substring(0, 1).toUpperCase() + first.substring(1);
                f_name.setText(fname);
            }
            if (!last.equals("")) {
                l_name.setText(last.substring(0, 1).toUpperCase() + last.substring(1));
            }

            if (!Global.empty(WelcomeActivity.current_user.filename)) {
                Picasso.with(getActivity())
                        .load(WelcomeActivity.current_user.filename)
                        .into(profile_pic);
            } else {
                profile_pic.setImageResource(R.drawable.single_pic);
            }
        }
        private void setBlockedProfilePics() {
            LayoutInflater inflater = activity.getLayoutInflater();
            if ( ContactActivity.invited_people != null) {
                Iterator<User> itr = ContactActivity.invited_people.iterator();
                User person;
                for (int i = 0; itr.hasNext(); i++) {
                    person = itr.next();
                    View profile_pic_view = inflater.inflate(R.layout.profile_rounded_fragment, profile_pics, false);
                    TextView profile_pic_text = (TextView) profile_pic_view.findViewById(R.id.profile_pic_text);
                    ImageView profile_pic = (ImageView) profile_pic_view.findViewById(R.id.profile_pic);
                    if (person.user_id == 0) {
                        profile_pic_text.setText(Character.toString(person.first_name.charAt(0)).toUpperCase());
                        profile_pic.setImageResource(person.color);
                    } else {
                        profile_pic_text.setText("");
                        if (!Global.empty(person.filename)) {
                            Picasso.with(activity)
                                    .load(person.filename)
                                    .into(profile_pic);
                        } else {
                            profile_pic.setImageResource(R.drawable.single_pic);
                        }
                    }

                    profile_pics.addView(profile_pic_view);

                }
            }
        }
        public static class SaveProfileTask extends AsyncTask<Void, Void, JSONObject> {
            private int status_code;

            @Override
            protected void onPreExecute() {
                spinner.setVisibility(View.VISIBLE);
                faded_screen.setVisibility(View.VISIBLE);
            }

            @Override
            protected JSONObject doInBackground(Void... v) {
                InputStream inputStream = null;
                String result = null;
                JSONObject jObject = null;
                String last_name = l_name.getText().toString();
                String first_name = f_name.getText().toString();

                try {

                    HttpClient httpClient = new DefaultHttpClient();
                    HttpContext localContext = new BasicHttpContext();
                    HttpPut httpPut = new HttpPut(Global.AWS_URL + "v1/users/" + Integer.toString(WelcomeActivity.current_user.user_id));
                    httpPut.setHeader("Accept", "application/json");
                    httpPut.setHeader("Content-type", "application/json");
                    String encoded = "";
                    if (image_was_uploaded) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        profile_pic_bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        byte[] data = stream.toByteArray();
                        encoded = Base64.encodeToString(data, Base64.DEFAULT);
                    }
                    JSONObject jsonBody = new JSONObject("{\"user\":{\"id\":\"" + Integer.toString(WelcomeActivity.current_user.user_id) + "\", \"first_name\":\"" + first_name + "\",\"last_name\":\"" + last_name + "\",\"filename\":\"" + encoded.toString() + "\"}}");
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
                    Global.saveUserToPhone(result, activity);
                    activity.finish(); //takes us back to MainActivity
                    Toast.makeText(activity, "Profile updated.", Toast.LENGTH_SHORT).show();
                } else{
                    Toast.makeText(activity, "Error! Please make sure you have a stable internet connection.", Toast.LENGTH_LONG).show();
                }
                Log.v(result.toString(), " <<<<<<<<");
                spinner.setVisibility(View.GONE);
                faded_screen.setVisibility(View.GONE);
                super.onPostExecute(result);
            }
        }
    }
}
