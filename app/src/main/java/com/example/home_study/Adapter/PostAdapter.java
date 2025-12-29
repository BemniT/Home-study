package com.example.home_study.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

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


// To locate the admin of the school from the user node
            DatabaseReference adminUserRef = FirebaseDatabase.getInstance().getReference()
                    .child("Users");
            DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference()
                            .child("School_Admins")
                    .child(post.getAdminId());
            adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) return;
                    String adminUserId = snapshot.child("userId").getValue(String.class);

                    if (adminUserId != null){
                        adminUserRef.child(adminUserId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                         if (!snapshot.exists()) return;

                                         String adminName = snapshot.child("name").getValue(String.class);
                                        String adminProfileImage = snapshot.child("profileImage").getValue(String.class);

                                        holder.author.setText(adminName);
                                        Picasso.get().load(adminProfileImage).placeholder(R.drawable.profile)
                                                .into(holder.authorProfile);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            //to save the liked post on the respective user on there user node
            DatabaseReference userLikesRef = FirebaseDatabase.getInstance().getReference()
                    .child("Users")
                    .child(userId)
                    .child("likedPosts")
                    .child(post.getPostId());

            //to save the number of likes of that post
            DatabaseReference postLikeCountRef = FirebaseDatabase.getInstance().getReference()
                    .child("Posts")
                    .child(postID)
                    .child("likeCount");
            //on the respective post to save the user Id
            DatabaseReference postLikedRef = FirebaseDatabase.getInstance().getReference().child("Posts")
                    .child(post.getPostId())
                    .child("likes")
                    .child(userId);

        userLikesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    holder.postLikeIcon.setImageResource(R.drawable.likefill);
                }else{
                    holder.postLikeIcon.setImageResource(R.drawable.like);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

            postLikeCountRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Integer count = snapshot.getValue(Integer.class);
                    holder.likeCount.setText((count == null ? 0 : count)+ " Likes");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });

            //Check if hte user liked ot not


            //Liking and disliking logic
            holder.postLikeIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

// Like and dislike logic the add and erase the user Id from the post likes node and post ID from the users node
                    userLikesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            boolean hasLiked = snapshot.exists();

                            if (hasLiked){
                                //unlike
                                holder.postLikeIcon.setImageResource(R.drawable.like);
                                userLikesRef.removeValue();
                                postLikedRef.removeValue();
                                updateLikeCount(postLikeCountRef,-1);
                            }else{
                                holder.postLikeIcon.setImageResource(R.drawable.likefill);
                                userLikesRef.setValue(true);
                                postLikedRef.setValue(true);
                                updateLikeCount(postLikeCountRef, 1);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
        }
                    });
                }
            });

        holder.postDate.setText(post.getTime());
        holder.postMessage.setText(post.getMessage());
        Picasso.get().load(post.getPostUrl()).placeholder(R.drawable.comment).into(holder.postImage);
//        holder.likeCount.setText(post.getLikeCount() + " Likes");

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
