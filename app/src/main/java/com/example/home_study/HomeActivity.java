package com.example.home_study;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.example.home_study.Model.Account;
import com.example.home_study.Prevalent.Continuity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    private ImageView chat;
    private CircleImageView profileTop;
    Account user;

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
        bottomNavigationView.setBackground(null);

        // Set the default selected item (Home icon with 'homefill')
        bottomNavigationView.setSelectedItemId(R.id.home);
        bottomNavigationView.getMenu().findItem(R.id.home).setIcon(R.drawable.homefill);


        chat = (ImageView) findViewById(R.id.chat);
        profileTop =  findViewById(R.id.profile);
        Picasso.get().load(Continuity.currentOnlineUser.getProfileImage()).placeholder(R.drawable.profile_image).into(profileTop);
//        Log.d("image",profileImage);
        profileTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });
        chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, ChatActivity.class);
                startActivity(intent);
            }
        });

        replaceFragment(new HomeFragment());
        // Set up the listener for item selection
        bottomNavigationView.setOnItemSelectedListener(item -> {
            final int ID = item.getItemId();

            // Reset all icons to default
            resetMenuIcons();

            // Update selected item's icon and fragment
            switch (ID) {
                case R.id.home:
                    item.setIcon(R.drawable.homefill); // Active icon
                    replaceFragment(new HomeFragment());
                    break;

                case R.id.book:
                    item.setIcon(R.drawable.bookfill); // Active icon
                    replaceFragment(new BookFragment());
                    break;

                case R.id.exam:
                    item.setIcon(R.drawable.examfill); // Active icon
                    replaceFragment(new ExamFragment());
                    break;

                case R.id.profile:
                    item.setIcon(R.drawable.bot_icon_filled); // Active icon
                    replaceFragment(new ClassFragment());
                    break;
            }
            return true;
        });
    }

    private void resetMenuIcons() {
        bottomNavigationView.getMenu().findItem(R.id.home).setIcon(R.drawable.home); // Default icon
        bottomNavigationView.getMenu().findItem(R.id.book).setIcon(R.drawable.book);
        bottomNavigationView.getMenu().findItem(R.id.exam).setIcon(R.drawable.exam);
        bottomNavigationView.getMenu().findItem(R.id.profile).setIcon(R.drawable.bot_icon_outline);

    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_layout, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}
