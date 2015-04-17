package com.example.yass8n.whozthis.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yass8n.whozthis.R;
import com.example.yass8n.whozthis.objects.Conversation;
import com.example.yass8n.whozthis.objects.Global;
import com.example.yass8n.whozthis.objects.PinnedSectionListView;
import com.example.yass8n.whozthis.objects.User;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.squareup.picasso.Picasso;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;


public class ContactActivity extends ActionBarActivity {
    public static ArrayList<User> objectsArrayOriginal; //lists, users, contacts...array of objects to display everything in the adapter
    public static Set selected_people;
    public static ArrayList<User> objectsArrayForAdapter;
    public static EditText search_field;
    private static UsersAdapter adapter;
    private String menu_text = "Add";
    private PinnedSectionListView list_contact_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_contact);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initializeVariables();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_contact, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onPrepareOptionsMenu (Menu menu){
        MenuItem bedMenuItem = menu.findItem(R.id.add);
        bedMenuItem.setTitle(menu_text);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.add) {
            if (menu_text.equals("Block")){
                Iterator<User> itr = selected_people.iterator();
                User person;
                while (itr.hasNext()) {
                    person = itr.next();
                    MainActivity.blocked_people.add(person);
                }
                BlockUsersAPI task = new BlockUsersAPI();
                task.execute();
                Handler mHandler = new Handler();
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        ProfileActivity.activity.finish();
                        Intent intent = new Intent(ContactActivity.this, ProfileActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //add Intent.FLAG_ACTIVITY_CLEAR_TOP and the finish() below so the activity gets destroyed and we cant go back to it when the user presses the back button
                        startActivity(intent);
                        finish();
                    }
                }, 1300);
            }else {
                Iterator<User> itr = selected_people.iterator();
                User person;
                while (itr.hasNext()) {
                    person = itr.next();
                    NewMessages.selected_people.add(person);
                }
                NewMessages.setProfilePics();
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }
    private class BlockUsersAPI extends AsyncTask<Void, Void, JSONObject> {

        @Override
        protected void onPreExecute() {
            DialogBlocked cdd=new DialogBlocked(ContactActivity.this);
            cdd.show();
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(cdd.getWindow().getAttributes());
            RelativeLayout screen_width = (RelativeLayout) findViewById(R.id.contacts);
            lp.width = screen_width.getWidth() / 2;
            lp.x = 0;
            cdd.getWindow().setAttributes(lp);
            cdd.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            cdd.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        public class DialogBlocked extends Dialog implements
                android.view.View.OnClickListener {

            public Activity c;

            DialogBlocked() {
                super(ContactActivity.this);
            }

            public DialogBlocked(Activity a) {
                super(a);
                // TODO Auto-generated constructor stub
                this.c = a;
            }

            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                requestWindowFeature(Window.FEATURE_NO_TITLE);
                setContentView(R.layout.dialog_users_blocked);

            }
            @Override
            public void onClick(View v) {
            }
        }

        @Override
        protected JSONObject doInBackground(Void... v) {

            JSONObject jObject = null;
            InputStream inputStream = null;
            String result = null;
            try {

                HttpClient httpClient = new DefaultHttpClient();
                HttpContext localContext = new BasicHttpContext();
                HttpPost httpPost = new HttpPost(Global.AWS_URL + "v1/blocked_users");

                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");

                StringBuilder sb = new StringBuilder();
//                httpPost.setEntity(new StringEntity("{\"conversation\":{\"title\":\"hey\",\"user_id\":1},\"phones\":[\"aa\",\"2097402793\"]}"));
                sb.append("{");sb.append('"');sb.append("blocked_user");sb.append('"');sb.append(":");
                sb.append("{");sb.append('"');sb.append("user_id");sb.append('"');sb.append(":");sb.append(WelcomeActivity.current_user.user_id);sb.append(',');
                sb.append('"');sb.append("blocked_id");sb.append('"');sb.append(":");sb.append("[");
                Iterator<User> itr = selected_people.iterator();
                User chosen_user = null;
                while(itr.hasNext()) {
                    chosen_user = itr.next(); //getting last person in set
                    sb.append(chosen_user.user_id);
                    if (itr.hasNext())
                        sb.append(",");
                }
                sb.append("]");sb.append('}');sb.append('}');
                String params = sb.toString();
                httpPost.setEntity(new StringEntity(params));

                HttpResponse response = httpClient.execute(httpPost, localContext);
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
            selected_people.clear();
        }
    }

    public void initializeVariables() {
        Bundle b = ContactActivity.this.getIntent().getExtras();
        if (b != null) {
            if (b.containsKey("block")) {
                if (b.getBoolean("block")) {
                    menu_text = "Block";
                }
            }
        }
        selected_people = new LinkedHashSet();
        list_contact_view = (PinnedSectionListView) findViewById(R.id.list_contact_scroll);
        adapter = new UsersAdapter();
        list_contact_view.setAdapter(adapter);
        search_field = (EditText) findViewById(R.id.search_field);
        search_field.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        /*This method is called to notify you that, within s, the count characters beginning at start are about to be replaced by new text with length after. It is an error to attempt to make changes to s from this callback.*/
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s.toString());
            }

        });
    }
    public void setInviteImage() {
        final RelativeLayout invite_bar = (RelativeLayout) findViewById(R.id.invited_bar);
        final RelativeLayout list_contact_view_container = (RelativeLayout) findViewById(R.id.list_contact_scroll_container);
        ImageView chosen_display_pic = (ImageView) findViewById(R.id.chosen_display_pic);
        TextView chosen_name = (TextView) findViewById(R.id.chosen_name);
        final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) list_contact_view_container.getLayoutParams();
        int size = selected_people.size();
        if (size > 0) {
            if (invite_bar.getVisibility() == View.GONE) {
                Animation slide_in_bottom = AnimationUtils.loadAnimation(getApplicationContext(),
                        R.anim.slide_in_bottom);
                Animation.AnimationListener al = new Animation.AnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation arg0) {
                        invite_bar.setVisibility(View.VISIBLE);
                        float height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
                        params.setMargins(0, 0, 0, (int) height); //left,top,right,bottom
                        list_contact_view_container.setLayoutParams(params);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                    @Override
                    public void onAnimationStart(Animation animation) {
                        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }
                };
                slide_in_bottom.setAnimationListener(al);
                invite_bar.startAnimation(slide_in_bottom);
            }
            Iterator<User> itr = selected_people.iterator();
            User chosen_user = null;
            while(itr.hasNext()) {
                chosen_user = itr.next(); //getting last person in set
            }

            chosen_name.setText(chosen_user.first_name + " " + chosen_user.last_name);
            if (!Global.empty(chosen_user.filename)) {
                Picasso.with(ContactActivity.this)
                        .cancelRequest(chosen_display_pic);

                Picasso.with(ContactActivity.this)
                        .load(chosen_user.filename)
                        .into(chosen_display_pic);
            }
            else if (Global.empty(chosen_user.last_name)){
//                this is a contact
                chosen_display_pic.setImageResource(chosen_user.color);
            }
            else {
                //this is a friend without a picture uploaded
                chosen_display_pic.setImageResource(R.drawable.single_pic);
            }
        } else { //no one invited anymore
            Animation slide_out_bottom = AnimationUtils.loadAnimation(getApplicationContext(),
                    R.anim.slide_out_bottom);
            Animation.AnimationListener al = new Animation.AnimationListener() {
                @Override
                public void onAnimationEnd(Animation arg0) {
                    invite_bar.setVisibility(View.GONE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationStart(Animation animation) {
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    params.setMargins(0, 0, 0, 0); //left,top,right,bottom
                    list_contact_view_container.setLayoutParams(params);

                }
            };
            slide_out_bottom.setAnimationListener(al);
            invite_bar.startAnimation(slide_out_bottom);
        }
    }

    public class UsersAdapter extends BaseAdapter implements PinnedSectionListView.PinnedSectionListAdapter, Filterable {
        private LayoutInflater inflater = getLayoutInflater();
        ArrayList<User> mOriginalValues;

        UsersAdapter() {

            int index = 0;
            this.inflater = (LayoutInflater) ContactActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            objectsArrayForAdapter = new ArrayList<>();
            User header = new User();
            header.first_name = "Friends";
            header.index = 0;
            index++;
            objectsArrayForAdapter.add(header);
            for (int i = 0; i < MainActivity.friends_array.size(); i++) {
                User friend = MainActivity.friends_array.get(i);
                friend.index = index;
                friend.checked = false;
                index++;
                objectsArrayForAdapter.add(friend);
            }
            if (menu_text.equals("Add")) {
                for (int i = 0; i < MainActivity.contacts_in_phone.size(); i++) {
                    User contact = MainActivity.contacts_in_phone.get(i);
                    contact.color = Global.colorArray[i % Global.colorArray.length];
                    contact.last_name = "";
                    contact.checked = false;
                    contact.index = index;
                    index++;
                    objectsArrayForAdapter.add(contact);
                }
            }
            objectsArrayOriginal = new ArrayList<User>(objectsArrayForAdapter);
        }
        class UserViewHolder {
            TextView title;
            ImageView image;
            TextView first_letter;
            ImageView checkmark;
        }

        @Override
        public int getCount() {
            return objectsArrayForAdapter.size();
        }

        @Override
        public Object getItem(int position) {
            return objectsArrayForAdapter.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View view = convertView;
            final UserViewHolder view_holder = new UserViewHolder();
            if (view == null) {
                view = this.inflater.inflate(R.layout.contact_list_fragment, parent, false);
                setViewHolder(view, view_holder);
            }
            final UserViewHolder holder = (UserViewHolder) view.getTag();
            final User item = objectsArrayForAdapter.get(position);

            if (item.index == 0){
                view = this.inflater.inflate(R.layout.contact_list_header, parent, false);
                TextView title = (TextView) view.findViewById(R.id.header);
                title.setText(item.first_name);
                view.setTag(view_holder);
                return view;
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    item.checked = !(item.checked);
                    if (item.checked) {
                        selected_people.add(item);
                    } else {
                        selected_people.remove(item);
                    }
                    search_field.setText("");
                    setInviteImage();
                    adapter.notifyDataSetChanged();
                }
            });
            if (item.checked && item.index != 0) {
                holder.checkmark.setImageResource(R.mipmap.attending);
            } else {
                holder.checkmark.setImageResource(android.R.color.transparent);
            }
            holder.title.setText(item.first_name + " " + item.last_name);
            holder.first_letter.setText(item.first_letter);
            if (!Global.empty(item.filename)) {
                Picasso.with(ContactActivity.this)
                        .cancelRequest(holder.image);

                Picasso.with(ContactActivity.this)
                        .load(item.filename)
                        .into(holder.image);
            }
            else if (Global.empty(item.last_name)){
//                this is a contact
                holder.image.setImageResource(item.color);
            }
            else {
                //this is a friend without a picture uploaded
                holder.image.setImageResource(R.drawable.single_pic);
            }
            return view;
        }
        private void setViewHolder(View view, UserViewHolder view_holder){
            view_holder.title = (TextView) view.findViewById(R.id.content);
            view_holder.image = (ImageView) view.findViewById(R.id.display_pic);
            view_holder.first_letter = (TextView) view.findViewById(R.id.first_letter);
            view_holder.checkmark = (ImageView) view.findViewById(R.id.checkmark);
            view.setTag(view_holder);
        }

        @Override public int getViewTypeCount() {
            return 2;
        }

        @Override public int getItemViewType(int position) {
            if (position == 0)
                return 0; //header
            else
                return 1; //contact/user/create list button
        }

        // We implement this method to return 'true' for all view types we want to pin
        //this method is overrided in SwipeMenuAdapter
        @Override
        public boolean isItemViewTypePinned(int viewType) {
            return viewType == 0;
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint,FilterResults results) {
                    objectsArrayForAdapter = (ArrayList<User>) results.values; // has the filtered values
                    notifyDataSetChanged();  // notifies the data with new filtered values
                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();        // Holds the results of a filtering operation in values
                    ArrayList<User> FilteredArrList = new ArrayList<User>();

                    if (mOriginalValues == null) {
                        mOriginalValues = new ArrayList<User>(objectsArrayForAdapter);
                    }

                    /********
                     *
                     *  If constraint(CharSequence that is received) is null returns the mOriginalValues(Original) values
                     *  else does the Filtering and returns FilteredArrList(Filtered)
                     *
                     ********/
                    if (constraint == null || constraint.length() == 0) {
                        // set the Original result to return
                        results.count = mOriginalValues.size();
                        results.values = mOriginalValues;
                    } else {
                        constraint = constraint.toString().toLowerCase();
                        for (int i = 0; i < mOriginalValues.size(); i++) {
                            User item = mOriginalValues.get(i);
                            if (item.index == 0 || (item.first_name.toLowerCase().startsWith(constraint.toString().toLowerCase()) ||
                                    item.last_name.toLowerCase().startsWith(constraint.toString().toLowerCase()))) {
                                FilteredArrList.add(item);
                            }
                        }
                        // set the Filtered result to return
                        results.count = FilteredArrList.size();
                        results.values = FilteredArrList;
                    }
                    return results;
                }
            };
            return filter;
        }

    }
}