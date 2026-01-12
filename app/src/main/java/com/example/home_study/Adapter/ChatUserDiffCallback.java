package com.example.home_study.Adapter;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.example.home_study.Model.ChatUser;

import java.util.List;
import java.util.Objects;

/**
 * DiffUtil callback for ChatUser list updates.
 * Compares by userId for item identity and returns a Bundle payload for small updates.
 */
public class ChatUserDiffCallback extends DiffUtil.Callback {

    private final List<ChatUser> oldList;
    private final List<ChatUser> newList;

    public ChatUserDiffCallback(List<ChatUser> oldList, List<ChatUser> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList != null ? oldList.size() : 0;
    }

    @Override
    public int getNewListSize() {
        return newList != null ? newList.size() : 0;
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        ChatUser a = oldList.get(oldItemPosition);
        ChatUser b = newList.get(newItemPosition);
        return a.getUserId() != null && a.getUserId().equals(b.getUserId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        ChatUser a = oldList.get(oldItemPosition);
        ChatUser b = newList.get(newItemPosition);

        if (!Objects.equals(a.getName(), b.getName())) return false;
        if (!Objects.equals(a.getProfileImage(), b.getProfileImage())) return false;
        if (!Objects.equals(a.getSubstitle(), b.getSubstitle())) return false;
        if (!Objects.equals(a.getLastMessage(), b.getLastMessage())) return false;
        if (!Objects.equals(a.getLastMessageSenderId(), b.getLastMessageSenderId())) return false;
        if (a.isLastMessageSeen() != b.isLastMessageSeen()) return false;
        if (a.getUnreadCount() != b.getUnreadCount()) return false;
        if (a.getLastMessageTime() != b.getLastMessageTime()) return false;
        if (!Objects.equals(a.getRole(), b.getRole())) return false;
        return true;
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        ChatUser a = oldList.get(oldItemPosition);
        ChatUser b = newList.get(newItemPosition);

        Bundle payload = new Bundle();
        boolean changed = false;

        if (!Objects.equals(a.getLastMessage(), b.getLastMessage())) {
            payload.putString("lastMessage", b.getLastMessage());
            changed = true;
        }
        if (!Objects.equals(a.getLastMessageSenderId(), b.getLastMessageSenderId())) {
            payload.putString("lastSenderId", b.getLastMessageSenderId());
            changed = true;
        }
        if (a.isLastMessageSeen() != b.isLastMessageSeen()) {
            payload.putBoolean("lastSeen", b.isLastMessageSeen());
            changed = true;
        }
        if (a.getUnreadCount() != b.getUnreadCount()) {
            payload.putInt("unread", b.getUnreadCount());
            changed = true;
        }
        if (a.getLastMessageTime() != b.getLastMessageTime()) {
            payload.putLong("time", b.getLastMessageTime());
            changed = true;
        }

        return changed ? payload : null;
    }
}