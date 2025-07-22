package com.example.smartparentalcontrolkids;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppUsageUploadService extends Service {

    private Handler handler = new Handler();
    private Runnable runnable;
    private long interval = 10 * 60 * 1000; // 10 minutes

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, buildNotification("Uploading App Usage"));

        runnable = new Runnable() {
            @Override
            public void run() {
                uploadAppUsageToFirebase();
                handler.postDelayed(this, interval);
            }
        };
        handler.post(runnable);

        return START_STICKY;
    }

    private Notification buildNotification(String title) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("usage_upload", "Usage Upload", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, "usage_upload")
                .setContentTitle(title)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }

    private void uploadAppUsageToFirebase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        // 1. Installed Apps
        List<Map<String, Object>> installedApps = new ArrayList<>();
        for (ApplicationInfo app : apps) {
                Map<String, Object> appMap = new HashMap<>();
                appMap.put("appName", pm.getApplicationLabel(app).toString());
                appMap.put("package", app.packageName);
                installedApps.add(appMap);

        }
        db.collection("child_devices").document(androidId).collection("AppMeta")
                .document("InstalledApps").set(Collections.singletonMap("apps", installedApps));

        // 2. Usage Stats
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long end = System.currentTimeMillis();
        long start = end - (24 * 60 * 60 * 1000);

        List<UsageStats> statsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end);
        List<Map<String, Object>> usageData = new ArrayList<>();

        for (UsageStats usage : statsList) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(usage.getPackageName(), 0);
                    long foregroundTime = usage.getTotalTimeInForeground();
                    if (foregroundTime > 0) {
                        Map<String, Object> appMap = new HashMap<>();
                        appMap.put("appName", pm.getApplicationLabel(appInfo).toString());
                        appMap.put("package", usage.getPackageName());
                        appMap.put("foregroundTime", foregroundTime);

                        usageData.add(appMap);
                    }

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        db.collection("child_devices").document(androidId).collection("AppMeta")
                .document("UsageStats").set(Collections.singletonMap("usage", usageData));

        // 3. Usage Sessions
        UsageEvents usageEvents = usageStatsManager.queryEvents(start, end);
        UsageEvents.Event event = new UsageEvents.Event();
        Map<String, Long> lastForegroundTime = new HashMap<>();
        List<Map<String, Object>> sessionData = new ArrayList<>();

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            String pkg = event.getPackageName();

            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastForegroundTime.put(pkg, event.getTimeStamp());
            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND && lastForegroundTime.containsKey(pkg)) {
                long startTime = lastForegroundTime.get(pkg);
                long endTime = event.getTimeStamp();
                long duration = endTime - startTime;

                try {
                    ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
                        Map<String, Object> sessionMap = new HashMap<>();
                        sessionMap.put("appName", pm.getApplicationLabel(appInfo).toString());
                        sessionMap.put("package", pkg);
                        sessionMap.put("startTime", startTime);
                        sessionMap.put("endTime", endTime);
                        sessionMap.put("duration", duration);
                        sessionData.add(sessionMap);

                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        db.collection("child_devices").document(androidId).collection("AppMeta")
                .document("UsageSessions").set(Collections.singletonMap("sessions", sessionData));

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
