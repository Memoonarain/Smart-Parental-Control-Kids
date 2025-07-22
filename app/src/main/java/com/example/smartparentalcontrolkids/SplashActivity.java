package com.example.smartparentalcontrolkids;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                String childId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                db.collectionGroup("children")
                        .whereEqualTo("childUid", childId)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);

                                // Get parent UID from the path: /device_bindings/{parentUid}/children/{childN}
                                String fullPath = doc.getReference().getPath();
                                String[] parts = fullPath.split("/");

                                String parentUid = parts[1]; // Index 1 = parentUid (0=device_bindings, 1=parentUid, etc.)
                                String childSlot = parts[3]; // Index 3 = childN
                                getSharedPreferences("prefs", MODE_PRIVATE)
                                        .edit()
                                        .putString("parentUid", parentUid)
                                        .putString("childSlot", childSlot)
                                        .apply();

                                Log.d("ParentUid", "Parent UID is: " + parentUid);
                                Log.d("ChildSlot", "Child slot is: " + childSlot);
                                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                                finish();
                                // You can now save this info locally
                            } else {
                                Log.d("ParentUid", "Binding not found");
                                startActivity(new Intent(SplashActivity.this, DeviceBindingActivity.class));
                                Toast.makeText(SplashActivity.this, "Binding not found", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        })
                        .addOnFailureListener(e ->{
                            Log.e("ParentUid", "Failed to fetch parent info", e);
                                    Toast.makeText(SplashActivity.this, "Failed to fetch parent info", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(SplashActivity.this, DeviceBindingActivity.class));
                                });
            }
        }, 1000);


    }
}