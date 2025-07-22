package com.example.smartparentalcontrolkids;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class DeviceInfoService extends Service {

    private Handler handler;
    private Runnable infoUploader;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundNotification();
        startRepeatingUpload();
    }

    private void startRepeatingUpload() {
        handler = new Handler(Looper.getMainLooper());
        infoUploader = new Runnable() {
            @Override
            public void run() {
                uploadDeviceInfo();
                handler.postDelayed(this, 10 * 60 * 1000); // 10 minutes
            }
        };
        handler.post(infoUploader);
    }

    private void uploadDeviceInfo() {
        Context context = getApplicationContext();
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("brand", Build.BRAND);
        deviceInfo.put("model", Build.MODEL);
        deviceInfo.put("manufacturer", Build.MANUFACTURER);
        deviceInfo.put("android_version", Build.VERSION.RELEASE);
        deviceInfo.put("battery", getBatteryLevel(context));
        deviceInfo.put("network", getNetworkType(context));
        deviceInfo.put("wifi_name", getWifiSSID(context));
        deviceInfo.put("timestamp", FieldValue.serverTimestamp());
        deviceInfo.put("childId", androidId);
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        String parentUid = prefs.getString("parentUid", null);
                        // Step 3: Upload device info to a subdocument named after childUid
                        db.collection("device_bindings")
                                .document(parentUid)
                                .collection("devices") // optional: use a subcollection like "devices"
                                .document(androidId) // use child ID as document name
                                .set(deviceInfo)
                                .addOnSuccessListener(unused -> {
                                    Log.d("ChildDevice", "Device info uploaded under parent " + parentUid);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("ChildDevice", "Upload failed", e);
                                });


    }

    private int getBatteryLevel(Context context) {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);
        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
        return (int) ((level / (float) scale) * 100);
    }

    private String getNetworkType(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm != null ? cm.getActiveNetworkInfo() : null;
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) return "WiFi";
            else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) return "Mobile";
        }
        return "Unknown";
    }

    private String getWifiSSID(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager != null ? wifiManager.getConnectionInfo() : null;
        return (info != null && info.getSSID() != null) ? info.getSSID() : "N/A";
    }

    private void startForegroundNotification() {
        String channelId = "DeviceInfoChannel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Device Info Service", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Child Device Monitoring")
                .setContentText("Uploading device info every 10 minutes")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        startForeground(1002, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (handler != null && infoUploader != null) {
            handler.removeCallbacks(infoUploader);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
