package com.example.meshup;

public class Message {
    private String senderName;
    private String macAddress;
    private String ipAddress;
    private String content;
    private long timestamp;
    private String messageId;
    private boolean isDelivered;

    public Message(String senderName, String macAddress, String ipAddress,
                   String content, String messageId) {
        this.senderName = senderName;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.messageId = messageId;
        this.isDelivered = false;
    }

    // Getters and setters
    public String getSenderName() { return senderName; }
    public String getMacAddress() { return macAddress; }
    public String getIpAddress() { return ipAddress; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
    public String getMessageId() { return messageId; }
    public boolean isDelivered() { return isDelivered; }
    public void setDelivered(boolean delivered) { isDelivered = delivered; }
}
