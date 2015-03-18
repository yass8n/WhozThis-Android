package com.example.yass8n.whozthis.objects;

/**
 * Created by yass8n on 3/16/15.
 */

import android.app.Application;
import android.util.Log;

public class Global extends Application {
    public static final String AWS_URL = "http://ec2-54-69-64-152.us-west-2.compute.amazonaws.com/whoz_rails/api/";
    public static final String FBASE_URL = "https://radiant-inferno-906.firebaseio.com/";

    public static Boolean empty(String string) {
        if (string == null || string.equals("") || string.equals("null"))
            return true;
        else
            return false;
    }

    public static void log(Object s, Object m) {
        String string = s.toString();
        String message = m.toString();
        int maxLogSize = 1000;
        for (int i = 0; i <= string.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = (i + 1) * maxLogSize;
            end = end > string.length() ? string.length() : end;
            Log.v("--->" + message + "<--", string.substring(start, end));
        }
    }
}
