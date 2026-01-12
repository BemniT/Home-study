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
import androidx.recyclerview.widget.DefaultItemAnimator;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ChatListAdapter adapter;
    private ImageView imageBack;
    private TextView all, admin, teacher;
    private List<ChatUser> chatUserList = new ArrayList<>();

    private final Set<String> loadedUserIds = new HashSet<>();
    private ChatCategory selectedCategory = ChatCategory.ALL;

    private DatabaseReference studentRef, coursesRef, assignmentRef, teachersRef, usersRef;

    // track chat listeners so we attach once per Chats/{chatId}
    private final Set<String> chatListeningIds = new HashSet<>();
    private final Map<String, ValueEventListener> chatListeners = new HashMap<>();
    private final Map<String, DatabaseReference> chatRefs = new HashMap<>();

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

        recyclerView = findViewById(R.id.userRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(v -> finish());

        all = findViewById(R.id.allCard);
        admin = findViewById(R.id.adminCard);
        teacher = findViewById(R.id.teachersCard);

        all.setOnClickListener(v -> {
            selectedCategory = ChatCategory.ALL;
            selectedPill(all, admin, teacher);
            applyFilter();
        });
        admin.setOnClickListener(v -> {
            selectedCategory = ChatCategory.ADMIN;
            selectedPill(admin, all, teacher);
            applyFilter();
        });
        teacher.setOnClickListener(v -> {
            selectedCategory = ChatCategory.TEACHER;
            selectedPill(teacher, all, admin);
            applyFilter();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatListAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(220);
        animator.setChangeDuration(200);
        animator.setMoveDuration(220);
        animator.setRemoveDuration(180);
        recyclerView.setItemAnimator(animator);

        studentRef = FirebaseDatabase.getInstance().getReference("Students");
        coursesRef = FirebaseDatabase.getInstance().getReference("Courses");
        assignmentRef = FirebaseDatabase.getInstance().getReference("TeacherAssignments");
        teachersRef = FirebaseDatabase.getInstance().getReference("Teachers");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        loadAdmins();
        loadStudentTeachers();
    }

    public enum ChatCategory {
        ALL,
        ADMIN,
        TEACHER
    }

    private void applyFilter() {
        // always build a new list instance to submit to adapter (avoids same-reference optimization)
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
        // submit a brand new list instance (adapter will copy too)
        adapter.submitList(new ArrayList<>(filtered));
    }

    private void loadStudentTeachers() {
        String userId = Continuity.userId;

        studentRef.orderByChild("userId")
                .equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot studentSnap) {
                        if (!studentSnap.exists()) {
                            Log.e("chatActivity", "No student record found ");
                            return;
                        }

                        for (DataSnapshot snapshot : studentSnap.getChildren()) {
                            String grade = snapshot.child("grade").getValue(String.class);
                            String section = snapshot.child("section").getValue(String.class);

                            Log.d("chat_debug", "Student grade= " + grade + ", student section= " + section);

                            if (grade == null || section == null) {
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
                for (DataSnapshot c : courseSnap.getChildren()) {
                    String courseGrade = c.child("grade").getValue(String.class);
                    String courseSection = c.child("section").getValue(String.class);

                    Log.d("chat_debug", "checking course -> grade= " + courseGrade + ", section= " + courseSection);
                    if (grade != null && section != null && grade.equals(courseGrade) && section.equals(courseSection)) {
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
                        for (DataSnapshot a : assignShot.getChildren()) {
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
        if (teacherId == null) return;
        if (loadedUserIds.contains(teacherId)) {
            return;
        }
        loadedUserIds.add(teacherId);

        teachersRef.child(teacherId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot teacherSnap) {
                        if (!teacherSnap.exists()) return;

                        String userId = teacherSnap.child("userId").getValue(String.class);
                        if (userId == null) return;

                        usersRef.child(userId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot userSnap) {
                                        if (!userSnap.exists()) return;
                                        String name = userSnap.child("name").getValue(String.class);
                                        String profileImage = userSnap.child("profileImage").getValue(String.class);

                                        // Prevent duplicated chat users with same userId
                                        boolean already = false;
                                        for (ChatUser cu : chatUserList) {
                                            if (userId.equals(cu.getUserId())) {
                                                already = true;
                                                break;
                                            }
                                        }
                                        if (already) return;

                                        ChatUser chatUser = new ChatUser(userId, name, profileImage, courseName, "TEACHER");
                                        chatUserList.add(chatUser);

                                        // Attach single chat listener per Chats/{chatId}
                                        listenForChatNode(chatUser);

                                        applyFilter();
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

    private void loadAdmins() {
        DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference("School_Admins");

        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot adminSnap : snapshot.getChildren()) {
                    String userId = adminSnap.child("userId").getValue(String.class);
                    if (userId == null) continue;

                    usersRef.child(userId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userShot) {
                                    if (!userShot.exists()) return;

                                    // Prevent duplicated chat users with same userId
                                    boolean already = false;
                                    for (ChatUser cu : chatUserList) {
                                        if (userId.equals(cu.getUserId())) {
                                            already = true;
                                            break;
                                        }
                                    }
                                    if (already) return;

                                    ChatUser admin = new ChatUser(
                                            userId,
                                            userShot.child("name").getValue(String.class),
                                            userShot.child("profileImage").getValue(String.class),
                                            "Director",
                                            "ADMIN"
                                    );

                                    // put admins at the top but only if not present
                                    chatUserList.add(0, admin);

                                    listenForChatNode(admin);
                                    applyFilter();
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

    /**
     * Listen to Chats/{chatId} root. This reads:
     * - lastMessage (text,senderId,seen,timeStamp)
     * - unread (map of userId -> count) — used for badges (efficient)
     *
     * We attach one listener per chatId (tracked) to avoid duplicates on scroll.
     * When the chat node changes we create a new ChatUser instance and replace it in the list
     * so DiffUtil detects the change immediately (fixes "must scroll to refresh" problem).
     */
    private void listenForChatNode(ChatUser existingUser) {
        if (existingUser == null || existingUser.getUserId() == null) return;

        String chatId = generateChatId(existingUser.getUserId(), Continuity.userId);
        if (chatListeningIds.contains(chatId)) return;

        chatListeningIds.add(chatId);

        DatabaseReference chatRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatId);

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Build preview fields from snapshot
                String text = null;
                String senderId = null;
                boolean seen = false;
                long time = 0L;
                int unread = 0;

                if (snapshot.exists()) {
                    DataSnapshot last = snapshot.child("lastMessage");
                    if (last.exists()) {
                        if (last.hasChild("text")) text = last.child("text").getValue(String.class);
                        else if (last.hasChild("message")) text = last.child("message").getValue(String.class);
                        else text = last.getValue(String.class);

                        senderId = last.child("senderId").getValue(String.class);

                        Object seenObj = last.child("seen").getValue();
                        if (seenObj instanceof Boolean) seen = (Boolean) seenObj;
                        else if (seenObj instanceof Long) seen = ((Long) seenObj) != 0L;
                        else if (seenObj instanceof Integer) seen = ((Integer) seenObj) != 0;

                        Long t = last.child("timeStamp").getValue(Long.class);
                        if (t == null) {
                            Integer ti = last.child("timeStamp").getValue(Integer.class);
                            if (ti != null) t = ti.longValue();
                            else {
                                Long t2 = last.child("timestamp").getValue(Long.class);
                                if (t2 != null) t = t2;
                            }
                        }
                        if (t != null) {
                            if (t < 1_000_000_000_000L) t = t * 1000L;
                            time = t;
                        }
                    }

                    // read unread map if present (preferred)
                    DataSnapshot unreadNode = snapshot.child("unread").child(Continuity.userId);
                    if (unreadNode.exists()) {
                        Long l = unreadNode.getValue(Long.class);
                        if (l == null) {
                            Integer i = unreadNode.getValue(Integer.class);
                            if (i != null) l = i.longValue();
                        }
                        if (l != null) unread = l.intValue();
                    } else {
                        // fallback counting (should be rare if unread map present)
                        DataSnapshot messagesNode = snapshot.child("messages");
                        if (messagesNode.exists()) {
                            int count = 0;
                            for (DataSnapshot m : messagesNode.getChildren()) {
                                String receiverId = m.child("receiverId").getValue(String.class);
                                Object seenObj = m.child("seen").getValue();
                                boolean mSeen = false;
                                if (seenObj instanceof Boolean) mSeen = (Boolean) seenObj;
                                else if (seenObj instanceof Long) mSeen = ((Long) seenObj) != 0L;
                                else if (seenObj instanceof Integer) mSeen = ((Integer) seenObj) != 0;

                                if (receiverId != null && receiverId.equals(Continuity.userId) && !mSeen) {
                                    count++;
                                }
                            }
                            unread = count;
                        }
                    }
                }

                // Find index in current chatUserList by userId
                int idx = -1;
                String targetUserId = existingUser.getUserId();
                for (int i = 0; i < chatUserList.size(); i++) {
                    ChatUser cu = chatUserList.get(i);
                    if (cu.getUserId() != null && cu.getUserId().equals(targetUserId)) {
                        idx = i;
                        break;
                    }
                }

                if (idx >= 0) {
                    ChatUser old = chatUserList.get(idx);
                    // create a new ChatUser instance preserving non-preview fields
                    ChatUser updated = new ChatUser(
                            old.getUserId(),
                            old.getName(),
                            old.getProfileImage(),
                            old.getSubstitle(),
                            old.getRole()
                    );
                    updated.setLastMessage(text);
                    updated.setLastMessageSenderId(senderId);
                    updated.setLastMessageSeen(seen);
                    updated.setLastMessageTime(time);
                    updated.setUnreadCount(unread);

                    // create a new list copy and replace the item inside it
                    List<ChatUser> newList = new ArrayList<>(chatUserList);
                    newList.set(idx, updated);

                    // replace reference atomically so subsequent reads see the new list
                    chatUserList = newList;

                    // sort & submit (sortAndApply will create yet another new list and submit)
                    sortAndApply();
                    updatePillBadges();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        chatRef.addValueEventListener(listener);
        chatListeners.put(chatId, listener);
        chatRefs.put(chatId, chatRef);
    }

    private String generateChatId(String a, String b) {
        if (a == null || b == null) return a + "_" + b;
        if (a.compareTo(b) < 0) return a + "_" + b;
        return b + "_" + a;
    }

    private int getUnreadForRole(String role) {
        int total = 0;
        for (ChatUser u : chatUserList) {
            if (role == null || role.equals(u.getRole())) {
                total += u.getUnreadCount();
            }
        }
        return total;
    }

    private void updatePillBadges() {
        int allUnread = getUnreadForRole(null);
        int adminUnread = getUnreadForRole("ADMIN");
        int teacherUnread = getUnreadForRole("TEACHER");

        all.setText(allUnread > 0 ? "All (" + allUnread + ")" : "All");
        admin.setText(adminUnread > 0 ? "Admins (" + adminUnread + ")" : "Admins");
        teacher.setText(teacherUnread > 0 ? "Teachers (" + teacherUnread + ")" : "Teachers");
    }

    private void sortAndApply() {
        // create a new sorted list instance and assign it to chatUserList
        List<ChatUser> sorted = new ArrayList<>(chatUserList);
        Collections.sort(sorted, (a, b) ->
                Long.compare(b.getLastMessageTime(), a.getLastMessageTime())
        );
        // replace the reference with the new list instance (important)
        chatUserList = sorted;
        // applyFilter will submit a fresh list to the adapter
        applyFilter();
    }

    private void selectedPill(View selected, View... others) {
        selected.setBackgroundResource(R.drawable.pill_selected);
        selected.animate().scaleX(1.05f).scaleY(1.05f).setDuration(120).start();

        for (View v : others) {
            v.setBackgroundResource(R.drawable.pill_unselected);
            v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // detach all chat listeners
        for (Map.Entry<String, ValueEventListener> e : chatListeners.entrySet()) {
            String chatId = e.getKey();
            ValueEventListener listener = e.getValue();
            DatabaseReference ref = chatRefs.get(chatId);
            if (ref != null && listener != null) {
                ref.removeEventListener(listener);
            }
        }
        chatListeners.clear();
        chatRefs.clear();
        chatListeningIds.clear();
    }
}