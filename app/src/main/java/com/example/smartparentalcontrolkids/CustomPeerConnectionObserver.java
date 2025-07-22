package com.example.smartparentalcontrolkids;
import android.util.Log;
import org.webrtc.*;

public class CustomPeerConnectionObserver implements PeerConnection.Observer {
    private static final String TAG = "PeerConnectionObserver";
    public interface IceCandidateListener {
        void onIceCandidateReceived(IceCandidate candidate);
    }

    private IceCandidateListener iceCandidateListener;

    public CustomPeerConnectionObserver(IceCandidateListener listener) {
        this.iceCandidateListener = listener;
    }


    @Override public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.d(TAG, "onSignalingChange: " + signalingState);
    }
    @Override public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.d(TAG, "onIceConnectionChange: " + iceConnectionState);
    }
    @Override public void onIceConnectionReceivingChange(boolean b) {
        Log.d(TAG, "onIceConnectionReceivingChange: " + b);
    }
    @Override public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.d(TAG, "onIceGatheringChange: " + iceGatheringState);
    }
    @Override public void onIceCandidate(IceCandidate iceCandidate) {
        Log.d(TAG, "onIceCandidate: " + iceCandidate.sdp);
        onIceCandidateReceived(iceCandidate);
    }
    @Override public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {}
    @Override public void onAddStream(MediaStream mediaStream) {
        Log.d(TAG, "onAddStream");
    }
    @Override public void onRemoveStream(MediaStream mediaStream) {}
    @Override public void onDataChannel(DataChannel dataChannel) {}
    @Override public void onRenegotiationNeeded() {}
    @Override public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {}

    // Custom method to override externally
    public void onIceCandidateReceived(IceCandidate iceCandidate) {}
}
