package com.example.smartparentalcontrolkids;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.*;
import org.webrtc.*;
import java.util.*;

public class ChildCameraActivity extends AppCompatActivity {
    private boolean isStreaming = false;
    private SurfaceViewRenderer mSurfaceView;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private VideoCapturer videoCapturer;
    private VideoTrack localVideoTrack;
    private EglBase eglBase;
    private final String TAG = "ChildCameraStream";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_child_camera);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mSurfaceView = findViewById(R.id.surface_view);
        String childId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String parentUid = getSharedPreferences("prefs", MODE_PRIVATE).getString("parentUid", null);
        listenForRequest(childId, parentUid);
    }
    void listenForRequest(String childId, String parentUid) {
        FirebaseFirestore.getInstance()
                .collection("camera_stream_requests")
                .document(childId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists() && !isStreaming) {
                        String to = snapshot.getString("to");
                        String from = snapshot.getString("from");
                        if (to.equals(childId) && from.equals(parentUid)) {
                            isStreaming = true;
                            // Clear the request to prevent retriggering
                            FirebaseFirestore.getInstance()
                                    .collection("camera_stream_requests")
                                    .document(childId)
                                    .delete();
                            startWebRTCStreaming(parentUid, childId);
                        }
                    }
                });
    }
    void startWebRTCStreaming(String parentUid, String childUid) {
        eglBase = EglBase.create();
        mSurfaceView.init(eglBase.getEglBaseContext(), null);
        mSurfaceView.setMirror(true);
        if (peerConnectionFactory == null){
       PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this)
                .createInitializationOptions());

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .createPeerConnectionFactory();
        }

        startLocalVideo(childUid, parentUid);

        Log.w("Testing Camera", "WebRTC streaming started");
    }
    // In ChildCameraActivity:
    private void startLocalVideo(String childUid, String parentUid) {
        try {
            videoCapturer = createCameraCapturer();
            if (videoCapturer == null) {
                Log.e(TAG, "Failed to create camera capturer");
                return;
            }

            VideoSource videoSource = peerConnectionFactory.createVideoSource(false);
            SurfaceTextureHelper surfaceTextureHelper =
                    SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());

            videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(),
                    videoSource.getCapturerObserver());
            videoCapturer.startCapture(640, 480, 30);

            localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);
            runOnUiThread(() -> {
                localVideoTrack.addSink(mSurfaceView);
                mSurfaceView.setVisibility(View.VISIBLE);
            });

            createConnection(childUid, parentUid);
        } catch (Exception e) {
            Log.e(TAG, "Error starting local video: " + e.getMessage());
        }
    }
    private VideoCapturer createCameraCapturer() {
        Camera2Enumerator enumerator = new Camera2Enumerator(this);
        for (String deviceName : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(deviceName)) {
                Log.w("Testing Camera", "Front camera found: " + deviceName);
                return enumerator.createCapturer(deviceName, null);

            }
        }
        Log.e("Testing Camera", "No front camera found");
        return null;
    }
    private void createConnection(String childUid, String parentUid) {
        List<PeerConnection.IceServer> iceServers = Collections.singletonList(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        );
        PeerConnection.RTCConfiguration config = new PeerConnection.RTCConfiguration(iceServers);
        peerConnection = peerConnectionFactory.createPeerConnection(config, new PeerConnection.Observer() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.w("Testing Camera", "onIceCandidate: " + iceCandidate.sdp);
                sendCandidateToFirestore(childUid, parentUid, iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                // Not used in Unified Plan
            }

            // ADD THIS FOR UNIFIED PLAN
            @Override
            public void onTrack(RtpTransceiver transceiver) {
                if (transceiver.getReceiver().track() instanceof VideoTrack) {
                    VideoTrack remoteVideoTrack = (VideoTrack) transceiver.getReceiver().track();
                    Log.w("Testing Camera", "onTrack: " + remoteVideoTrack.id());
                    runOnUiThread(() -> remoteVideoTrack.addSink(mSurfaceView));
                }
            }

            // Implement all other required methods with empty bodies
            @Override public void onSignalingChange(PeerConnection.SignalingState state) {

                Log.d(TAG, "Signaling state changed to: " + state);
            };
            @Override public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
                Log.d(TAG, "ICE connection state changed to: " + state);
                if (state == PeerConnection.IceConnectionState.CONNECTED) {
                    Toast.makeText(ChildCameraActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onIceConnectionReceivingChange(boolean receiving) {}
            @Override public void onIceGatheringChange(PeerConnection.IceGatheringState state) {}
            @Override public void onIceCandidatesRemoved(IceCandidate[] candidates) {}
            @Override public void onRemoveStream(MediaStream stream) {}
            @Override public void onDataChannel(DataChannel dataChannel) {}
            @Override public void onRenegotiationNeeded() {}
            @Override public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {}
        });

        // Add track with transceiver for Unified Plan
        RtpTransceiver transceiver = peerConnection.addTransceiver(localVideoTrack,
                new RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY));

        peerConnection.createOffer(new SdpObserverAdapter() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SdpObserverAdapter(), sessionDescription);
                Log.w("Testing Camera", " settled local description to: " + sessionDescription.description);
                Map<String, Object> offer = new HashMap<>();
                offer.put("sdp", sessionDescription.description);
                offer.put("type", sessionDescription.type.canonicalForm());
                offer.put("from", childUid);
                offer.put("to", parentUid);

                // Initialize ICE candidates collection
                FirebaseFirestore.getInstance()
                        .collection("camera_signaling")
                        .document(childUid + "_" + parentUid)
                        .collection("ice_candidates")
                        .document("init")
                        .set(new HashMap<>()).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.w("Testing Camera", "ICE candidates collection initialized successfully.");
                            }
                        });
                FirebaseFirestore.getInstance()
                        .collection("camera_signaling")
                        .document(childUid + "_" + parentUid)
                        .set(offer).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.w("Testing Camera", "Offer sent successfully.");
                            }
                        });
            }
        }, new MediaConstraints());

        listenForAnswer(childUid, parentUid);
        listenForRemoteIceCandidates(childUid, parentUid);
    }


    private Map<String, Object> toMap(IceCandidate candidate) {
        Map<String, Object> map = new HashMap<>();
        map.put("sdp", candidate.sdp);
        map.put("sdpMid", candidate.sdpMid);
        map.put("sdpMLineIndex", candidate.sdpMLineIndex);
        return map;
    }
    private void listenForAnswer(String childUid, String parentUid) {
        FirebaseFirestore.getInstance()
                .collection("camera_signaling")
                .document(childUid + "_" + parentUid)
                .collection("answer")
                .limit(1)  // Only listen to the first answer
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        DocumentSnapshot doc = snapshots.getDocuments().get(0);
                        if (doc.exists() && "answer".equals(doc.getString("type"))) {
                            String sdp = doc.getString("sdp");
                            SessionDescription answer = new SessionDescription(
                                    SessionDescription.Type.ANSWER, sdp);

                            // Only set if in correct state
                            if (peerConnection.signalingState() == PeerConnection.SignalingState.HAVE_LOCAL_OFFER) {
                                peerConnection.setRemoteDescription(new SdpObserverAdapter() {
                                    @Override
                                    public void onSetSuccess() {
                                        Log.w(TAG, "Remote ANSWER set successfully");
                                    }
                                    @Override
                                    public void onSetFailure(String error) {
                                        Log.e(TAG, "Failed to set answer: " + error);
                                    }
                                }, answer);
                            }
                        }
                    }
                });
    }

    private void listenForRemoteIceCandidates(String childUid, String parentUid) {
        FirebaseFirestore.getInstance()
                .collection("camera_signaling")
                .document(childUid + "_" + parentUid)
                .collection("ice_candidates")
                .document("parent")
                .collection("candidates")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (querySnapshot != null) {
                        Log.w("Testing Camera", "Remote ICE candidates received");
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            IceCandidate candidate = new IceCandidate(
                                    doc.getString("sdpMid"),
                                    Objects.requireNonNull(doc.getLong("sdpMLineIndex")).intValue(),
                                    doc.getString("sdp")
                            );
                            Log.w("Testing Camera", "Remote ICE candidate: " + candidate.sdp);
                            peerConnection.addIceCandidate(candidate);
                        }
                    }
                });
    }
    private void sendCandidateToFirestore(String childUid, String parentUid, IceCandidate candidate) {
        Map<String, Object> map = new HashMap<>();
        map.put("sdp", candidate.sdp);
        map.put("sdpMid", candidate.sdpMid);
        map.put("sdpMLineIndex", candidate.sdpMLineIndex);

        FirebaseFirestore.getInstance()
                .collection("camera_signaling")
                .document(childUid + "_" + parentUid)
                .collection("ice_candidates")
                .document("child")
                .collection("candidates")
                .add(map).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.w("Testing Camera", "ICE candidate sent successfully.");
                    }
                });

    }
    // In both activities:
    private void startConnectionTimer() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (peerConnection != null &&
                    peerConnection.iceConnectionState() != PeerConnection.IceConnectionState.CONNECTED) {
                Log.e(TAG, "Connection timed out");
                runOnUiThread(() ->
                        Toast.makeText(this, "Connection timed out", Toast.LENGTH_SHORT).show());
                // Optionally restart the connection
            }
        }, 15000); // 15 seconds timeout
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isStreaming = false;
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            videoCapturer.dispose();
        }
        if (mSurfaceView != null) {
            mSurfaceView.release();
        }
        if (peerConnection != null) {
            peerConnection.close();
        }
        if (eglBase != null) {
            eglBase.release();
        }
    }

}