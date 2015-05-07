package com.example.yass8n.whozthis.objects;

/**
 * Created by yass8n on 4/17/15.
 */
public class Message {
    public String user_id;
    public String fake_id;
    public String comment;
    public String fname;
    public String lname;
    public String timestamp;
    public String color;
    public String key;
    public boolean seen;

    public void Message(){
        this.seen=false;
    }
}
