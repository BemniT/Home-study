package com.example.home_study;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.tom_roush.fontbox.ttf.IndexToLocationTable.TAG;

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

/**
 * ChatActivity — supports Parents, shows admin title, and ensures real-time preview updates.
 */
public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ChatListAdapter adapter;
    private ImageView imageBack;
    private TextView all, admin, teacher, parents;
    private List<ChatUser> chatUserList = new ArrayList<>();

    private final Set<String> loadedUserIds = new HashSet<>();
    private ChatCategory selectedCategory = ChatCategory.ALL;

    private DatabaseReference studentRef, coursesRef, assignmentRef, teachersRef, usersRef, parentsRef, adminsRef;

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
        parents = findViewById(R.id.ParentsCard); // new Parents pill (case-sensitive to your id)

        all.setOnClickListener(v -> {
            selectedCategory = ChatCategory.ALL;
            selectedPill(all, admin, teacher, parents);
            applyFilter();
        });
        admin.setOnClickListener(v -> {
            selectedCategory = ChatCategory.ADMIN;
            selectedPill(admin, all, teacher, parents);
            applyFilter();
        });
        teacher.setOnClickListener(v -> {
            selectedCategory = ChatCategory.TEACHER;
            selectedPill(teacher, all, admin, parents);
            applyFilter();
        });
        parents.setOnClickListener(v -> {
            selectedCategory = ChatCategory.PARENTS;
            selectedPill(parents, all, admin, teacher);
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
        parentsRef = FirebaseDatabase.getInstance().getReference("Parents");
        adminsRef = FirebaseDatabase.getInstance().getReference("School_Admins");

        loadAdmins();           // loads admin entries (title used)
        loadStudentTeachers();  // loads teachers and also parents for the student
    }

    public enum ChatCategory {
        ALL,
        ADMIN,
        TEACHER,
        PARENTS
    }

    private void applyFilter() {
        List<ChatUser> filtered = new ArrayList<>();
        for (ChatUser u : chatUserList) {
            if (selectedCategory == ChatCategory.ALL) {
                filtered.add(u);
            } else if (selectedCategory == ChatCategory.ADMIN && u.getRole() != null && !u.getRole().isEmpty() && isAdminRole(u)) {
                filtered.add(u);
            } else if (selectedCategory == ChatCategory.TEACHER && "TEACHER".equals(u.getRole())) {
                filtered.add(u);
            } else if (selectedCategory == ChatCategory.PARENTS && "PARENT".equals(u.getRole())) {
                filtered.add(u);
            }
        }
        adapter.submitList(new ArrayList<>(filtered));
    }

    // Helper to decide whether a ChatUser corresponds to an admin-type entry (we stored admin title in role)
    private boolean isAdminRole(ChatUser u) {
        if (u == null || u.getRole() == null) return false;
        // we treat roles that are not TEACHER/PARENT as admin titles (simple heuristic)
        String r = u.getRole();
        return !"TEACHER".equalsIgnoreCase(r) && !"PARENT".equalsIgnoreCase(r);
    }

    private void loadStudentTeachers() {
        String userId = Continuity.userId;

        studentRef.orderByChild("userId")
                .equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot studentSnap) {
                        if (!studentSnap.exists()) {
                            Log.e("chatActivity", "No student record found ");
                            progressBar.setVisibility(GONE);
                            return;
                        }

                        for (DataSnapshot snapshot : studentSnap.getChildren()) {
                            String grade = snapshot.child("grade").getValue(String.class);
                            String section = snapshot.child("section").getValue(String.class);

                            // Attempt to get studentId (for parent lookup)
                            String studentId = null;
                            if (snapshot.child("studentId").exists()) {
                                studentId = snapshot.child("studentId").getValue(String.class);
                            }

                            Log.d("chat_debug", "Student grade= " + grade + ", section= " + section + ", studentId=" + studentId);

                            loadCoursesForStudent(grade, section);

                            // If we have studentId, load parents; otherwise try to retrieve from Users node
                            if (studentId != null && !studentId.isEmpty()) {
                                loadParentsForStudent(studentId);
                            } else {
                                // fallback: read Users/currentUser for studentId field
                                usersRef.child(userId).get().addOnSuccessListener(uSnap -> {
                                    if (uSnap.exists() && uSnap.child("studentId").exists()) {
                                        String sid = uSnap.child("studentId").getValue(String.class);
                                        if (sid != null && !sid.isEmpty()) loadParentsForStudent(sid);
                                    }
                                });
                            }

                            break;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w("chatActivity", "student lookup cancelled", error.toException());
                        progressBar.setVisibility(GONE);
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
                    if (grade != null && section != null && grade.equals(courseGrade) && section.equals(courseSection)) {
                        String courseId = c.getKey();
                        String courseName = c.child("name").getValue(String.class);
                        loadTeachersForCourse(courseId, courseName);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("chatActivity", "Failed reading courses", error.toException());
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
                        Log.w("chatActivity", "Failed reading assignments", error.toException());
                    }
                });
    }

    private void resolveTeacher(String teacherId, String courseName) {
        if (teacherId == null) return;

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
                                        for (ChatUser cu : chatUserList) {
                                            if (userId.equals(cu.getUserId())) return;
                                        }

                                        if (loadedUserIds.contains(teacherId)) return;
                                        loadedUserIds.add(teacherId);

                                        ChatUser chatUser = new ChatUser(userId, name, profileImage, courseName, "TEACHER");
                                        // append
                                        List<ChatUser> newList = new ArrayList<>(chatUserList);
                                        newList.add(chatUser);
                                        chatUserList = newList;

                                        // Attach single chat listener per Chats/{chatId}
                                        listenForChatNode(chatUser);

                                        sortAndApply();
                                        progressBar.setVisibility(GONE);
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.w("chatActivity", "Failed reading user " + userId, error.toException());
                                    }
                                });
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w("chatActivity", "Failed reading teacher " + teacherId, error.toException());
                    }
                });
    }

    private void loadAdmins() {
        // School_Admins stores admin entries with title (position). We will read title and use it as role.
        adminsRef.get().addOnSuccessListener(snapshot -> {
            for (DataSnapshot adminSnap : snapshot.getChildren()) {
                String userId = adminSnap.child("userId").getValue(String.class);
                String title = adminSnap.child("title").getValue(String.class); // e.g., "Principal"
                if (userId == null) continue;

                // Avoid duplicates
                if (loadedUserIds.contains(userId)) {
                    continue;
                }
                loadedUserIds.add(userId);

                usersRef.child(userId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userShot) {
                                if (!userShot.exists()) return;
                                // Use title as role so UI displays actual position
                                ChatUser admin = new ChatUser(
                                        userId,
                                        userShot.child("name").getValue(String.class),
                                        userShot.child("profileImage").getValue(String.class),
                                        title != null ? title : "Management",
                                        title != null ? title : "Management"
                                );

                                // put admins at the top
                                List<ChatUser> newList = new ArrayList<>(chatUserList);
                                newList.add(0, admin);
                                chatUserList = newList;

                                listenForChatNode(admin);
                                sortAndApply();
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.w("chatActivity", "Failed reading admin user", error.toException());
                            }
                        });
            }
        }).addOnFailureListener(e -> Log.w("chatActivity", "Failed reading admins", e));
    }

    private void loadParentsForStudent(String studentId) {
        if (studentId == null || studentId.isEmpty()) return;

        // Fast path: query parents by studentId
        parentsRef.orderByChild("studentId").equalTo(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        if (snap.exists()) {
                            for (DataSnapshot p : snap.getChildren()) {
                                handleParentSnapshot(p, studentId);
                            }
                        } else {
                            // Fallback: scan all parents and match either studentId or children entries
                            parentsRef.get().addOnSuccessListener(allSnap -> {
                                for (DataSnapshot p2 : allSnap.getChildren()) {
                                    // if this parent already processed by fast path, skip
                                    handleParentSnapshot(p2, studentId);
                                }
                            }).addOnFailureListener(e -> Log.w(TAG, "Failed scanning Parents fallback", e));
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w(TAG, "parents query cancelled", error.toException());
                    }
                });
    }

    // helper to extract and add parent ChatUser if it matches studentId
    // in ChatActivity.java (replace existing handleParentSnapshot)
    // ChatActivity.java — replace existing handleParentSnapshot with this
    private void handleParentSnapshot(@NonNull DataSnapshot parentSnap, @NonNull String studentId) {
        try {
            // Debug: log raw snapshot to inspect schema when needed
            Log.d("chat_debug", "Parent raw: key=" + parentSnap.getKey() + " value=" + parentSnap.getValue());

            String parentUserId = parentSnap.child("userId").getValue(String.class);
            String rel = parentSnap.child("relationship").getValue(String.class);
            String parentStudentId = parentSnap.child("studentId").getValue(String.class);

            boolean matches = false;
            if (studentId.equals(parentStudentId)) matches = true;

            if (!matches && parentSnap.hasChild("children")) {
                DataSnapshot childrenNode = parentSnap.child("children");
                for (DataSnapshot childEntry : childrenNode.getChildren()) {
                    // childEntry may be a simple value or nested map — handle both
                    String childKey = childEntry.getKey();
                    Object childValObj = childEntry.getValue();
                    String childVal = childValObj instanceof String ? (String) childValObj : null;

                    // If the child entry is a map with nested fields, try to find a studentId inside it
                    if (childVal == null && childEntry.hasChild("studentId")) {
                        childVal = childEntry.child("studentId").getValue(String.class);
                    }

                    if (studentId.equals(childVal) || studentId.equals(childKey)) {
                        matches = true;
                        break;
                    }
                }
            }

            Log.d("chat_debug", "handleParentSnapshot: parentKey=" + parentSnap.getKey()
                    + " parentUserId=" + parentUserId + " matches=" + matches + " rel=" + rel);

            if (!matches) return;
            if (parentUserId == null) {
                Log.w("chat_debug", "parent has no userId, skipping: " + parentSnap.getKey());
                return;
            }

            // Ensure loadedUserIds holds Users.userId values only
            if (loadedUserIds.contains(parentUserId)) {
                Log.d("chat_debug", "parent already loaded (skipping): " + parentUserId);
                return;
            }
            loadedUserIds.add(parentUserId);

            usersRef.child(parentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot userSnap) {
                    if (!userSnap.exists()) {
                        Log.w("chat_debug", "Users/" + parentUserId + " does not exist");
                        return;
                    }
                    String name = userSnap.child("name").getValue(String.class);
                    String profileImage = userSnap.child("profileImage").getValue(String.class);

                    ChatUser parent = new ChatUser(
                            parentUserId,
                            name != null ? name : "Parent",
                            profileImage,
                            rel != null ? rel : "Parent", // subtitle shows relationship (Father/Mother)
                            "PARENT" // role used for filtering
                    );

                    // append parent and update list
                    List<ChatUser> newList = new ArrayList<>(chatUserList);
                    newList.add(parent);
                    chatUserList = newList;

                    listenForChatNode(parent);
                    sortAndApply();
                    Log.d("chat_debug", "Added parent userId=" + parentUserId + " name=" + name);
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {
                    Log.w("chat_debug", "Failed reading parent user " + parentUserId, error.toException());
                }
            });
        } catch (Exception ex) {
            Log.w("chat_debug", "handleParentSnapshot error", ex);
        }
    }
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
                String text = null;
                String senderId = null;
                boolean seen = false;
                long time = 0L;
                int unread = 0;

                if (snapshot.exists()) {
                    DataSnapshot last = snapshot.child("lastMessage");
                    if (last.exists()) {
                        // normal lastMessage structure
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
                    } else {
                        // fallback: iterate messages and pick latest by timestamp
                        DataSnapshot messagesNode = snapshot.child("messages");
                        if (messagesNode.exists()) {
                            DataSnapshot lastMsg = null;
                            long lastTs = Long.MIN_VALUE;
                            for (DataSnapshot m : messagesNode.getChildren()) {
                                Long t = m.child("timeStamp").getValue(Long.class);
                                if (t == null) {
                                    Integer ti = m.child("timeStamp").getValue(Integer.class);
                                    if (ti != null) t = ti.longValue();
                                    else {
                                        Long t2 = m.child("timestamp").getValue(Long.class);
                                        if (t2 != null) t = t2;
                                    }
                                }
                                long tm = t != null ? (t < 1_000_000_000_000L ? t * 1000L : t) : 0L;
                                if (tm >= lastTs) {
                                    lastTs = tm;
                                    lastMsg = m;
                                }
                            }
                            if (lastMsg != null) {
                                if (lastMsg.hasChild("text")) text = lastMsg.child("text").getValue(String.class);
                                else if (lastMsg.hasChild("message")) text = lastMsg.child("message").getValue(String.class);
                                else text = lastMsg.getValue(String.class);

                                senderId = lastMsg.child("senderId").getValue(String.class);

                                Object seenObj = lastMsg.child("seen").getValue();
                                if (seenObj instanceof Boolean) seen = (Boolean) seenObj;
                                else if (seenObj instanceof Long) seen = ((Long) seenObj) != 0L;
                                else if (seenObj instanceof Integer) seen = ((Integer) seenObj) != 0;

                                time = lastTs;
                            }
                        }
                    }

                    // unread map preferred
                    DataSnapshot unreadNode = snapshot.child("unread").child(Continuity.userId);
                    if (unreadNode.exists()) {
                        Long l = unreadNode.getValue(Long.class);
                        if (l == null) {
                            Integer i = unreadNode.getValue(Integer.class);
                            if (i != null) l = i.longValue();
                        }
                        if (l != null) unread = l.intValue();
                    } else {
                        // fallback: count messages addressed to current user and not seen
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

                    List<ChatUser> newList = new ArrayList<>(chatUserList);
                    newList.set(idx, updated);
                    chatUserList = newList;

                    sortAndApply();
                    updatePillBadges();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("chatActivity", "chat listener cancelled", error.toException());
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
            if (role == null || role.equalsIgnoreCase(u.getRole())) {
                total += u.getUnreadCount();
            }
        }
        return total;
    }

    private void updatePillBadges() {
        int allUnread = getUnreadForRole(null);
        int adminUnread = getUnreadForRole(null); // admins are included in the 'all' role; we compute by role title heuristic
        int teacherUnread = getUnreadForRole("TEACHER");
        int parentsUnread = getUnreadForRole("PARENT");

        all.setText(allUnread > 0 ? "All (" + allUnread + ")" : "All");
        admin.setText(adminUnread > 0 ? "Managements (" + adminUnread + ")" : "Managements");
        teacher.setText(teacherUnread > 0 ? "Teachers (" + teacherUnread + ")" : "Teachers");
        parents.setText(parentsUnread > 0 ? "Parents (" + parentsUnread + ")" : "Parents");
    }

    private void sortAndApply() {
        List<ChatUser> sorted = new ArrayList<>(chatUserList);
        Collections.sort(sorted, (a, b) ->
                Long.compare(b.getLastMessageTime(), a.getLastMessageTime())
        );
        chatUserList = sorted;
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