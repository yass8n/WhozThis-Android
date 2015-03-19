package com.example.yass8n.whozthis.objects;

/**
 * Created by yass8n on 3/16/15.
 */
public class User {
    public String phone;
    public String first_name;
    public String last_name;
    public String filename;
    public int user_id;

    public void User(){
        this.phone = "";
        this.first_name = "";
        this.last_name = "";
        this.filename = "";
        this.user_id = 0;
    }
    public void User(String phone, String first_name, String last_name, String filename, int user_id){
        this.phone = stripPhone(phone);
        this.first_name = first_name;
        this.last_name = last_name;
        this.filename = filename;
        this.user_id = user_id;
    }
    static public String stripPhone(String phone){
        phone  = phone.replace(")", "");
        phone  = phone.replace("(", "");
        phone  = phone.replace(" ", "");
        phone  = phone.replace("-", "");
        phone  = phone.replace("/", "");
        if (phone.substring(0,1).equals("1"))
            phone = phone.substring(1);
        return phone;
    }
}
