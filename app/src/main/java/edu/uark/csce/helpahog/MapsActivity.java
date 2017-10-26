package edu.uark.csce.helpahog;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.ui.IconGenerator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

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

    ArrayList<Polygon> buildingPolyList = new ArrayList<>();
    ArrayList<GroundOverlay> buildingLabels = new ArrayList<>();
    boolean indoor_mode = false;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        String buildingsString = getIntent().getStringExtra("jsonArray");

        JSONArray buildingsArray;

        try{
            buildingsArray = new JSONArray(buildingsString);

            for(int i=0; i< buildingsArray.length(); i++) {
                JSONObject currentBuilding = buildingsArray.getJSONObject(i);
                PolygonOptions options = new PolygonOptions();
                ArrayList<LatLng> shapeCoordinates = shapeArray(currentBuilding.getString("shape"));
                LatLngBounds.Builder builder = new LatLngBounds.Builder();

                for (int j = 0; j < shapeCoordinates.size(); j++) {
                    options.add(shapeCoordinates.get(j));

                    builder.include(shapeCoordinates.get(j));
                }

                options.strokeColor(Color.RED);
                options.fillColor(Color.RED);

                LatLng position = new LatLng(Double.parseDouble(currentBuilding.getString("latitude")), Double.parseDouble(currentBuilding.getString("longitude")));

                IconGenerator ig = new IconGenerator(getApplicationContext());
                ig.setBackground(null);
                ig.setTextAppearance(getApplicationContext(), R.style.labelAppearance);

                buildingLabels.add(mMap.addGroundOverlay(new GroundOverlayOptions().position(position, 50).image(BitmapDescriptorFactory.fromBitmap(ig.makeIcon(currentBuilding.getString("code"))))));
                buildingLabels.get(i).setZIndex(100);

                buildingPolyList.add(mMap.addPolygon(options));

            }
            shapeArray(buildingsArray.getJSONObject(64).getString("shape"));
        }catch(Exception e){
            e.printStackTrace();
        }

        LatLng jbht = new LatLng(36.0660869774214, -94.1737827243542);

        GroundOverlayOptions jbht_floor_1_options = new GroundOverlayOptions().image(BitmapDescriptorFactory.fromResource(R.mipmap.jbht_f1)).position(jbht, 101f, 68f).bearing(93f).zIndex(200).visible(false);
        final GroundOverlay jbht_floor_1 = mMap.addGroundOverlay(jbht_floor_1_options);
        GroundOverlayOptions jbht_floor_2_options = new GroundOverlayOptions().image(BitmapDescriptorFactory.fromResource(R.mipmap.jbht_f2)).position(jbht, 101f, 68f).bearing(93f).zIndex(200).visible(false);
        final GroundOverlay jbht_floor_2 = mMap.addGroundOverlay(jbht_floor_2_options);
        GroundOverlayOptions jbht_floor_3_options = new GroundOverlayOptions().image(BitmapDescriptorFactory.fromResource(R.mipmap.jbht_f3)).position(jbht, 101f, 68f).bearing(93f).zIndex(200).visible(false);
        final GroundOverlay jbht_floor_3 = mMap.addGroundOverlay(jbht_floor_3_options);
        GroundOverlayOptions jbht_floor_4_options = new GroundOverlayOptions().image(BitmapDescriptorFactory.fromResource(R.mipmap.jbht_f4)).position(jbht, 101f, 68f).bearing(93f).zIndex(200).visible(false);
        final GroundOverlay jbht_floor_4 = mMap.addGroundOverlay(jbht_floor_4_options);
        GroundOverlayOptions jbht_floor_5_options = new GroundOverlayOptions().image(BitmapDescriptorFactory.fromResource(R.mipmap.jbht_f5)).position(jbht, 101f, 68f).bearing(93f).zIndex(200).visible(false);
        final GroundOverlay jbht_floor_5 = mMap.addGroundOverlay(jbht_floor_5_options);

        final TextView floorIndicator = (TextView)findViewById(R.id.floor_indicator);

        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener(){
            @Override
            public void onCameraMove(){
                float zoomLevel = mMap.getCameraPosition().zoom;

                int floor = 0;

                if(zoomLevel > 19.3){
                    if(floor != 5) {
                        floor = 5;
                        jbht_floor_1.setVisible(false);
                        jbht_floor_2.setVisible(false);
                        jbht_floor_3.setVisible(false);
                        jbht_floor_4.setVisible(false);
                        jbht_floor_5.setVisible(true);
                    }
                }else if(zoomLevel > 19){
                    if(floor !=4) {
                        floor = 4;
                        jbht_floor_1.setVisible(false);
                        jbht_floor_2.setVisible(false);
                        jbht_floor_3.setVisible(false);
                        jbht_floor_4.setVisible(true);
                        jbht_floor_5.setVisible(false);
                    }
                }else if(zoomLevel > 18.6){
                    if(floor !=3) {
                        floor = 3;
                        jbht_floor_1.setVisible(false);
                        jbht_floor_2.setVisible(false);
                        jbht_floor_3.setVisible(true);
                        jbht_floor_4.setVisible(false);
                        jbht_floor_5.setVisible(false);
                    }
                }else if(zoomLevel > 18.2){
                    if(floor !=2) {
                        floor = 2;
                        jbht_floor_1.setVisible(false);
                        jbht_floor_2.setVisible(true);
                        jbht_floor_3.setVisible(false);
                        jbht_floor_4.setVisible(false);
                        jbht_floor_5.setVisible(false);
                    }
                }else if(zoomLevel > 17.8){
                    if(floor !=1) {
                        floor = 1;
                        jbht_floor_1.setVisible(true);
                        jbht_floor_2.setVisible(false);
                        jbht_floor_3.setVisible(false);
                        jbht_floor_4.setVisible(false);
                        jbht_floor_5.setVisible(false);
                    }
                }

                floorIndicator.setText("Showing Level " + floor);

                if(zoomLevel > 17.8){
                    floorIndicator.setVisibility(TextView.VISIBLE);
                    if(!indoor_mode) {
                        indoor_mode = true;
                        for (int i = 0; i < buildingPolyList.size(); i++) {
                            buildingPolyList.get(i).setVisible(false);
                            buildingLabels.get(i).setVisible(false);
                        }

                    }
                }else{
                    if(indoor_mode){
                        indoor_mode = false;
                        floor = 0;
                        floorIndicator.setVisibility(TextView.GONE);
                        jbht_floor_1.setVisible(false);
                        jbht_floor_2.setVisible(false);
                        jbht_floor_3.setVisible(false);
                        jbht_floor_4.setVisible(false);
                        jbht_floor_5.setVisible(false);
                        for(int i=0; i < buildingPolyList.size(); i++){
                            buildingPolyList.get(i).setVisible(true);
                            buildingLabels.get(i).setVisible(true);
                        }
                    }
                }
            }
        });



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
