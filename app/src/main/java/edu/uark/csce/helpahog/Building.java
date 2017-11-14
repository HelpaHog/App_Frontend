package edu.uark.csce.helpahog;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
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
 * Created by toren on 11/13/17.
 *
 * This is an object that will be used to store data for each
 * campus building.
 */



public class Building {
    public boolean INDOOR_LOADED = false;

    public class Room{
        Polygon shape;
        String roomNumber;
        public Room(Polygon _shape, String _roomNumber){
            shape = _shape;
            roomNumber = _roomNumber;
        }
    }

    private Polygon shape;
    private LatLng position;
    private String code;
    private JSONObject json;

    private Marker buildingLabel;

    public ArrayList<ArrayList<Room>> indoor = new ArrayList<>();
    public ArrayList<ArrayList<Polygon>> indoor_poly = new ArrayList<>();

    LatLngBounds.Builder builder = new LatLngBounds.Builder();      //builder for the building label
    GoogleMap map;
    Context context;

    int a,r,g,b;

    public Building(JSONObject _json, GoogleMap _map, Context _context){
       newInstance(_json, _map, _context);
    }

    public Building(Building in){
        newInstance(in.getJson(), in.getMap(), in.getContext());
    }

    private void newInstance(JSONObject _json, GoogleMap _map, Context _context){
        json = _json;
        context = _context;
        map = _map;

        int time = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        //Set night mode if time is before 6am or 6pm local time
        if(time <= 6 && time >= 18){
            a=0xff; r=0x91; g=0x00; b=0x00;
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_night));
        }else{
            a=0xff; r=0xff; g=0x70; b=0x70;
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_day));
        }

        try {
            position = new LatLng(Double.parseDouble(json.getString("latitude")), Double.parseDouble(json.getString("longitude")));
            PolygonOptions options = parseBuildingShape(json.getString("shape"));
            shape = map.addPolygon(options);
            code = json.getString("code");
            buildingLabel = map.addMarker(generateMarker());
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    int currentFloorShown = -1;
    public void selectFloor(int i){
        if (currentFloorShown != -1) {
            //Set everything to invisible
            for (int j = 0; j < indoor_poly.get(currentFloorShown).size(); j++) {
                indoor_poly.get(currentFloorShown).get(j).setVisible(false);
            }
        }
        if(i!=0) {
            //Set selected floor visible
            for (int j = 0; j < indoor_poly.get(i - 1).size(); j++) {
                indoor_poly.get(i - 1).get(j).setVisible(true);
                currentFloorShown = i - 1;
            }
        }

    }
    public Polygon getShape(){
        return shape;
    }

    public LatLng getPosition(){
        return position;
    }

    public void visible(boolean selection){
        shape.setVisible(selection);
    }

    public String getCode(){
        return code;
    }

    public Context getContext(){
        return context;
    }

    public JSONObject getJson(){
        return json;
    }

    public GoogleMap getMap(){
        return map;
    }

    public void setIndoorMap(JSONArray _indoorJSON) throws JSONException{
        for(int i=0; i<_indoorJSON.length(); i++){
            ArrayList<Room> rooms = new ArrayList<>();
            ArrayList<Polygon> polygons = new ArrayList<>();
            JSONArray array = _indoorJSON.getJSONArray(i);

            for(int j=0; j< array.length(); j++){
                JSONObject roomObject = array.getJSONObject(j);

                Polygon roomShape = map.addPolygon(parseRoomShape(roomObject.getJSONArray("shape")));
                polygons.add(roomShape);

                Room room = new Room(roomShape, roomObject.getString("room"));
                rooms.add(room);
            }
            indoor.add(rooms);
            indoor_poly.add(polygons);
        }

        INDOOR_LOADED = true;
    }

    public MarkerOptions generateMarker() throws JSONException{
        IconGenerator ig = new IconGenerator(context);
        ig.setBackground(null);
        ig.setTextAppearance(context, R.style.labelAppearance);

        MarkerOptions markerOptions = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(ig.makeIcon(code)));
        markerOptions.position(position);
        return markerOptions;
    }

    //Parses the shape of the building from a JSON String
    private PolygonOptions parseBuildingShape(String input) throws ArrayIndexOutOfBoundsException{
        PolygonOptions options = new PolygonOptions();
        String[] tokens = input.split(",");

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

    //Parses the shape of the building from a JSON String
    private PolygonOptions parseRoomShape(JSONArray input) throws ArrayIndexOutOfBoundsException{
        PolygonOptions options = new PolygonOptions();
        try {
            for (int i = 0; i < input.length(); i++) {
                double lat = input.getDouble(i);
                double lng = input.getDouble(++i);
                options.add(new LatLng(lat, lng)).visible(false);
                builder.include(new LatLng(lat, lng));
            }
        }catch(JSONException e){
            e.printStackTrace();
        }

        //sets the color of the buildings (see day/night mode above)
        options.strokeColor(Color.argb(a, r, g, b));
        options.fillColor(Color.argb(a, r, g, b));
        options.zIndex(100);
        return options;
    }
}
