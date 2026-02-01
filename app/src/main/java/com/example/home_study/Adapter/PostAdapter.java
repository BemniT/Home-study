package com.example.home_study.Adapter;

import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.Model.AdminInfo;
import com.example.home_study.Model.Post;
import com.example.home_study.Prevalent.Continuity;
import com.example.home_study.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList;
    private final HashMap<String, AdminInfo> adminCache = new HashMap<>();

    public PostAdapter(List<Post> postList) {
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostAdapter.PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.post_card, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostAdapter.PostViewHolder holder, int position) {
        Post post = postList.get(position);
        String userId = Continuity.userId;
        String postID = post.getPostId();

        bindAdminInfo(post, holder);

        // mark as seen by this user
        DatabaseReference seenRef = FirebaseDatabase.getInstance()
                .getReference("Posts")
                .child(post.getPostId())
                .child("seenBy")
                .child(Continuity.userId);
        seenRef.setValue(true);

        // like count
        DatabaseReference postLikeCountRef = FirebaseDatabase.getInstance().getReference()
                .child("Posts")
                .child(postID)
                .child("likeCount");

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
            public void onCancelled(@NonNull DatabaseError error) {}
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
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        holder.postLikeIcon.setOnClickListener(v -> {
            v.setEnabled(false);

            boolean newLikedState = !post.isLikedByMe();
            int delta = newLikedState ? 1 : -1;

            post.setLikedByMe(newLikedState);
            holder.postLikeIcon.setImageResource(newLikedState ? R.drawable.likefill : R.drawable.like);

            DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("Posts").child(postID);

            DatabaseReference postLikeRef = postRef.child("likes").child(userId);

            DatabaseReference userLikeRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(userId)
                    .child("likedPosts")
                    .child(postID);

            postRef.child("likeCount").runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @NonNull
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData currentData) {
                    Integer current = currentData.getValue(Integer.class);
                    if (current == null) current = 0;
                    currentData.setValue(Math.max(0, current + delta));
                    return com.google.firebase.database.Transaction.success(currentData);
                }

                @Override
                public void onComplete(@androidx.annotation.Nullable DatabaseError error, boolean committed, @androidx.annotation.Nullable DataSnapshot snapshot) {
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

        // load post image (remote or placeholder)
        if (post.getPostUrl() == null || post.getPostUrl().isEmpty()) {
            Picasso.get()
                    .load(R.drawable.math)
                    .fit().centerCrop()
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.image_placeholder)
                    .into(holder.postImage);
        } else {
            Picasso.get()
                    .load(post.getPostUrl())
                    .fit().centerCrop()
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.image_placeholder)
                    .into(holder.postImage);
        }

        // Format time to relative string like "3 minutes ago"
        holder.postDate.setText(formatRelativeTime(post.getTime()));
        holder.postMessage.setText(post.getMessage());
    }

    private void bindAdminInfo(Post post, PostViewHolder holder) {
        String adminId = post.getAdminId();

        if (adminId == null) return;

        if (adminCache.containsKey(adminId)) {
            AdminInfo info = adminCache.get(adminId);
            holder.author.setText(info.name);
            Picasso.get()
                    .load(info.profileImage)
                    .placeholder(R.drawable.profile)
                    .into(holder.authorProfile);
            return;
        }

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
                                AdminInfo info = new AdminInfo(name, image);
                                adminCache.put(adminId, info);

                                int adapterPosition = holder.getAdapterPosition();
                                if (adapterPosition == RecyclerView.NO_POSITION) return;

                                Post boundPost = postList.get(adapterPosition);
                                if (boundPost == null || boundPost.getAdminId() == null) return;
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
        return postList != null ? postList.size() : 0;
    }

    public class PostViewHolder extends RecyclerView.ViewHolder {

        private TextView author, postDate, postMessage, likeCount;
        private ImageView postImage, postLikeIcon;
        private CircleImageView authorProfile;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);


            authorProfile = itemView.findViewById(R.id.author_profile);
            author = itemView.findViewById(R.id.postAuthor);
            postDate = itemView.findViewById(R.id.postTime);
            postMessage = itemView.findViewById(R.id.postMessage);
            likeCount = itemView.findViewById(R.id.postLikeCount);
            postLikeIcon = itemView.findViewById(R.id.likeIcon);
            postImage = itemView.findViewById(R.id.postImage);
        }
    }

    // --------- Time parsing & formatting helpers ---------

    /**
     * Converts ISO-like timestamp strings such as:
     *   "2026-01-12T14:43:50.768379"
     * into a relative time string like "3 minutes ago".
     *
     * The parser is forgiving: it accepts optional fractional seconds.
     */
    private String formatRelativeTime(String isoTimestamp) {
        long ts = parseIsoLikeToMillis(isoTimestamp);
        if (ts <= 0L) return ""; // fallback empty
        CharSequence relative = DateUtils.getRelativeTimeSpanString(ts, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
        return relative != null ? relative.toString() : "";
    }

    /**
     * Parse an ISO-like datetime string to milliseconds since epoch in the device default timezone.
     * Accepts patterns like:
     *   yyyy-MM-dd'T'HH:mm:ss
     *   yyyy-MM-dd'T'HH:mm:ss.SSS
     *   yyyy-MM-dd'T'HH:mm:ss.SSSSSS
     *
     * Returns 0 on failure.
     */
    private long parseIsoLikeToMillis(String s) {
        if (s == null) return 0L;
        try {
            String t = s.trim();
            // regex to capture date/time and optional fractional seconds
            Pattern p = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})[T ](\\d{2}):(\\d{2}):(\\d{2})(?:\\.(\\d+))?.*$");
            Matcher m = p.matcher(t);
            if (!m.matches()) return 0L;

            int year = Integer.parseInt(m.group(1));
            int month = Integer.parseInt(m.group(2));
            int day = Integer.parseInt(m.group(3));
            int hour = Integer.parseInt(m.group(4));
            int minute = Integer.parseInt(m.group(5));
            int second = Integer.parseInt(m.group(6));
            String frac = m.group(7);

            int milli = 0;
            if (frac != null && !frac.isEmpty()) {
                // keep at most 3 digits for milliseconds; pad/truncate as necessary
                if (frac.length() > 3) frac = frac.substring(0, 3);
                while (frac.length() < 3) frac = frac + "0";
                milli = Integer.parseInt(frac);
            }

            Calendar cal = Calendar.getInstance(); // device default timezone
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DAY_OF_MONTH, day);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, second);
            cal.set(Calendar.MILLISECOND, milli);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            Log.e("PostAdapter", "Failed to parse timestamp: " + s, e);
            return 0L;
        }
    }

    // ----------------------------------------------------

    private void updateLikeCount(DatabaseReference ref, int i) {
        ref.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData currentData) {
                Integer current = currentData.getValue(Integer.class);
                if (current == null) current = 0;
                currentData.setValue(Math.max(0, current + i));
                return com.google.firebase.database.Transaction.success(currentData);
            }

            @Override
            public void onComplete(@androidx.annotation.Nullable DatabaseError error, boolean committed, @androidx.annotation.Nullable DataSnapshot snapshot) {}
        });
    }
}