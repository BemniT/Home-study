package com.example.home_study;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.home_study.Adapter.ExamAdapter;
import com.example.home_study.Model.Book;

import java.util.ArrayList;
import java.util.List;


 public class ExamFragment extends Fragment {


    private RecyclerView examRecycler;
     private List bookList;
    private ExamAdapter examAdapter;
    private String selectedSubject, selectedChapter;
     public View onCreateView(LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
         View view = inflater.inflate(R.layout.fragment_exam, container, false);
         examRecycler = view.findViewById(R.id.examRecyclerView);
         examRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
         examRecycler.setHasFixedSize(true);
         bookList = new ArrayList<>();

         loadBooks();

         ExamAdapter examAdapter = new ExamAdapter(bookList, getActivity());
         examRecycler.setAdapter(examAdapter);

         return view;
     }

     private void loadBooks()
     {
         bookList.add(new Book("English", "Grade 9", R.drawable.english));
         bookList.add(new Book("Mathematics", "Grade 9", R.drawable.math));
         bookList.add(new Book("Physics", "Grade 9", R.drawable.physics));
         bookList.add(new Book("Biology", "Grade 9", R.drawable.biology));
         bookList.add(new Book("Chemistry", "Grade 9", R.drawable.chemistry));
         bookList.add(new Book("Geography", "Grade 9", R.drawable.history));
         bookList.add(new Book("History", "Grade 9", R.drawable.history));

     }
}
