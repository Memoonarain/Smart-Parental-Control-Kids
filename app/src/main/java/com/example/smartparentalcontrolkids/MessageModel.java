package com.example.smartparentalcontrolkids;
public class MessageModel {
    private String senderId;
    private String text;
    private long timestamp;

    public MessageModel() {}

    public MessageModel(String senderId, String text, long timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getSenderId() { return senderId; }
    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }
}
