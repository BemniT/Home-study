package com.example.home_study.Model;

public class ChatUser {

    private String userId, name, profileImage, substitle, role;
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
