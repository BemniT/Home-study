package com.example.home_study.Adapter;
import androidx.recyclerview.widget.DiffUtil;

import com.example.home_study.Model.ChatUser;

import java.util.List;

public class ChatUserDiffCallback extends DiffUtil.Callback {

    private final List<ChatUser> oldList;
    private final List<ChatUser> newList;

    public ChatUserDiffCallback(List<ChatUser> oldList, List<ChatUser> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList == null ? 0 : oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList == null ? 0 : newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldPos, int newPos) {
        return oldList.get(oldPos).getUserId()
                .equals(newList.get(newPos).getUserId());
    }

    @Override
    public boolean areContentsTheSame(int oldPos, int newPos) {
        ChatUser old = oldList.get(oldPos);
        ChatUser neu = newList.get(newPos);

        return old.getUnreadCount() == neu.getUnreadCount()
                && old.isLastMessageSeen() == neu.isLastMessageSeen()
                && old.getLastMessageTime() == neu.getLastMessageTime()
                && safe(old.getLastMessage()).equals(safe(neu.getLastMessage()));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}