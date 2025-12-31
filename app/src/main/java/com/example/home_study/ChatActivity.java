package com.example.home_study;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.Adapter.ChatListAdapter;
import com.example.home_study.Model.ChatUser;
import com.example.home_study.Prevalent.Continuity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ChatListAdapter adapter;
    private ImageView imageBack;
    private TextView all,admin,teacher;
    private List<ChatUser> chatUserList = new ArrayList<>();
    List<ChatUser> allChats = new ArrayList<>();
    List<ChatUser> visibleChats = new ArrayList<>();

    private final Set<String> loadedUserIds = new HashSet<>();
    private ChatCategory selectedCategory = ChatCategory.ALL;

    private DatabaseReference studentRef, coursesRef, assignmentRef, teachersRef, usersRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chatActivity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = (RecyclerView) findViewById(R.id.userRecyclerView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        imageBack = (ImageView) findViewById(R.id.imageBack);
        imageBack.setOnClickListener(v->{
            finish();
        });

        all = findViewById(R.id.allCard);
        admin=  findViewById(R.id.adminCard);
        teacher = findViewById(R.id.teachersCard);

        all.setOnClickListener(v -> {
            selectedCategory = ChatCategory.ALL;
            selectedPill(all,admin,teacher);
            applyFilter();
        });
        admin.setOnClickListener(v -> {
            selectedCategory = ChatCategory.ADMIN;
            selectedPill(admin,all,teacher);

            applyFilter();
        });
        teacher.setOnClickListener(v -> {
            selectedCategory = ChatCategory.TEACHER;
            selectedPill(teacher,all,admin);

            applyFilter();
        });


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatListAdapter(visibleChats);
        recyclerView.setAdapter(adapter);

        studentRef = FirebaseDatabase.getInstance().getReference("Students");
        coursesRef = FirebaseDatabase.getInstance().getReference("Courses");
        assignmentRef = FirebaseDatabase.getInstance().getReference("TeacherAssignments");
        teachersRef = FirebaseDatabase.getInstance().getReference("Teachers");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        loadAdmins();
        loadStudentTeachers();

    }

    public enum ChatCategory{
        ALL,
        ADMIN,
        TEACHER
    }

    private void applyFilter() {

        List<ChatUser> filtered = new ArrayList<>();

        for (ChatUser u : chatUserList) {
            if (selectedCategory == ChatCategory.ALL) {
                filtered.add(u);
            } else if (selectedCategory == ChatCategory.ADMIN && "ADMIN".equals(u.getRole())) {
                filtered.add(u);
            } else if (selectedCategory == ChatCategory.TEACHER && "TEACHER".equals(u.getRole())) {
                filtered.add(u);
            }
        }

        adapter.submitList(filtered);
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

        if (loadedUserIds.contains(teacherId)){
            return;
        }
        loadedUserIds.add(teacherId);
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
                                        listenForUnreadCounts(chatUser);
                                        listenForChatMeta(chatUser);
                                        applyFilter();
//                                        adapter.notifyDataSetChanged();
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

                                    listenForUnreadCounts(admin);
                                    listenForChatMeta(admin);
                                    applyFilter();
//                                    adapter.notifyDataSetChanged();
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

    private void listenForUnreadCounts(ChatUser chatUser)
    {
        String chatId = generateChatId(chatUser.getUserId(), Continuity.userId);

        DatabaseReference unreadRef = FirebaseDatabase.getInstance().getReference("ChatMeta")
                .child(chatId)
                .child("unread")
                .child(Continuity.userId);

        unreadRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = snapshot.exists() ? snapshot.getValue(Integer.class) : 0;
                chatUser.setUnreadCount(count);
//                adapter.notifyDataSetChanged();
                applyFilter();
                updatePillBadges();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String generateChatId(String a, String b) {
        if (a.compareTo(b) < 0) return a + "_" + b;
        return b + "_" + a;
    }

    private void listenForChatMeta(ChatUser user){
        String chatId = generateChatId(user.getUserId(), Continuity.userId);

        DatabaseReference metaRef = FirebaseDatabase.getInstance()
                .getReference("ChatMeta")
                .child(chatId);

        metaRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                DataSnapshot last = snapshot.child("lastMessage");

                user.setLastMessage(last.child("text").getValue(String.class));
                user.setLastMessageSenderId(last.child("senderId").getValue(String.class));
                user.setLastMessageSeen(Boolean.TRUE.equals(last.child("seen").getValue(Boolean.class)));


                Long time = last.child("timestamp").getValue(Long.class);
                user.setLastMessageTime(time != null ? time:0L);

                int unread = snapshot.child("unread")
                        .child(Continuity.userId).getValue(Integer.class) != null
                        ? snapshot.child("unread").child(Continuity.userId).getValue(Integer.class) :0;

                user.setUnreadCount(unread);

                sortAndApply();
                updatePillBadges();
//                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private int getUnreadForRole(String role){
        int total = 0;
        for (ChatUser u : chatUserList){
            if (role == null || role.equals(u.getRole())){
                total += u.getUnreadCount();
            }
        }
        return total;
    }

    private void updatePillBadges() {
        TextView all = findViewById(R.id.allCard);
        TextView admin = findViewById(R.id.adminCard);
        TextView teacher = findViewById(R.id.teachersCard);

        int allUnread = getUnreadForRole(null);
        int adminUnread = getUnreadForRole("ADMIN");
        int teacherUnread = getUnreadForRole("TEACHER");

        all.setText(allUnread > 0 ? "All (" + allUnread + ")" : "All");
        admin.setText(adminUnread > 0 ? "Admins (" + adminUnread + ")" : "Admins");
        teacher.setText(teacherUnread > 0 ? "Teachers (" + teacherUnread + ")" : "Teachers");

    }
    private void sortAndApply() {
        List<ChatUser> sorted = new ArrayList<>(chatUserList);
        Collections.sort(sorted, (a, b) ->
                Long.compare(b.getLastMessageTime(), a.getLastMessageTime())
        );
        chatUserList = sorted;
        applyFilter();
    }
    private void selectedPill(View selected, View... others){
        selected.setBackgroundResource(R.drawable.pill_selected);
        selected.animate().scaleX(1.05f).scaleY(1.05f).setDuration(120).start();

        for (View v: others ){
            v.setBackgroundResource(R.drawable.pill_unselected);
            v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();

        }
    }
}