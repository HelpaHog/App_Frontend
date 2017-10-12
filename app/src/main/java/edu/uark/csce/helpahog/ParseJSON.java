package edu.uark.csce.helpahog;

import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

public class ParseJSON extends AsyncTask<String, Void, Void> {
    Context context;

    public ParseJSON(Context _context){
        context = _context;
    }

    public Void doInBackground(String... params){


        return null;
    }
}