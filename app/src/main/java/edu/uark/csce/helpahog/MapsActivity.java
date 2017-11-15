package edu.uark.csce.helpahog;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.ui.IconGenerator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static edu.uark.csce.helpahog.R.id.floor_selector;
import static edu.uark.csce.helpahog.R.id.map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener{

    private GoogleMap mMap;
    ArrayList<Building> buildings= new ArrayList<>();
    int a,r,g,b;

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

    //Gets result of requesting location permissions
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

    //Checks location permissions
    public boolean checkLocationPermission(){
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }


    boolean indoor_mode = false; //Used by class inside mMap, must be global
    boolean SHOW_BUILDING_LABELS = false;
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        int time = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        //Set night mode if time is before 6am or 6pm local time
        if(time <= 6 || time >= 18){
            a=0xff; r=0x91; g=0x00; b=0x00;
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.map_style_night));
        }else{
            a=0xff; r=0xff; g=0x70; b=0x70;
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.map_style_day));
        }

        //Get the buildings json array from the activity intent (it will be a string)
        String buildingsString = getIntent().getStringExtra("BuildingsArray");
        String JBHT_indoor = getIntent().getStringExtra("JBHT_Indoor");

        Building JBHT_TMP = null;
        try{
            BuildMapAsync mapBuilder = new BuildMapAsync();
            Param params = new Param(mMap, buildingsString, JBHT_indoor, getApplicationContext());
            buildings = mapBuilder.execute(params).get();
            for(int i=0; i<buildings.size(); i++){
                buildings.get(i).addToMap(mMap);
                if(buildings.get(i).getCode().equals("JBHT")){
                    JBHT_TMP = buildings.get(buildings.size()-1);
                }
            }

            //JBHT_TMP.setIndoorMap(new JSONArray(JBHT_indoor), mMap);
            final Building JBHT = JBHT_TMP;

            //Arraylist because it must be final... stupid java stuff
            final ArrayList<Integer> selected_floor = new ArrayList<>();

            final TextView floorIndicator = (TextView)findViewById(R.id.floor_indicator);
            final RadioGroup floorSelector = (RadioGroup)findViewById(R.id.floor_selector);
            floorSelector.setVisibility(RadioGroup.GONE);

            floorSelector.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup radioGroup, int i) {
                    JBHT.selectFloor(i);
                    Log.i("CHECKED", ""+i);
                }
            });

            mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener(){
                @Override
                public void onCameraMove(){
                    float zoomLevel = mMap.getCameraPosition().zoom;
                    Log.i("Zoom Level", ""+zoomLevel);
                    if(zoomLevel > 16){
                        if(!SHOW_BUILDING_LABELS){
                            SHOW_BUILDING_LABELS = true;
                            for(int i=0; i<buildings.size(); i++){
                                buildings.get(i).labelVisible(true);
                            }
                        }
                    }else{
                        if(SHOW_BUILDING_LABELS){
                            SHOW_BUILDING_LABELS = false;
                            for(int i=0; i<buildings.size(); i++){
                                buildings.get(i).labelVisible(false);
                            }
                        }
                    }

                    if(zoomLevel > 19){
                        if(!indoor_mode){
                            indoor_mode = true;
                            floorSelector.check(1);
                            floorSelector.setVisibility(RadioGroup.VISIBLE);
                        }
                        for(int i=0; i<buildings.size(); i++){
                            buildings.get(i).visible(false);
                        }
                    }else{
                        if(indoor_mode){
                            indoor_mode = false;
                            floorSelector.check(0);
                            floorSelector.setVisibility(RadioGroup.GONE);

                            for(int i=0; i<buildings.size(); i++){
                                buildings.get(i).visible(true);
                            }
                        }
                    }
                }
            });

        }catch(Exception e){
            e.printStackTrace();
        }

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


    private class Param{
        GoogleMap map;
        String buildingString;
        String JBHT_Indoor;
        Context context;
        public Param(GoogleMap _map, String _buildingString, String _JBHT_Indoor, Context _context){
            map = _map;
            buildingString = _buildingString;
            JBHT_Indoor = _JBHT_Indoor;
            context = _context;
        }
    }

    class BuildMapAsync extends AsyncTask<Param, Void, ArrayList<Building>>{
        protected ArrayList<Building> doInBackground(Param... params){
            ArrayList<Building> buildings = new ArrayList<>();
            GoogleMap mMap = params[0].map;
            String buildingsString = params[0].buildingString;
            String JBHT_indoor = params[0].JBHT_Indoor;
            Context context = params[0].context;


            //put the json string into a json array
            JSONArray buildingsArray;

            //Try/catch for JSONException
            try {
                buildingsArray = new JSONArray(buildingsString);

                //Render the building shapes
                for (int i = 0; i < buildingsArray.length(); i++) {
                    //place the current building being rendered into a json object
                    JSONObject currentBuilding = buildingsArray.getJSONObject(i);
                    Log.i("CURRENT BUILDING", currentBuilding.getString("code"));
                    try {
                        buildings.add(new Building(currentBuilding, context));
                    } catch (ArrayIndexOutOfBoundsException e) {
                        Log.i("ERROR", currentBuilding.getString("code"));
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }

            return buildings;
        }
    }
}


