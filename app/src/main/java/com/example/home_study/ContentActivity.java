package com.example.home_study;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.home_study.Adapter.ContentAdapter;
import com.example.home_study.Model.Content;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ContentActivity extends AppCompatActivity {

    private ImageView back;
    private RecyclerView contentRecycler;
    private ContentAdapter contentAdapter;
    private List<Content> contentList;
    private String bookTitle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        Intent intent = getIntent();
        bookTitle = intent.getStringExtra("bookTitle").toString();
        contentRecycler = findViewById(R.id.contentRecycler);
        back = findViewById(R.id.backImage);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent1 = new Intent(ContentActivity.this, );

            }
        });
        contentRecycler.setLayoutManager(new LinearLayoutManager(this));
        fetchChapters(bookTitle);
        contentRecycler = findViewById(R.id.contentRecycler);
        contentList = new ArrayList<>();
        contentAdapter = new ContentAdapter(contentList,this);

        contentRecycler.setAdapter(contentAdapter);

        Log.d("BookTitle", "Book: "+ bookTitle);

    }

    private void fetchChapters(String chapter)
    {
        DatabaseReference contentRe = FirebaseDatabase.getInstance().getReference()
                .child("TextBooks")
                .child(chapter).child("content");

        contentRe.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contentList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren())
                {
                    Content content = dataSnapshot.getValue(Content.class);
                    contentList.add(content);
                }
                contentAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(ContentActivity.this, "Failed to load Content: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}