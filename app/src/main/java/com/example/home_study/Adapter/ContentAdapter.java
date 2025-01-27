package com.example.home_study.Adapter;

import static androidx.fragment.app.FragmentManager.TAG;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.home_study.Model.Content;
import com.example.home_study.PdfViewerActivity;
import com.example.home_study.R;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

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
//                .placeholder(R.drawable.placeholder)
//                .error(R.drawable.error_image)
                .into(holder.contentImage);


        Log.d("Content", "ContentName: "+ content.getContentName());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null)
                {
                    listener.onContentSelected(content);
                }
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
