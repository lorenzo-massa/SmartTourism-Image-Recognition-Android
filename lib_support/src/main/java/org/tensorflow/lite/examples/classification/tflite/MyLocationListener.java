package org.tensorflow.lite.examples.classification.tflite;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

public class MyLocationListener implements LocationListener {

    double latitude;
    double longitude;

    public double[] getCurrentLocation() {
        return new double[] {latitude, longitude};
    }
    @Override
    public void onLocationChanged(Location location) {
        // Handle location updates here
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        // Do something with latitude and longitude values
        Log.d("Location", "[UPDATE] Latitude: " + latitude + " Longitude: " + longitude);
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
