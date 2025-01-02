package com.example.home_study.Adapter;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.Model.Content;
import com.example.home_study.R;

import org.w3c.dom.Text;

import java.util.List;

public class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.ViewHolder> {


    private List<Content> contentList;
    private Activity contentActivity;

    public ContentAdapter(List<Content> contentList, Activity contentActivity) {
        this.contentList = contentList;
        this.contentActivity = contentActivity;
    }

    @NonNull
    @Override
    public ContentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.content_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContentAdapter.ViewHolder holder, int position) {

        Content content = contentList.get(position);

        holder.chapter.setText(content.getContentName());
        holder.chapterSubject.setText(content.getContentSubject());


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =  new Intent(contentActivity, )
            }
        });


    }

    @Override
    public int getItemCount() {
        return contentList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

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