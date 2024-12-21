package com.example.home_study;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.widget.ImageView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    private ImageView chat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setBackground(null);

        // Set the default selected item (Home icon with 'homefill')
        bottomNavigationView.setSelectedItemId(R.id.home);
        bottomNavigationView.getMenu().findItem(R.id.home).setIcon(R.drawable.homefill);

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
                    item.setIcon(R.drawable.profile); // Active icon
                    replaceFragment(new ProfileFragment());
                    break;
            }
            return true;
        });
    }

    private void resetMenuIcons() {
        bottomNavigationView.getMenu().findItem(R.id.home).setIcon(R.drawable.home); // Default icon
        bottomNavigationView.getMenu().findItem(R.id.book).setIcon(R.drawable.book);
        bottomNavigationView.getMenu().findItem(R.id.exam).setIcon(R.drawable.exam);
        bottomNavigationView.getMenu().findItem(R.id.profile).setIcon(R.drawable.profile);
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_layout, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}
