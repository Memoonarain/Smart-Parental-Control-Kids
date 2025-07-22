package com.example.smartparentalcontrolkids;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    String childId;
    String parentUid;
    private boolean hasNavigatedUsageAccess = false;
    private boolean hasNavigatedBatterySettings = false;
    private boolean hasNavigatedOverlay = false;
    String childSlot;
    List<String> missingPermissions;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        childId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        parentUid = getSharedPreferences("prefs", MODE_PRIVATE).getString("parentUid", null);
        childSlot = getSharedPreferences("prefs", MODE_PRIVATE).getString("childSlot", null);

        findViewById(R.id.testServiceButton).setOnClickListener(v -> {
            startActivity(new Intent(this, ChildCameraActivity.class));
        });

        checkAllPermissions();
    }







    void startServices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, AppUsageUploadService.class));
            startForegroundService(new Intent(this, DeviceInfoService.class));
            startForegroundService(new Intent(this, LocationUpdateService.class));
        } else {
            startService(new Intent(this, DeviceInfoService.class));
            startService(new Intent(this, AppUsageUploadService.class));
            startService(new Intent(this, LocationUpdateService.class));
        }

    }

    void checkAllPermissions() {
        missingPermissions = new ArrayList<>();
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (!missingPermissions.isEmpty()) {
            // Request only once
            ActivityCompat.requestPermissions(this,
                    missingPermissions.toArray(new String[0]),
                    REQUEST_CODE_PERMISSIONS);
            return;
        }

        if (!hasUsageStatsPermission()) {
            if (!hasNavigatedUsageAccess) {
                hasNavigatedUsageAccess = true;
                Toast.makeText(this, "Grant Usage Access permission", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            }
            return;
        }

        if (!isIgnoringBatteryOptimizations()) {
            if (!hasNavigatedBatterySettings) {
                hasNavigatedBatterySettings = true;
                Toast.makeText(this, "Allow battery optimization ignore", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            }
            return;
        }

        if (!Settings.canDrawOverlays(this)) {
            if (!hasNavigatedOverlay) {
                hasNavigatedOverlay = true;
                Toast.makeText(this, "Allow Draw Over Other Apps", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
            return;
        }

        Log.d("PermissionDebug", "All permissions OK");
        Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
        startServices();
    }
    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
    private boolean isIgnoringBatteryOptimizations() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
                startServices();
            } else {
                Log.d("PermissionDebug", "Missing permissions: " + missingPermissions.toString());

                Log.d("PermissionDebug", "Usage access: " + hasUsageStatsPermission());
                Log.d("PermissionDebug", "Battery optim ignore: " + isIgnoringBatteryOptimizations());
                Log.d("PermissionDebug", "Draw overlay: " + Settings.canDrawOverlays(this));
                //Log.d("PermissionDebug", "Notification access: " + hasNotificationAccess());
                Toast.makeText(this, "Please grant all permissions to continue", Toast.LENGTH_LONG).show();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        // Small delay to avoid firing instantly after permission screen closes
        new Handler(Looper.getMainLooper()).postDelayed(this::checkAllPermissions, 500);
    }

}