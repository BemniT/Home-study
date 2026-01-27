package com.example.home_study;

import android.animation.IntEvaluator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ExamContentActivity — loads chapters for a course and shows exam availability.
 *
 * Changes made:
 * - Show a centered illustration + text when there are no chapters (emptyStateContainer).
 * - Keep greeting rotation running but use creative, personalized messages when no chapters.
 * - Do not overwrite greeting with a plain "no chapters" message; greeting remains a friendly set.
 */
public class ExamContentActivity extends AppCompatActivity implements ExamContentAdapter.OnExamContentClickListener {

    private static final String TAG = "ExamContentActivity";
    private static final int REQUEST_OPEN_EXAM = 1234;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView headerTitle;
    private TextView emptyStateText;
    private View emptyStateContainer;
    private ImageView emptyStateImage;
    private ExamContentAdapter contentAdapter;
    private final List<ExamContent> examContentList = new ArrayList<>();

    // header / greeting
    private TextView welcomeText, subWelcome;
    private Handler greetingHandler;
    private Runnable greetingRunnable;
    private final List<String> greetingTemplates = new ArrayList<>();
    private int currentGreetingIndex = 0;
    private String studentName = "Student";
    private final long GREETING_INTERVAL_MS = 12000;
    private final long FADE_DURATION_MS = 300;

    // Firebase refs
    private DatabaseReference coursesRef;
    private DatabaseReference examsRef;
    private DatabaseReference resultsRef;

    // UI performance indicator (subject-level performance)
    private CircularProgressIndicator headerProgress;
    private TextView headerPercent;

    // inputs
    private String courseId;
    private String examBookTitle;

    // derived
    private String curriculumId; // e.g. biology_9
    private String curriculumSubject; // e.g. "biology"
    private String curriculumGrade;   // e.g. "9"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_content);

        recyclerView = findViewById(R.id.examContentRecycler);
        progressBar = findViewById(R.id.contentProgress);
        headerTitle = findViewById(R.id.littleTitle);
        emptyStateText = findViewById(R.id.emptyStateText);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        emptyStateImage = findViewById(R.id.emptyStateImage);
        headerProgress = findViewById(R.id.headerProgress);
        headerPercent = findViewById(R.id.headerPercent);
        welcomeText = findViewById(R.id.welcomeText);
        subWelcome = findViewById(R.id.subWelcome);

        studentName = Continuity.currentOnlineUser != null ? Continuity.currentOnlineUser.getName() : "Student";
        if (studentName == null || studentName.trim().isEmpty()) studentName = "Student";

        // Intent extras
        Intent intent = getIntent();
        courseId = intent != null ? intent.getStringExtra("courseId") : null;
        examBookTitle = intent != null ? intent.getStringExtra("examBookTitle") : null;
        if (examBookTitle == null && courseId != null) examBookTitle = courseId;

        headerTitle.setText(examBookTitle != null ? examBookTitle : "Chapters");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        contentAdapter = new ExamContentAdapter(examContentList, this);
        recyclerView.setAdapter(contentAdapter);

        coursesRef = FirebaseDatabase.getInstance().getReference("Courses");
        examsRef = FirebaseDatabase.getInstance().getReference("Exams");
        resultsRef = FirebaseDatabase.getInstance().getReference("ExamResults");

        // Start with neutral greeting list; we'll switch to chapter or empty messages once we know state
        prepareGreetingTemplatesAndStart(false);

        // start loading
        loadChapters();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_EXAM && resultCode == RESULT_OK) {
            // refresh to show updated scores
            loadChapters();
        }
    }

    private void loadChapters() {
        progressBar.setVisibility(View.VISIBLE);
        emptyStateContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        examContentList.clear();
        contentAdapter.notifyDataSetChanged();

        if (courseId != null && !courseId.trim().isEmpty()) {
            coursesRef.child(courseId).get().addOnSuccessListener(snapshot -> {
                if (!snapshot.exists()) {
                    Log.w(TAG, "Courses/" + courseId + " not found");
                    showNoChaptersState("Course not found");
                    return;
                }

                // extract grade and subject
                String grade = null;
                String subject = null;

                if (snapshot.child("grade").exists()) {
                    grade = snapshot.child("grade").getValue(String.class);
                } else if (snapshot.child("gradeLevel").exists()) {
                    grade = snapshot.child("gradeLevel").getValue(String.class);
                }

                if (snapshot.child("subject").exists()) {
                    subject = snapshot.child("subject").getValue(String.class);
                } else if (snapshot.child("name").exists()) {
                    subject = snapshot.child("name").getValue(String.class);
                }

                if (grade == null || subject == null) {
                    String storedCurr = snapshot.child("curriculumId").getValue(String.class);
                    if (storedCurr != null && !storedCurr.isEmpty()) {
                        curriculumId = storedCurr;
                        int idx = curriculumId.lastIndexOf("_");
                        if (idx > 0) {
                            curriculumSubject = curriculumId.substring(0, idx);
                            curriculumGrade = curriculumId.substring(idx + 1);
                        }
                    }
                } else {
                    curriculumGrade = grade.trim();
                    curriculumSubject = subject.trim().toLowerCase();
                    curriculumId = curriculumSubject + "_" + curriculumGrade;
                }

                if (curriculumGrade == null || curriculumSubject == null) {
                    Log.w(TAG, "Could not derive curriculum for course " + courseId);
                    showNoChaptersState("Course metadata incomplete");
                    return;
                }

                loadChaptersFromCurriculum(curriculumGrade, curriculumSubject);
                loadSubjectPerformance(curriculumId);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed reading course " + courseId, e);
                showNoChaptersState("Failed to load course info");
            });
        } else {
            showNoChaptersState("No course specified");
        }
    }

    private void loadChaptersFromCurriculum(String grade, String subject) {
        if (grade == null || subject == null) {
            showNoChaptersState("Invalid curriculum path");
            return;
        }

        String gradeNode = "grade_" + grade;
        DatabaseReference chaptersRef = FirebaseDatabase.getInstance()
                .getReference("Curriculum")
                .child(gradeNode)
                .child(subject)
                .child("chapters");

        chaptersRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                progressBar.setVisibility(View.GONE);
                Log.i(TAG, "No chapters found at Curriculum/" + gradeNode + "/" + subject + "/chapters");
                showNoChaptersState("No chapters yet");
                return;
            }

            List<DataSnapshot> chapters = new ArrayList<>();
            for (DataSnapshot s : snapshot.getChildren()) chapters.add(s);

            chapters.sort((a, b) -> {
                Long oa = a.child("order").getValue(Long.class);
                Long ob = b.child("order").getValue(Long.class);
                oa = oa != null ? oa : 0L;
                ob = ob != null ? ob : 0L;
                return Long.compare(oa, ob);
            });

            final AtomicInteger remaining = new AtomicInteger(chapters.size());
            examContentList.clear();
            contentAdapter.notifyDataSetChanged();

            for (DataSnapshot chapterSnap : chapters) {
                buildExamContentFromChapter(chapterSnap, remaining);
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Log.e(TAG, "Failed reading curriculum chapters", e);
            showNoChaptersState("Failed to load chapters");
        });
    }

    private void buildExamContentFromChapter(DataSnapshot chapterSnap, AtomicInteger remaining) {
        if (chapterSnap == null || !chapterSnap.exists()) {
            if (remaining.decrementAndGet() == 0) finalizeChaptersLoading();
            return;
        }

        final String chapterId = chapterSnap.child("id").getValue(String.class) != null
                ? chapterSnap.child("id").getValue(String.class)
                : chapterSnap.getKey();

        final String title = chapterSnap.child("title").getValue(String.class);
        final boolean hasExam = chapterSnap.child("hasExam").getValue(Boolean.class) != null
                ? chapterSnap.child("hasExam").getValue(Boolean.class)
                : false;

        final ExamContent item = new ExamContent(chapterId, title != null ? title : chapterId, hasExam);

        if (curriculumId == null || chapterId == null) {
            examContentList.add(item);
            contentAdapter.notifyItemInserted(examContentList.size() - 1);
            if (remaining.decrementAndGet() == 0) finalizeChaptersLoading();
            return;
        }

        DatabaseReference examMetaRef = examsRef.child(curriculumId).child(chapterId);
        examMetaRef.get().addOnSuccessListener(examSnap -> {
            if (examSnap.exists()) {
                Long duration = examSnap.child("durationMinutes").getValue(Long.class);
                Long totalQ = examSnap.child("totalQuestions").getValue(Long.class);
                Long pass = examSnap.child("passScore").getValue(Long.class);

                Boolean published = examSnap.child("published").getValue(Boolean.class);
                if (published == null) {
                    String pubStr = examSnap.child("published").getValue(String.class);
                    if (pubStr != null) published = Boolean.parseBoolean(pubStr);
                }
                if (published == null) {
                    Long pubNum = examSnap.child("published").getValue(Long.class);
                    if (pubNum != null) published = pubNum != 0;
                }
                if (published == null) published = false;

                item.setHasExam(true);
                item.setExamPublished(published);
                item.setDurationMinutes(duration != null ? duration.intValue() : 0);
                item.setTotalQuestions(totalQ != null ? totalQ.intValue() : 0);
                item.setPassScore(pass != null ? pass.intValue() : 0);
            } else {
                item.setHasExam(hasExam);
                item.setExamPublished(false);
            }

            String uid = Continuity.userId;
            final String resultKey = curriculumId + "_" + chapterId;
            if (uid != null && !uid.isEmpty()) {
                resultsRef.child(uid).child(resultKey).get().addOnSuccessListener(resultSnap -> {
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

                    if (remaining.decrementAndGet() == 0) finalizeChaptersLoading();
                }).addOnFailureListener(e -> {
                    examContentList.add(item);
                    contentAdapter.notifyItemInserted(examContentList.size() - 1);
                    if (remaining.decrementAndGet() == 0) finalizeChaptersLoading();
                });
            } else {
                examContentList.add(item);
                contentAdapter.notifyItemInserted(examContentList.size() - 1);
                if (remaining.decrementAndGet() == 0) finalizeChaptersLoading();
            }
        }).addOnFailureListener(e -> {
            Log.w(TAG, "Failed reading Exams/" + curriculumId + "/" + chapterId + " : " + e.getMessage());
            item.setHasExam(hasExam);
            item.setExamPublished(false);
            examContentList.add(item);
            contentAdapter.notifyItemInserted(examContentList.size() - 1);
            if (remaining.decrementAndGet() == 0) finalizeChaptersLoading();
        });
    }

    private void finalizeChaptersLoading() {
        progressBar.setVisibility(View.GONE);

        if (examContentList.isEmpty()) {
            showNoChaptersState("No chapters available after metadata checks");
            return;
        }

        // chapters present
        emptyStateContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        contentAdapter.notifyDataSetChanged();

        // start normal rotating greetings
        prepareGreetingTemplatesAndStart(true);
    }

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

        Intent intent = new Intent(this, ExamCenterActivity.class);
        intent.putExtra("examKey", curriculumId + "_" + content.getChapterId());
        intent.putExtra("chapterTitle", content.getTitle());
        intent.putExtra("durationMinutes", content.getDurationMinutes());
        intent.putExtra("totalQuestions", content.getTotalQuestions());
        intent.putExtra("passScore", content.getPassScore());
        startActivityForResult(intent, REQUEST_OPEN_EXAM);
    }

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
                String examKey = examSnap.getKey();
                if (examKey == null) continue;
                if (!examKey.startsWith(curriculumId + "_")) continue;

                Integer score = examSnap.child("score").getValue(Integer.class);
                Integer total = examSnap.child("total").getValue(Integer.class);
                if (total == null) {
                    total = examSnap.child("totalQuestions").getValue(Integer.class);
                    if (total == null) total = examSnap.child("totalQuestion").getValue(Integer.class);
                }

                if (score != null && total != null && total > 0) {
                    sumScore += score;
                    sumTotal += total;
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
            Log.w(TAG, "Failed reading user results for performance", e);
            updateHeaderProgress(0);
        });
    }

    private void updateHeaderProgress(int percent) {
        if (headerProgress == null || headerPercent == null) return;
        headerPercent.setText(percent + "%");
        int start = headerProgress.getProgress();
        ObjectAnimator anim = ObjectAnimator.ofInt(headerProgress, "progress", start, percent);
        anim.setDuration(600);
        anim.setEvaluator(new IntEvaluator());
        anim.start();
    }

    private void showNoChaptersState(String reason) {
        Log.i(TAG, "Empty chapters state: " + reason);
        progressBar.setVisibility(View.GONE);
        examContentList.clear();
        contentAdapter.notifyDataSetChanged();

        // show illustration + message
        emptyStateContainer.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        // set a friendly illustration message (kept separate from rotating greetings)
        if (emptyStateText != null) {
            emptyStateText.setText("No chapters yet — new lessons are on the way! Check back soon.");
        }

        // use creative personalized greeting messages (but don't show plain 'no chapters' as a bland header)
        prepareGreetingTemplatesAndStart(false);

        updateHeaderProgress(0);
    }

    private void stopGreetingRotation() {
        if (greetingHandler != null && greetingRunnable != null) {
            greetingHandler.removeCallbacks(greetingRunnable);
        }
    }

    /**
     * prepareGreetingTemplatesAndStart(true) -> normal rotating greetings
     * prepareGreetingTemplatesAndStart(false) -> friendly personalized greetings when no chapters
     */
    private void prepareGreetingTemplatesAndStart(boolean hasChapters) {
        stopGreetingRotation();
        greetingTemplates.clear();

        if (hasChapters) {
            greetingTemplates.add("Hello " + studentName + " 👋 Here are the chapters");
            greetingTemplates.add("Ready to explore " + (examBookTitle != null ? examBookTitle : "your subject") + ", " + studentName + "?");
            greetingTemplates.add("Keep it up, " + studentName + " — you're doing great!");
            greetingTemplates.add("Let's sharpen those skills, " + studentName + "!");
            subWelcome.setVisibility(View.VISIBLE);
        } else {
            // friendly, creative messages for empty state (still greeting-like)
            greetingTemplates.add("Hey " + studentName + " 👋 — we're preparing lessons for you. Check back soon!");
            greetingTemplates.add("No chapters available yet. Want to suggest a topic to your teacher?");
            greetingTemplates.add("Good things take time. New chapters for " + (examBookTitle != null ? examBookTitle : "your subject") + " will arrive soon.");
            subWelcome.setVisibility(View.GONE);

        }

        if (greetingHandler == null) greetingHandler = new Handler(Looper.getMainLooper());
        currentGreetingIndex = 0;
        if (welcomeText != null && !greetingTemplates.isEmpty()) welcomeText.setText(greetingTemplates.get(0));

        greetingRunnable = new Runnable() {
            @Override
            public void run() {
                if (welcomeText == null || greetingTemplates.isEmpty()) return;
                currentGreetingIndex = (currentGreetingIndex + 1) % greetingTemplates.size();
                String nextText = greetingTemplates.get(currentGreetingIndex);
                welcomeText.animate().alpha(0f).setDuration(FADE_DURATION_MS).withEndAction(() -> {
                    welcomeText.setText(nextText);
                    welcomeText.animate().alpha(1f).setDuration(FADE_DURATION_MS).start();
                }).start();
                greetingHandler.postDelayed(this, GREETING_INTERVAL_MS);
            }
        };

        greetingHandler.removeCallbacks(greetingRunnable);
        greetingHandler.postDelayed(greetingRunnable, GREETING_INTERVAL_MS);
    }

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

    // Keep the rest of your existing methods (loadChaptersFromCurriculum, buildExamContentFromChapter, parse helpers, etc.)
}