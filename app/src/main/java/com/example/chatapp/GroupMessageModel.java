package com.example.chatapp;

public class GroupMessageModel {

    private String messageId;
    private String senderId;
    private String message;
    private long time;
    private boolean isImage;

    public GroupMessageModel() {
        // Default constructor required for calls to DataSnapshot.getValue(MessageModel.class)
    }

    public GroupMessageModel(String messageId, String senderId, String message, long time, boolean isImage) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.message = message;
        this.time = time;
        this.isImage = isImage;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getMessage() {
        return message;
    }

    public long getTime() {
        return time;
    }

    public boolean isImage() {
        return isImage;
    }


}