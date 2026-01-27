// ChatUserDiffCallback.java
package com.example.home_study.Adapter;

import androidx.annotation.NonNull;
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

    @Override public int getOldListSize() { return oldList != null ? oldList.size() : 0; }
    @Override public int getNewListSize() { return newList != null ? newList.size() : 0; }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        ChatUser a = oldList.get(oldItemPosition);
        ChatUser b = newList.get(newItemPosition);
        // identify by userId
        return a.getUserId() != null && a.getUserId().equals(b.getUserId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        ChatUser a = oldList.get(oldItemPosition);
        ChatUser b = newList.get(newItemPosition);

        // Compare relevant fields — if any differ, contents changed -> update UI
        if (safeNeq(a.getName(), b.getName())) return false;
        if (safeNeq(a.getProfileImage(), b.getProfileImage())) return false;
        if (safeNeq(a.getLastMessage(), b.getLastMessage())) return false;
        if (a.getLastMessageTime() != b.getLastMessageTime()) return false;
        if (a.isLastMessageSeen() != b.isLastMessageSeen()) return false;
        if (a.getUnreadCount() != b.getUnreadCount()) return false;
        if (safeNeq(a.getRole(), b.getRole())) return false;
        if (safeNeq(a.getSubstitle(), b.getSubstitle())) return false;

        return true;
    }

    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        ChatUser a = oldList.get(oldItemPosition);
        ChatUser b = newList.get(newItemPosition);
        android.os.Bundle diff = new android.os.Bundle();
        if (safeNeq(a.getLastMessage(), b.getLastMessage())) diff.putString("lastMessage", b.getLastMessage());
        if (a.getLastMessageTime() != b.getLastMessageTime()) diff.putLong("time", b.getLastMessageTime());
        if (a.isLastMessageSeen() != b.isLastMessageSeen()) diff.putBoolean("lastSeen", b.isLastMessageSeen());
        if (a.getUnreadCount() != b.getUnreadCount()) diff.putInt("unread", b.getUnreadCount());
        return diff;
    }

    private boolean safeNeq(String a, String b) {
        if (a == null && b == null) return false;
        if (a == null || b == null) return true;
        return !a.equals(b);
    }
}