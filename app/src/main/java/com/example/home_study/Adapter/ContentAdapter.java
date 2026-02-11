package com.example.home_study.Adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.home_study.ContentActivity;
import com.example.home_study.Model.Content;
import com.example.home_study.R;

import java.io.File;
import java.util.List;

public class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.ViewHolder> {

    private List<Content> contentList;
    private OnContentSelectedListener listener;
    private Activity contentActivity;

    public interface OnContentSelectedListener {
        void onContentSelected(Content content);
    }

    public ContentAdapter(List<Content> contentList, OnContentSelectedListener listener, Activity contentActivity) {
        this.contentList = contentList;
        this.listener = listener;
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

        Glide.with(contentActivity)
                .load(content.getContentImage())
                .into(holder.contentImage);

        // Determine icon state:
        // if no pdfUrl -> hide icon
        String pdfUrl = content.getPdfUrl();
        if (pdfUrl == null || pdfUrl.isEmpty()) {
            holder.actionIcon.setVisibility(View.GONE);
        } else {
            holder.actionIcon.setVisibility(View.VISIBLE);
            // map pdfUrl to cache filename
            String fileName = pdfUrl.substring(pdfUrl.lastIndexOf('/') + 1);
            File cached = new File(contentActivity.getCacheDir(), fileName);
            if (cached.exists()) {
                // show "open" icon (use system drawable)
                holder.actionIcon.setImageResource(android.R.drawable.ic_menu_view);
            } else {
                // show "download" icon
                holder.actionIcon.setImageResource(android.R.drawable.stat_sys_download);
            }
        }

        // row click -> same as actionIcon click (open if cached else download)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onContentSelected(content);
        });

        // action icon click -> call activity method to handle download/open
        holder.actionIcon.setOnClickListener(v -> {
            if (contentActivity instanceof ContentActivity) {
                ((ContentActivity) contentActivity).handleActionForContent(content, holder.getAdapterPosition());
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
        private ImageView actionIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            chapter = itemView.findViewById(R.id.chapter);
            chapterSubject = itemView.findViewById(R.id.chapterSubject);
            contentImage = itemView.findViewById(R.id.contentImage);
            actionIcon = itemView.findViewById(R.id.actionIcon);
        }
    }
}