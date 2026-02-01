package com.example.home_study;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.Adapter.AssessmentAdapter;
import com.example.home_study.Model.Assessment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class SubjectPointBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_COURSE_ID = "courseId";
    private static final String ARG_SUBJECT = "subject";
    private static final String ARG_STUDENT_ID = "studentID";

    private TextView tvTeacher, tvSubjectInfo, tvPercent, tvMotivation, noAssessment;
    private CircleImageView teacherProfileImage;
    private ObjectAnimator skeletonAnimator;

    private AutoCompleteTextView semesterDropdown;
    private ArrayAdapter<String> semesterAdapter;
    private List<String> semesterList = new ArrayList<>();
    private String selectedSemesterKey = null;

    private RecyclerView rvAssessments;
    private CircularProgressIndicator circular;
    private AssessmentAdapter adapter;
    private List<Assessment> assessments = new ArrayList<>();

    private void startSkeleton(View skeleton){
        skeletonAnimator = ObjectAnimator.ofFloat(skeleton,"alpha", 0.4f, 1f);
        skeletonAnimator.setDuration(700);
        skeletonAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        skeletonAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        skeletonAnimator.start();
    }

    private void stopSkeleton(){
        if (skeletonAnimator != null){
            skeletonAnimator.cancel();
            skeletonAnimator = null;
        }
    }

    public static SubjectPointBottomSheet newInstance(String courseId, String subject, String studentId) {
        SubjectPointBottomSheet sheet = new SubjectPointBottomSheet();
        Bundle b = new Bundle();
        b.putString(ARG_COURSE_ID, courseId);
        b.putString(ARG_STUDENT_ID, studentId);
        b.putString(ARG_SUBJECT, subject);
        sheet.setArguments(b);
        return sheet;
    }

    @Override
    public void onStart() {
        super.onStart();

        View view = getView();
        if (view != null){
            View parent = (View) view.getParent();

            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);

            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int peekHeight = (int) (screenHeight * 0.75f);
            behavior.setFitToContents(false);
            behavior.setHalfExpandedRatio(0.75f);
            behavior.setPeekHeight(peekHeight);
            behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
            behavior.setDraggable(true);

            parent.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bottomsheet_subject_points, container, false);

        View skeleton = v.findViewById(R.id.skeletonContainer);
        View content = v.findViewById(R.id.contentContainer);

        content.setVisibility(View.INVISIBLE);
        skeleton.setVisibility(View.VISIBLE);

        startSkeleton(skeleton);
        v.setAlpha(0f);
        v.animate().alpha(1f).setDuration(700).start();

        tvTeacher = v.findViewById(R.id.tvTeacherName);
        tvSubjectInfo = v.findViewById(R.id.tvSubjectInfo);
        tvPercent = v.findViewById(R.id.tvPercentage);
        teacherProfileImage = v.findViewById(R.id.teacherProfileImage);
        tvMotivation = v.findViewById(R.id.tvMotivational);
        noAssessment = v.findViewById(R.id.noAssessment);

        rvAssessments = v.findViewById(R.id.rvAssessments);
        circular = v.findViewById(R.id.circularBar);

        rvAssessments.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AssessmentAdapter(assessments);
        rvAssessments.setAdapter(adapter);

        // semester dropdown setup
        semesterDropdown = v.findViewById(R.id.semesterDropdown);
        semesterAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, semesterList);
        semesterDropdown.setAdapter(semesterAdapter);
        semesterDropdown.setOnItemClickListener((parent, view1, position, id) -> {
            selectedSemesterKey = semesterList.get(position);
            // reload assessments for this semester
            loadAssessmentsForSemester(getArguments().getString(ARG_COURSE_ID),
                    getArguments().getString(ARG_STUDENT_ID),
                    selectedSemesterKey);
        });

        String courseId = getArguments().getString(ARG_COURSE_ID);
        String studentId = getArguments().getString(ARG_STUDENT_ID);
        String subject = getArguments().getString(ARG_SUBJECT);

        tvSubjectInfo.setText(subject);

        // first fetch teacher assignment (unchanged)
        DatabaseReference assignRef = FirebaseDatabase.getInstance()
                .getReference("TeacherAssignments");

        assignRef.orderByChild("courseId")
                .equalTo(courseId)
                .limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;
                        for (DataSnapshot s : snapshot.getChildren()){
                            String teacherId = s.child("teacherId").getValue(String.class);
                            fetchTeacherUser(teacherId);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { }
                });

        // load semesters + assessments
        detectSemestersAndLoadFirst(courseId, studentId);

        return v;
    }

    /**
     * Detect semester child nodes under ClassMarks/{courseId}/{studentId}
     * If semester nodes exist, populate dropdown and load the first semester's assessments.
     * Otherwise fall back to older structure where "assessments" is directly under the student node.
     */
    private void detectSemestersAndLoadFirst(String courseId, String studentId) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("ClassMarks")
                .child(courseId)
                .child(studentId);

        ref.get().addOnSuccessListener(snap -> {
            stopSkeleton();
            View v = getView();
            if (v == null) return;
            View skeleton = v.findViewById(R.id.skeletonContainer);
            View content = v.findViewById(R.id.contentContainer);

            skeleton.animate().alpha(0f).setDuration(300).withEndAction(()->{
                content.setVisibility(View.VISIBLE);
                skeleton.setVisibility(View.GONE);
            }).start();

            if (!snap.exists()) {
                // nothing stored
                showNoAssessmentsState();
                return;
            }

            // find semester-like children. We treat a child as a semester if it contains "assessments"
            semesterList.clear();
            for (DataSnapshot child : snap.getChildren()) {
                if (child.hasChild("assessments")) {
                    semesterList.add(child.getKey());
                }
            }

            if (!semesterList.isEmpty()) {
                // populate dropdown and select the first semester
                semesterAdapter.notifyDataSetChanged();
                selectedSemesterKey = semesterList.get(0);
                semesterDropdown.setText(selectedSemesterKey, false);
                loadAssessmentsForSemester(courseId, studentId, selectedSemesterKey);
            } else {
                // fallback: maybe the structure is ClassMarks/courseId/studentId/assessments
                if (snap.hasChild("assessments")) {
                    // load the plain assessments
                    loadAssessmentsFromSnapshot(snap.child("assessments"));
                } else {
                    // no assessments
                    showNoAssessmentsState();
                }
            }
        }).addOnFailureListener(e -> {
            Log.e("SubjectPoint", "failed to load classmarks", e);
            stopSkeleton();
            showNoAssessmentsState();
        });
    }

    private void loadAssessmentsForSemester(String courseId, String studentId, String semesterKey) {
        if (semesterKey == null) {
            showNoAssessmentsState();
            return;
        }

        // show a quick loading state: clear current list and show progress
        assessments.clear();
        adapter.notifyDataSetChanged();
        noAssessment.setVisibility(View.GONE);
        rvAssessments.setVisibility(View.GONE);
        circular.setProgress(0);
        tvPercent.setText("--");
        tvMotivation.setText("");

        DatabaseReference semRef = FirebaseDatabase.getInstance()
                .getReference("ClassMarks")
                .child(courseId)
                .child(studentId)
                .child(semesterKey)
                .child("assessments");

        semRef.get().addOnSuccessListener(assSnap -> {
            if (!assSnap.exists()) {
                showNoAssessmentsState();
                return;
            }

            loadAssessmentsFromSnapshot(assSnap);
        }).addOnFailureListener(e -> {
            Log.e("SubjectPoint", "failed to load semester assessments", e);
            showNoAssessmentsState();
        });
    }

    /**
     * Given a snapshot for assessments (children are individual assessments),
     * populate the assessments list and update percentage & UI.
     */
    private void loadAssessmentsFromSnapshot(DataSnapshot assessSnap) {
        assessments.clear();

        int totalScore = 0;
        int totalMax = 0;

        for (DataSnapshot a : assessSnap.getChildren()) {
            String name = a.child("name").getValue(String.class);
            Integer score = a.child("score").getValue(Integer.class);
            Integer max = a.child("max").getValue(Integer.class);

            if (name == null || score == null || max == null) continue;

            assessments.add(new Assessment(name, score, max));

            totalScore += score;
            totalMax += max;
        }

        adapter.notifyDataSetChanged();

        if (assessments.isEmpty()) {
            showNoAssessmentsState();
            return;
        }

        // update percentage and UI
        int percent = (totalMax > 0) ? (int) ((totalScore * 100f) / totalMax) : 0;
        tvPercent.setText(percent + "%");
        setMotivation(tvMotivation, percent);
        circular.setProgress(0);
        ObjectAnimator.ofInt(circular, "progress", percent).setDuration(700).start();

        noAssessment.setVisibility(View.GONE);
        rvAssessments.setVisibility(View.VISIBLE);
    }

    private void showNoAssessmentsState() {
        rvAssessments.setVisibility(View.GONE);
        noAssessment.setVisibility(View.VISIBLE);
        tvPercent.setText("--");
        tvMotivation.setText("No assessment yet. your journey starts soon.");
        circular.setProgress(0);
    }

    private void fetchTeacherUser(String teacherId) {
        DatabaseReference teacherRef = FirebaseDatabase.getInstance()
                .getReference("Teachers")
                .child(teacherId);

        teacherRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                String teacherUserId = snapshot.child("userId").getValue(String.class);
                fetchTeacherUserInfo(teacherUserId);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void fetchTeacherUserInfo(String teacherUserId) {
        DatabaseReference teacherUserRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(teacherUserId);

        teacherUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                String name = snapshot.child("name").getValue(String.class);
                String image = snapshot.child("profileImage").getValue(String.class);
                tvTeacher.setText(name);
                if (image != null && !image.isEmpty()){
                    Picasso.get().load(image).placeholder(R.drawable.profile_image).into(teacherProfileImage);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void setMotivation(TextView tv, int percent) {
        String msg;
        if (percent >= 90) {
            msg = "Outstanding work. This is excellence in motion.";
        } else if (percent >= 75) {
            msg = "Great job. Keep the momentum going.";
        } else if (percent >= 60) {
            msg = "Good effort. A little more focus will push you higher.";
        } else if (percent >= 45) {
            msg = "You’re capable of more. Let’s sharpen the basics.";
        } else {
            msg = "This doesn’t define you. Progress starts with persistence.";
        }

        tv.setText(msg);
        tv.setAlpha(0f);
        tv.animate().alpha(1f).setDuration(400).start();
    }
}