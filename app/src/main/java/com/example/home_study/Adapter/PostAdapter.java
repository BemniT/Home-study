package com.example.home_study.Adapter;

import android.util.Log;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
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
        String userId = Continuity.currentOnlineUser !=null ?Continuity.currentOnlineUser.getusername():null;
        String postID = post != null ? post.getPostId() : null;

        if (postID == null){
            Log.e("PostAdapter", "User ID or Post ID is null!");
            Toast.makeText(holder.itemView.getContext(), "Invalid data encountered", Toast.LENGTH_SHORT).show();
            return;
        }


            holder.author.setText(post.getAuthor());
            holder.postDate.setText(post.getTime());
            holder.postMessage.setText(post.getMessage());
            Picasso.get().load(post.getImageUrl()).placeholder(R.drawable.profile).into(holder.postImage);
            Picasso.get().load(post.getAuthorProfile()).placeholder(R.drawable.profile).into(holder.authorProfile);
            holder.likeCount.setText(post.getLikes()+"");

            DatabaseReference userLikesRef = FirebaseDatabase.getInstance().getReference()
                    .child("Users")
                    .child(userId)
                    .child("liked");

            DatabaseReference postLikesRef = FirebaseDatabase.getInstance().getReference()
                    .child("Posts")
                    .child(postID)
                    .child("likes");

//        check if the user like the post
            userLikesRef.child(postID).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists())
                    {
                        holder.postLikeIcon.setImageResource(R.drawable.likefilled);
                    }
                    else {
                        holder.postLikeIcon.setImageResource(R.drawable.like);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });




            holder.postLikeIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    userLikesRef.child(postID).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            if (snapshot.exists()) {
                                userLikesRef.child(postID).removeValue();
                                postLikesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            int currentLikes = snapshot.getValue(Integer.class);
                                            postLikesRef.setValue(currentLikes - 1);
                                            holder.likeCount.setText(String.valueOf(currentLikes - 1));
                                            holder.postLikeIcon.setImageResource(R.drawable.like);
                                        } else {
                                            int currentLikes = snapshot.getValue(Integer.class);
                                            postLikesRef.setValue(currentLikes + 1);
                                            holder.likeCount.setText(String.valueOf(currentLikes + 1));
                                            holder.postLikeIcon.setImageResource(R.drawable.likefilled);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }else {

                                userLikesRef.child(postID).setValue(true);
                                postLikesRef.child(postID).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            int currentLikes = snapshot.getValue(Integer.class);
                                            postLikesRef.setValue(currentLikes + 1);
                                            holder.likeCount.setText(String.valueOf(currentLikes + 1));
                                            holder.postLikeIcon.setImageResource(R.drawable.likefilled);
                                        } else {
                                            postLikesRef.setValue(1);
                                            holder.likeCount.setText("1");
                                            holder.postLikeIcon.setImageResource(R.drawable.likefilled);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }
                        }
                        @Override
                        public void onCancelled (@NonNull DatabaseError error){

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

        }




    @Override
    public int getItemCount() {
        return postList != null ? postList.size():0;
    }

    public class PostViewHolder extends RecyclerView.ViewHolder {

        private TextView author, postDate, postMessage, likeCount, noPost;

        private ImageView postImage, postLikeIcon, postComment;

        private CircleImageView authorProfile;
        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            noPost = itemView.findViewById(R.id.no_post);
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
