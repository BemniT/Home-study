package com.example.home_study.Adapter;

import android.app.Activity;
import android.content.Intent;
import android.location.GnssAntennaInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.home_study.ExamContentActivity;
import com.example.home_study.Model.ExamContent;
import com.example.home_study.R;

import java.util.List;

public class ExamContentAdapter extends RecyclerView.Adapter<ExamContentAdapter.ViewHolder> {

    private List<ExamContent> examContentList;

    private Activity examContentActivity;
    private OnExamContentSelectedListener listener;
    public interface OnExamContentSelectedListener{
        void onExamContentSelected(ExamContent examContent);
    }
    public ExamContentAdapter(List<ExamContent> examContentList, OnExamContentSelectedListener listener, Activity examContentActivity) {
        this.examContentList = examContentList;
        this.listener = listener;
        this.examContentActivity = examContentActivity;
    }


    @NonNull
    @Override
    public ExamContentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.content_card,parent,false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ExamContentAdapter.ViewHolder holder, int position) {

        ExamContent examContent = examContentList.get(position);
        holder.chapter.setText(examContent.getContentName());
        holder.chapterSubject.setText(examContent.getContentSubject());
        Glide.with(examContentActivity)
                .load(examContent.getContentImage())
                .into(holder.contentImage);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null){
                    listener.onExamContentSelected(examContent);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return examContentList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView chapter, chapterSubject;
        private ImageView contentImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            chapter = itemView.findViewById(R.id.chapter);
            chapterSubject = itemView.findViewById(R.id.chapterSubject);
            contentImage = itemView.findViewById(R.id.contentImage);

        }
    }

}
