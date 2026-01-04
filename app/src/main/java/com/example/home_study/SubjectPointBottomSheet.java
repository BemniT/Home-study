
package com.example.home_study;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        v.animate()
                .alpha(1f)
                .setDuration(700)
                .start();


        tvTeacher = v.findViewById(R.id.tvTeacherName);
        tvSubjectInfo = v.findViewById(R.id.tvSubjectInfo);
        tvPercent = v.findViewById(R.id.tvPercentage);
        teacherProfileImage = v.findViewById(R.id.teacherProfileImage);
        tvMotivation = v.findViewById(R.id.tvMotivational);
        noAssessment = v.findViewById(R.id.noAssessment);

        RecyclerView rv = v.findViewById(R.id.rvAssessments);
        CircularProgressIndicator circular = v.findViewById(R.id.circularBar);

        String courseId = getArguments().getString(ARG_COURSE_ID);
        String studentId = getArguments().getString(ARG_STUDENT_ID);
        String subject = getArguments().getString(ARG_SUBJECT);
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

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        List<Assessment> assessments = new ArrayList<>();
        AssessmentAdapter adapter = new AssessmentAdapter(assessments);
        rv.setAdapter(adapter);




        tvSubjectInfo.setText(subject);


        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("ClassMarks")
                .child(courseId)
                .child(studentId);

        ref.get().addOnSuccessListener(snap -> {
            stopSkeleton();
            skeleton.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(()->{
                        content.setVisibility(View.VISIBLE);
                        skeleton.setVisibility(View.GONE);
                        if (assessments.isEmpty()){
                            rv.setVisibility(View.GONE);
                            noAssessment.setVisibility(View.VISIBLE);

                            tvPercent.setText("--");
                            tvMotivation.setText("No assessment yet. your journey starts soon.");
                            circular.setProgress(0);
                        }else {
                            rv.setVisibility(View.VISIBLE);
                            noAssessment.setVisibility(View.GONE);
                        }
                    }).start();

            if (!snap.exists()) return;


            // 2️⃣ Assessments
            DataSnapshot assessSnap = snap.child("assessments");

            assessments.clear();

            int totalScore = 0;
            int totalMax = 0;

            for (DataSnapshot a : assessSnap.getChildren()) {
                String name = a.child("name").getValue(String.class);
                Integer score = a.child("score").getValue(Integer.class);
                Integer max = a.child("max").getValue(Integer.class);

                Log.e("ASSESS", "found: " + name);

                if (name == null || score == null || max == null) continue;

                assessments.add(new Assessment(name, score, max));

                totalScore += score;
                totalMax += max;
            }

            adapter.notifyDataSetChanged();




            // 3️⃣ Circular percentage
            if (totalMax > 0) {
                int percent = (int) ((totalScore * 100f) / totalMax);

                tvPercent.setText(percent + "%");
                setMotivation(tvMotivation, percent);
                circular.setProgress(0);
                ObjectAnimator.ofInt(circular, "progress", percent)
                        .setDuration(700)
                        .start();
            }
        });

        return v;
    }

    private void fetchTeacherUser(String teacherId) {

        DatabaseReference teacherRef = FirebaseDatabase.getInstance()
                .getReference("Teachers")
                .child(teacherId);

        teacherRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String teacherUserId = snapshot.child("userId").getValue(String.class);
                fetchTeacherUserInfo(teacherUserId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void fetchTeacherUserInfo(String teacherUserId) {

        DatabaseReference teacherUserRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(teacherUserId);

        teacherUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String name = snapshot.child("name").getValue(String.class);
                String image = snapshot.child("profileImage").getValue(String.class);

                tvTeacher.setText(name);

                if (image != null && !image.isEmpty()){
                    Picasso.get().load(image).placeholder(R.drawable.profile_image).into(teacherProfileImage);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
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