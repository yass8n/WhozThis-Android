package com.example.yass8n.whozthis.fragments;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
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
public class SignInFragment extends Fragment implements View.OnClickListener {
    private EditText phone;
    private EditText password;
    private String p_num;
    private String p_word;
    private Button sign_in_button;
    private static boolean PASSWORD_BASED_KEY = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sign_in, container, false);
        sign_in_button = (Button) rootView.findViewById(R.id.sign_in);
        phone = (EditText) rootView.findViewById(R.id.phone);
        password = (EditText) rootView.findViewById(R.id.password);
        setTextChangedListeners();
        sign_in_button.setOnClickListener(SignInFragment.this);
        return rootView;
    }
    public void setTextChangedListeners(){
        phone.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {}
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkAllFields();
            }
        });
        password.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {}
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkAllFields();
            }
        });
    }
    public void checkAllFields(){
        setAllFields();
        if( !Global.empty(p_num) && !Global.empty(p_word)/* && p_num.length() == 10*/){
            sign_in_button.setBackgroundColor(getResources().getColor(R.color.green));
        } else{
            sign_in_button.setBackgroundColor(getResources().getColor(R.color.app_green));
        }
    }
    public void setAllFields(){
        p_num = phone.getText().toString();
        p_word = password.getText().toString();
    }

    @Override
    public void onClick(View v) {
        setAllFields();
        if (v.getId() == R.id.sign_in) {
            if (Global.empty(p_num) || Global.empty(p_word)) {
                Toast.makeText(getActivity(), "Please fill in all the fields before proceeding.", Toast.LENGTH_LONG).show();
//            } else if (!users_phone_number.equals(p_num)) {
//                Toast.makeText(getActivity(), "Sorry, you can only Sign up with your own phone number.", Toast.LENGTH_LONG).show();
//            } else if (p_num.length() != 10) {
//                Toast.makeText(getActivity(), "Phone number must be 10 digits", Toast.LENGTH_LONG).show();
            } else {
                SignInAPI task = new SignInAPI();
                task.execute();
            }
        }
    }

    public class SignInAPI extends AsyncTask<String, Void, JSONObject> {
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
                //I Know it is insecure to pass the password in the URL...frankly, I don't care.
                HttpPost httpPost = new HttpPost(Global.AWS_URL + "v1/users/sign_in");
//                    HttpPost httpPost = new HttpPost("http://ec2-54-69-64-152.us-west-2.compute.amazonaws.com/whoz_rails/api/v1/users/friends");

                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");

                httpPost.setEntity(new StringEntity("{\"user\":{\"password\":\"" + p_word + "\",\"phone\":\"" + p_num + "\"}}"));

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
                Log.v(e.toString(), "ERROR");

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

            } else if (result.has("error")){
                try {
                    String error = result.getString("error");
                    if (error.equals("not authorized")){
                        Toast.makeText(getActivity(), "Invalid phone number or password", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e){
                    Log.e(e.toString(), " Exception");
                }
            } else {
                Toast.makeText(getActivity(), "Error! Please make sure you have a stable internet connection.", Toast.LENGTH_LONG).show();
            }
//           emulator phone number 5555215554

        }
    }
}

