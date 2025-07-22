package com.example.smartparentalcontrolkids;

import android.Manifest;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.*;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LocationUpdateService extends Service {

    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private LocationCallback locationCallback;
    private String childId;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Step 2: Fallback to SharedPreferences
        if (childId == null) {
            childId=Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        }

        if (childId == null) {
            Log.e("LocationService", "No child ID found. Stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(1, createNotification());
        startLocationUpdates();
        return START_STICKY;
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, "location_channel")
                .setContentTitle("Live Location Tracking")
                .setContentText("Child's location is being shared.")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your actual icon
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "location_channel",
                    "Location Updates",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create()
                .setInterval(100_000) // 10 seconds
                .setFastestInterval(50_000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                Location location = result.getLastLocation();
                if (location != null) {
                    uploadLocation(location);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationService", "Location permission not granted.");
            return;
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    private void uploadLocation(Location location) {
        Map<String, Object> data = new HashMap<>();
        data.put("latitude", location.getLatitude());
        data.put("longitude", location.getLongitude());
        data.put("timestamp", System.currentTimeMillis());

        db.collection("live_locations").document(childId)
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d("LocationService", "Location updated for " + childId))
                .addOnFailureListener(e -> Log.e("LocationService", "Update failed: " + e.getMessage()));
        String timestamp = String.valueOf(System.currentTimeMillis());

        db.collection("location_history")
                .document(childId)
                .collection("history")
                .document(timestamp)
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d("LocationService", "Location saved for " + childId))
                .addOnFailureListener(e -> Log.e("LocationService", "Update failed: " + e.getMessage()));

    }

    @Override
    public void onDestroy() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
