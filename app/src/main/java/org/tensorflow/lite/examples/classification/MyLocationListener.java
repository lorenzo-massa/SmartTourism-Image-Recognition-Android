package org.tensorflow.lite.examples.classification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.tensorflow.lite.examples.classification.tflite.DatabaseAccess;

import java.util.Random;

public class MyLocationListener implements LocationListener {

    double latitude;
    double longitude;
    private final String TAG = "MyLocationListener";
    private long lastNotificationTime = System.currentTimeMillis();
    private String lastMonument = null;


    private final Context contex;

    public MyLocationListener(Context context) {
        this.contex = context;
    }

    public double[] getCurrentLocation() {
        return new double[]{latitude, longitude};
    }

    @Override
    public void onLocationChanged(Location location) {
        // Handle location updates here
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        // Do something with latitude and longitude values
        Log.d("Location", "[UPDATE] Latitude: " + latitude + " Longitude: " + longitude);
        sendNotification();
    }

    private void sendNotification() {
        if (DatabaseAccess.getSharedPreferences().getBoolean("pref_key_notifications", false) //Check if notifications are enabled
                && System.currentTimeMillis() - lastNotificationTime >= MainActivity.NOTIFICATION_TIME
        ) {
            // Define the target location
            String nearestMonument = DatabaseAccess.getNearestMonument(latitude, longitude, MainActivity.MAX_DISTANCE);
            Log.d(TAG, "nearestMonument: " + nearestMonument);

            // Check proximity and send notification if within range and enough time has passed
            // Check if the user has already been notified about this monument
            if (nearestMonument != null && (!nearestMonument.equals(lastMonument) || // Check if the user has already been notified about this monument
                    System.currentTimeMillis() - lastNotificationTime >= 3 * MainActivity.NOTIFICATION_TIME) // or if enough time has passed since the last notification
            ) {
                // Update the last notification time
                lastNotificationTime = System.currentTimeMillis();
                lastMonument = nearestMonument;

                // Create an explicit intent for the app's main activity
                Intent intent = new Intent(contex, GuideActivity.class);
                intent.putExtra("monument_id", nearestMonument);
                intent.putExtra("language", MainActivity.language);
                intent.putExtra("user_id", MainActivity.uniqueID);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(contex, 0, intent, PendingIntent.FLAG_IMMUTABLE);

                // Create and send notification
                NotificationCompat.Builder builder = new NotificationCompat.Builder(contex, "channel_id")
                        .setSmallIcon(R.drawable.done)
                        .setContentTitle("Nearby Monument")
                        .setContentText("You are near " + nearestMonument + "! Click here to learn more.")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setContentIntent(pendingIntent) // Set the pending intent
                        .setAutoCancel(true) // Dismiss the notification when the user taps on it
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(contex);
                NotificationChannel channel = new NotificationChannel("channel_id", "channel_name", NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);

                if (ActivityCompat.checkSelfPermission(contex, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    int notificationId = new Random().nextInt();
                    notificationManager.notify(notificationId, builder.build());
                    Log.d(TAG, "Notification sent: " + notificationId);
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
}
