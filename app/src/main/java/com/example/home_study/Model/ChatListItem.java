package com.example.home_study.Model;

public class ChatListItem {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_USER = 1;


    private ChatUser chatUser;
    private String headerTitle;

    private int type;

    public ChatListItem(String headerTitle) {
        this.headerTitle = headerTitle;
        this.type = TYPE_HEADER;
    }

    public ChatListItem(ChatUser chatUser) {
        this.type = TYPE_USER;
        this.chatUser = chatUser;
    }

    public ChatUser getChatUser() {
        return chatUser;
    }

    public String getHeaderTitle() {
        return headerTitle;
    }

    public int getType() {
        return type;
    }
}

