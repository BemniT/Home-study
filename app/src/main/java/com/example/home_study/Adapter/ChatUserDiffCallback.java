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
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldPos, int newPos) {
        return oldList.get(oldPos).getUserId()
                .equals(newList.get(newPos).getUserId());
    }

    @Override
    public boolean areContentsTheSame(int oldPos, int newPos) {
        ChatUser o = oldList.get(oldPos);
        ChatUser n = newList.get(newPos);

        return o.getLastMessageTime() == n.getLastMessageTime()
                && o.getUnreadCount() == n.getUnreadCount()
                && o.isLastMessageSeen() == n.isLastMessageSeen();
    }
}