package com.example.home_study.Model;

public class Message {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String text;
    private long timeStamp;
    private boolean seen;
    private boolean deleted;
    private boolean edited;
    private String imageUrl;
    private String type; // "text" or "image" (or other types)

    public Message() { }

    public Message(String senderId, String receiverId, String text, long timeStamp,
                   boolean seen, boolean deleted, boolean edited) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.timeStamp = timeStamp;
        this.seen = seen;
        this.deleted = deleted;
        this.edited = edited;
        this.type = "text";
    }

    // getters & setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimeStamp() { return timeStamp; }
    public void setTimeStamp(long timeStamp) { this.timeStamp = timeStamp; }

    public boolean isSeen() { return seen; }
    public void setSeen(boolean seen) { this.seen = seen; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public boolean isEdited() { return edited; }
    public void setEdited(boolean edited) { this.edited = edited; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}