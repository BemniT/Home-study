package com.example.home_study.Model;

import com.google.firebase.Timestamp;

import java.util.Map;

public class Chat {

    public String studentId, teacherId, lastMessage;

    public Timestamp lastTimestamp;
    public Map<String, Boolean> participant;

    public Chat() {
    }
}
