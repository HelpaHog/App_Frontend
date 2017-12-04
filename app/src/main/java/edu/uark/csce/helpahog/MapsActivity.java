package edu.uark.csce.helpahog;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Calendar;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.InfoWindowAdapter, GoogleMap.OnInfoWindowClickListener, GoogleMap.CancelableCallback{

    private GoogleMap mMap;
    private ArrayList<Floor> indoorMap;
    private ArrayList<Building> buildings;
    private ArrayList<Marker> bikeLoops;
    private ArrayList<Marker> helpPhones;
    private ArrayList<Marker> wheelchairEntrances;
    private ArrayList<Marker> dining;
    private ArrayList<LatLng> roomPositions = new ArrayList<>();
    private ArrayList<EmergencyContact> emergencyNumbers = new ArrayList<>();

    //Boolean to indicate a search is being performed
    private boolean isSearching = false;

    private Building JBHT;

    boolean SHOW_BUILDING_LABELS = true;
    boolean INDOOR_MODE = false;

    RadioGroup floorSelector;

    //create a reference for the search bar
    private AutoCompleteTextView actv;

    FloatingActionButton layersButton;
    FloatingActionButton bikeButton;
    FloatingActionButton helpPhoneButton;
    FloatingActionButton wheelChairButton;
    FloatingActionButton diningButton;

    //String array for building names
    private ArrayList<String> bldgNames = new ArrayList<>();

    //Marker used to indicate search locations
    private Marker srchMark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        layersButton = (FloatingActionButton)findViewById(R.id.layersButton);
        bikeButton = (FloatingActionButton)findViewById(R.id.bikeLayerButton);
        helpPhoneButton = (FloatingActionButton)findViewById(R.id.helpPhoneButton);
        wheelChairButton = (FloatingActionButton)findViewById(R.id.accessibleButton);
        diningButton = (FloatingActionButton)findViewById(R.id.diningButton);

        floorSelector = (RadioGroup)findViewById(R.id.floor_selector);
        floorSelector.setVisibility(RadioGroup.GONE);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        emergencyNumbers.add(new EmergencyContact("UAPD", "4795752222"));
        emergencyNumbers.add(new EmergencyContact("SAFE Ride", "4795757233"));
        emergencyNumbers.add(new EmergencyContact("Health Center", "4795754451"));
        emergencyNumbers.add(new EmergencyContact("Mental Health Crisis", "4795755276"));


    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setMapStyle();

        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.getUiSettings().setIndoorLevelPickerEnabled(false);

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
        mMap.setInfoWindowAdapter(this);
        mMap.setOnInfoWindowClickListener(this);
    }


    void initAutoComplete(){
        //Create a reference to the autocomplete widget in our resource file
        actv = (AutoCompleteTextView)findViewById(R.id.autocomplete);

        //Create the list for the textview
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, bldgNames);
        actv.setAdapter(adapter);

        //Set click listener for the list provided by the autocomplete search bar
        actv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick (AdapterView<?> parent, View v, int index, long id){
                //close the keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

                /**Discern whether the selected item is a building or a room,
                 * Buildings are limited to indexes 0 through 314
                 * Rooms will be every index after 315
                 */
                int nameIndex = bldgNames.indexOf(actv.getText().toString());

                //Handle clicks on buildings
                if(nameIndex < 315) {
                    buildingSearch(nameIndex);
                }
                //HandleClicks on rooms
                else{
                    //Get floor number to pass to the roomSearch function
                    String text = actv.getText().toString();
                    int floor = Integer.valueOf(text.substring(text.length() - 2, text.length() - 1));

                    roomSearch(nameIndex, floor);
                }
            }
        });
    }

    //Stub method to handle searches on buildings
    void buildingSearch(int nameIndex){
        isSearching = true;
        //Retrieve LatLng location of the building selected
        LatLng bldgPos = buildings.get(nameIndex).getPosition();

        //Maneuver the camera to the specified location
        CameraPosition pos = new CameraPosition.Builder().target(bldgPos).zoom(18).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos), 3000, null);

        //Remove marker placed in previous search
        if (srchMark != null)
            srchMark.remove(); //Remove markers from previous search

        //Place a marker on the desired location of the search
        srchMark = mMap.addMarker(new MarkerOptions().position(bldgPos));
        srchMark.setTag(0);
    }

    //Stub method to handle searches on rooms
    void roomSearch(int nameIndex, int floor){
        isSearching = true;
        //Subtract the indexes of the building portion of the array
        int roomIndex = nameIndex - 315;

        //Retrieve LatLng location of the Room selected from the roomPositions array
        LatLng roomPos = roomPositions.get(roomIndex);



        //Maneuver the camera to the specified location
        CameraPosition pos = new CameraPosition.Builder().target(roomPos).zoom(19).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos), 3000, this);

        floorSelector.check(floor);
        indoorMap.get(floor-1).setRoomLabelsVisible(true);
        indoorMap.get(floor-1).setRoomsVisible(true);

        //Remove marker placed in previous search
        if (srchMark != null)
            srchMark.remove(); //Remove markers from previous search

        //Place a marker on the desired location of the search
        srchMark = mMap.addMarker(new MarkerOptions().position(roomPos));
        srchMark.setTag(0);
    }

    public void onFinish(){
        isSearching = false;
    }

    public void onCancel(){
        isSearching = false;
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
                            buildings.get(i).setLabelVisible(true);
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
                            buildings.get(i).setLabelVisible(false);
                        }
                        if(!isSearching) {
                            floorSelector.check(1);
                            indoorMap.get(0).setRoomLabelsVisible(true);
                            indoorMap.get(0).setRoomsVisible(true);
                        }
                        floorSelector.setVisibility(RadioGroup.VISIBLE);
                    }
                }
            }
        });

        layersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bikeButton.getVisibility() == bikeButton.VISIBLE) {
                    bikeButton.setVisibility(bikeButton.GONE);
                    helpPhoneButton.setVisibility(helpPhoneButton.GONE);
                    wheelChairButton.setVisibility(wheelChairButton.GONE);
                    diningButton.setVisibility(diningButton.GONE);
                }else{
                    bikeButton.setVisibility(bikeButton.VISIBLE);
                    helpPhoneButton.setVisibility(helpPhoneButton.VISIBLE);
                    wheelChairButton.setVisibility(wheelChairButton.VISIBLE);
                    diningButton.setVisibility(diningButton.VISIBLE);
                }
            }
        });

        bikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bikeLoops == null){
                    bikeLoops = addExtrasToMap(loadExtras(R.raw.bike_racks, R.mipmap.bike_icon_scaled), 0);
                }else {

                    if (bikeLoops.get(0).isVisible()) {
                        for (int i = 0; i < bikeLoops.size(); i++) {
                            bikeLoops.get(i).setVisible(false);
                        }
                    } else {
                        for (int i = 0; i < bikeLoops.size(); i++) {
                            bikeLoops.get(i).setVisible(true);
                        }
                    }
                }
            }
        });

        helpPhoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(helpPhones == null){
                    helpPhones = addExtrasToMap(loadExtras(R.raw.emergency_boxes, R.mipmap.emergency_icon_scaled), 3);
                }else {

                    if (helpPhones.get(0).isVisible()) {
                        for (int i = 0; i < helpPhones.size(); i++) {
                            helpPhones.get(i).setVisible(false);
                        }
                    } else {
                        for (int i = 0; i < helpPhones.size(); i++) {
                            helpPhones.get(i).setVisible(true);
                        }
                    }
                }
            }
        });

        wheelChairButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(wheelchairEntrances == null){
                    wheelchairEntrances = addExtrasToMap(loadExtras(R.raw.accessable_points, R.mipmap.wheelchair_scaled), 0);
                }else {

                    if (wheelchairEntrances.get(0).isVisible()) {
                        for (int i = 0; i < wheelchairEntrances.size(); i++) {
                            wheelchairEntrances.get(i).setVisible(false);
                        }
                    } else {
                        for (int i = 0; i < wheelchairEntrances.size(); i++) {
                            wheelchairEntrances.get(i).setVisible(true);
                        }
                    }
                }
            }
        });

        diningButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(dining == null){
                    ArrayList<MarkerOptions> options = loadExtras(R.raw.dining, R.mipmap.food_scaled);
                    JSONArray jsonArray = JSONProvider.getJSONFromFile(getApplicationContext(), R.raw.dining);
                    try {
                        for (int i = 0; i < options.size(); i++) {
                            options.get(i).title(jsonArray.getJSONObject(i).getString("name"));
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    dining = addExtrasToMap(options, 2);
                }else {

                    if (dining.get(0).isVisible()) {
                        for (int i = 0; i < dining.size(); i++) {
                            dining.get(i).setVisible(false);
                        }
                    } else {
                        for (int i = 0; i < dining.size(); i++) {
                            dining.get(i).setVisible(true);
                        }
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
        int fileId;
        int iconId;

        public Params(){
        }

        public Params(int _fileId, int _iconId){
            fileId = _fileId;
            iconId = _iconId;
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


    @Override
    public View getInfoContents(Marker marker) {
        if((int)marker.getTag() == 1) {
            View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.building_infowindow, null, false);

            TextView title = view.findViewById(R.id.building_title);
            TextView description = view.findViewById(R.id.building_description);

            title.setText(buildings.get(Integer.parseInt(marker.getTitle())).getBuildingName());
            description.setText(buildings.get(Integer.parseInt(marker.getTitle())).getBuildingAddress());


            return view;
        }

        if((int)marker.getTag() == 2){
            try {
                JSONObject diningJSON = JSONProvider.getJSONFromFile(getApplicationContext(), R.raw.dining).getJSONObject(Integer.parseInt(marker.getTitle()));

                View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.building_infowindow, null, false);

                TextView title = view.findViewById(R.id.building_title);
                TextView description = view.findViewById(R.id.building_description);

                title.setText(diningJSON.getString("name"));

                String desc = diningJSON.getString("description");

                Document doc = Jsoup.parse(desc);
                desc = doc.body().toString();
                desc = desc.replaceAll("<[^>]*>", "");

                description.setText(desc);

                return view;
            }catch(JSONException e){
                e.printStackTrace();
            }
        }

        if((int)marker.getTag() == 3){
            View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.emergency_infowindow, null, false);
            return view;
        }
        return null;
    }



    @Override
    public View getInfoWindow(Marker marker){

        return null;
    }


    public void onInfoWindowClick(Marker marker){
        if((int)marker.getTag() == 1){
            String url = "https://directory.uark.edu/buildings/" + buildings.get(Integer.parseInt(marker.getTitle())).getId();

            Intent intent  = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        }if((int)marker.getTag() == 2){
            String url = "https://www.dineoncampus.com/razorbacks/";

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        }if((int)marker.getTag() == 3){
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle("Select a number to call");


            View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.emergency_dialog, null, false);
            EmergencyListAdapter listAdapter = new EmergencyListAdapter(getApplicationContext(), emergencyNumbers);

            ListView listView = view.findViewById(R.id.emergency_list);
            listView.setAdapter(listAdapter);

            dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });

            dialogBuilder.setView(view);
            dialogBuilder.show();
        }
    }

    public void emergencyCall(String number) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
        startActivity(intent);
    }

    class EmergencyContact{
        String name;
        String number;

        public EmergencyContact(String _name, String _number){
            name = _name;
            number = _number;
        }
    }

    public class EmergencyListAdapter extends ArrayAdapter<EmergencyContact>{
        public EmergencyListAdapter(Context context, ArrayList<EmergencyContact> contacts){
            super(context, 0, contacts);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            final EmergencyContact contact = getItem(position);

            if(convertView == null){
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.emergency_listitem, parent, false);
            }

            TextView name = (TextView)convertView.findViewById(R.id.emergency_name);
            TextView number = (TextView)convertView.findViewById(R.id.emergency_number);

            name.setText(contact.name);
            number.setText(contact.number);

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    emergencyCall(contact.number);
                }
            });

            return convertView;
        }
    }


    public static class BuildingsList{
        public static ArrayList<Building> buildings;
        public static Building JBHT;
        public static Context context;

        public BuildingsList(){
            buildings = new ArrayList<>();
        }

        public static void getBuildings(){
            JSONArray jsonArray = JSONProvider.getJSONFromFile(context, R.raw.buildings);

            for(int i=0; i<jsonArray.length(); i++){
                try {
                    JSONObject buildingObject = jsonArray.getJSONObject(i);
                    Building currentBuilding = new Building(buildingObject, context);
                    buildings.add(currentBuilding);
                    Log.i("BUILDING", buildingObject.getString("code"));

                    if(buildingObject.getString("code").equals("JBHT")){
                        JBHT = new Building(currentBuilding);
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }

        }
    }

    public ArrayList<Marker> addExtrasToMap(ArrayList<MarkerOptions> markerList, int tag){

        ArrayList<Marker> markers = new ArrayList<>();
        for(int i=0; i<markerList.size(); i++){
            Marker marker = mMap.addMarker(markerList.get(i));
            marker.setTag(tag);
            if(tag != 0) {
                marker.setTitle(Integer.toString(i));
            }
            markers.add(marker);
        }
        return markers;
    }

    public ArrayList<MarkerOptions> loadExtras(int fileId, int iconId){
        JSONArray jsonArray = JSONProvider.getJSONFromFile(getApplicationContext(), fileId);
        ArrayList<MarkerOptions> markerOptionsList = new ArrayList<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                LatLng position = new LatLng(jsonArray.getJSONObject(i).getDouble("latitude"), jsonArray.getJSONObject(i).getDouble("longitude"));
                MarkerOptions options = new MarkerOptions().position(position).icon(BitmapDescriptorFactory.fromResource(iconId));
                options.visible(true);
                markerOptionsList.add(options);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return markerOptionsList;
    }


    class BuildingsLoader extends AsyncTask<Params, Void, ArrayList<Building>>{
        protected ArrayList<Building> doInBackground(Params... params){
            ArrayList<Building> buildings = new ArrayList<>();
            JSONArray jsonArray = JSONProvider.getJSONFromFile(getApplicationContext(), R.raw.buildings);

            buildings = BuildingsList.buildings;
            return buildings;
        }

        protected void onPostExecute(ArrayList<Building> result) {
            if (buildings == null) {
                for (int i = 0; i < result.size(); i++) {
                    /*if (result.get(i).getBuildingCode().equals("JBHT"))
                        JBHT = result.get(i);*/

                    buildings = result;
                    Building currentBuilding = result.get(i);
                    //Log.i("ADDING BUILDING", currentBuilding.getBuildingCode());
                    result.get(i).shape = mMap.addPolygon(currentBuilding.getPolygonOptions());
                    result.get(i).label = mMap.addMarker(currentBuilding.getLabelOptions().title(Integer.toString(i)));
                    result.get(i).label.setTag(1);
                    bldgNames.add(result.get(i).getBuildingName());
                }
            }


            if (indoorMap == null) {
                indoorMap = getIndoorMap(BuildingsList.JBHT);
                for (int i = 0; i < indoorMap.size(); i++) {
                    Floor floor = indoorMap.get(i);
                    for (int j = 0; j < floor.rooms.size(); j++) {
                        Room room = floor.rooms.get(j);
                        room.shape = mMap.addPolygon(room.getShapeOptions());
                        room.label = mMap.addMarker(room.getLabelOptions());
                        room.label.setTag(0);
                        //Create separate array of room positions for use in searches
                        roomPositions.add(room.getPosition());

                        //Add rooms to the search autocomplete in the format Bldg Code: Room # (Floor #)
                        bldgNames.add("JBHT " + "Room " + room.getRoomNumber() + " (Floor " + Integer.toString(i + 1) + ")" );
                    }
                }
            }

            //Initialize list of buildings and rooms once they've been loaded in
            initAutoComplete();
        }

        private ArrayList<Floor> getIndoorMap(Building building) {
            try {
                if (building.HAS_INDOOR_MAP) {
                    building.indoorMap = new ArrayList<>();
                    JSONArray indoorArray = JSONProvider.getJSONFromFile(getApplicationContext(), R.raw.jbht_indoor);
                    for (int j = 0; j < indoorArray.length(); j++) {
                        building.indoorMap.add(new Floor(getApplicationContext(), indoorArray.getJSONArray(j)));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return building.indoorMap;
        }
    }
}


