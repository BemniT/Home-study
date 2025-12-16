package com.example.home_study.Adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.ChatDialogue;
import com.example.home_study.Model.ChatTeacher;
import com.example.home_study.R;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class Chat_ListTeacherAdapter extends RecyclerView.Adapter<Chat_ListTeacherAdapter.ViewHolder> {

    private List<ChatTeacher> list;

    public Chat_ListTeacherAdapter(List<ChatTeacher> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public Chat_ListTeacherAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_container_card, parent,false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Chat_ListTeacherAdapter.ViewHolder holder, int position) {
        ChatTeacher teacher = list.get(position);
        holder.teacherName.setText(teacher.getName());

        holder.itemView.setOnClickListener( v -> {
           Intent intent = new Intent(v.getContext(), ChatDialogue.class);
           intent.putExtra("teacherUserID", teacher.getUserId());
           intent.putExtra("teacherName", teacher.getName());
           v.getContext().startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView teacherName;
        private CircleImageView profileImage;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            teacherName = itemView.findViewById(R.id.userName);
            profileImage = itemView.findViewById(R.id.imageProfile);
        }
    }
}
