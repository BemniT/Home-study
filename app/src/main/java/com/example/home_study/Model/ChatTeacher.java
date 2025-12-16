package com.example.home_study.Model;

public class ChatTeacher {

    private String teacherId;
    private String userId;
    private String name;

    public ChatTeacher() {
    }

    public ChatTeacher(String teacherId, String userId, String name) {
        this.teacherId = teacherId;
        this.userId = userId;
        this.name = name;
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

