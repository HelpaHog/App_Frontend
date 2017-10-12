package edu.uark.csce.helpahog;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.data.geojson.GeoJsonLayer;

import org.json.JSONArray;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;

import static edu.uark.csce.helpahog.R.id.map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener{

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        //Redundant comment
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);
    }


    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch(requestCode){
            case 1: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //mMap.setMyLocationEnabled(true);
                }//else{
                    //mMap.setMyLocationEnabled(false);
                //}
            }
        }
    }

    public boolean checkLocationPermission(){
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        String buildingsString = getIntent().getStringExtra("jsonArray");

        JSONArray buildingsArray;

        try{
            buildingsArray = new JSONArray(buildingsString);

            for(int i=0; i< buildingsArray.length(); i++) {

                PolygonOptions options = new PolygonOptions();
                ArrayList<LatLng> shapeCoordinates = shapeArray(buildingsArray.getJSONObject(i).getString("shape"));

                for (int j = 0; j < shapeCoordinates.size(); j++) {
                    options.add(shapeCoordinates.get(j));
                    if (buildingsArray.getJSONObject(i).getString("code").equals("JBHT"))
                        System.out.println(shapeCoordinates.get(j).toString());
                }

                options.strokeColor(Color.BLACK);
                options.fillColor(Color.RED);

                mMap.addPolygon(options);
            }
            shapeArray(buildingsArray.getJSONObject(64).getString("shape"));
        }catch(Exception e){
            e.printStackTrace();
        }


        LatLng ax = new LatLng(36.065802, -94.173934);
        mMap.addMarker(new MarkerOptions().position(ax).title("Acxiom Lab"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(ax));



        if(checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);
        }else{
            mMap.setMyLocationEnabled(false);
        }
        mMap.setOnMyLocationButtonClickListener(this);
    }

    @Override
    public boolean onMyLocationButtonClick(){
        return false;
    }

    //Tokenizes the "shape" string for each JSON building element
    public ArrayList<LatLng> shapeArray(String input){
        String[] tokens = input.split(",");
        ArrayList<LatLng> coordArray = new ArrayList<>();

        for(int i=0; i<tokens.length; i++){
            String[] tmp = tokens[i].split(" ");
            String lat = tmp[0];
            String lng = tmp[1];

            coordArray.add(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)));
        }
        return coordArray;
    }
}
