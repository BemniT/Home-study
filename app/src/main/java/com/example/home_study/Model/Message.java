package com.example.home_study.Model;

import com.google.firebase.Timestamp;

import org.bouncycastle.util.Times;

public class Message {

    private String senderId, receiverId, text;

    private Timestamp timeStamp;
    private boolean read;

    public Message() {
    }

    public Message(String senderId, String receiverId, String text, Timestamp timeStamp, boolean read) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.timeStamp = timeStamp;
        this.read = read;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Timestamp getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Timestamp timeStamp) {
        this.timeStamp = timeStamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
