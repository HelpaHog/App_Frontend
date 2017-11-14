package edu.uark.csce.helpahog;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Timer;
import java.util.TimerTask;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        final Intent mainActivityIntent = new Intent(getApplicationContext(), MapsActivity.class);
        mainActivityIntent.putExtra("BuildingsArray", getJSONData(R.raw.buildings).toString());
        mainActivityIntent.putExtra("JBHT_Indoor", getJSONData(R.raw.jbht_indoor).toString());

        new Timer().schedule(new TimerTask(){
            @Override
            public void run(){
                startActivity(mainActivityIntent);
                finish();
            }
        }, 2000);
    }

    public JSONArray getJSONData(int id){
        InputStream iStream = getResources().openRawResource(id);
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];

        try{
            Reader reader = new BufferedReader(new InputStreamReader(iStream, "UTF-8"));
            int n;
            while ((n=reader.read(buffer)) != -1){
                writer.write(buffer, 0, n);
            }

            iStream.close();
        }catch(Exception e){
            e.printStackTrace();
        }

        String jsonString = writer.toString();
        JSONArray buildings;

        try {
            buildings = new JSONArray(jsonString);

            return buildings;
        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }
}
