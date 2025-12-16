package com.example.home_study.Model;

import com.google.firebase.Timestamp;

import org.bouncycastle.util.Times;

public class Message {

    private String senderId, receiverId, text;

    private long timeStamp;


    public Message() {
    }

    public Message(String senderId, String receiverId, String text, long timeStamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.timeStamp = timeStamp;

    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getText() {
        return text;
    }

    public long getTimeStamp() {
        return timeStamp;
    }
}
