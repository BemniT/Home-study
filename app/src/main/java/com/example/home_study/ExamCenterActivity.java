package com.example.home_study;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.Toast;

import com.example.home_study.Adapter.ExamCenterAdapter;
import com.example.home_study.Model.ExamCenter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ExamCenterActivity extends AppCompatActivity {

    private RecyclerView questionRecycler;
    private ExamCenterAdapter examCenterAdapter;
    private List<ExamCenter> questionList;
    private DatabaseReference questionRef;
    private String selectedSubject, selectedChapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_center);
        
        questionRecycler = findViewById(R.id.examCenterRecycler);
        questionRecycler.setLayoutManager(new LinearLayoutManager(this));
        
        selectedSubject = getIntent().getStringExtra("subject");
        selectedChapter = getIntent().getStringExtra("chapter");
        
        questionList = new ArrayList<>();
        examCenterAdapter = new ExamCenterAdapter(questionList);
        
        questionRecycler.setAdapter(examCenterAdapter);

        questionRef = FirebaseDatabase.getInstance().getReference().child("Exams")
                        .child(selectedSubject).child(selectedChapter);
        
        fetchQuestions();
    }

    private void fetchQuestions() {

        questionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                questionList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                    ExamCenter question = dataSnapshot.getValue(ExamCenter.class);
                    questionList.add(question);
                }

                examCenterAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ExamCenterActivity.this, "Failed to loaded questions", Toast.LENGTH_SHORT).show();
            }
        });
    }
}