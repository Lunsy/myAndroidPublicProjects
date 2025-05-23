package com.example.taxiapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.taxiapp.databinding.ActivityPassengerMapsBinding;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class PassengerMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityPassengerMapsBinding binding;
    private static final int CHECK_SETTINGS_CODE = 123;
    private static final int REQUEST_LOCATION_PERMISSION = 121;
    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;

    private boolean isLocationUpdatesActive;

    private Button signOutButton, settingsButton, bookTaxiButton;

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private int searchRadius = 1;
    private boolean isDriverFound = false;
    private String nearestDriverId;
    private  DatabaseReference driversGeoFire, nearestDriverLocation;
    private Marker driverMarker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_maps);

        binding = ActivityPassengerMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.map, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);

        settingsButton = findViewById(R.id.settings_Passenger_Button);
        signOutButton = findViewById(R.id.sign_out_Passenger_Button);
        bookTaxiButton = findViewById(R.id.bookTaxiButton);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        fusedLocationClient = LocationServices.
                getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        signOutButton.setOnClickListener((view) -> {
            auth.signOut();
            signOutPassenger();

        });

        bookTaxiButton.setOnClickListener((view) ->{
            
            bookTaxiButton.setText("Getting your taxi...");
            
            gettingNearestTaxi();
        });

        driversGeoFire = FirebaseDatabase.
                getInstance("https://mytaxiapp-8615b-default-rtdb.firebaseio.com/").
                getReference().child("driversGeoFire");

        buildLocationRequest();
        buildLocationCallback();
        buildLocationSettingsRequest();
        startLocationUpdates();
    }

    private void gettingNearestTaxi() {

        GeoFire geoFire = new GeoFire(driversGeoFire);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(
                currentLocation.getLatitude(),
                currentLocation.getLongitude()
            ), searchRadius);

        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                if(!isDriverFound){

                    isDriverFound = true;
                    nearestDriverId = key;

                    getNearestDriverLocation();

                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

                if(!isDriverFound){

                    searchRadius++;
                    gettingNearestTaxi();

                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void getNearestDriverLocation() {

        bookTaxiButton.setText("Getting your location...");
        nearestDriverLocation = FirebaseDatabase
                .getInstance("https://mytaxiapp-8615b-default-rtdb.firebaseio.com/")
                .getReference()
                .child("driversGeoFire")
                .child(nearestDriverId)
                .child("l");

        nearestDriverLocation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists()){

                    List<Object> driverLocationParameters =
                            (List<Object>) snapshot.getValue();

                    double latitude = 0;
                    double longitude = 0;

                    if (driverLocationParameters.get(0) != null){

                        latitude = Double.parseDouble(
                                driverLocationParameters.get(0).toString()
                        );

                    }
                    if (driverLocationParameters.get(1) != null){

                        longitude = Double.parseDouble(
                                driverLocationParameters.get(1).toString()
                        );

                    }

                    LatLng driverLatLng = new LatLng(latitude, longitude);

                    if (driverMarker != null){

                        driverMarker.remove();

                    }

                    Location driverLocation = new Location("");
                    driverLocation.setLatitude(latitude);
                    driverLocation.setLongitude(longitude);

                    float distanceToDriver =
                            driverLocation.distanceTo(currentLocation);

                    bookTaxiButton.setText("Your driver is here " + distanceToDriver);

                    driverMarker = mMap.addMarker(
                            new MarkerOptions().position(driverLatLng)
                                    .title("Your driver is here ")
                    );

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void updateLocationUI() {

        if (currentLocation != null){

            LatLng passengerLocation = new LatLng(currentLocation.getLatitude(),
                    currentLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(passengerLocation));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
            mMap.addMarker(new MarkerOptions().position(passengerLocation).
                    title("Passenger Location"));



            String passengerUserId = currentUser.getUid();
            DatabaseReference passengersGeoFire = FirebaseDatabase.getInstance(
                            "https://mytaxiapp-8615b-default-rtdb.firebaseio.com/").
                    getReference().child("passengersGeoFire");

            DatabaseReference passengers = FirebaseDatabase.getInstance(
                            "https://mytaxiapp-8615b-default-rtdb.firebaseio.com/").
                    getReference().child("passengers");
            passengers.setValue(true);

            GeoFire geoFire = new GeoFire(passengersGeoFire);
            geoFire.setLocation(passengerUserId,
                    new GeoLocation(currentLocation.getLatitude(),
                            currentLocation.getLongitude()));

        }
    }
    private void signOutPassenger() {

        String passengerUserId = currentUser.getUid();
        DatabaseReference passengers = FirebaseDatabase.getInstance(
                        "https://mytaxiapp-8615b-default-rtdb.firebaseio.com/").
                getReference().child("passengers");



        GeoFire geoFire = new GeoFire(passengers);
        geoFire.removeLocation(passengerUserId);



        Intent intent = new Intent(PassengerMapsActivity.this,
                ChooseModeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (currentLocation != null) {

            // Add a marker in Sydney and move the camera
            LatLng passengerLocation = new LatLng(currentLocation.getLatitude(),
                    currentLocation.getLongitude());
            mMap.addMarker(new MarkerOptions().position(passengerLocation).title("Passenger Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(passengerLocation));

        }
    }

    private void stopLocationUpdates() {

        if (!isLocationUpdatesActive){
            return;
        }

        fusedLocationClient.removeLocationUpdates(locationCallback).
                addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        isLocationUpdatesActive = false;

                    }
                });

    }

    private void startLocationUpdates() {

        isLocationUpdatesActive = true;


        settingsClient.checkLocationSettings(locationSettingsRequest).
                addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(
                            LocationSettingsResponse locationSettingsResponse) {
                        if (ActivityCompat.checkSelfPermission(PassengerMapsActivity.this,
                                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                                PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(PassengerMapsActivity.this,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                                        PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        fusedLocationClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback,
                                Looper.myLooper()
                        );

                        updateLocationUI();
                    }
                }).
                addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        int statusCode = ((ApiException) e).getStatusCode();

                        if (statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED){
                            try {
                                ResolvableApiException resolvableApiException =
                                        (ResolvableApiException) e;
                                resolvableApiException.startResolutionForResult(
                                        PassengerMapsActivity.this,
                                        CHECK_SETTINGS_CODE
                                );
                            } catch (IntentSender.SendIntentException sie){
                                sie.printStackTrace();
                            }
                        }
                        else if(statusCode == LocationSettingsStatusCodes.
                                SETTINGS_CHANGE_UNAVAILABLE){
                            String message = "Adjust location settings in your" +
                                    "device";
                            Toast.makeText(PassengerMapsActivity.this, message,
                                    Toast.LENGTH_LONG).show();

                            isLocationUpdatesActive = false;

                        }

                        updateLocationUI();
                    }
                });

    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHECK_SETTINGS_CODE) {

            if (resultCode == Activity.RESULT_OK) {
                Log.d("PassengerMapsActivity", "User has agreed to changes" +
                        "settings");
                startLocationUpdates();
            }
            if (requestCode == Activity.RESULT_CANCELED) {
                Log.d("PassengerMapsActivity", "User has not agreed to changes" +
                        "settings");
                isLocationUpdatesActive = false;

                updateLocationUI();
            }
        }

    }

    private void buildLocationSettingsRequest() {

        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                currentLocation = locationResult.getLastLocation();

                updateLocationUI();

            }
        };
    }



    private void buildLocationRequest() {

        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isLocationUpdatesActive && checkLocationPermission()){
            startLocationUpdates();
        } else if (!checkLocationPermission()){
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {

        boolean shouldProvideRationale = ActivityCompat.
                shouldShowRequestPermissionRationale(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                );
        if (shouldProvideRationale) {

            showSnackBar(
                    "Location permission is needed for" +
                            "app functionality",
                    "OK",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(
                                    PassengerMapsActivity.this,
                                    new String[]{
                                            android.Manifest.permission.ACCESS_FINE_LOCATION
                                    },
                                    REQUEST_LOCATION_PERMISSION
                            );
                        }
                    }
            );
        } else {
            ActivityCompat.requestPermissions(
                    PassengerMapsActivity.this,
                    new String[]{
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    REQUEST_LOCATION_PERMISSION
            );
        }
    }


    private void showSnackBar(
            final String mainText,
            final String action,
            View.OnClickListener listener
    ) {
        Snackbar.make(
                        findViewById(android.R.id.content),
                        mainText,
                        Snackbar.LENGTH_INDEFINITE)
                .setAction(
                        action,
                        listener
                ).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {

            if (grantResults.length <= 0) {
                Log.d("onRequestPermissionsResult",
                        "Request was cancelled");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isLocationUpdatesActive){
                    startLocationUpdates();
                }
            } else {
                showSnackBar(
                        "Turn on location on settings",
                        "Settings",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                );
                                Uri uri = Uri.fromParts(
                                        "package",
                                        getPackageName(),
                                        null
                                );
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        }
                );
            }
        }
    }

    private boolean checkLocationPermission() {

        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }
}