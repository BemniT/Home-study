package com.example.home_study.Model;

import com.google.firebase.Timestamp;

import java.util.List;
import java.util.Map;

public class Chat {

    private List<String> participants;

    private String lastMessage;
    private Timestamp lastTimestamp;

    public Chat(List<String> participants, String lastMessage, Timestamp lastTimestamp) {
        this.participants = participants;
        this.lastMessage = lastMessage;
        this.lastTimestamp = lastTimestamp;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public Timestamp getLastTimestamp() {
        return lastTimestamp;
    }
}
