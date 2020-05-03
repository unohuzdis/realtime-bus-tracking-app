package com.fremtidmedia.httpwww.busadvisory;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;
import android.util.Log;
import android.view.View;

import android.content.DialogInterface;
import android.app.AlertDialog;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.here.android.mpa.mapping.MapRoute;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import biz.kasual.materialnumberpicker.MaterialNumberPicker;

public class MapActivity extends Activity {

    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    IBeaconDevice searchBeacon;

    private Map map = null;
    private MapFragment mapFragment = null;
    private GeoCoordinate userLocation;
    private GeoCoordinate busLocation;
    private PositioningManager positioningManager = null;
    private PositioningManager.OnPositionChangedListener positionListener;
    private boolean tracking = false;
    private MapRoute m_mapRoute;
    private int arrTime;
    private String busId;
    private int tripId;
    private double stoplng, stoplat;
    private int reminderTime;
    private Timer t = null;
    private BusTask tt = null;
    private FloatingActionButton fabEXIT;
    private TextView BottomBar;
    private TextView numText;
    private TextView mins;
    private SharedPreferences sp;
    private String _id;
    private String busName;

//    private ArrayList<MapRoute> mRoute = new ArrayList<>();
    private ArrayList<MapMarker> busStops = new ArrayList<>();
    private ArrayList<MapObject> markerList = new ArrayList<>();
    private RequestQueue queue;
    private String id;
    private String triggerURL;


    private void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[0]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }



    public void centerView (View views) {
        centerView(userLocation);
    }

    public void onResume() {
        super.onResume();
        if (positioningManager != null) {
             positioningManager.start(
                PositioningManager.LocationMethod.GPS_NETWORK);
    }
}

    public void onPause() {
        super.onPause();
        if (positioningManager != null) {
            positioningManager.stop();
        }

    }

    public void onDestroy() {
        if (positioningManager != null) {
            // Cleanup
            positioningManager.removeListener(
                    positionListener);
        }
        tt.cancel();
        t.cancel();
        map = null;
        super.onDestroy();
    }

    public void makePostRequest(String url){
        JSONObject jsonObject = new JSONObject();
        try {
            tripId = sp.getInt("tripId", -1);
            if(tripId == -1) {
                tripId = 0;
                sp.edit().putInt("tripId", 1).apply();
            } else {
                sp.edit().putInt("tripId", tripId + 1).apply();
            }
            jsonObject.put("userId", id);
            jsonObject.put("tripId", tripId);
            jsonObject.put("busId", busId);
            JSONArray coordinates = new JSONArray();
            coordinates.put(stoplng);
            coordinates.put(stoplat);
            JSONObject stopLocation = new JSONObject();
            stopLocation.put("type", "Point");
            stopLocation.put("coordinates", coordinates);
            stopLocation.put("reminderTime", reminderTime);
            jsonObject.put("stopLocation", stopLocation);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, null,  new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }
        );
        queue.add(jsonObjectRequest);
    }
    public void postTriggerRequest(String url){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("userId", id);
            jsonObject.put("tripId", tripId);
            jsonObject.put("beaconTrigger", busId);
            jsonObject.put("busId", busId);
            JSONArray coordinates = new JSONArray();
            coordinates.put(userLocation.getLongitude());
            coordinates.put(userLocation.getLatitude());
            JSONObject stopLocation = new JSONObject();
            stopLocation.put("type", "Point");
            stopLocation.put("coordinates", coordinates);
            jsonObject.put("startLocation", stopLocation);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, null,  new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    _id = (String) response.get("_id");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }
        );
        queue.add(jsonObjectRequest);
    }
    public void patchTriggerRequest(String url){
        JSONObject jsonObject = new JSONObject();
        try {
            JSONArray coordinates = new JSONArray();
            coordinates.put(userLocation.getLongitude());
            coordinates.put(userLocation.getLatitude());
            JSONObject stopLocation = new JSONObject();
            stopLocation.put("type", "Point");
            stopLocation.put("coordinates", coordinates);
            stopLocation.put("reminderTime", reminderTime);
            jsonObject.put("endlocation", stopLocation);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PATCH, url + _id, null,  new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }
        );
        queue.add(jsonObjectRequest);
    }


    private void makeGetRequest(String url){
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                            try {
                                busId = response.getString("busId");
                               JSONArray location = response.getJSONArray("buslocation");
                               JSONObject object1 = location.getJSONObject(0);
                               JSONObject object2 = object1.getJSONObject("location");
                               JSONArray object3 = object2.getJSONArray("coordinates");
                               String buslng = object3.getString(0);
                               String buslat = object3.getString(1);
                               busLocation = new GeoCoordinate(Double.parseDouble(buslat), Double.parseDouble(buslng) );
                               Log.d("Location", busLocation.getLatitude() + ", " +  busLocation.getLongitude());
                            }
                            catch (Exception e){

                                Log.e("HERE", "Caught: " + e.getMessage());

                            }

                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO: Handle error

                        }
                    });
            queue.add(jsonObjectRequest);
        }


    private void busGetRequest(String url){
        numText = findViewById(R.id.textView97);
        mins = findViewById(R.id.mins);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            arrTime = response.getInt("trafficTime");
                            Log.d("Time", Integer.toString(arrTime));
                            if (arrTime <= 1) {
                                mins.setText("minute");
                            }
                            if (arrTime < 1) {
                                numText.setText("<1");
                            }
                            if (arrTime >= 1) {
                                numText.setText(Integer.toString(arrTime));
                            }
                            if (arrTime > 1) {
                                mins.setText("minutes");
                            }
                        }
                        catch (Exception e){

                            Log.e("HERE", "Caught: " + e.getMessage());

                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error

                    }
                });
        queue.add(jsonObjectRequest);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                initialize();
                break;
        }
    }

    private void initialize (){
        sp = getSharedPreferences("bus advisory app",  Context.MODE_PRIVATE);
        setContentView(R.layout.activity_map);
//        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024);
//        Network network = new BasicNetwork(new HurlStack());
        queue = Volley.newRequestQueue(this);
        queue.start();
        makeGetRequest("https://oyojktxw02.execute-api.us-east-1.amazonaws.com/dev/buslocation");
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(MapActivity.this, new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String newToken = instanceIdResult.getToken();
                id = newToken;
                Log.e("This Token", newToken);
            }
        });
        KontaktSDK.initialize("zwPcatzTlLvusdiKXJKImhTqqhVbAJyN");
        kontaktDetect();

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapfragment);

        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    map = mapFragment.getMap();
                    map.setCenter(userLocation, Map.Animation.NONE);
                    map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) /1.75);

                    try {

                        Image userImage = new Image();
                        userImage.setImageResource(R.drawable.ic_action_person_pin);
                        map.getPositionIndicator().setMarker(userImage);
                        createStops();

                    } catch (Exception e) {
                        Log.e("HERE", e.getMessage());
                    }


                    positioningManager = PositioningManager.getInstance();
                    positionListener = new PositioningManager.OnPositionChangedListener() {
                        @Override
                        public void onPositionUpdated(PositioningManager.LocationMethod method, GeoPosition position, boolean isMapMatched) {
                            userLocation = position.getCoordinate();
                            BottomBar = findViewById(R.id.BottomBAR);
                            GeoCoordinate stop = closestStop(busStops);

                            if ((stoplat = stop.getLatitude()) == 49.939073 && (stoplng = stop.getLongitude()) == -119.394334){
                                BottomBar.setText("UBCO Exchange");
                                if (!tracking) {
                                    busName = "ubcoa";
                                    busGetRequest("https://oyojktxw02.execute-api.us-east-1.amazonaws.com/dev/bustime/ubco-a");
                                }
                            }
                            else if (stop.getLatitude() == 49.934023 && stop.getLongitude() == -119.401581){
                                BottomBar.setText("Academy Hill Stop");
                                if (!tracking) {
                                    busName = "ubcob";
                                    busGetRequest("https://oyojktxw02.execute-api.us-east-1.amazonaws.com/dev/bustime/ubco-b");
                                }
                            }

                        }
                        @Override
                        public void onPositionFixChanged(PositioningManager.LocationMethod method, PositioningManager.LocationStatus status) { }
                    };

                    try {
                        positioningManager.addListener(new WeakReference<>(positionListener));
                        if(!positioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK)) {
                            Log.e("HERE", "PositioningManager.start: Failed to start...");
                        }
                    } catch (Exception e) {
                        Log.e("HERE", "Caught: " + e.getMessage());
                    }

                    map.getPositionIndicator().setVisible(true);

                } else {
                    System.out.println("ERROR: Cannot initialize Map Fragment");
                }
            }
        });

/*      TODO: INI's CODE                            */

        //TRACKING & its animation (might not use)
        //final TextView TRACKING = findViewById(R.id.tracking);
        final TextView newTRACKING = findViewById(R.id.NEWtrack);
        newTRACKING.setVisibility(View.INVISIBLE);

        final Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(200);
        anim.setStartOffset(20);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);

        //final TextView GoText = findViewById(R.id.GoText);
        //GoText.setVisibility(View.INVISIBLE);
        final FloatingActionButton fabGO = findViewById(R.id.startBell);
        //fabGO.hide();


        fabEXIT = findViewById(R.id.floatingActionButtonEXIT);
        fabEXIT.hide();
        fabEXIT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Info", "Exit pressed");

                //ARE U SURE ALERT
                AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                builder.setTitle("Confirmation");
                builder.setMessage("Are you sure you want to cancel your reminder?");

                builder.setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fabEXIT.hide();
                        if(!markerList.isEmpty()) {
                            map.removeMapObjects(markerList);
                            markerList.clear();
                        }
                        if(m_mapRoute != null) {
                            map.removeMapObject(m_mapRoute);
                        }
                        if (tracking){
                            tt.cancel();
                            t.cancel();
                            topicUnSubscribe("bus" + busName + "time" + reminderTime);

                            tracking = false;
                        }

                        fabGO.show();
                        //GoText.setVisibility(View.INVISIBLE);
                        newTRACKING.setVisibility(View.INVISIBLE);
                        anim.cancel();

                        dialog.dismiss();
                    }
                });

                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                    }
                });

                AlertDialog al = builder.create();
                al.show();





            }
        });






        fabGO.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Info", "GO pressed");

                //NUMBER PICKER
                MaterialNumberPicker numberPicker = new MaterialNumberPicker(MapActivity.this);

                numberPicker.setMinValue(1);
                numberPicker.setMaxValue(100);
                numberPicker.setBackgroundColor(Color.WHITE);
                numberPicker.setSeparatorColor(Color.TRANSPARENT);
                numberPicker.setTextColor(Color.BLACK);
                numberPicker.setTextSize(50);
                numberPicker.setWrapSelectorWheel(true);
                numberPicker.buildLayer();

                final AlertDialog.Builder newAL = new AlertDialog.Builder(MapActivity.this);

                TextView AlTitle = new TextView(MapActivity.this);
                AlTitle.setText("Remind me before the bus arrival \n (in minutes) at my stop");
                AlTitle.setTextSize(17);
                AlTitle.setTextColor(Color.BLACK);
                AlTitle.setTypeface(null, Typeface.BOLD);
                centerView(busLocation);
                //newAL.setTitle("Remind me before the bus arrival \n (in minutes) at my stop");
                newAL.setCustomTitle(AlTitle);
                newAL.setView(numberPicker);
                if (!tracking) {
                    tracking = true;
                    t = new Timer();
                    tt = new BusTask();
                    t.schedule(tt, 0, 7000);
                }

                newAL.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        fabEXIT.show();
                        fabGO.hide();
                        newTRACKING.setVisibility(View.VISIBLE);
                        anim.start();

                        // THANK YOU ALERT
                        final AlertDialog.Builder TY = new AlertDialog.Builder(MapActivity.this);
                        TY.setTitle("Reminder");
                        TY.setMessage("Thank you for setting a reminder. We will notifiy you.");

                        TY.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                reminderTime = which;
                                makePostRequest("https://oyojktxw02.execute-api.us-east-1.amazonaws.com/dev/triprequest");
                                topicSubscribe("bus" + busName + "time" + reminderTime);
                                // TODO
                                dialog.dismiss();

                            } });

                        AlertDialog TYdone = TY.create();
                        TYdone.show();


                    }
                });

                newAL.create().show();


            }
        });

//        TextView t0 = findViewById(R.id.textView97);
        BottomBar = findViewById(R.id.BottomBAR);

        BottomBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!tracking) {
                    fabGO.show();
                    tracking = true;
                    t = new Timer();
                    tt = new BusTask();
                    t.schedule(tt, 0, 7000);
                }
                centerView(busLocation);
            }
        });




    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        triggerURL = "https://oyojktxw02.execute-api.us-east-1.amazonaws.com/dev/triggers";
        checkPermissions();

        }



    class BusTask extends TimerTask {

        @Override
         public void run() {
            makeGetRequest("https://oyojktxw02.execute-api.us-east-1.amazonaws.com/dev/buslocation");
            GeoCoordinate stop = closestStop(busStops);
            if (stop.getLatitude() == 49.939073 && stop.getLongitude() == -119.394334){
                busGetRequest("https://oyojktxw02.execute-api.us-east-1.amazonaws.com/dev/bustime/ubco-a");
            }
            else if (stop.getLatitude() == 49.934023 && stop.getLongitude() == -119.401581){
                busGetRequest("https://oyojktxw02.execute-api.us-east-1.amazonaws.com/dev/bustime/ubco-b");
            }

            createBus(busLocation);
            Log.d("HERE", "Bus location updated");
        }
    }


    private void createStops() {
        try {
            Image image = new Image();
            image.setImageResource(R.drawable.ic_trip_origin);
            MapMarker stop1 = new MapMarker(new GeoCoordinate(49.939073, -119.394334, 0.0), image);
            map.addMapObject(stop1);
            MapMarker stop2 = new MapMarker(new GeoCoordinate(49.934023, -119.401581, 0.0), image);
            map.addMapObject(stop2);
            busStops.add(stop1);
            busStops.add(stop2);
        } catch (Exception e) {
            Log.e("HERE", e.getMessage());
        }

    }

    private void centerView (GeoCoordinate location){
        map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 1.5);
        map.setCenter(location, Map.Animation.NONE);
    }

    private void createBus(GeoCoordinate location) {
        try {
            Image image = new Image();
            image.setImageResource(R.drawable.ic_action_directions_bus);

            if(!markerList.isEmpty()) {
                map.removeMapObjects(markerList);
                markerList.clear();
            }
            MapMarker busMarker = new MapMarker(location, image);
            markerList.add(busMarker);
            map.addMapObjects(markerList);

        }catch (Exception e) {
            Log.e("HERE", "Caught: " + e.getMessage());
        }

    }


    private GeoCoordinate closestStop(ArrayList<MapMarker> stops ) {
        double smallestHyp = Double.POSITIVE_INFINITY;
        MapMarker closest = null;
        for (int i = 0; i < stops.size() ; i++) {
            double tempY = Math.abs(stops.get(i).getCoordinate().getLatitude() - userLocation.getLatitude());
            double tempX = Math.abs(stops.get(i).getCoordinate().getLongitude() - userLocation.getLongitude());
            double tempHyp = Math.hypot(tempY, tempX);
            if ( tempHyp < smallestHyp ){
                smallestHyp = tempHyp;
                closest = stops.get(i);

            }
        }
        if(closest == null)
            return null;
        return new GeoCoordinate(closest.getCoordinate().getLatitude(),closest.getCoordinate().getLongitude());


    }


    private void topicSubscribe(String topic){
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(MapActivity.this, "Couldn't connect to server", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    private void topicUnSubscribe(String topic){
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(MapActivity.this, "Couldn't connect to server", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    private void kontaktDetect() {
        IBeaconListener iBeaconListener = new IBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
                Log.e("beacon", ibeacon.toString());
                if (busId != null && ibeacon.getAddress().equals(busId)) {
                    postTriggerRequest(triggerURL);

                }

        }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> iBeacons, IBeaconRegion region) {

            }

            @Override
            public void onIBeaconLost(IBeaconDevice iBeacon, IBeaconRegion region) {
                if (busId != null && _id != null && iBeacon.getAddress().equals(busId)) {
                    patchTriggerRequest(triggerURL);

                }
            }
            }

            ;
    }
}
