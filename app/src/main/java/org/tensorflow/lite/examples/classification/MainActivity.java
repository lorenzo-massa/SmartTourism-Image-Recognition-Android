package org.tensorflow.lite.examples.classification;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.tensorflow.lite.examples.classification.tflite.DatabaseAccess;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements MonumentAdapter.OnButtonClickListener {

    public static boolean isRunning = false;
    final double MAX_DISTANCE = 100;  //TODO test range
    private String language;
    private String uniqueID;

    private static final String TAG = "MainActivity";

    private LocationListener locationListener;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isRunning = true;

        language = getIntent().getStringExtra("language");
        uniqueID = getIntent().getStringExtra("user_id");

        DatabaseAccess databaseAccess = DatabaseAccess.getInstance();
        databaseAccess.updateDatabaseColdStart();


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
        locationListener = new LocationListener() {
            private long lastNotificationTime = System.currentTimeMillis();

            @Override
            public void onLocationChanged(Location location) {
                double currentLat = location.getLatitude();
                double currentLng = location.getLongitude();

                Log.d(TAG, "onLocationChanged: " + currentLat + " " + currentLng);

                if (System.currentTimeMillis() - lastNotificationTime >= 60000){
                    // Define the target location
                    String nearestMonument = DatabaseAccess.getNearestMonument(currentLat, currentLng, MAX_DISTANCE);
                    Log.d(TAG, "onLocationChanged: " + nearestMonument);

                    // Check proximity and send notification if within range and enough time has passed
                    if (nearestMonument != null) {
                        // Update the last notification time
                        lastNotificationTime = System.currentTimeMillis();

                        // Create an explicit intent for the app's main activity
                        Intent intent = new Intent(MainActivity.this, GuideActivity.class);
                        intent.putExtra("monument_id", nearestMonument);
                        intent.putExtra("language", language);
                        intent.putExtra("user_id", uniqueID);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

                        // Create and send notification
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, "channel_id")
                                .setSmallIcon(R.drawable.done)
                                .setContentTitle("Nearby Monument")
                                .setContentText("You are near " + nearestMonument + "! Click here to learn more.")
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setDefaults(NotificationCompat.DEFAULT_ALL)
                                .setContentIntent(pendingIntent) // Set the pending intent
                                .setAutoCancel(true) // Dismiss the notification when the user taps on it
                                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);
                        NotificationChannel channel = new NotificationChannel("channel_id", "channel_name", NotificationManager.IMPORTANCE_HIGH);
                        notificationManager.createNotificationChannel(channel);

                        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                            int notificationId = new Random().nextInt();
                            notificationManager.notify(notificationId, builder.build());
                        }
                    }
                }


            }

            @Override
            public void onProviderDisabled(String provider) {
                // Handle provider disabled
                Log.d(TAG, "onProviderDisabled: " + provider);
            }

            @Override
            public void onProviderEnabled(String provider) {
                // Handle provider enabled
                Log.d(TAG, "onProviderEnabled: " + provider);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // Handle status changed
                Log.d(TAG, "onStatusChanged: " + provider + " status: " + status);
            }
        };


        // Request location updates
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);


    }

    @Override
    protected void onResume() {
        super.onResume();

        isRunning = true;

        //Update attributes and categories if you edit preferences
        RecyclerView recyclerView = findViewById(R.id.listCardView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        MonumentAdapter monumentAdapter = new MonumentAdapter(DatabaseAccess.getInstance().getListCategoriesOrdered(), this);
        recyclerView.setAdapter(monumentAdapter);


        //check if notifications are enabed in preferences
        if (!DatabaseAccess.getSharedPreferences().getBoolean("pref_key_notifications", true)) {
            locationManager.removeUpdates(locationListener);
        } else {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
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
        Intent intent = new Intent(MainActivity.this, GuideActivity.class);
        intent.putExtra("monument_id", monument);
        intent.putExtra("language", language);
        intent.putExtra("user_id", uniqueID);
        startActivity(intent);
    }

}

