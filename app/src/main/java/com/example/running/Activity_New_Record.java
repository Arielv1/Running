package com.example.running;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class Activity_New_Record extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "Activity_New_Record";

    private Button btnStart;
    private Button btnPause;
    private Button btnStop;
    private Button btnConfirm;
    private Button btnCancel;
    private Chronometer tmrChronometer;
    private long timeOfPause;
    private boolean runningChronometer;
    private boolean stoppedChronometer;

    private long savedState;
    private String cardioActivityChoice;

    private int lastRadioChoiceIndex = 0;

    TextView txt_distance;
    TextView txt_speed;
    //google map object
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    LocationRequest locationRequest;

    private long time = 1;
    private double speed;
    private double distance;
    private Location lastLocation = null;

    Runnable secondlyRun;
    private Handler handler = new Handler();
    private boolean timer_start = false;
    private int timer_value = 1;
    private final int DELAY = 1000;
    private final int ZOOM_VALUE = 20;

    private final static int LOCATION_REQUEST_CODE = 23;
    boolean locationPermission = false;

    //polyline object
    private List<Polyline> polylines = null;
    private Polyline polyline;
    private ArrayList<LatLng> points = new ArrayList();

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "NewRun - onSavedInstance " + tmrChronometer.getBase());

    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        savedState = savedInstanceState.getLong("CHRONO_STATE");
        Log.d(TAG, "NewRun - onRestoreInstanceState " + savedState);
        tmrChronometer.setBase(savedState);
        tmrChronometer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("ViewLogger", "NewRun - onPause Invoked");
    }

    @Override
    protected void onStart() {
        Log.d("ViewLogger", "NewRun - onStart Invoked");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d("ViewLogger", "NewRun - onResume Invoked");
        super.onResume();
    }

    @Override
    protected void onStop() {
        Log.d("ViewLogger", "NewRun - onStop Invoked");
        super.onStop();
    }

    @Override
    protected void onRestart() {
        Log.d("ViewLogger", "NewRun - onDestroy Invoked");
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        Log.d("ViewLogger", "NewRun - onDestroy Invoked");
        super.onDestroy();
    }

    int counter = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("ViewLogger", "NewRun - onCreate Invoked");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_record);

        setUpViews();

        setUpFragments();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationRequest();

        requestPermision();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.d(TAG, "onLocationResult");
                if (locationResult == null) {
                    return;
                }
//                polylines = new ArrayList<>();
                PolylineOptions polyOptions = new PolylineOptions();
//                ArrayList<LatLng> localpoints = new ArrayList<>();
                if (polyline != null) polyline.remove();
                LatLng lastLatLng = null;
                Location currentLocation = locationResult.getLastLocation();
                for (Location location : locationResult.getLocations()) {
                    LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
                    points.add(point);
                    Log.d(TAG, "point is:" + point.toString());
                    lastLatLng = point;
                }
                if (lastLocation != null) {
                    // if one of them not equel then location changed.... so we get distance
                    if (lastLocation.getLatitude() != currentLocation.getLatitude() && lastLocation.getLongitude() != currentLocation.getLongitude()) {
                        distance += lastLocation.distanceTo(currentLocation);
                    }
                }
                lastLocation = locationResult.getLastLocation();
                txt_distance.setText("" + distance);
                txt_speed.setText("" + (distance / time));
                polyOptions.addAll(points);
                polyOptions.width(15);
                polyOptions.color(Color.BLACK);
                polyOptions.geodesic(true);
                polyline = mMap.addPolyline(polyOptions);
                Log.d(TAG, "polyline is:" + polyline.toString());
//                polylines.add(polyline);
                Log.d(TAG, "LastLatLng is:" + lastLatLng.latitude + " lng is: " + lastLatLng.longitude);

                if (lastLatLng != null)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastLatLng, ZOOM_VALUE));
            }
        };

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start(view);
            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pause(view);
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop(view);
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), Activity_Main_Menu.class));
            }
        });

        tmrChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                Toast.makeText(Activity_New_Record.this, "" + counter, Toast.LENGTH_SHORT).show();
                counter++;
            }
        });

        //init google map fragment to show map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void requestPermision() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            locationPermission = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0  && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //if permission granted.
                    locationPermission = true;
                    getMyLocation();

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    //to get user location
    @SuppressLint("MissingPermission")
    private void getMyLocation() {

        if (locationPermission) {
            Log.d(TAG, "before calling Requect Location Update");
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getMyLocation();
    }

    private void start(View view) {

        if(!runningChronometer && !stoppedChronometer) {
            tmrChronometer.setBase(SystemClock.elapsedRealtime() - timeOfPause);
            tmrChronometer.start();
            runningChronometer = true;
        }
    }

    private void pause(View view) {

        if(runningChronometer && !stoppedChronometer) {
            timeOfPause = SystemClock.elapsedRealtime() - tmrChronometer.getBase();
            tmrChronometer.stop();
            runningChronometer = false;
        }
        else if (!stoppedChronometer){
            tmrChronometer.setBase(SystemClock.elapsedRealtime() - timeOfPause);
            tmrChronometer.start();
            runningChronometer = true;
        }
    }

    private void stop(View view) {

        tmrChronometer.stop();
        runningChronometer = false;
        stoppedChronometer = true;
        btnCancel.setVisibility(View.VISIBLE);
        btnConfirm.setVisibility(View.VISIBLE);

    }

    private void setUpViews() {
        txt_distance = findViewById(R.id.new_run_LBL_actual_distance);
        txt_speed = findViewById(R.id.new_run_LBL_actual_avg_speed);
        btnStart = findViewById(R.id.new_run_BTN_start);
        btnPause = findViewById(R.id.new_run_BTN_pause);
        btnStop = findViewById(R.id.new_run_BTN_stop);
        tmrChronometer = findViewById(R.id.new_run_TMR_chronometer);
        btnCancel = findViewById(R.id.new_run_BTN_cancel);
        btnConfirm = findViewById(R.id.new_run_BTN_confirm);
    }

    private void setUpFragments() {
        Fragment_Radio_Buttons fragment_radio_buttons = Utils.getInstance().createFragmentRadioButtons(this, callback, R.id.new_run_LAY_radio_buttons, false);

    }

    Callback_RadioChoice callback = new Callback_RadioChoice() {
        @Override
        public void setRadioButtonChoice(String radioChoiceValue) {
            cardioActivityChoice = radioChoiceValue;

        }
    };

}