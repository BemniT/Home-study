package com.example.home_study.Adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.Model.AdminInfo;
import com.example.home_study.Model.Post;
import com.example.home_study.Prevalent.Continuity;
import com.example.home_study.R;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder>
{

    private List<Post> postList;

    private  final HashMap<String, AdminInfo> adminCache = new HashMap<>();

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
        String userId = Continuity.userId;
        String postID = post.getPostId();

        bindAdminInfo(post, holder);
// To locate the admin of the school from the user node


        DatabaseReference seenRef = FirebaseDatabase.getInstance()
                .getReference("Posts")
                .child(post.getPostId())
                .child("seenBy")
                .child(Continuity.userId);
        seenRef.setValue(true);


            //to save the number of likes of that post
            DatabaseReference postLikeCountRef = FirebaseDatabase.getInstance().getReference()
                    .child("Posts")
                    .child(postID)
                    .child("likeCount");

            //on the respective post to save the user Id
            DatabaseReference postLikedRef = FirebaseDatabase.getInstance().getReference("Posts")
                    .child(postID)
                    .child("likes")
                    .child(userId);

            postLikeCountRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Integer count = snapshot.getValue(Integer.class);
                    if (count == null) count = 0;

                    post.setLikeCount(count);
                    holder.likeCount.setText(count + " Likes");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            postLikedRef.addListenerForSingleValueEvent(new ValueEventListener() {
              @Override
              public void onDataChange(@NonNull DataSnapshot snapshot) {
                  boolean liked = snapshot.exists();
                  post.setLikedByMe(liked);

                  holder.postLikeIcon.setImageResource(
                          liked ? R.drawable.likefill : R.drawable.like
                  );

                  holder.likeCount.setText(post.getLikeCount() + " Likes");
              }

              @Override
              public void onCancelled(@NonNull DatabaseError error) {

              }
          });

        holder.postLikeIcon.setOnClickListener(v -> {
            v.setEnabled(false);

            boolean newLikedState = !post.isLikedByMe();
            int delta = newLikedState ? 1 : -1;

            post.setLikedByMe(newLikedState);
            holder.postLikeIcon.setImageResource(
                    newLikedState ? R.drawable.likefill : R.drawable.like
            );

            DatabaseReference postRef = FirebaseDatabase.getInstance()
                    .getReference("Posts")
                    .child(postID);

            DatabaseReference postLikeRef = postRef
                    .child("likes")
                    .child(userId);

            DatabaseReference userLikeRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(userId)
                    .child("likedPosts")
                    .child(postID);

            postRef.child("likeCount").runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    Integer current = currentData.getValue(Integer.class);
                    if (current == null) current = 0;
                    currentData.setValue(Math.max(0, current + delta));
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                    v.setEnabled(true);

                    if (committed && snapshot != null) {
                        Integer updated = snapshot.getValue(Integer.class);
                        if (updated != null) {
                            post.setLikeCount(updated);
                            holder.likeCount.setText(updated + " Likes");
                        }
                    }
                }
            });

            if (newLikedState) {
                postLikeRef.setValue(true);
                userLikeRef.setValue(true);
            } else {
                postLikeRef.removeValue();
                userLikeRef.removeValue();
            }
        });


        if (post.getPostUrl() == null || post.getPostUrl().isEmpty()){
            Picasso.get().load(R.drawable.math).fit().centerCrop().placeholder(R.drawable.examfill).error(R.drawable.examfill).into(holder.postImage);
        } else {
            Picasso.get().load(post.getPostUrl()).fit().centerCrop().placeholder(R.drawable.examfill).error(R.drawable.examfill).into(holder.postImage);
        }
        holder.postDate.setText(post.getTime());
        holder.postMessage.setText(post.getMessage());

        }


    private void bindAdminInfo(Post post, PostViewHolder holder) {
        String adminId = post.getAdminId();

        // 1. Cached → use immediately
        if (adminCache.containsKey(adminId)) {
            AdminInfo info = adminCache.get(adminId);
            holder.author.setText(info.name);
            Picasso.get()
                    .load(info.profileImage)
                    .placeholder(R.drawable.profile)
                    .into(holder.authorProfile);
            return;
        }

        // 2. Not cached → fetch once
        DatabaseReference adminRef = FirebaseDatabase.getInstance()
                .getReference("School_Admins")
                .child(adminId);

        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot adminSnap) {
                if (!adminSnap.exists()) return;

                String userId = adminSnap.child("userId").getValue(String.class);
                if (userId == null) return;

                FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(userId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnap) {
                                if (!userSnap.exists()) return;

                                String name = userSnap.child("name").getValue(String.class);
                                String image = userSnap.child("profileImage").getValue(String.class);
                                Log.e("profileIMG", name);
                                AdminInfo info = new AdminInfo(name, image);
                                adminCache.put(adminId, info);

                                int adapterPosition = holder.getAdapterPosition();
                                if (adapterPosition == RecyclerView.NO_POSITION) return;

                                Post boundPost = postList.get(adapterPosition);
                                if (!boundPost.getAdminId().equals(adminId)) return;

                                holder.author.setText(name);
                                Picasso.get()
                                        .load(image)
                                        .placeholder(R.drawable.profile)
                                        .into(holder.authorProfile);


                            }

                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    @Override
    public int getItemCount() {
        return postList != null ? postList.size():0;
    }

    public class PostViewHolder extends RecyclerView.ViewHolder {

        private TextView author, postDate, postMessage, likeCount, noPost;

        private ImageView postImage, postLikeIcon;

        private CircleImageView authorProfile;
//        boolean isLiked; int likeCountValue;
        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            noPost = itemView.findViewById(R.id.no_post);
            authorProfile = itemView.findViewById(R.id.author_profile);
            author =  itemView.findViewById(R.id.postAuthor);
            postDate = itemView.findViewById(R.id.postTime);
            postMessage = itemView.findViewById(R.id.postMessage);
            likeCount = itemView.findViewById(R.id.postLikeCount);
            postLikeIcon = itemView.findViewById(R.id.likeIcon);
//            postComment = itemView.findViewById(R.id.postComment);
            postImage = itemView.findViewById(R.id.postImage);


        }
    }

    private void updateLikeCount(DatabaseReference ref, int i) {
        ref.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer current = currentData.getValue(Integer.class);
                if (current == null) current= 0;
                currentData.setValue(Math.max(0, current + i));
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
            }
        });

    }
}
