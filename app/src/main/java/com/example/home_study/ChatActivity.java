package com.example.home_study;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.Adapter.ChatListAdapter;
import com.example.home_study.Model.ChatTeacher;
import com.example.home_study.Model.ChatUser;
import com.example.home_study.Prevalent.Continuity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ChatListAdapter adapter;
    private ImageView imageBack;
    private List<ChatUser> chatUserList = new ArrayList<>();
    private Set<String> loadedTeacherIds = new HashSet<>();

    private DatabaseReference studentRef, coursesRef, assignmentRef, teachersRef, usersRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

        recyclerView = (RecyclerView) findViewById(R.id.userRecyclerView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        imageBack = (ImageView) findViewById(R.id.imageBack);
        imageBack.setOnClickListener(v->{
            finish();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatListAdapter(chatUserList);
        recyclerView.setAdapter(adapter);

        studentRef = FirebaseDatabase.getInstance().getReference("Students");
        coursesRef = FirebaseDatabase.getInstance().getReference("Courses");
        assignmentRef = FirebaseDatabase.getInstance().getReference("TeacherAssignments");
        teachersRef = FirebaseDatabase.getInstance().getReference("Teachers");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        loadAdmins();
        loadStudentTeachers();

    }

    private void loadStudentTeachers() {
        String userId = Continuity.userId;

        studentRef.orderByChild("userId")
                .equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot studentSnap) {
                        if (!studentSnap.exists()){
                            Log.e("chatActivity", "No student record fount ");
                            return;
                        }

                        for (DataSnapshot snapshot : studentSnap.getChildren())
                        {
                            String grade = snapshot.child("grade").getValue(String.class);
                            String section = snapshot.child("section").getValue(String.class);

                            Log.d("chat_debug", "Student grade= " + grade + ", student section= " + section);

                            if (grade == null || section == null){
                                Log.d("chat_debug", "grade and section is null");
                            }

                            loadCoursesForStudent(grade, section);
                            break;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadCoursesForStudent(String grade, String section) {

        coursesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot courseSnap) {
                for (DataSnapshot c : courseSnap.getChildren()){
                    String courseGrade = c.child("grade").getValue(String.class);
                    String courseSection = c.child("section").getValue(String.class);

                    Log.d("chat_debug","checking course -> grade= " + courseGrade + ", section= " + courseSection);
                    if ( grade.equals(courseGrade) && section.equals(courseSection))
                    {
                        String courseId = c.getKey();
                        String courseName = c.child("name").getValue(String.class);
                        loadTeachersForCourse(courseId, courseName);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadTeachersForCourse(String courseId, String courseName) {

        assignmentRef.orderByChild("courseId")
                .equalTo(courseId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot assignShot) {
                        for (DataSnapshot a : assignShot.getChildren()){

                            String teacherId = a.child("teacherId").getValue(String.class);
                            resolveTeacher(teacherId, courseName);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void resolveTeacher(String teacherId, String courseName) {

        if (loadedTeacherIds.contains(teacherId)){
            return;
        }
        loadedTeacherIds.add(teacherId);
        teachersRef.child(teacherId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot teacherSnap) {

                        String userId = teacherSnap.child("userId").getValue(String.class);

                        usersRef.child(userId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot userSnap) {

                                        if (!userSnap.exists()) return;
                                        String name = userSnap.child("name").getValue(String.class);
                                        String profileImage = userSnap.child("profileImage").getValue(String.class);

                                        ChatUser chatUser = new ChatUser(userId, name, profileImage,courseName,"TEACHER");
                                        chatUserList.add(chatUser);

                                        adapter.notifyDataSetChanged();
                                        progressBar.setVisibility(GONE);
                                        recyclerView.setVisibility(VISIBLE);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }

                });
    }

    private void loadAdmins(){
        DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference("School_Admins");

        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot adminSnap : snapshot.getChildren()){
                    String userId = adminSnap.child("userId").getValue(String.class);

                    usersRef.child(userId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userShot) {
                                    if (!userShot.exists()) return;

                                    ChatUser admin = new ChatUser(
                                            userId,
                                            userShot.child("name").getValue(String.class),
                                            userShot.child("profileImage").getValue(String.class),
                                            "Director",
                                            "ADMIN"
                                            );

                                    chatUserList.add(0, admin);
                                    adapter.notifyDataSetChanged();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}