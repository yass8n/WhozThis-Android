package com.example.yass8n.whozthis.objects;

import java.util.ArrayList;

/**
 * Created by yass8n on 3/16/15.
 */
public class Conversation {
    public ArrayList<User> users = new ArrayList<User>();
    public String title;
    public int owner_id;
    public int id;
    private String date;

    public void Conversation(){
        this.title = "";
    }

    public void Conversation(String title, int owner_id, int id, String date){
        this.title = title;
        this.owner_id = owner_id;
        this.id = id;
        this.date = date;
    }
    public void setDate(String date){
        this.date = date.substring(5, 10);
        this.date = this.date.replace('-','/');
    }
    public String getDate(){
        return this.date;
    }
}
