package com.example.home_study;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.home_study.Adapter.ChatBotAdapter;
import com.example.home_study.Adapter.SubjectAdapter;
import com.example.home_study.Model.BotMessage;
import com.example.home_study.Model.Subject;
import com.example.home_study.Prevalent.Continuity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ClassFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ClassFragment extends Fragment implements SubjectAdapter.OnSubjectClickListener{

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private RecyclerView recyclerView;
    private SubjectAdapter adapter;
    private List<Subject> subjectList = new ArrayList<>();

    private DatabaseReference studentsRef;
    private DatabaseReference coursesRef;

    private String studentID;


    public ClassFragment() {
        // Required empty public constructor
    }

    public static ClassFragment newInstance(String param1, String param2) {
        ClassFragment fragment = new ClassFragment();
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

        View view = inflater.inflate(R.layout.fragment_class, container, false);

        recyclerView = view.findViewById(R.id.subjectRecyclerView);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new SubjectAdapter(subjectList, subject -> {
            onSubjectClick(subject);
        });
        recyclerView.setAdapter(adapter);

        studentsRef = FirebaseDatabase.getInstance().getReference("Students");
        coursesRef = FirebaseDatabase.getInstance().getReference("Courses");

        loadStudentGradeAndSection();

        return view;
    }


    private void loadStudentGradeAndSection() {
        String userId = Continuity.userId;

        studentsRef.orderByChild("userId")
                .equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;

                        for (DataSnapshot s : snapshot.getChildren()) {
                            String grade = s.child("grade").getValue(String.class);
                            String section = s.child("section").getValue(String.class);
                            studentID = s.getKey();
                            loadCoursesForStudent(grade, section);
                            break;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }


    private void loadCoursesForStudent(String grade, String section) {

        coursesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                subjectList.clear();

                for (DataSnapshot c : snapshot.getChildren()) {

                    String courseGrade = c.child("grade").getValue(String.class);
                    String courseSection = c.child("section").getValue(String.class);

                    if (grade.equals(courseGrade) && section.equals(courseSection)) {

                        String courseId = c.getKey();
                        String name = c.child("name").getValue(String.class);

                        subjectList.add(
                                new Subject(courseId, name, courseGrade, courseSection)
                        );
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    @Override
    public void onSubjectClick(Subject subject) {
        String courseTd = subject.getCourseId();
        String subjectName = subject.getName();


        SubjectPointBottomSheet sheet = SubjectPointBottomSheet.newInstance(courseTd, subjectName, studentID);

        sheet.show(getParentFragmentManager(),"subject_points");
    }
}