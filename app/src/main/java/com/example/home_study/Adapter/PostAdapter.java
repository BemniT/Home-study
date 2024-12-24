package com.example.home_study.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.Model.Post;
import com.example.home_study.Prevalent.Continuity;
import com.example.home_study.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

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


        String userId = Continuity.currentOnlineUser.getusername();


        DocumentReference postRef = FirebaseFirestore.getInstance()
                .collection("Posts")
                .document(post.getPostId());

        postRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> likedBy = (List<String>) documentSnapshot.get("likedBy");
                if (likedBy != null && likedBy.contains(userId)) {

                    holder.postLikeIcon.setImageResource(R.drawable.likefilled);
                } else {
                    holder.postLikeIcon.setImageResource(R.drawable.like);
                }
            }
        });

        holder.author.setText(post.getAuthor());
        holder.postDate.setText(post.getTime());
        holder.postMessage.setText(post.getMessage());
        holder.postLikeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userId = Continuity.currentOnlineUser.getusername(); // Get the current user ID

                DocumentReference postRef = FirebaseFirestore.getInstance()
                        .collection("Posts")
                        .document(post.getPostId());

                postRef.get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> likedBy = (List<String>) documentSnapshot.get("likedBy");
                        if (likedBy == null) {

                           likedBy = new ArrayList<>();
                        }
                        if (likedBy.contains(userId)){
                            likedBy.remove(userId);
                            postRef.update("likedBy", likedBy, "likes", likedBy.size());
                            holder.postLikeIcon.setImageResource(R.drawable.like);
                        }
                        else {

                            likedBy.add(userId);
                            postRef.update("likedBy", likedBy, "likes", likedBy.size());
                            holder.postLikeIcon.setImageResource(R.drawable.likefilled); // Liked state
                        }

                        holder.likeCount.setText(String.valueOf(likedBy.size()));
                    }
                });
            }
        });
        holder.postComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(), "Comment Unavailable", Toast.LENGTH_SHORT).show();
            }
        });
        Picasso.get().load(post.getImageUrl()).placeholder(R.drawable.profile).into(holder.postImage);
        Picasso.get().load(post.getAuthorProfile()).placeholder(R.drawable.profile).into(holder.authorProfile);
        holder.likeCount.setText(post.getLikes()+"");
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public class PostViewHolder extends RecyclerView.ViewHolder {

        private TextView author, postDate, postMessage, likeCount;

        private ImageView postImage, postLikeIcon, postComment;

        private CircleImageView authorProfile;
        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            authorProfile = itemView.findViewById(R.id.author_profile);
            author =  itemView.findViewById(R.id.postAuthor);
            postDate = itemView.findViewById(R.id.postTime);
            postMessage = itemView.findViewById(R.id.postMessage);
            likeCount = itemView.findViewById(R.id.postLikeCount);
            postLikeIcon = itemView.findViewById(R.id.likeIcon);
            postComment = itemView.findViewById(R.id.postComment);
            postImage = itemView.findViewById(R.id.postImage);


        }
    }
}
