package edu.uark.csce.helpahog;

import android.content.Context;
import android.graphics.Color;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.ui.IconGenerator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by toren on 11/20/17.
 */

public class Building {
    public boolean HAS_INDOOR_MAP = false;

    public Polygon shape;
    private PolygonOptions shapeOptions;
    private LatLng position;
    public Marker label;
    private MarkerOptions labelOptions;
    private String buildingName;
    private String buildingCode;
    private String buildingAddress;
    private JSONObject json;
    private int id;

    public ArrayList<Floor> indoorMap;


    //Hex values for building polygon colors
    int a, r, g, b;


    LatLngBounds.Builder builder = new LatLngBounds.Builder();
    Context context;


    public Building(JSONObject _json, Context _context){
        newInstance(_json, _context);
    }

    //Copy constructor
    public Building(Building in){
        newInstance(in.getJSON(), in.getContext());
    }

    //sets members when a new Building is instantiated
    private void newInstance(JSONObject _json, Context _context){
        json = _json;
        context = _context;


        try {
            position = new LatLng(Double.parseDouble(json.getString("latitude")), Double.parseDouble(json.getString("longitude")));
            shapeOptions = parseBuildingShape(json.getString("shape"));
            buildingName = json.getString("name");
            buildingCode = json.getString("code");
            buildingAddress = json.getString("address") + "\n" + json.getString("city") + ", " + json.getString("state") + " " + json.getString("zip");
            labelOptions = generateMarker(buildingCode, position).snippet(json.getString("address"));
            id = json.getInt("id");

            if(buildingCode.equals("JBHT"))
                HAS_INDOOR_MAP = true;

        }catch(Exception e){
            e.printStackTrace();
        }
    }


    //Parses the shape of the building from a JSON String
    private PolygonOptions parseBuildingShape(String input) throws ArrayIndexOutOfBoundsException{
        PolygonOptions options = new PolygonOptions();
        String[] tokens = input.split(",");


        //Get the time of day
        int time = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);


        //Set night mode if time is before 6am or 6pm local time
        if(time <= 6 || time >= 18){
            a=0xff; r=0xff; g=0x55; b=0x55;
        }else{
            a=0xff; r=0xff; g=0x70; b=0x70;
        }


        //Split each latlng value delimited by a space character
        for (int i = 0; i < tokens.length; i++) {
            String[] tmp = tokens[i].split(" ");
            String lat = tmp[0];
            String lng = tmp[1];

            options.add(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng))).visible(true);
            builder.include(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)));
        }


        //sets the color of the buildings (see day/night mode above)
        options.strokeColor(Color.argb(a, r, g, b));
        options.fillColor(Color.argb(a, r, g, b));
        options.zIndex(110);
        return options;
    }

    public MarkerOptions generateMarker(String text, LatLng labelPosition) throws JSONException {
        IconGenerator ig = new IconGenerator(context);
        ig.setBackground(null);
        ig.setTextAppearance(context, R.style.labelAppearance);

        MarkerOptions markerOptions = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(ig.makeIcon(text)));
        markerOptions.position(labelPosition);
        return markerOptions;
    }

    public void setLabelVisible(boolean selection){
        label.setVisible(selection);
    }

    public void setBuildingVisible(boolean selection){
        shape.setVisible(selection);
    }


    public JSONObject getJSON(){
        return json;
    }

    public Context getContext(){
        return context;
    }

    public PolygonOptions getPolygonOptions(){
        return shapeOptions;
    }

    public LatLng getPosition(){ return position;}

    public MarkerOptions getLabelOptions(){
        return labelOptions;
    }

    public String getBuildingName(){
        return buildingName;
    }

    public String getBuildingCode(){
        return buildingCode;
    }

    public String getBuildingAddress(){
        return buildingAddress;
    }

    public int getId(){
        return id;
    }


}

class Floor{
    public ArrayList<Room> rooms;
    public JSONArray floorArray;

    Context context;

    public Floor(Context _context, JSONArray _floorArray){
        context = _context;
        floorArray = _floorArray;
        rooms = parseRooms();
    }

    //Parse all of the room objects from the json file to a Room object
    private ArrayList<Room> parseRooms(){
        ArrayList<Room> tmp = new ArrayList<>();
        for(int i=0; i<floorArray.length(); i++){
            try {
                Room room = new Room(context, floorArray.getJSONObject(i));
                tmp.add(room);
            }catch(JSONException e){
                e.printStackTrace();
            }
        }

        return tmp;
    }

    public void setRoomLabelsVisible(boolean selection){
        for(int i=0; i<rooms.size(); i++){
            rooms.get(i).setLabelVisible(selection);
        }
    }

    public void setRoomsVisible(boolean selection){
        for(int i=0; i<rooms.size(); i++){
            rooms.get(i).setRoomVisible(selection);
        }
    }
}

class Room{
    private PolygonOptions shapeOptions;
    private MarkerOptions  labelOptions;
    private JSONObject jsonObject;

    public Polygon shape;
    public Marker label;
    public String roomNumber;
    public LatLng position;

    LatLngBounds.Builder builder = new LatLngBounds.Builder();

    Context context;

    int a, r, g, b;


    public Room(Context _context, JSONObject _jsonObject){
        context = _context;
        jsonObject = _jsonObject;

        a=0xFF; r=0x68; g=0x68; b=0x68;

        shapeOptions = parseRoomShape();
        position = parsePosition();
        roomNumber = parseRoomNumber();
        labelOptions = generateMarker(roomNumber, position);
    }

    //Get the room number from the json object
    private String parseRoomNumber(){
        try{
            return jsonObject.getString("room");
        }catch(JSONException e){
            e.printStackTrace();
        }
        return null;
    }

    //put the position from the json object into a LatLng object
    private LatLng parsePosition(){
        try {
            double lat = jsonObject.getJSONArray("position").getDouble(0);
            double lng = jsonObject.getJSONArray("position").getDouble(1);

            LatLng pos = new LatLng(lat, lng);
            return pos;
        }catch(JSONException e){
            e.printStackTrace();
        }
        System.out.println("null");
        return null;
    }

    //parse the shape of the room from the json object to a polygonsoptions object
    private PolygonOptions parseRoomShape(){
        PolygonOptions options = new PolygonOptions();
        try {
            JSONArray shapeArray = jsonObject.getJSONArray("shape");

            for (int i = 0; i < shapeArray.length(); i++) {
                double lat = shapeArray.getDouble(i);
                double lng = shapeArray.getDouble(++i);
                options.add(new LatLng(lat, lng)).visible(true);
                builder.include(new LatLng(lat, lng));
            }
        }catch(JSONException e){
            e.printStackTrace();
        }

        //sets the color of the buildings (see day/night mode above)
        options.strokeColor(Color.BLACK);
        options.fillColor(Color.argb(a, r, g, b));
        options.zIndex(100);
        return options;
    }

    //Create a marker with the room number as an icon
    private MarkerOptions generateMarker(String text, LatLng labelPosition){
        IconGenerator ig = new IconGenerator(context);
        ig.setBackground(null);
        ig.setTextAppearance(context, R.style.labelAppearance);

        MarkerOptions markerOptions = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(ig.makeIcon(text)));
        markerOptions.position(labelPosition);
        return markerOptions;
    }

    public PolygonOptions getShapeOptions(){
        return shapeOptions;
    }

    public MarkerOptions getLabelOptions(){
        return labelOptions;
    }

    public String getRoomNumber(){ return roomNumber; }

    public LatLng getPosition(){ return position; }

    public void setLabelVisible(boolean selection){
        label.setVisible(selection);
    }

    public void setRoomVisible(boolean selection){
        shape.setVisible(selection);
    }
}
