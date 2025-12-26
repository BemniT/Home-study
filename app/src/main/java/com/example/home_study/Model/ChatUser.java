package com.example.home_study.Model;

public class ChatUser {

    private String userId, name, profileImage, substitle, role;

    //this is for the last message logic
    private String lastMessage;
    private long lastMessageTime;
    private boolean lastMessageSeen;
    private String lastMessageSenderId;

    private int unreadCount;

    public ChatUser() {
    }

    public ChatUser(String userId, String name, String profileImage, String substitle, String role) {
        this.userId = userId;
        this.name = name;
        this.profileImage = profileImage;
        this.substitle = substitle;
        this.role = role;
//        this.unreadCount = unreadCount;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public boolean isLastMessageSeen() {
        return lastMessageSeen;
    }

    public void setLastMessageSeen(boolean lastMessageSeen) {
        this.lastMessageSeen = lastMessageSeen;
    }

    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getSubstitle() {
        return substitle;
    }

    public String getRole() {
        return role;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
