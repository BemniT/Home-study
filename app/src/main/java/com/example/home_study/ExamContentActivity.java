package com.example.home_study;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.Adapter.ExamContentAdapter;
import com.example.home_study.Model.ExamContent;
import com.example.home_study.Prevalent.Continuity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.animation.ObjectAnimator;
import android.animation.IntEvaluator;
import com.google.android.material.progressindicator.CircularProgressIndicator;
// ... other imports



/**
 * Loads chapters for a course and shows exam availability. Clickable items open ExamCenterActivity.
 *
 * Expects intent extras:
 *  - "courseId" (preferred): the database key / course id stored on Courses
 *  - "examBookTitle" (optional): human-friendly subject title used as fallback/static demo
 */
public class ExamContentActivity extends AppCompatActivity implements ExamContentAdapter.OnExamContentClickListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView headerTitle, emptyState;
    private ExamContentAdapter contentAdapter;
    private List<ExamContent> examContentList = new ArrayList<>();

    private TextView welcomeText;         // binds to R.id.welcomeText in your header
    private Handler greetingHandler;
    private Runnable greetingRunnable;
    private List<String> greetingTemplates = new ArrayList<>();
    private int currentGreetingIndex = 0;
    private String studentName = "Student"; // fallback
    private final long GREETING_INTERVAL_MS = 20000; // change interval if you like
    private final long FADE_DURATION_MS = 300;
    private DatabaseReference chaptersRef;
    private DatabaseReference examsRef;
    private DatabaseReference resultsRef;
    private CircularProgressIndicator headerProgress;
    private TextView headerPercent;
    private DatabaseReference coursesRef;  // Updated: Now queries Courses directly for curriculumId

    private String courseId;          // database courseId (preferred)
    private String examBookTitle;     // fallback human title
    private String curriculumId;      // derived from Courses

    // track user answers keyed by questionId


// Firebase results ref (you already have resultsRef, reuse it)

    @Override
    protected void onPause() {
        super.onPause();
        stopGreetingRotation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopGreetingRotation();
    }

    private void stopGreetingRotation() {
        if (greetingHandler != null && greetingRunnable != null) {
            greetingHandler.removeCallbacks(greetingRunnable);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_content);

        recyclerView = findViewById(R.id.examContentRecycler);
        progressBar = findViewById(R.id.contentProgress);
        headerTitle = findViewById(R.id.littleTitle);
        emptyState = findViewById(R.id.emptyStateText);
        headerProgress = findViewById(R.id.headerProgress);
        headerPercent = findViewById(R.id.headerPercent);
        welcomeText = findViewById(R.id.welcomeText);

        studentName = Continuity.currentOnlineUser.getName();

        if (studentName.isEmpty()){
            studentName.equals("Student");
        }else {
            prepareGreetingTemplatesAndStart();
        }

        // Intent extras
        Intent intent = getIntent();
        courseId = intent.getStringExtra("courseId");
        examBookTitle = intent.getStringExtra("examBookTitle");

        if (examBookTitle == null && courseId != null) {
            // You may derive a friendly title from courseId if desired
            examBookTitle = courseId;
        }
        headerTitle.setText(examBookTitle != null ? examBookTitle : "Chapters");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        contentAdapter = new ExamContentAdapter(examContentList, this);
        recyclerView.setAdapter(contentAdapter);

        // Firebase refs
        coursesRef = FirebaseDatabase.getInstance().getReference("Courses");  // Updated: Direct query to Courses
        examsRef = FirebaseDatabase.getInstance().getReference("Exams");
        resultsRef = FirebaseDatabase.getInstance().getReference("ExamResults");

        // start loading
        loadChapters();
    }

    private void loadChapters() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        if (courseId != null && !courseId.isEmpty()) {
            // Updated: Query Courses directly for curriculumId (no more CourseChapters)
            coursesRef.child(courseId).get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    curriculumId = snapshot.child("curriculumId").getValue(String.class);
                    if (curriculumId != null) {
                        loadChaptersFromCurriculum(curriculumId);
                        loadSubjectPerformance(curriculumId);

                    } else {
                        showEmpty();
                    }
                } else {
                    showEmpty();
                }
            }).addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ExamContentActivity.this, "Failed to load course info", Toast.LENGTH_SHORT).show();
            });
        } else {
            // No courseId: fallback to static or by title. Here we fallback to static demo list as before.
            loadStaticChapters(examBookTitle);
            progressBar.setVisibility(View.GONE);
            contentAdapter.notifyDataSetChanged();
            if (examContentList.isEmpty()) showEmpty();
        }
    }

    private void loadChaptersFromCurriculum(String curriculumId) {
        DatabaseReference subjectChaptersRef = FirebaseDatabase.getInstance().getReference("SubjectChapters").child(curriculumId);
        subjectChaptersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                examContentList.clear();
                if (!snapshot.exists()) {
                    showEmpty();
                    return;
                }
                List<DataSnapshot> chapters = new ArrayList<>();
                for (DataSnapshot s : snapshot.getChildren()) chapters.add(s);

                // Sort by order
                chapters.sort((a, b) -> {
                    Long orderA = a.child("order").getValue(Long.class);
                    Long orderB = b.child("order").getValue(Long.class);
                    return Long.compare(orderA != null ? orderA : 0, orderB != null ? orderB : 0);
                });

                for (DataSnapshot chapterSnap : chapters) {
                    buildExamContentFromChapter(chapterSnap, curriculumId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ExamContentActivity.this, "Failed to load chapters", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ... (rest of the file remains the same)

    private void buildExamContentFromChapter(DataSnapshot chapterSnap, String curriculumId) {
        if (!chapterSnap.exists()) return;

        final String chapterId = chapterSnap.getKey();
        final String title = chapterSnap.child("title").getValue(String.class);
        final boolean hasExam = chapterSnap.child("hasExam").getValue(Boolean.class) != null
                ? chapterSnap.child("hasExam").getValue(Boolean.class)
                : false;

        final ExamContent item = new ExamContent(chapterId, title != null ? title : chapterId, hasExam);

        // read exam meta from Exams/{curriculumId}_{chapterId}
        final String examKey = curriculumId + "_" + chapterId;
        Log.d("ExamContentActivity", "Looking up examKey=" + examKey);

        examsRef.child(examKey).get().addOnSuccessListener(examSnap -> {
            Log.d("ExamContentActivity", "examSnap.exists=" + examSnap.exists() + " value=" + examSnap.getValue());
            if (examSnap.exists()) {
                // duration
                Long duration = examSnap.child("durationMinutes").getValue(Long.class);

                // totalQuestions: try common variants in case of typos
                Long totalQ = examSnap.child("totalQuestion").getValue(Long.class);
                if (totalQ == null) totalQ = examSnap.child("totalQuestion").getValue(Long.class);

                Long pass = examSnap.child("passScore").getValue(Long.class);

                String chapterImage = examSnap.child("chapterImage").getValue(String.class);
                if (chapterImage == null) {
                    // optionally fallback to chapter node image if you store it there:
                    chapterImage = chapterSnap.child("image").getValue(String.class);
                }
                if (chapterImage != null) {
                    item.setChapterImage(chapterImage);
                }
                // published: handle Boolean, String ("true"/"false"), or number (1/0)
                Boolean published = examSnap.child("published").getValue(Boolean.class);
                if (published == null) {
                    String pubStr = examSnap.child("published").getValue(String.class);
                    if (pubStr != null) published = Boolean.parseBoolean(pubStr);
                }
                if (published == null) {
                    Long pubNum = examSnap.child("published").getValue(Long.class);
                    if (pubNum != null) published = pubNum != 0;
                }

                // If still null, default to false but log so you can fix DB
                if (published == null) {
                    published = false;
                    Log.w("ExamContentActivity", "published field missing or unrecognized type for " + examKey);
                }

                item.setHasExam(true);
                item.setExamPublished(published);
                item.setDurationMinutes(duration != null ? duration.intValue() : 0);
                item.setTotalQuestions(totalQ != null ? totalQ.intValue() : 0);
                item.setPassScore(pass != null ? pass.intValue() : 0);
            } else {
                // exam node missing — fall back to chapter's hasExam flag
                item.setHasExam(hasExam);
                item.setExamPublished(false);
                Log.d("ExamContentActivity", "No exam node for " + examKey + " (falling back to chapter.hasExam=" + hasExam + ")");
            }

            // Read user result ExamResults/{userId}/{examKey}
            String uid = Continuity.userId;
            if (uid != null) {
                resultsRef.child(uid).child(examKey).get().addOnSuccessListener(resultSnap -> {
                    if (resultSnap.exists()) {
                        Integer score = resultSnap.child("score").getValue(Integer.class);
                        Integer total = resultSnap.child("total").getValue(Integer.class);
                        Boolean passed = resultSnap.child("passed").getValue(Boolean.class);
                        Long takenAt = resultSnap.child("takenAt").getValue(Long.class);

                        item.setUserTaken(true);
                        if (score != null) item.setUserScore(score);
                        if (total != null) item.setUserTotal(total);
                        item.setUserPassed(passed != null ? passed : false);
                        item.setTakenAt(takenAt != null ? takenAt : 0L);
                    }
                    examContentList.add(item);
                    contentAdapter.notifyItemInserted(examContentList.size() - 1);
                    progressBar.setVisibility(View.GONE);
                    emptyState.setVisibility(examContentList.isEmpty() ? View.VISIBLE : View.GONE);
                }).addOnFailureListener(e -> {
                    examContentList.add(item);
                    contentAdapter.notifyItemInserted(examContentList.size() - 1);
                    progressBar.setVisibility(View.GONE);
                    emptyState.setVisibility(examContentList.isEmpty() ? View.VISIBLE : View.GONE);
                });
            } else {
                examContentList.add(item);
                contentAdapter.notifyItemInserted(examContentList.size() - 1);
                progressBar.setVisibility(View.GONE);
                emptyState.setVisibility(examContentList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }).addOnFailureListener(e -> {
            Log.e("ExamContentActivity", "Failed reading Exams/" + examKey, e);
            item.setHasExam(hasExam);
            item.setExamPublished(false);
            examContentList.add(item);
            contentAdapter.notifyItemInserted(examContentList.size() - 1);
            progressBar.setVisibility(View.GONE);
            emptyState.setVisibility(examContentList.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }
    // ... (rest of the file remains the same)
    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        examContentList.clear();
        contentAdapter.notifyDataSetChanged();
        emptyState.setVisibility(View.VISIBLE);
    }

    /**
     * Fallback static list (keeps compatibility with your previous static demo behavior)
     */
    private void loadStaticChapters(String bookTitle) {
        examContentList.clear();
        if (bookTitle == null) return;
        if (bookTitle.equalsIgnoreCase("Biology")) {
            examContentList.add(new ExamContent("chapter_01", "Introduction to Biology", true));
            examContentList.add(new ExamContent("chapter_02", "Cell Structure and Function", true));
            examContentList.add(new ExamContent("chapter_03", "Genetics", true));
        } else if (bookTitle.equalsIgnoreCase("Physics")) {
            examContentList.add(new ExamContent("chapter_p1", "Introduction to Physics", true));
            examContentList.add(new ExamContent("chapter_p2", "Motion and Forces", true));
            examContentList.add(new ExamContent("chapter_p3", "Energy and Work", true));
        }
    }

    /**
     * Called by adapter when user taps a chapter.
     * Opens ExamCenterActivity if exam is available/published.
     */
    @Override
    public void onExamContentSelected(ExamContent content) {
        if (!content.hasExam()) {
            Toast.makeText(this, "No exam for this chapter", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!content.isExamPublished()) {
            Toast.makeText(this, "Exam is not published yet", Toast.LENGTH_SHORT).show();
            return;
        }

        // pass useful extras to ExamCenterActivity
        Intent intent = new Intent(this, ExamCenterActivity.class);
        intent.putExtra("examKey", curriculumId + "_" + content.getChapterId());
        intent.putExtra("chapterTitle", content.getTitle());
        intent.putExtra("durationMinutes", content.getDurationMinutes());
        intent.putExtra("totalQuestions", content.getTotalQuestions());
        intent.putExtra("passScore", content.getPassScore());
        startActivity(intent);
    }

    /**
     * Load performance for the given curriculum (only exam results whose examKey starts with curriculumId + "_")
     * Summation uses result nodes that include both 'score' and 'total' (or 'totalQuestions').
     */
    private void loadSubjectPerformance(String curriculumId) {
        if (curriculumId == null || curriculumId.trim().isEmpty()) {
            updateHeaderProgress(0);
            return;
        }
        String uid = Continuity.userId;
        if (uid == null || uid.trim().isEmpty()) {
            updateHeaderProgress(0);
            return;
        }

        DatabaseReference userResultsRef = resultsRef.child(uid);
        userResultsRef.get().addOnSuccessListener(snapshot -> {
            long sumScore = 0;
            long sumTotal = 0;

            if (!snapshot.exists()) {
                updateHeaderProgress(0);
                return;
            }

            for (DataSnapshot examSnap : snapshot.getChildren()) {
                String examKey = examSnap.getKey(); // e.g. biology_10_chapter_01
                if (examKey == null) continue;
                if (!examKey.startsWith(curriculumId + "_")) continue; // only current curriculum

                Integer score = examSnap.child("score").getValue(Integer.class);
                Integer total = examSnap.child("total").getValue(Integer.class);
                if (total == null) {
                    total = examSnap.child("totalQuestions").getValue(Integer.class);
                    if (total == null) total = examSnap.child("totalQuestion").getValue(Integer.class);
                }

                if (score != null && total != null && total > 0) {
                    sumScore += score;
                    sumTotal += total;
                } else if (score != null && (total == null || total == 0)) {
                    // If no total available, try to query Exams/{examKey}/totalQuestions (optional, async).
                    // For simplicity we skip these entries here; they won't contribute to sumTotal.
                    // Alternatively, you can fetch exam metadata to get totalQuestions.
                }
            }

            int percent = 0;
            if (sumTotal > 0) {
                percent = (int) Math.round((sumScore * 100.0) / sumTotal);
                percent = Math.max(0, Math.min(100, percent));
            } else {
                percent = 0;
            }
            updateHeaderProgress(percent);
        }).addOnFailureListener(e -> {
            Log.w("ExamContentActivity", "Failed reading user results for performance", e);
            updateHeaderProgress(0);
        });
    }

    /** Animate and update the CircularProgressIndicator and the percent label */
    private void updateHeaderProgress(int percent) {
        if (headerProgress == null || headerPercent == null) return;

        headerPercent.setText(percent + "%");

        // animate progress from current value to target
        int start = headerProgress.getProgress();
        ObjectAnimator anim = ObjectAnimator.ofInt(headerProgress, "progress", start, percent);
        anim.setDuration(600);
        anim.setEvaluator(new IntEvaluator());
        anim.start();
    }

    private void prepareGreetingTemplatesAndStart() {
        // Example templates — change wording as you like.
        greetingTemplates.clear();
        greetingTemplates.add("Hey " + studentName + " 👋");
        greetingTemplates.add("Ready to explore " + (examBookTitle != null ? examBookTitle : "your subject") + ", " + studentName + "?");
        greetingTemplates.add("Keep it up, " + studentName + " — you're doing great!");
        greetingTemplates.add("Let's sharpen those skills, " + studentName + "!");
        // shuffle order if you want randomness:
        // Collections.shuffle(greetingTemplates);

        // initialize handler and runnable
        if (greetingHandler == null) greetingHandler = new Handler(Looper.getMainLooper());
        currentGreetingIndex = 0;

        // show first greeting immediately (no animation)
        if (welcomeText != null && !greetingTemplates.isEmpty()) {
            welcomeText.setText(greetingTemplates.get(0));
        }

        // define runnable
        greetingRunnable = new Runnable() {
            @Override
            public void run() {
                if (welcomeText == null || greetingTemplates.isEmpty()) return;

                // next index
                currentGreetingIndex = (currentGreetingIndex + 1) % greetingTemplates.size();
                String nextText = greetingTemplates.get(currentGreetingIndex);

                // animate: fade out -> change -> fade in
                welcomeText.animate()
                        .alpha(0f)
                        .setDuration(FADE_DURATION_MS)
                        .setInterpolator(new DecelerateInterpolator())
                        .withEndAction(() -> {
                            welcomeText.setText(nextText);
                            welcomeText.animate()
                                    .alpha(1f)
                                    .setDuration(FADE_DURATION_MS)
                                    .setInterpolator(new DecelerateInterpolator())
                                    .start();
                        })
                        .start();

                // schedule next
                greetingHandler.postDelayed(this, GREETING_INTERVAL_MS);
            }
        };

        // start repeating after interval
        greetingHandler.removeCallbacks(greetingRunnable);
        greetingHandler.postDelayed(greetingRunnable, GREETING_INTERVAL_MS);
    }
}