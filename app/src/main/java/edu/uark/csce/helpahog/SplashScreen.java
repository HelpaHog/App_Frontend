package edu.uark.csce.helpahog;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.Timer;
import java.util.TimerTask;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        new Timer().schedule(new TimerTask(){
            @Override
            public void run(){
                startActivity(new Intent(getApplicationContext(), MapsActivity.class));
            }
        }, 5000);
    }
}
