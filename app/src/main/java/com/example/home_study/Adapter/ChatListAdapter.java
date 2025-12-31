package com.example.home_study.Adapter;

import android.content.Intent;
import android.text.format.DateUtils;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    private List<ChatUser> list = new ArrayList<>();

    public ChatListAdapter(List<ChatUser> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ChatListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_container_card, parent,false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatListAdapter.ViewHolder holder, int position) {
        ChatUser chatUser = list.get(position);
        holder.teacherName.setText(chatUser.getName());
        Picasso.get().load(chatUser.getProfileImage()).placeholder(R.drawable.profile_image).into(holder.profileImage);

        

        if (chatUser.getLastMessage() != null){
            holder.lastMessage.setText(chatUser.getLastMessage());


        }else {
            holder.lastMessage.setText(chatUser.getSubstitle());

        }

        if (chatUser.getLastMessageTime() > 0 )
        {
            String test = String.valueOf(chatUser.getLastMessageTime());
            Log.e("time", test);

            holder.lastMessageTime.setVisibility(View.VISIBLE);
            holder.lastMessageTime.setText(
                    DateUtils.getRelativeTimeSpanString(
                            chatUser.getLastMessageTime(),
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                    )

            );
        }
        //case 1: if the last messgae sent by this current user

        String senderId = chatUser.getLastMessageSenderId();
        if (senderId!= null && senderId.equals(Continuity.userId)){
            holder.seenIcon.setVisibility(View.VISIBLE);
            holder.unreadCounter.setVisibility(View.GONE);

            holder.seenIcon.setImageResource(chatUser.isLastMessageSeen() ? R.drawable.double_check : R.drawable.single_check);
        } else {

            holder.seenIcon.setVisibility(View.GONE);
            if (chatUser.getUnreadCount() > 0){
                holder.unreadCounter.setVisibility(View.VISIBLE);
                holder.unreadCounter.setText(String.valueOf(chatUser.getUnreadCount()));
            }else {
                holder.unreadCounter.setVisibility(View.GONE);
            }
        }




        holder.itemView.setOnClickListener( v -> {
           Intent intent = new Intent(v.getContext(), ChatDialogue.class);
           intent.putExtra("otherUserId", chatUser.getUserId());
           intent.putExtra("name", chatUser.getName());
            intent.putExtra("image", chatUser.getProfileImage());
            intent.putExtra("role", chatUser.getRole());
           v.getContext().startActivity(intent);
        });

//        holder.backBtn.setOnClickListener(v -> {
//            Intent intent = new Intent(v.getContext(), HomeActivity.class);
//            v.getContext().startActivity(intent);
//        });

    }


    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView teacherName, lastMessage, unreadCounter, lastMessageTime;
        private CircleImageView profileImage;
        private ImageView backBtn, seenIcon;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            unreadCounter = itemView.findViewById(R.id.badgeReceivedMessage);
            lastMessage = itemView.findViewById(R.id.lastMessage);
            teacherName = itemView.findViewById(R.id.userName);
            profileImage = itemView.findViewById(R.id.imageProfile);
            backBtn = itemView.findViewById(R.id.imageBack);
            lastMessageTime = itemView.findViewById(R.id.lastMessageTime);
            seenIcon = itemView.findViewById(R.id.seenIcon);
        }
    }

    public void submitList(List<ChatUser> newList) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(
                new ChatUserDiffCallback(this.list, newList)
        );
        this.list = newList;
        diff.dispatchUpdatesTo(this);
    }
}
