package com.example.yass8n.whozthis.activities;

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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;



public class ContactActivity extends ActionBarActivity {
    public static ArrayList<User> objectsArrayOriginal; //lists, users, contacts...array of objects to display everything in the adapter
    public static ArrayList<User> invited_people;
    public static ArrayList<User> objectsArrayForAdapter;
    public static EditText search_field;
    private static UsersAdapter adapter;
    private PinnedSectionListView list_contact_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_contact);
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.add) {
            Toast.makeText(ContactActivity.this, "ADDING", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    public void initializeVariables() {
        invited_people = new ArrayList<User>();
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
        int size = invited_people.size();
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
            User chosen_user = invited_people.get(invited_people.size()-1);

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
                index++;
                objectsArrayForAdapter.add(friend);
            }
            for (int i = 0; i < MainActivity.contacts_in_phone.size(); i++) {
                User contact = MainActivity.contacts_in_phone.get(i);
                contact.color = Global.colorArray[i % Global.colorArray.length];
                contact.last_name = "";
                contact.index = index;
                index++;
                objectsArrayForAdapter.add(contact);
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
                        invited_people.add(item);
                    } else {
                        invited_people.remove(item);
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