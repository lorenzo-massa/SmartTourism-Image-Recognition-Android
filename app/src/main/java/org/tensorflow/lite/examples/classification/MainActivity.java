package org.tensorflow.lite.examples.classification;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.tensorflow.lite.examples.classification.tflite.DatabaseAccess;

public class MainActivity extends AppCompatActivity implements MonumentAdapter.OnButtonClickListener {

    public static final double MAX_DISTANCE = 500;  //TODO test range
    public static final double MAX_DISTANCE_RECOGNIZED = 150; //TODO test range
    static final long NOTIFICATION_TIME = 5 * 60000; //60000 = 1 minute

    public static String categoryPath = "file:///android_asset/currentCategories/";
    public static String guidePath = "currentGuide/";
    private static final String TAG = "MainActivity";
    public static boolean isRunning = false;
    public static String language;
    public static String uniqueID;
    public static MyLocationListener locationListener;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isRunning = true;

        language = getIntent().getStringExtra("language");
        uniqueID = getIntent().getStringExtra("user_id");

        //Preferences button
        ActionMenuItemView btnPreferences = findViewById(R.id.settings);
        btnPreferences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PreferencesActivity.class);
                startActivity(intent);
            }
        });

        //FAB button
        FloatingActionButton btnFAB = findViewById(R.id.fab);
        btnFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ClassifierActivity.class);
                intent.putExtra("language", language);
                startActivity(intent);
            }
        });


        // Get the current location
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener(MainActivity.this);


        // Request location updates
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Consider calling ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.e(TAG, "ERROR: checkSelfPermission(ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION)");
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60 * 1000, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60 * 1000, 0, locationListener);


    }

    @Override
    protected void onResume() {
        super.onResume();

        if (language == null) {
            // Set a default language
            Log.e(TAG, "[ERROR] Language is null");
        }

        isRunning = true;

        //Update attributes and categories if you edit preferences
        DatabaseAccess databaseAccess = DatabaseAccess.getInstance(this);
        databaseAccess.updateDatabaseColdStart();

        RecyclerView recyclerView = findViewById(R.id.listCardView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        MonumentAdapter monumentAdapter = new MonumentAdapter(DatabaseAccess.getInstance(this).getListCategoriesOrdered(), this);
        recyclerView.setAdapter(monumentAdapter);


        //check if notifications are enabed in preferences
        if (!DatabaseAccess.getSharedPreferences().getBoolean("pref_key_notifications", true)
                && !DatabaseAccess.getSharedPreferences().getBoolean("pref_key_gps_classifier", true)) {
            locationManager.removeUpdates(locationListener);
        } else {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60 * 1000, 0, locationListener);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60 * 1000, 0, locationListener);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        isRunning = false;
    }

    @Override
    public void onButtonClick(String monument) {
        if (PreferencesActivity.language != null) {
            language = PreferencesActivity.language;
        }
        Intent intent = new Intent(MainActivity.this, GuideActivity.class);
        intent.putExtra("monument_id", monument);
        intent.putExtra("language", language);
        intent.putExtra("user_id", uniqueID);
        startActivity(intent);
    }

}

