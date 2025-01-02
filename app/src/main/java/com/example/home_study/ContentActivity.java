package com.example.home_study;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.ImageView;

public class ContentActivity extends AppCompatActivity {

    private ImageView back;
    private RecyclerView contentRecycler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        contentRecycler = findViewById(R.id.contentRecycler);
        back = findViewById(R.id.backImage);


    }
}