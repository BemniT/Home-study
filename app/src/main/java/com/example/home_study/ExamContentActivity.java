package com.example.home_study;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.home_study.Adapter.ContentAdapter;
import com.example.home_study.Adapter.ExamContentAdapter;
import com.example.home_study.Model.Content;
import com.example.home_study.Model.ExamContent;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class ExamContentActivity extends AppCompatActivity {


    private RecyclerView recyclerView;

    private List<ExamContent> examContentList;
    private String examBookTitle;

    private ExamContentAdapter contentAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_content);


        recyclerView = findViewById(R.id.examContentRecycler);

        Intent intent = getIntent();
        examBookTitle = intent.getStringExtra("examBookTitle").toString();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        examContentList = new ArrayList<>();

        loadStaticChapters(examBookTitle);
        contentAdapter = new ExamContentAdapter(examContentList, this::onExamContentSelected, this);

        recyclerView.setAdapter(contentAdapter);
        Log.d("examBookTitle", "Book: "+ examBookTitle);

    }

    private void loadStaticChapters(String bookTitle)
    {
        examContentList.clear();

        if (bookTitle.equals("Biology")) {
            Log.i("load","We'er in, this method is triggered");
            examContentList.add(new ExamContent("Introduction to Biology", "Biology", R.drawable.biology));
            examContentList.add(new ExamContent("Cell Structure and Function", "Biology", R.drawable.biology));
            examContentList.add(new ExamContent("Genetics", "Biology", R.drawable.biology));
        } else if (bookTitle.equalsIgnoreCase("Physics")) {
            examContentList.add(new ExamContent("Introduction to Physics", "Physics", R.drawable.physics));
            examContentList.add(new ExamContent("Motion and Forces", "Physics", R.drawable.physics));
            examContentList.add(new ExamContent("Energy and Work", "Physics", R.drawable.physics));
        } else {
//            contentList.add(new Content("Default Chapter 1", bookTitle, R.drawable.default_image, null));
//            examContentList.add(new Content("Default Chapter 2", bookTitle, R.drawable.default_image, null));
        }

    }

    private void onExamContentSelected(ExamContent examContent) {

        Intent intent = new Intent(ExamContentActivity.this, ExamCenterActivity.class);
        intent.putExtra("chapter", examContent.getContentName());
        intent.putExtra("subject", examContent.getContentSubject());
        startActivity(intent);
    }
}