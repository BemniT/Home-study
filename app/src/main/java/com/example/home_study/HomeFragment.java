package com.example.home_study;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.home_study.Adapter.PostAdapter;
import com.example.home_study.Model.Post;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ShimmerFrameLayout shimmerLayout;
    private View noPostContainer;
    private ImageView noPostImage;
    private TextView noPostMessage;
    private TextView noPostSub;
    private Button noPostAction;

    // minimum skeleton display to avoid flicker (ms)
    private static final long MIN_SHIMMER_MS = 600;

    public HomeFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view  = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.homeRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary_secondary);
        swipeRefreshLayout.setOnRefreshListener(this::fetchPosts);

        // shimmer + no-post views
        shimmerLayout = view.findViewById(R.id.shimmerLayout);
        noPostContainer = view.findViewById(R.id.no_post_container);
        noPostImage = view.findViewById(R.id.no_post_image);
        noPostMessage = view.findViewById(R.id.no_post_message);
        noPostSub = view.findViewById(R.id.no_post_sub);




        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        // initial load
        fetchPosts();

        return view;
    }

    private void showShimmer(boolean show) {
        if (shimmerLayout == null) return;
        if (show) {
            shimmerLayout.setVisibility(View.VISIBLE);
            shimmerLayout.startShimmer();
            // hide content while shimmer shows
            recyclerView.setVisibility(View.INVISIBLE);
            noPostContainer.setVisibility(View.GONE);
        } else {
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void fetchPosts() {
        // show shimmer
        showShimmer(true);
        final long start = System.currentTimeMillis();

        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        // single read
        postRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long elapsed = System.currentTimeMillis() - start;
                long delay = Math.max(0, MIN_SHIMMER_MS - elapsed);

                // schedule UI update after minimum shimmer time to avoid flicker
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    postList.clear();

                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Post post = dataSnapshot.getValue(Post.class);
                        if (post != null) postList.add(post);
                    }

                    Collections.reverse(postList);
                    postAdapter.notifyDataSetChanged();

                    // stop shimmer and show either content or empty state
                    showShimmer(false);

                    if (postList.isEmpty()) {
                        recyclerView.setVisibility(View.INVISIBLE);
                        noPostContainer.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        noPostContainer.setVisibility(View.GONE);
                    }
                }, delay);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    showShimmer(false);
                    Toast.makeText(requireContext(), "Something went wrong loading posts.", Toast.LENGTH_SHORT).show();
                    if (postList.isEmpty()) {
                        recyclerView.setVisibility(View.INVISIBLE);
                        noPostContainer.setVisibility(View.VISIBLE);
                    }
                }, MIN_SHIMMER_MS);
            }
        });
    }
}