package com.example.yass8n.whozthis.objects;

import android.widget.Toast;

import com.example.yass8n.whozthis.activities.MainActivity;

import java.util.InputMismatchException;

/**
 * Created by yass8n on 3/16/15.
 */
public class User {
    public String phone;
    public String first_name;
    public String last_name;
    public String first_letter;
    public String filename;
    public int user_id;
    public int index;
    public boolean checked;

    public void User(){
        this.phone = "";
        this.first_name = "";
        this.last_name = "";
        this.filename = "";
        this.first_letter = "";
        this.user_id = 0;
        this.index = 0;
        this.checked = false;
    }
    public void User(String phone, String first_name, String last_name, String filename, int user_id){
        this.phone = stripPhone(phone);
        this.first_name = first_name;
        this.last_name = last_name;
        this.filename = filename;
        if (!Global.empty(first_name))
            this.first_letter = Character.toString(first_name.charAt(0)).toUpperCase();
        this.user_id = user_id;
    }
    public static String stripPhone(String phone){
        phone  = phone.replace(")", "");
        phone  = phone.replace("(", "");
        phone  = phone.replace(" ", "");
        phone  = phone.replace("-", "");
        phone  = phone.replace("/", "");
        if (phone.substring(0,1).equals("1"))
            phone = phone.substring(1);
        return phone;
    }

    @Override
    public boolean equals(Object other){
        if(other == null) return false;
        if(other == this) return true;
        if(!(other instanceof User)) return false;
        User user = (User)other;
        return user_id == user.user_id;
    }
}
