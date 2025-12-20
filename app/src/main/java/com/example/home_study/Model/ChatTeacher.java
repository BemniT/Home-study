package com.example.home_study.Model;

public class ChatTeacher {

    private String teacherId;
    private String userId;
    private String name, profileImage;

    public ChatTeacher() {
    }

    public ChatTeacher(String teacherId, String userId, String name, String profileImage) {
        this.teacherId = teacherId;
        this.userId = userId;
        this.name = name;
        this.profileImage = profileImage;
    }

    public String getProfileImage() {
        return profileImage;
    }


    public String getTeacherId() {
        return teacherId;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }
}

