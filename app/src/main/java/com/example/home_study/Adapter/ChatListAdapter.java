package com.example.home_study.Adapter;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.ChatDialogue;
import com.example.home_study.Model.ChatUser;
import com.example.home_study.Prevalent.Continuity;
import com.example.home_study.R;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    private List<ChatUser> list = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public ChatListAdapter(List<ChatUser> list) {
        this.list = list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    @NonNull
    @Override
    public ChatListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_container_card, parent, false);
        return new ViewHolder(view);
    }

    // Full bind
    @Override
    public void onBindViewHolder(@NonNull ChatListAdapter.ViewHolder holder, int position) {
        ChatUser chatUser = list.get(position);
        bindFull(holder, chatUser);
    }

    // Partial bind using payloads
    // inside onBindViewHolder(holder, position, payloads)
    @Override
    public void onBindViewHolder(@NonNull ChatListAdapter.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            onBindViewHolder(holder, position);
            return;
        }

        Object payload = payloads.get(payloads.size() - 1);
        // get current item safely (position could be NO_POSITION, guard it)
        ChatUser current = null;
        if (position >= 0 && position < list.size()) current = list.get(position);

        if (payload instanceof Bundle) {
            Bundle b = (Bundle) payload;
            if (b.containsKey("lastMessage")) {
                String lm = b.getString("lastMessage");
                holder.lastMessage.setText(lm != null ? lm : holder.lastMessage.getText());
            }
            if (b.containsKey("time")) {
                long ts = b.getLong("time");
                if (ts > 0) {
                    if (ts < 1_000_000_000_000L) ts = ts * 1000L;
                    holder.lastMessageTime.setVisibility(View.VISIBLE);
                    holder.lastMessageTime.setText(formatTime(ts));
                } else {
                    holder.lastMessageTime.setVisibility(View.GONE);
                }
            }
            if (b.containsKey("lastSeen")) {
                boolean seen = b.getBoolean("lastSeen");
                // Only show seen icon if the last message was SENT by the current user
                String senderIdInItem = current != null ? current.getLastMessageSenderId() : null;
                if (senderIdInItem != null && senderIdInItem.equals(Continuity.userId)) {
                    holder.seenIcon.setVisibility(View.VISIBLE);
                    holder.seenIcon.setImageResource(seen ? R.drawable.double_check : R.drawable.single_check);
                    holder.seenIcon.setAlpha(0f);
                    holder.seenIcon.animate().alpha(1f).setDuration(200).start();
                    holder.unreadCounter.setVisibility(View.GONE);
                } else {
                    // If the last message is from someone else, hide seen icon (we show unread badge instead)
                    holder.seenIcon.setVisibility(View.GONE);
                    // If bundle provided unread, handle it below; otherwise leave unread as-is
                }
            }
            if (b.containsKey("unread")) {
                int unread = b.getInt("unread");
                // If unread > 0 and last message was NOT sent by current user, show badge
                String senderIdInItem = current != null ? current.getLastMessageSenderId() : null;
                if (unread > 0 && (senderIdInItem == null || !senderIdInItem.equals(Continuity.userId))) {
                    holder.unreadCounter.setVisibility(View.VISIBLE);
                    holder.unreadCounter.setText(String.valueOf(unread));
                    ObjectAnimator.ofFloat(holder.unreadCounter, "scaleX", 0.8f, 1f).setDuration(160).start();
                    ObjectAnimator.ofFloat(holder.unreadCounter, "scaleY", 0.8f, 1f).setDuration(160).start();
                } else {
                    holder.unreadCounter.setVisibility(View.GONE);
                }
            }
            return;
        }

        // fallback
        onBindViewHolder(holder, position);
    }

    private void bindFull(@NonNull ViewHolder holder, ChatUser chatUser) {
        holder.teacherName.setText(chatUser.getName());

        String url = chatUser.getProfileImage();
        Picasso.get()
                .load(url)
                .placeholder(R.drawable.profile_image)
                .fit()
                .centerCrop()
                .noFade()
                .into(holder.profileImage);

        if (chatUser.getLastMessage() != null) {
            holder.lastMessage.setText(chatUser.getLastMessage());
        } else {
//            holder.lastMessage.setText(chatUser.getSubstitle());
            holder.lastMessage.setText("Start a converstion");

        }

        if (chatUser.getLastMessageTime() > 0) {
            long ts = chatUser.getLastMessageTime();
            if (ts < 1_000_000_000_000L) ts = ts * 1000L;
            holder.lastMessageTime.setVisibility(View.VISIBLE);
            holder.lastMessageTime.setText(formatTime(ts));
        } else {
            holder.lastMessageTime.setVisibility(View.GONE);
        }

        String senderId = chatUser.getLastMessageSenderId();
        if (senderId != null && senderId.equals(Continuity.userId)) {
            holder.seenIcon.setVisibility(View.VISIBLE);
            holder.unreadCounter.setVisibility(View.GONE);
            holder.seenIcon.setImageResource(chatUser.isLastMessageSeen() ? R.drawable.double_check : R.drawable.single_check);
        } else {
            holder.seenIcon.setVisibility(View.GONE);
            if (chatUser.getUnreadCount() > 0) {
                holder.unreadCounter.setVisibility(View.VISIBLE);
                holder.unreadCounter.setText(String.valueOf(chatUser.getUnreadCount()));
            } else {
                holder.unreadCounter.setVisibility(View.GONE);
            }
        }

        // role pill
        holder.userRole.setText(chatUser.getRole() != null ? chatUser.getRole() : "");

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ChatDialogue.class);
            intent.putExtra("otherUserId", chatUser.getUserId());
            intent.putExtra("name", chatUser.getName());
            intent.putExtra("image", chatUser.getProfileImage());
            intent.putExtra("role", chatUser.getRole());
            v.getContext().startActivity(intent);
        });
    }

    private String formatTime(long millis) {
        try {
            Date d = new Date(millis);
            return timeFormat.format(d);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView teacherName, lastMessage, unreadCounter, lastMessageTime, userRole;
        private CircleImageView profileImage;
        private ImageView seenIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            unreadCounter = itemView.findViewById(R.id.badgeReceivedMessage);
            lastMessage = itemView.findViewById(R.id.lastMessage);
            teacherName = itemView.findViewById(R.id.userName);
            profileImage = itemView.findViewById(R.id.imageProfile);
            lastMessageTime = itemView.findViewById(R.id.lastMessageTime);
            seenIcon = itemView.findViewById(R.id.seenIcon);
            userRole = itemView.findViewById(R.id.userRole);
        }
    }

    public void submitList(List<ChatUser> newList) {
        List<ChatUser> safeNew = newList != null ? new ArrayList<>(newList) : new ArrayList<>();
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(
                new ChatUserDiffCallback(this.list, safeNew)
        );
        this.list = safeNew;
        diff.dispatchUpdatesTo(this);
    }
}