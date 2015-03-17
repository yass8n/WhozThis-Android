package com.example.yass8n.whozthis.objects;

import java.util.ArrayList;

/**
 * Created by yass8n on 3/16/15.
 */
public class Conversation {
    public ArrayList users = new ArrayList<User>();
    public String title;
    public int owner_id;

    public void Conversation(String title, int owner_id){
        this.title = title;
        this.owner_id = owner_id;
    }
}
