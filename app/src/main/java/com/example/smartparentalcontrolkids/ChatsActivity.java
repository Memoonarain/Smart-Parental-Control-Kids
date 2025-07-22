package com.example.smartparentalcontrolkids;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
public class ChatsActivity extends AppCompatActivity {
    String parentId,deviceName;
    private RecyclerView rView;
    private EditText etMessage;
    private MaterialButton btnSend;
    private TextView TopHeading;
    private ImageButton backButton;
    private FirebaseFirestore db;

    private String currentUserId,  chatId;
    private List<MessageModel> messageList;
    private MessageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chats);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        backButton.setOnClickListener(v -> onBackPressed());
        rView = findViewById(R.id.rView);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        TopHeading = findViewById(R.id.TopHeading);
        currentUserId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        db = FirebaseFirestore.getInstance();
        parentId = getIntent().getStringExtra("parentId");
        deviceName = getIntent().getStringExtra("deviceName");
        TopHeading.setText(deviceName);
        chatId = generateChatId(currentUserId, parentId);

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList, currentUserId);
        rView.setLayoutManager(new LinearLayoutManager(this));
        rView.setAdapter(adapter);

        findViewById(R.id.backButton).setOnClickListener(v ->onBackPressed());
        loadMessages();
        btnSend.setOnClickListener(v -> sendMessage());

    }
    private void loadMessages() {
        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;
                    messageList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        messageList.add(doc.toObject(MessageModel.class));
                    }
                    adapter.notifyDataSetChanged();
                    rView.scrollToPosition(messageList.size() - 1);
                });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        MessageModel message = new MessageModel(currentUserId, text, System.currentTimeMillis());

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(doc -> etMessage.setText(""));
    }

    private String generateChatId(String id1, String id2) {
        return id1.compareTo(id2) < 0 ? id1 + "_" + id2 : id2 + "_" + id1;
    }

}