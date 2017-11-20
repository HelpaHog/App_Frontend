package edu.uark.csce.helpahog;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.RadioGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener{

    private GoogleMap mMap;
    private ArrayList<Floor> indoorMap;
    private ArrayList<Building> buildings;

    private Building JBHT;

    boolean SHOW_BUILDING_LABELS = true;
    boolean INDOOR_MODE = false;

    RadioGroup floorSelector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        floorSelector = (RadioGroup)findViewById(R.id.floor_selector);
        floorSelector.setVisibility(RadioGroup.GONE);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setMapStyle();

        if(checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);
        }else{
            mMap.setMyLocationEnabled(false);
        }
        mMap.setOnMyLocationButtonClickListener(this);

        Params params = new Params();
        BuildingsLoader loadBuildings = new BuildingsLoader();
        loadBuildings.execute(params);

        setFloorChangeListener();
        setOnCameraMoveListener();
    }

    void setFloorChangeListener(){
        floorSelector.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                for(int j=0; j<indoorMap.size(); j++){
                    indoorMap.get(j).setRoomLabelsVisible(false);
                    indoorMap.get(j).setRoomsVisible(false);

                }
                indoorMap.get(i-1).setRoomLabelsVisible(true);
                indoorMap.get(i-1).setRoomsVisible(true);
            }
        });
    }

    void setOnCameraMoveListener(){
        //final RadioGroup radioGroupFinal = floorSelector;
        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                float zoomLevel = mMap.getCameraPosition().zoom;
                //Log.i("Zoom Level", ""+zoomLevel);

                //Listen to zoom for displaying building code labels
                if(zoomLevel > 16){
                    if(!SHOW_BUILDING_LABELS) {
                        SHOW_BUILDING_LABELS = true;
                        for (int i = 0; i < buildings.size(); i++) {
                            buildings.get(i).setLabelVisible(true);
                        }
                    }
                }else{
                    if(SHOW_BUILDING_LABELS) {
                        SHOW_BUILDING_LABELS = false;
                        for (int i = 0; i < buildings.size(); i++) {
                            buildings.get(i).setLabelVisible(false);
                        }
                    }
                }


                //Listen to zoom for indoor mode
                if(zoomLevel < 19){

                    if(!INDOOR_MODE){
                        INDOOR_MODE = true;

                        for(int i=0; i<buildings.size(); i++){
                            buildings.get(i).setBuildingVisible(true);
                        }
                        for(int i=0; i<indoorMap.size(); i++){
                            indoorMap.get(i).setRoomLabelsVisible(false);
                            indoorMap.get(i).setRoomsVisible(false);
                        }

                        floorSelector.setVisibility(RadioGroup.GONE);
                    }
                }else{
                    if(INDOOR_MODE){
                        INDOOR_MODE = false;

                        for(int i=0; i<buildings.size(); i++){
                            buildings.get(i).setBuildingVisible(false);
                        }


                        floorSelector.check(1);
                        floorSelector.setVisibility(RadioGroup.VISIBLE);
                    }
                }
            }
        });
    }


    //Gets result of requesting location permissions
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch(requestCode){
            case 1: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //mMap.setMyLocationEnabled(true);
                }
            }
        }
    }


    //Checks location permissions
    public boolean checkLocationPermission(){
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }


    @Override
    public boolean onMyLocationButtonClick(){
        return false;
    }

    //This could be used later to pass parameters to the AsyncTasks
    class Params{
        public Params(){

        }
    }

    public void setMapStyle(){
        int time = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        //Set night mode if time is before 6am or 6pm local time
        if(time <= 6 || time >= 18){
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.map_style_night));
        }else{
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.map_style_day));
        }
    }

    class BuildingsLoader extends AsyncTask<Params, Void, ArrayList<Building>>{
        protected ArrayList<Building> doInBackground(Params... params){
            ArrayList<Building> buildings = new ArrayList<>();
            JSONArray jsonArray = JSONProvider.getJSONFromFile(getApplicationContext(), R.raw.buildings);


            for(int i=0; i<jsonArray.length(); i++){
                try {
                    JSONObject buildingObject = jsonArray.getJSONObject(i);
                    Building currentBuilding = new Building(buildingObject, getApplicationContext());
                    buildings.add(currentBuilding);
                    //Log.i("BUIDING", buildingObject.getString("code"));

                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            return buildings;
        }

        protected void onPostExecute(ArrayList<Building> result){
            if(buildings == null) {
                for (int i = 0; i < result.size(); i++) {
                    if (result.get(i).getBuildingCode().equals("JBHT"))
                        JBHT = result.get(i);

                    buildings = result;
                    Building currentBuilding = result.get(i);
                    //Log.i("ADDING BUILDING", currentBuilding.getBuildingCode());
                    result.get(i).shape = mMap.addPolygon(currentBuilding.getPolygonOptions());
                    result.get(i).label = mMap.addMarker(currentBuilding.getLabelOptions());
                }
            }


            if(indoorMap == null){
                indoorMap = getIndoorMap(JBHT);
                for(int i=0; i<indoorMap.size(); i++){
                    Floor floor = indoorMap.get(i);
                    for(int j=0; j < floor.rooms.size(); j++){
                        Room room = floor.rooms.get(j);
                        room.shape = mMap.addPolygon(room.getShapeOptions());
                        room.label = mMap.addMarker(room.getLabelOptions());
                    }
                }
            }
        }

        private ArrayList<Floor> getIndoorMap(Building building){
            try {
                if (building.HAS_INDOOR_MAP) {
                    building.indoorMap = new ArrayList<>();
                    JSONArray indoorArray = JSONProvider.getJSONFromFile(getApplicationContext(), R.raw.jbht_indoor);
                    for (int j = 0; j < indoorArray.length(); j++) {
                        building.indoorMap.add(new Floor(getApplicationContext(), indoorArray.getJSONArray(j)));
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            return building.indoorMap;
        }
    }

    class IndoorMapLoader extends AsyncTask<Building, Void, ArrayList<Floor>>{
        protected ArrayList<Floor> doInBackground(Building... buildings){
            try {
                if (buildings[0].HAS_INDOOR_MAP) {
                    buildings[0].indoorMap = new ArrayList<>();
                    JSONArray indoorArray = JSONProvider.getJSONFromFile(getApplicationContext(), R.raw.jbht_indoor);
                    for (int j = 0; j < indoorArray.length(); j++) {
                        buildings[0].indoorMap.add(new Floor(getApplicationContext(), indoorArray.getJSONArray(j)));
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            return buildings[0].indoorMap;
        }

        protected void onPostExecute(ArrayList<Floor> results){
            if(indoorMap == null){
                indoorMap = results;
                for(int i=0; i<results.size(); i++){
                    Floor floor = results.get(i);
                    for(int j=0; j < floor.rooms.size(); j++){
                        Room room = floor.rooms.get(j);
                        room.shape = mMap.addPolygon(room.getShapeOptions());
                        room.label = mMap.addMarker(room.getLabelOptions());
                    }
                }
            }
        }
    }
}


