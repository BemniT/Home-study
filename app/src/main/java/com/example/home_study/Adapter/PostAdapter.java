package com.example.home_study.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.Model.Post;
import com.example.home_study.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder>
{
    private List<Post> postList;

    public PostAdapter(List<Post> postList) {
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostAdapter.PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.post_card,parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostAdapter.PostViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.author.setText(post.getAuthor());
        holder.postDate.setText(post.getTime());
        holder.postMessage.setText(post.getMessage());
        holder.likeCount.setText(post.getLikes() + " Likes");
        Picasso.get().load(post.getImageUrl()).into(holder.postImage);
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public class PostViewHolder extends RecyclerView.ViewHolder {

        private TextView author, postDate, postMessage, likeCount;

        private ImageView postImage, postLikeIcon, postComment;
        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            author =  itemView.findViewById(R.id.postAuthor);
            postDate = itemView.findViewById(R.id.postTime);
            postMessage = itemView.findViewById(R.id.postMessage);
            likeCount = itemView.findViewById(R.id.postLikeCount);
            postLikeIcon = itemView.findViewById(R.id.likeIcon);
            postComment = itemView.findViewById(R.id.postComment);


        }
    }
}
