package org.tensorflow.lite.examples.classification.tflite;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.opencv.android.CameraActivity;

public class MyLocationListener implements LocationListener {

    double latitude;
    double longitude;

    Context contex;

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
        // Create and send notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(contex, "channel_id")
                .setSmallIcon(0)
                .setContentTitle("Nearby Location")
                .setContentText("Latitude: " + latitude + " Longitude: " + longitude)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(contex);

        if (ActivityCompat.checkSelfPermission(contex, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1, builder.build());
        }else{
            Log.d("Location", "No permission to send notification");
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Handle provider disabled
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Handle provider enabled
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Handle status changed
    }
}
