package com.example.yass8n.whozthis.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.yass8n.whozthis.R;
import com.example.yass8n.whozthis.activities.MainActivity;
import com.example.yass8n.whozthis.activities.WelcomeActivity;
import com.example.yass8n.whozthis.objects.Global;
import com.example.yass8n.whozthis.objects.User;

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

/**
 * Created by yass8n on 3/16/15.
 */
public class SignUpFragment extends Fragment implements View.OnClickListener {
    private static final int SELECT_IMAGE = 1;
    private EditText phone;
    private EditText password;
    private EditText first_name;
    private EditText last_name;
    private ImageView profile_pic;
    private String p_num;
    private String f_name;
    private String l_name;
    private String p_word;
    private Bitmap profile_pic_bitmap;
    private boolean image_was_uploaded = false;
    String users_phone_number;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sign_up, container, false);
        phone = (EditText)rootView.findViewById(R.id.phone);
        password = (EditText)rootView.findViewById(R.id.password);
        first_name = (EditText)rootView.findViewById(R.id.first_name);
        last_name = (EditText)rootView.findViewById(R.id.last_name);
        profile_pic = (ImageView)rootView.findViewById(R.id.profile_pic);
        profile_pic.setOnClickListener(this);
        Button sign_up_button = (Button)rootView.findViewById(R.id.sign_up);
        sign_up_button.setOnClickListener(SignUpFragment.this);
        TelephonyManager tMgr = (TelephonyManager)getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        users_phone_number = User.stripPhone(tMgr.getLine1Number());
        phone.setText(users_phone_number);
//        Toast.makeText(getActivity(), users_phone_number, Toast.LENGTH_SHORT).show();
        return rootView;
    }

    @Override
    public void onClick(View v) {
        p_num = phone.getText().toString();
        f_name = first_name.getText().toString();
        l_name = last_name.getText().toString();
        p_word = password.getText().toString();
        if (v.getId() == R.id.sign_up){
            if(Global.empty(p_num) || Global.empty(f_name) || Global.empty(l_name) || Global.empty(p_word)){
                Toast.makeText(getActivity(), "Please fill in all the fields before proceeding", Toast.LENGTH_SHORT).show();
//            } else if (!users_phone_number.equals(User.stripPhone(p_num))) {
//                Toast.makeText(getActivity(), "Sorry, you can only Sign up with your own phone number.", Toast.LENGTH_LONG).show();
            } else if (!isInteger(p_num)) {
                Toast.makeText(getActivity(), "Phone number must be all digits", Toast.LENGTH_LONG).show();
            } else if (p_num.length() != 10 && p_num.length() != 11) {
                Toast.makeText(getActivity(), "Phone number must be 10 or 11 digits", Toast.LENGTH_LONG).show();
            } else {
                SignUpAPI task = new SignUpAPI();
                task.execute();
            }
        } else if (v.getId() == R.id.profile_pic){
            //this intent accesses the users pictures
            startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI), SELECT_IMAGE);
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
                    profile_pic_bitmap = BitmapFactory.decodeStream(image_stream);
                    profile_pic.setImageBitmap(profile_pic_bitmap);
                    image_was_uploaded = true;
                } catch (Exception e){
                    Toast.makeText(getActivity(), "Failed to upload photo.", Toast.LENGTH_SHORT).show();
                    Log.e(e.toString(), " EXCEPTION");
                }
            }
        }
    }
    public static boolean isInteger(String s) {
        try {
            Long.parseLong(s);
        } catch(NumberFormatException e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
    }

    public class SignUpAPI extends AsyncTask<String, Void, JSONObject> {
        private int status_code;
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
                HttpPost httpPost = new HttpPost(Global.AWS_URL + "v1/users/sign_up");
//                    HttpPost httpPost = new HttpPost("http://ec2-54-69-64-152.us-west-2.compute.amazonaws.com/whoz_rails/api/v1/users/friends");

                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");

                String encoded = "";
                if (image_was_uploaded) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    profile_pic_bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] data = stream.toByteArray();
                    encoded = Base64.encodeToString(data, Base64.DEFAULT);
                }

                JSONObject jsonBody = new JSONObject("{\"user\":{\"password\":\"" + p_word + "\",\"phone\":\"" + User.stripPhone(p_num) + "\", \"first_name\":\"" + f_name + "\",\"last_name\":\"" + l_name + "\",\"filename\":\"" + encoded.toString() + "\"}}");
                httpPost.setEntity(new StringEntity(jsonBody.toString()));

                HttpResponse response = httpClient.execute(httpPost, localContext);
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
                    Global.saveUserToPhone(result, getActivity());
                    getActivity().startActivity(new Intent(getActivity(), MainActivity.class));
                    super.onPostExecute(result);
                } else if (status_code == 422){
                    if (result.has("phone")){
                        try {
                            JSONArray phone = new JSONArray(result.getString("phone"));
                            Toast.makeText(getActivity(), "Phone number "+phone.getString(0), Toast.LENGTH_LONG).show();
                        } catch (Exception e){
                            Log.e(e.toString(), "Exception phone");
                        }
                    }
                    else {
                        Toast.makeText(getActivity(), "Error! Please make sure you have a stable internet connection.", Toast.LENGTH_LONG).show();
                    }
                }
//           emulator phone number 5555215554
            }
        }

    }
