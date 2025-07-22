package com.example.smartparentalcontrolkids;
public class ChatUserModel {
    private String deviceId;
    private String deviceName;
    private String lastMessage;
    private String lastMessageTime;
    private  String imageUrl;

    public String getImageUrl() {
        return imageUrl;
    }

    public ChatUserModel(String deviceId, String deviceName, String imageUrl, String lastMessage, String lastMessageTime) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.imageUrl = imageUrl;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    public ChatUserModel() {} // Required for Firebase

    public ChatUserModel(String deviceId, String deviceName, String lastMessage, String lastMessageTime) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    public String getDeviceId() { return deviceId; }
    public String getDeviceName() { return deviceName; }
    public String getLastMessage() { return lastMessage; }
    public String getLastMessageTime() { return lastMessageTime; }

    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public void setLastMessageTime(String lastMessageTime) { this.lastMessageTime = lastMessageTime; }
}
