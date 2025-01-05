package com.example.home_study;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.home_study.Adapter.BookAdapter;
import com.example.home_study.Model.Book;
import com.example.home_study.Prevalent.Continuity;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BookFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BookFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private List bookList;
    private RecyclerView bookRecycler;

    public BookFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BookFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BookFragment newInstance(String param1, String param2) {
        BookFragment fragment = new BookFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book, container, false);
        bookRecycler = view.findViewById(R.id.bookRecyclerView);
        bookRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        bookList = new ArrayList<>();
        BookAdapter bookAdapter = new BookAdapter(bookList, getActivity());

        loadBooks();

        bookRecycler.setAdapter(bookAdapter);

        bookRecycler.setHasFixedSize(true);

        return view;
    }

    private void loadBooks()
    {
        bookList.add(new Book("Biology", "Grade 9", R.drawable.biology));
        bookList.add(new Book("Mathematics", "Grade 9", R.drawable.math));
        bookList.add(new Book("Physics", "Grade 9", R.drawable.biology));
        bookList.add(new Book("Chemistry", "Grade 9", R.drawable.biology));
        bookList.add(new Book("Chemistry", "Grade 9", R.drawable.biology));
        bookList.add(new Book("Chemistry", "Grade 9", R.drawable.biology));

    }

}