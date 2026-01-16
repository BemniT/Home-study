package com.example.home_study;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.animation.ObjectAnimator;

import com.example.home_study.Prevalent.Continuity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;
import com.squareup.picasso.Picasso;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private ImageView chat;
    private CircleImageView profileTop;
    private ViewPager2 viewPager;
    private TextView chatBadge;

    // Firebase unread listener
    private DatabaseReference chatsRef;
    private ValueEventListener chatsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        // remove shifting and set icon size if needed (you already do this)
        bottomNavigationView.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
        int iconSizePx = getResources().getDimensionPixelSize(com.intuit.sdp.R.dimen._18sdp);
        bottomNavigationView.setItemIconSize(iconSizePx);

        chat = findViewById(R.id.chat);
        profileTop = findViewById(R.id.profile);
        viewPager = findViewById(R.id.view_pager);
        chatBadge = findViewById(R.id.chat_badge); // the TextView added to layout

        // load profile image (guard nulls)
        if (Continuity.currentOnlineUser != null && Continuity.currentOnlineUser.getProfileImage() != null) {
            Picasso.get().load(Continuity.currentOnlineUser.getProfileImage())
                    .placeholder(R.drawable.profile_image)
                    .into(profileTop);
        }

        profileTop.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));
        chat.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ChatActivity.class)));

        // Setup ViewPager2 (same as before)
        viewPager.setAdapter(new ScreenSlidePagerAdapter(this));
        viewPager.setOffscreenPageLimit(1);
        viewPager.setUserInputEnabled(true);

        // Initial bottom nav state
        bottomNavigationView.getMenu().findItem(R.id.home).setIcon(R.drawable.homefill);
        bottomNavigationView.setSelectedItemId(R.id.home);

        // Sync nav with page swipes (same as before)
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                super.onPageSelected(position);
                resetMenuIcons();
                switch (position) {
                    case 0:
                        bottomNavigationView.setSelectedItemId(R.id.home);
                        bottomNavigationView.getMenu().findItem(R.id.home).setIcon(R.drawable.homefill);
                        break;
                    case 1:
                        bottomNavigationView.setSelectedItemId(R.id.book);
                        bottomNavigationView.getMenu().findItem(R.id.book).setIcon(R.drawable.bookfill);
                        break;
                    case 2:
                        bottomNavigationView.setSelectedItemId(R.id.exam);
                        bottomNavigationView.getMenu().findItem(R.id.exam).setIcon(R.drawable.examfill);
                        break;
                    case 3:
                        bottomNavigationView.setSelectedItemId(R.id.profile);
                        bottomNavigationView.getMenu().findItem(R.id.profile).setIcon(R.drawable.bot_icon_filled);
                        break;
                }
            }
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            resetMenuIcons();
            int id = item.getItemId();
            switch (id) {
                case R.id.home:
                    item.setIcon(R.drawable.homefill);
                    viewPager.setCurrentItem(0, true);
                    break;
                case R.id.book:
                    item.setIcon(R.drawable.bookfill);
                    viewPager.setCurrentItem(1, true);
                    break;
                case R.id.exam:
                    item.setIcon(R.drawable.examfill);
                    viewPager.setCurrentItem(2, true);
                    break;
                case R.id.profile:
                    item.setIcon(R.drawable.bot_icon_filled);
                    viewPager.setCurrentItem(3, true);
                    break;
            }
            return true;
        });

        // Start listening for unread counts
        startUnreadListener();
    }

    private void startUnreadListener() {
        // reference to all chats; expects per-chat unread counts stored at Chats/{chatId}/unread/{userId}
        chatsRef = FirebaseDatabase.getInstance().getReference("Chats");

        chatsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalUnread = 0;
                if (snapshot.exists()) {
                    for (DataSnapshot chatSnap : snapshot.getChildren()) {
                        DataSnapshot unreadNode = chatSnap.child("unread").child(Continuity.userId);
                        if (unreadNode.exists()) {
                            Long l = unreadNode.getValue(Long.class);
                            if (l == null) {
                                Integer i = unreadNode.getValue(Integer.class);
                                if (i != null) l = i.longValue();
                            }
                            if (l != null && l > 0) totalUnread += l.intValue();
                        }
                    }
                }
                final int finalTotal = totalUnread;
                runOnUiThread(() -> updateChatBadge(finalTotal));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // ignore or log
            }
        };

        chatsRef.addValueEventListener(chatsListener);
    }

    private void updateChatBadge(int totalUnread) {
        if (totalUnread <= 0) {
            if (chatBadge.getVisibility() == View.VISIBLE) {
                chatBadge.setVisibility(View.GONE);
            }
            return;
        }

        // show limit e.g., 99+ if large
        String text = (totalUnread > 99) ? "99+" : String.valueOf(totalUnread);
        chatBadge.setText(text);

        if (chatBadge.getVisibility() != View.VISIBLE) {
            chatBadge.setVisibility(View.VISIBLE);
            // pop animation for visibility
            chatBadge.setScaleX(0.7f);
            chatBadge.setScaleY(0.7f);
            chatBadge.animate().scaleX(1f).scaleY(1f).setDuration(220).start();
        } else {
            // subtle pulse on update
            ObjectAnimator.ofFloat(chatBadge, "scaleX", 1f, 1.12f, 1f).setDuration(220).start();
            ObjectAnimator.ofFloat(chatBadge, "scaleY", 1f, 1.12f, 1f).setDuration(220).start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // remove listener
        if (chatsRef != null && chatsListener != null) {
            chatsRef.removeEventListener(chatsListener);
        }
    }

    private void resetMenuIcons() {
        bottomNavigationView.getMenu().findItem(R.id.home).setIcon(R.drawable.home);
        bottomNavigationView.getMenu().findItem(R.id.book).setIcon(R.drawable.book);
        bottomNavigationView.getMenu().findItem(R.id.exam).setIcon(R.drawable.exam);
        bottomNavigationView.getMenu().findItem(R.id.profile).setIcon(R.drawable.bot_icon_outline);
    }

    private static class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new HomeFragment();
                case 1: return new BookFragment();
                case 2: return new ExamFragment();
                case 3: return new ClassFragment();
                default: return new HomeFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}