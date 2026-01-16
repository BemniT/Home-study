package com.example.home_study;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.home_study.Adapter.SkeletonSubjectAdapter;
import com.example.home_study.Adapter.SubjectAdapter;
import com.example.home_study.Model.Subject;
import com.example.home_study.Prevalent.Continuity;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Shows a grid of subjects for the student's grade & section.
 */
public class ClassFragment extends Fragment implements SubjectAdapter.OnSubjectClickListener {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private TextView title, subTitle;
    private View emptyStateLayout;
    private ShimmerFrameLayout shimmerClass;
    private RecyclerView recyclerView, skeletonRecycler;
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
        skeletonRecycler = view.findViewById(R.id.skeletonRecycler);
        shimmerClass = view.findViewById(R.id.shimmerClass);
        shimmerClass.startShimmer();

        // Grid layout with 2 columns
        GridLayoutManager subjectGrid = new GridLayoutManager(requireContext(), 2);
        GridLayoutManager skeletonGrid = new GridLayoutManager(requireContext(), 2);

        // Prefetch a few items to reduce jank when scrolling in a grid
        subjectGrid.setInitialPrefetchItemCount(4);
        skeletonGrid.setInitialPrefetchItemCount(4);

        recyclerView.setLayoutManager(subjectGrid);
        skeletonRecycler.setLayoutManager(skeletonGrid);

        // Skeleton placeholder while loading
        skeletonRecycler.setAdapter(new SkeletonSubjectAdapter(6));
        skeletonRecycler.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        adapter = new SubjectAdapter(subjectList, this);
        recyclerView.setAdapter(adapter);

        // Performance tuning for RecyclerView
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(8); // cache a few offscreen views
        recyclerView.setNestedScrollingEnabled(false);

        // RecycledViewPool: allow more cached views for viewType 0 (adjust as needed)
        RecyclerView.RecycledViewPool pool = recyclerView.getRecycledViewPool();
        pool.setMaxRecycledViews(0, 20);

        // Use a lighter item animator (disable change animations to prevent extra invalidations)
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(false);
        recyclerView.setItemAnimator(animator);

        title = view.findViewById(R.id.titleText);
        subTitle = view.findViewById(R.id.subTitleText);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);

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
        // Load courses once
        coursesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Replace the list with a fresh instance to avoid adapter side-reference issues
                List<Subject> newList = new ArrayList<>();

                for (DataSnapshot c : snapshot.getChildren()) {
                    String courseGrade = c.child("grade").getValue(String.class);
                    String courseSection = c.child("section").getValue(String.class);

                    if (grade != null && section != null && grade.equals(courseGrade) && section.equals(courseSection)) {
                        String courseId = c.getKey();
                        String name = c.child("name").getValue(String.class);

                        newList.add(new Subject(courseId, name, courseGrade, courseSection, 0));
                    }
                }

                // assign and notify
                subjectList = newList;
                adapter.setSubjects(subjectList);

                // stop shimmer and show content
                shimmerClass.stopShimmer();
                shimmerClass.setVisibility(View.GONE);
                title.setVisibility(View.VISIBLE);
                subTitle.setVisibility(View.VISIBLE);
                skeletonRecycler.setVisibility(View.GONE);

                if (subjectList.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyStateLayout.setVisibility(View.VISIBLE);
                } else {
                    emptyStateLayout.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
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
        sheet.show(getParentFragmentManager(), "subject_points");
    }
}