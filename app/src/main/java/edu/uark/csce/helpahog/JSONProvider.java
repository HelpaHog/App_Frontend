package edu.uark.csce.helpahog;

import android.content.Context;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by toren on 11/20/17.
 */


//All this does is provide a static method for retreiving JSON data from a raw resource file
//Not a true content provider
public class JSONProvider {
    static int id;
    public static JSONArray getJSONFromFile(Context context, int _id){
        id = _id;

        InputStream iStream = context.getResources().openRawResource(id);
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];

        try{
            Reader reader = new BufferedReader(new InputStreamReader(iStream, "UTF-8"));
            int n;
            while((n=reader.read(buffer)) != -1){
                writer.write(buffer, 0, n);
            }

            iStream.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        String jsonString = writer.toString();
        JSONArray array;

        try{
            array = new JSONArray(jsonString);
            return array;
        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }
}
