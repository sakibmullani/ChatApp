package com.example.chatapp;

public class MessageModel {

    String msgid, senderId, message;
    long time;
    String imageUrl;
    private String senderName, receiverId, receiverName;
    private boolean isImage;


    public MessageModel(String msgid, String senderId, String message, long time) {
        this.msgid = msgid;
        this.senderId = senderId;
        this.message = message;
        this.time = time;
    }

    public MessageModel() {

    }

    public MessageModel(String msgid, String senderId, String message, long time, String senderName) {
        this.msgid = msgid;
        this.senderId = senderId;
        this.message = message;
        this.time = time;
        this.senderName = senderName;

    }

    public MessageModel(String msgid, String senderId, String receiverId, String receiverName, String message, long time, boolean isImage) {
        this.msgid = msgid;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.message = message;
        this.time = time;
        this.isImage=isImage;
    }

    public String getMsgid() {
        return msgid;
    }

    public void setMsgid(String msgid) {
        this.msgid = msgid;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isImage() {
        return isImage;
    }

    public void setImage(boolean image) {
        isImage = image;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }
}
