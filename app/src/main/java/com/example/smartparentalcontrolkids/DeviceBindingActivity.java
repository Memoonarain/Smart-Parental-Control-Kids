package com.example.smartparentalcontrolkids;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.UUID;

public class DeviceBindingActivity extends AppCompatActivity {

    private static final String TAG = "DeviceBinding";
    String childId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_device_binding);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        SharedPreferences prefs = getSharedPreferences("child_prefs", MODE_PRIVATE);
        childId = prefs.getString("child_uid", null);

        if (childId == null) {
            childId =Settings.Secure.getString(DeviceBindingActivity.this.getContentResolver(), Settings.Secure.ANDROID_ID);
            prefs.edit().putString("child_uid", childId).apply();
        }

        Button nextButton = findViewById(R.id.next);
        EditText edtBindingCode = findViewById(R.id.edt_binding_code);
        nextButton.setOnClickListener(v -> {
            String bindingCode = edtBindingCode.getText().toString().trim();

            if (bindingCode.length() < 9 || !bindingCode.matches("\\d+")) {
                edtBindingCode.setError("Please enter a valid 9-digit code");
                return;
            }

            int bindingCodeInt;
            try {
                bindingCodeInt = Integer.parseInt(bindingCode);
            }
            catch (NumberFormatException e) {
                edtBindingCode.setError("Invalid code format");
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();


            db.collectionGroup("children")
                    .whereEqualTo("code", bindingCodeInt)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);

                            // Extract full path: device_bindings/{parentUid}/children/{childN}
                            String path = doc.getReference().getPath();
                            String[] parts = path.split("/");

                            String parentUid = parts[1]; // /device_bindings/{parentUid}/children/{childN}
                            String childSlot = parts[3];
                            getSharedPreferences("prefs", MODE_PRIVATE)
                                    .edit()
                                    .putString("parentUid", parentUid)
                                    .putString("childSlot", childSlot)
                                    .apply();
                            // Update Firestore with child UID
                            doc.getReference().update(
                                    "childUid", childId,
                                    "boundAt", FieldValue.serverTimestamp()
                            ).addOnSuccessListener(aVoid -> {
                                // Save locally
                                getSharedPreferences("prefs", MODE_PRIVATE)
                                        .edit()
                                        .putString("parentUid", parentUid)
                                        .putString("childSlot", childSlot)
                                        .apply();

                                Toast.makeText(this, "Device successfully bound", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            }).addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to bind device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });

                        } else {
                            Toast.makeText(this, "Invalid or expired binding code", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to fetch binding info", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    });
        });
    }
}
