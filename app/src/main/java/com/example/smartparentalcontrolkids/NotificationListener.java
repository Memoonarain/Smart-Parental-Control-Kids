package com.example.smartparentalcontrolkids;

import android.app.Notification;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class NotificationListener extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();

        if (notification == null) return;

        String packageName = sbn.getPackageName();
        CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        long timestamp = sbn.getPostTime();

        // Optional: Convert package name to app label
        String appName = getAppNameFromPackage(packageName);

        // Upload to Firestore
        Map<String, Object> notifData = new HashMap<>();
        notifData.put("appName", appName);
        notifData.put("title", title != null ? title.toString() : "No Title");
        notifData.put("text", text != null ? text.toString() : "No Text");
        notifData.put("package", packageName);
        notifData.put("timestamp", timestamp);

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        FirebaseFirestore.getInstance()
                .collection("child_devices")
                .document(deviceId)
                .collection("notifications")
                .add(notifData)
                .addOnSuccessListener(documentReference ->
                        Log.d("NotificationListener", "Notification logged"))
                .addOnFailureListener(e ->
                        Log.e("NotificationListener", "Failed to log notification", e));
    }

    private String getAppNameFromPackage(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (Exception e) {
            return packageName;
        }
    }
}
