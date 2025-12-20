package com.example.home_study.Adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.ChatDialogue;
import com.example.home_study.Model.ChatTeacher;
import com.example.home_study.Model.ChatUser;
import com.example.home_study.R;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    private List<ChatUser> list;

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

        private TextView teacherName;
        private CircleImageView profileImage;
        private ImageView backBtn;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            teacherName = itemView.findViewById(R.id.userName);
            profileImage = itemView.findViewById(R.id.imageProfile);
            backBtn = itemView.findViewById(R.id.imageBack);
        }
    }
}
