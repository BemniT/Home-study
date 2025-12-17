package com.example.home_study.Model;

import com.google.firebase.Timestamp;

import org.bouncycastle.util.Times;

public class Message {

    private String senderId, receiverId, text, messageId;

    private long timeStamp;
    private boolean seen;

    private boolean deleted, edited;



    public Message() {
    }

    public Message(String senderId, String receiverId, String text, long timeStamp, boolean seen, boolean deleted, boolean edited) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.timeStamp = timeStamp;
        this.seen = seen;
        this.deleted = deleted;
        this.edited = edited;

    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
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

    public boolean isSeen() {
        return seen;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isEdited() {
        return edited;
    }
}
