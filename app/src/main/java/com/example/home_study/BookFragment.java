package com.example.home_study;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.home_study.Adapter.BookAdapter;
import com.example.home_study.db.SubjectEntity;
import com.example.home_study.repo.ContentRepository;

import java.util.ArrayList;
import java.util.List;

public class BookFragment extends Fragment {

    private RecyclerView bookRecycler;
    private BookAdapter bookAdapter;
    private List<com.example.home_study.Model.Book> bookList;
    private ContentRepository repository;
    private String gradeKey = "grade_7"; // change according to current student

    public BookFragment() { }

    public static BookFragment newInstance() { return new BookFragment(); }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new ContentRepository(requireContext());
        bookList = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book, container, false);
        bookRecycler = view.findViewById(R.id.bookRecyclerView);
        bookRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        bookRecycler.setHasFixedSize(true);
        bookAdapter = new BookAdapter(bookList, getActivity());
        bookRecycler.setAdapter(bookAdapter);

        // Observe subjects in room for the grade
        repository.getSubjectsForGradeLive(gradeKey).observe(getViewLifecycleOwner(), new Observer<List<SubjectEntity>>() {
            @Override
            public void onChanged(List<SubjectEntity> subjectEntities) {
                bookList.clear();
                if (subjectEntities != null) {
                    for (SubjectEntity s : subjectEntities) {
                        // translate subjectEntity to your Book model
                        int icon = com.example.home_study.ResourceUtils.chooseDrawableForBook(s.title); // make chooseDrawableForBook static or replicate mapping
                        bookList.add(new com.example.home_study.Model.Book(s.title, s.grade, icon, s.key));
                    }
                }
                bookAdapter.notifyDataSetChanged();
            }
        });

        // Trigger initial sync from Firebase (will update Room)
        repository.syncSubjectsForGrade(gradeKey);

        return view;
    }
}