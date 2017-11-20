package edu.uark.csce.helpahog;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.maps.MapsInitializer;

import org.json.JSONArray;

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

        final Intent intent = new Intent(getApplicationContext(), MapsActivity.class);

        MapsInitializer.initialize(getApplicationContext());

        new Timer().schedule(new TimerTask(){
            @Override
            public void run(){
                startActivity(intent);
                finish();
            }
        }, 2000);
    }
}
