package com.example.home_study;

import android.animation.IntEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.Model.ExamCenter;
import com.example.home_study.Prevalent.Continuity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ExamCenterActivity — explanation header is collapsible by the user with an expand icon that rotates.
 *
 * Changes:
 * - Binds ImageView expandExplanation (id=@+id/expandExplanation)
 * - Tapping either the explanationHeaderText or the expand icon toggles collapse/expand
 * - Icon rotates to indicate expand/collapse state
 */
public class ExamCenterActivity extends AppCompatActivity {

    private static final String TAG = "ExamCenterActivity";

    // Top bar + meta
    private MaterialToolbar toolbar;
    private ImageView imageBackBtn;
    private TextView examChapterTitle;
    private TextView questionCounterTv;
    private TextView timerText;

    // Content
    private NestedScrollView contentScroll;
    private TextView questionTv;
    private RecyclerView optionsRecycler;
    private OptionAdapter optionAdapter;
    private LinearProgressIndicator progressLinear;
    private View skeletonContainer;
    private CircularProgressIndicator loadingIndicator;

    // Explanation area (inside card)
    private MaterialCardView explanationCard;
    private TextView explanationHeaderText;
    private TextView explanationBody;
    private ImageView expandExplanation; // new expand icon
    private boolean explanationExpanded = false;

    // Navigation
    private MaterialButton nextBtn;
    private MaterialButton previousBtn;

    // Data
    private final List<ExamCenter> questionList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private final Map<String, String> userAnswers = new HashMap<>(); // qId -> selectedText
    private boolean optionsLocked = false;
    private boolean hasAnsweredCurrentQuestion = false;
    private boolean examSubmitted = false;

    // Firebase
    private DatabaseReference examsRef;

    // exam identifiers / metadata
    private String examKey;
    private String curriculumId;
    private String chapterId;
    private String chapterTitleExtra;
    private String selectedSubject;
    private String selectedChapter;
    private int examPassScore = 0;
    private int examTotalQuestions = 0;
    private int examDurationMinutes = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_exam_center);

        bindViews();
        setupUi();
        examsRef = FirebaseDatabase.getInstance().getReference("Exams");

        Intent intent = getIntent();
        if (intent != null) {
            examKey = intent.getStringExtra("examKey");
            selectedSubject = intent.getStringExtra("subject");
            selectedChapter = intent.getStringExtra("chapter");
            chapterTitleExtra = intent.getStringExtra("chapterTitle");
            examPassScore = intent.getIntExtra("passScore", 0);
            examTotalQuestions = intent.getIntExtra("totalQuestions", 0);
            examDurationMinutes = intent.getIntExtra("durationMinutes", 0);
        }

        if (chapterTitleExtra != null && !chapterTitleExtra.trim().isEmpty()) {
            examChapterTitle.setText(chapterTitleExtra.trim());
        }

        parseExamKey();

        if ((examChapterTitle.getText() == null || examChapterTitle.getText().toString().isEmpty()) && chapterId != null) {
            examChapterTitle.setText(chapterId);
        }

        showSkeleton(true);

        if ((examKey == null || examKey.trim().isEmpty()) &&
                (selectedSubject == null || selectedSubject.trim().isEmpty() ||
                        selectedChapter == null || selectedChapter.trim().isEmpty())) {
            Toast.makeText(this, "Missing exam information. Please open the exam from the course screen.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        resolveAndFetchQuestions();

        nextBtn.setOnClickListener(v -> {
            if (currentQuestionIndex < questionList.size() - 1) {
                currentQuestionIndex++;
                displayQuestion();
            } else {
                if (!examSubmitted) confirmAndSubmitExam();
                else Toast.makeText(this, "Exam submitted — review mode.", Toast.LENGTH_SHORT).show();
            }
        });

        previousBtn.setOnClickListener(v -> {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--;
                displayQuestion();
            }
        });

        imageBackBtn.setOnClickListener(v -> {
            if (examSubmitted) {
                Intent out = new Intent();
                out.putExtra("examKey", examKey);
                setResult(RESULT_OK, out);
            }
            finish();
        });
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        // toggle explanation by tapping header text or expand icon
        View.OnClickListener toggleExplanation = v -> {
            // Only allow toggling when explanation card is visible AND (user answered OR exam submitted)
            if (explanationCard.getVisibility() != View.VISIBLE) return;
            if (!hasAnsweredCurrentQuestion && !examSubmitted) return;
            if (explanationExpanded) collapseExplanation();
            else expandExplanation();
        };
        explanationHeaderText.setOnClickListener(toggleExplanation);
        expandExplanation.setOnClickListener(toggleExplanation);
    }

    private void bindViews() {
        toolbar = findViewById(R.id.examCenterToolBar);
        imageBackBtn = findViewById(R.id.imageBackBtn);
        examChapterTitle = findViewById(R.id.examChapterTitle);
        questionCounterTv = findViewById(R.id.questionCounter);
//        timerText = findViewById(R.id.timerText);

        contentScroll = findViewById(R.id.contentScroll);
        questionTv = findViewById(R.id.question);
        optionsRecycler = findViewById(R.id.optionsRecycler);
        progressLinear = findViewById(R.id.progressLinear);
        skeletonContainer = findViewById(R.id.skeletonContainer);
        loadingIndicator = findViewById(R.id.loadingIndicator);

        explanationCard = findViewById(R.id.explanationCard);
        explanationHeaderText = findViewById(R.id.explanationHeaderText);
        explanationBody = findViewById(R.id.explanationBody);
        expandExplanation = findViewById(R.id.expandExplanation); // bind the icon

        nextBtn = findViewById(R.id.nextBtn);
        previousBtn = findViewById(R.id.previousBtn);
    }

    private void setupUi() {
        optionAdapter = new OptionAdapter();
        optionsRecycler.setLayoutManager(new LinearLayoutManager(this));
        optionsRecycler.setAdapter(optionAdapter);
        optionsRecycler.setNestedScrollingEnabled(false);

        explanationCard.setVisibility(View.GONE);
        explanationBody.setVisibility(View.GONE);
        explanationExpanded = false;
        if (expandExplanation != null) expandExplanation.setRotation(0f); // default

        progressLinear.setProgress(0);
    }

    private void parseExamKey() {
        if (examKey == null) return;
        if (examKey.contains("_chapter_")) {
            int idx = examKey.indexOf("_chapter_");
            curriculumId = examKey.substring(0, idx);
            chapterId = examKey.substring(idx + 1);
        } else {
            int last = examKey.lastIndexOf('_');
            if (last > 0) {
                curriculumId = examKey.substring(0, last);
                chapterId = examKey.substring(last + 1);
            } else {
                curriculumId = examKey;
                chapterId = null;
            }
        }
        Log.d(TAG, "Parsed examKey=" + examKey + " -> curriculumId=" + curriculumId + " chapterId=" + chapterId);
    }

    private void resolveAndFetchQuestions() {
        loadingIndicator.setVisibility(View.VISIBLE);
        showSkeleton(true);

        if (curriculumId != null && chapterId != null) {
            DatabaseReference questionsNode = examsRef.child(curriculumId).child(chapterId).child("questions");
            questionsNode.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) fetchQuestionsFrom(questionsNode);
                else {
                    DatabaseReference candidate = examsRef.child(curriculumId).child(chapterId);
                    candidate.get().addOnSuccessListener(snap2 -> {
                        if (snap2.exists()) {
                            boolean looksLikeQuestions = false;
                            for (DataSnapshot child : snap2.getChildren()) {
                                if (child.child("question").exists() || child.child("options").exists()) {
                                    looksLikeQuestions = true;
                                    break;
                                }
                            }
                            if (looksLikeQuestions) fetchQuestionsFrom(candidate);
                            else { loadingIndicator.setVisibility(View.GONE); showNoQuestions("No questions found"); }
                        } else { loadingIndicator.setVisibility(View.GONE); showNoQuestions("No exam found"); }
                    }).addOnFailureListener(e -> { loadingIndicator.setVisibility(View.GONE); showNoQuestions("Failed to resolve exam node"); });
                }
            }).addOnFailureListener(e -> { loadingIndicator.setVisibility(View.GONE); showNoQuestions("Failed to lookup exam questions"); });
            return;
        }

        if (selectedSubject != null && selectedChapter != null) {
            String key = selectedSubject.trim() + "_" + selectedChapter.trim();
            DatabaseReference candidate = examsRef.child(key).child("questions");
            candidate.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) fetchQuestionsFrom(candidate);
                else {
                    DatabaseReference candidate2 = examsRef.child(selectedSubject.trim()).child(selectedChapter.trim());
                    candidate2.get().addOnSuccessListener(snap2 -> {
                        if (snap2.exists()) fetchQuestionsFrom(candidate2);
                        else { loadingIndicator.setVisibility(View.GONE); showNoQuestions("No questions found"); }
                    }).addOnFailureListener(e -> { loadingIndicator.setVisibility(View.GONE); showNoQuestions("Failed to resolve questions"); });
                }
            }).addOnFailureListener(e -> { loadingIndicator.setVisibility(View.GONE); showNoQuestions("Failed to lookup questions"); });
            return;
        }

        loadingIndicator.setVisibility(View.GONE);
        showNoQuestions("Missing exam key or chapter info");
    }

    private void fetchQuestionsFrom(DatabaseReference nodeRef) {
        loadingIndicator.setVisibility(View.VISIBLE);
        questionList.clear();
        nodeRef.get().addOnSuccessListener(snapshot -> {
            loadingIndicator.setVisibility(View.GONE);
            questionList.clear();
            if (!snapshot.exists()) { showNoQuestions("No questions available"); return; }

            List<DataSnapshot> children = new ArrayList<>();
            for (DataSnapshot c : snapshot.getChildren()) children.add(c);

            children.sort((a, b) -> {
                String ka = a.getKey() != null ? a.getKey() : "";
                String kb = b.getKey() != null ? b.getKey() : "";
                try {
                    int ia = Integer.parseInt(ka), ib = Integer.parseInt(kb);
                    return Integer.compare(ia, ib);
                } catch (Exception ignore) { return ka.compareTo(kb); }
            });

            for (DataSnapshot child : children) {
                if (!child.hasChild("question") && !child.hasChild("options") && !child.hasChild("correct") && !child.hasChild("correctAnswer")) continue;
                String qText = firstNonNullString(child.child("question").getValue(String.class), child.child("prompt").getValue(String.class), child.child("text").getValue(String.class), child.child("title").getValue(String.class));
                String explanation = firstNonNullString(child.child("explanation").getValue(String.class), child.child("explain").getValue(String.class));
                String correctRaw = firstNonNullString(child.child("correct").getValue(String.class), child.child("correctAnswer").getValue(String.class), child.child("answer").getValue(String.class));
                List<String> options = parseOptions(child.child("options"));
                if (options.isEmpty()) options = parseOptions(child);
                String correct = resolveCorrectAnswerText(correctRaw, options);

                ExamCenter q = new ExamCenter();
                q.setId(child.getKey());
                q.setQuestion(qText != null ? qText : "");
                q.setExplanation(explanation != null ? explanation : "");
                q.setOptions(options != null ? options : new ArrayList<>());
                q.setCorrectAnswer(correct != null ? correct : "");
                Long pts = child.child("points").getValue(Long.class);
                if (pts != null) q.setPoints(pts.intValue());
                questionList.add(q);
            }

            showSkeleton(false);
            if (!questionList.isEmpty()) { currentQuestionIndex = 0; displayQuestion(); }
            else showNoQuestions("No question available");
        }).addOnFailureListener(e -> { loadingIndicator.setVisibility(View.GONE); showNoQuestions("Failed to load questions"); });
    }

    private String firstNonNullString(String... candidates) {
        if (candidates == null) return null;
        for (String s : candidates) if (s != null && !s.trim().isEmpty()) return s;
        return null;
    }

    private void displayQuestion() {
        if (questionList.isEmpty()) return;

        optionsLocked = false;
        ExamCenter q = questionList.get(currentQuestionIndex);
        String qId = q.getId();

        String prevSelected = qId != null ? userAnswers.get(qId) : null;
        int prevIndex = -1;
        if (prevSelected != null && q.getOptions() != null) {
            for (int i = 0; i < q.getOptions().size(); i++) {
                if (prevSelected.equals(q.getOptions().get(i)) || prevSelected.equalsIgnoreCase(q.getOptions().get(i))) { prevIndex = i; break; }
            }
        }

        hasAnsweredCurrentQuestion = prevSelected != null;
        explanationCard.setVisibility(prevIndex >= 0 ? View.VISIBLE : View.GONE);

        if (prevIndex >= 0) {
            explanationHeaderText.setText((prevIndex == resolveCorrectIndex(q.getCorrectAnswer(), q.getOptions())) ? "Explanation — Correct" : "Explanation — Review");
            explanationBody.setText(q.getExplanation() != null && !q.getExplanation().isEmpty() ? q.getExplanation() : "No explanation provided.");
            // default to expanded when first answered; user can collapse afterwards.
            expandExplanation();
        } else {
            collapseExplanationQuietly();
            explanationCard.setVisibility(View.GONE);
        }

        questionTv.setText((currentQuestionIndex + 1) + ". " + (q.getQuestion() == null || q.getQuestion().trim().isEmpty() ? "(Question text not provided)" : q.getQuestion()));
        questionCounterTv.setText("Question " + (currentQuestionIndex + 1) + " of " + questionList.size());
        int percent = (int) (((currentQuestionIndex + 1) / (float) questionList.size()) * 100);
        animateLinearProgress(progressLinear, percent);

        optionAdapter.setQuestion(q, resolveCorrectIndex(q.getCorrectAnswer(), q.getOptions()));
        if (prevIndex >= 0) optionAdapter.markAnswered(prevIndex, resolveCorrectIndex(q.getCorrectAnswer(), q.getOptions()), prevIndex == resolveCorrectIndex(q.getCorrectAnswer(), q.getOptions()));
        else optionAdapter.markAnswered(-1, resolveCorrectIndex(q.getCorrectAnswer(), q.getOptions()), false);

        optionAdapter.setEnabled(!examSubmitted);

        nextBtn.setEnabled(hasAnsweredCurrentQuestion || examSubmitted);
        contentScroll.post(() -> contentScroll.scrollTo(0, 0));
        updateNavigationState();
    }

    private void onOptionClicked(@NonNull String selectedText, int selectedIndex, @NonNull ExamCenter q) {
        if (optionsLocked) return;
        optionsLocked = true;

        if (q.getId() != null) userAnswers.put(q.getId(), selectedText);
        hasAnsweredCurrentQuestion = true;
        nextBtn.setEnabled(true);

        int correctIndex = resolveCorrectIndex(q.getCorrectAnswer(), q.getOptions());
        boolean selectedIsCorrect = (selectedIndex == correctIndex);

        explanationHeaderText.setText(selectedIsCorrect ? "Explanation — Correct" : "Explanation — Review");
        explanationBody.setText(q.getExplanation() != null && !q.getExplanation().isEmpty() ? q.getExplanation() : "No explanation provided.");
        explanationCard.setVisibility(View.VISIBLE);
        expandExplanation();

        optionAdapter.markAnswered(selectedIndex, correctIndex, selectedIsCorrect);
        optionAdapter.setEnabled(!examSubmitted);

        optionsLocked = false;

        contentScroll.post(() -> {
            explanationCard.getParent().requestChildFocus(explanationCard, explanationCard);
            explanationCard.post(() -> {
                int y = explanationCard.getTop();
                contentScroll.smoothScrollTo(0, y);
            });
        });
    }

    private int resolveCorrectIndex(String correctRaw, List<String> options) {
        if (correctRaw == null || options == null || options.isEmpty()) return -1;
        for (int i = 0; i < options.size(); i++) {
            if (correctRaw.equals(options.get(i)) || correctRaw.equalsIgnoreCase(options.get(i))) return i;
        }
        String cr = correctRaw == null ? "" : correctRaw.trim();
        if (cr.length() == 1 && Character.isLetter(cr.charAt(0))) {
            int idx = Character.toUpperCase(cr.charAt(0)) - 'A';
            if (idx >= 0 && idx < options.size()) return idx;
        }
        try {
            int idx = Integer.parseInt(cr);
            if (idx >= 0 && idx < options.size()) return idx;
        } catch (Exception ignored) {}
        return -1;
    }

    private void animateLinearProgress(LinearProgressIndicator lp, int toPercent) {
        if (lp == null) return;
        int from = lp.getProgress();
        ObjectAnimator anim = ObjectAnimator.ofInt(lp, "progress", from, toPercent);
        anim.setDuration(300);
        anim.setEvaluator(new IntEvaluator());
        anim.start();
    }

    private void confirmAndSubmitExam() {
        new AlertDialog.Builder(this)
                .setTitle("Submit exam")
                .setMessage("Are you sure you want to submit? You won't be able to change answers afterwards.")
                .setPositiveButton("Submit", (d, which) -> submitExamResults())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitExamResults() {
        if (examKey == null || examKey.isEmpty()) {
            Toast.makeText(this, "Missing exam id", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = Continuity.userId;
        if (uid == null || uid.isEmpty()) {
            Toast.makeText(this, "You must be signed in to submit results", Toast.LENGTH_SHORT).show();
            return;
        }

        int earnedPoints = 0;
        int totalPoints = 0;

        for (ExamCenter q : questionList) {
            int points = q.getPoints() > 0 ? q.getPoints() : 1;
            totalPoints += points;
            String selected = q.getId() != null ? userAnswers.get(q.getId()) : null;
            String correctText = q.getCorrectAnswer();
            boolean isCorrect = selected != null && correctText != null && (selected.equals(correctText) || selected.equalsIgnoreCase(correctText));
            if (isCorrect) earnedPoints += points;
        }

        int percent = totalPoints > 0 ? (int) Math.round((earnedPoints * 100.0) / totalPoints) : 0;

        boolean passed;
        if (examPassScore > 0) {
            if (examPassScore <= 100) passed = percent >= examPassScore;
            else passed = earnedPoints >= examPassScore;
        } else {
            passed = totalPoints > 0 && (percent >= 50);
        }

        final int earnedFinal = earnedPoints;
        final int totalFinal = totalPoints;
        final boolean passedFinal = passed;
        final int percentFinal = percent;

        Map<String, Object> resultObj = new HashMap<>();
        resultObj.put("score", earnedFinal);
        resultObj.put("total", totalFinal);
        resultObj.put("passed", passedFinal);
        resultObj.put("takenAt", ServerValue.TIMESTAMP);
        resultObj.put("percent", percentFinal);

        Map<String, Object> answersMap = new HashMap<>();
        for (ExamCenter q : questionList) {
            String qId = q.getId();
            if (qId == null) continue;
            Map<String, Object> one = new HashMap<>();
            one.put("selected", userAnswers.get(qId));
            one.put("correct", q.getCorrectAnswer());
            one.put("points", q.getPoints() > 0 ? q.getPoints() : 1);
            answersMap.put(qId, one);
        }

        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        Map<String, Object> updates = new HashMap<>();
        updates.put("/ExamResults/" + uid + "/" + examKey, resultObj);
        updates.put("/ExamAnswers/" + uid + "/" + examKey, answersMap);

        DatabaseReference resultRef = root.child("ExamResults").child(uid).child(examKey);
        resultRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                new AlertDialog.Builder(this)
                        .setTitle("Overwrite result?")
                        .setMessage("You already have a saved result for this exam. Overwrite it with the new result?")
                        .setPositiveButton("Overwrite", (d, w) -> root.updateChildren(updates).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) onResultSavedSuccessfully(earnedFinal, totalFinal, passedFinal, percentFinal);
                            else Toast.makeText(this, "Failed to save result", Toast.LENGTH_SHORT).show();
                        }))
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                root.updateChildren(updates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) onResultSavedSuccessfully(earnedFinal, totalFinal, passedFinal, percentFinal);
                    else Toast.makeText(this, "Failed to save result", Toast.LENGTH_SHORT).show();
                });
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to check existing results", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onBackPressed() {
        if (examSubmitted) {
            // ensure caller gets update (if not already set)
            Intent out = new Intent();
            out.putExtra("examKey", examKey);
            setResult(RESULT_OK, out);
        }
        super.onBackPressed();
    }
    private void onResultSavedSuccessfully(int earned, int total, boolean passed, int percent) {
        examSubmitted = true;
        optionAdapter.setEnabled(false);
        nextBtn.setEnabled(true);
        previousBtn.setEnabled(true);
        updateNavigationState();

        // Notify caller that results changed
        Intent out = new Intent();
        out.putExtra("examKey", examKey);
        out.putExtra("score", earned);
        out.putExtra("total", total);
        out.putExtra("percent", percent);
        out.putExtra("passed", passed);
        setResult(RESULT_OK, out);

        new AlertDialog.Builder(this)
                .setTitle("Exam submitted")
                .setMessage("Score: " + earned + " / " + total + "\n" + (passed ? "Passed" : "Failed") + "\n" + percent + "%")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showNoQuestions(String reason) {
        Log.w(TAG, reason);
        loadingIndicator.setVisibility(View.GONE);
        showSkeleton(false);
        questionTv.setText("No questions available");
        optionAdapter.setQuestion(null, -1);
        explanationCard.setVisibility(View.GONE);
        questionCounterTv.setText("Question 0 of 0");
        nextBtn.setEnabled(false);
        previousBtn.setEnabled(false);
    }

    private void showSkeleton(boolean show) {
        if (skeletonContainer == null) return;
        if (show) {
            skeletonContainer.setVisibility(View.VISIBLE);
            questionTv.setVisibility(View.INVISIBLE);
            optionsRecycler.setVisibility(View.INVISIBLE);
            explanationCard.setVisibility(View.INVISIBLE);
        } else {
            skeletonContainer.setVisibility(View.GONE);
            questionTv.setVisibility(View.VISIBLE);
            optionsRecycler.setVisibility(View.VISIBLE);
            explanationCard.setVisibility(View.VISIBLE);
        }
    }

    // parseOptions and resolveCorrectAnswerText: keep your existing implementations

    // ---------- expand / collapse explanation (with icon rotation) ----------
    private void expandExplanation() {
        if (explanationBody == null) return;
        if (explanationExpanded) return;

        explanationBody.setVisibility(View.VISIBLE);
        explanationBody.measure(
                View.MeasureSpec.makeMeasureSpec(explanationBody.getWidth() > 0 ? explanationBody.getWidth() : getResources().getDisplayMetrics().widthPixels, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.UNSPECIFIED);
        final int targetHeight = explanationBody.getMeasuredHeight();

        if (targetHeight <= 0) {
            explanationBody.post(() -> { if (!explanationExpanded) expandExplanation(); });
            return;
        }

        if (explanationBody.getLayoutParams() != null) {
            explanationBody.getLayoutParams().height = 0;
            explanationBody.requestLayout();
        }

        ValueAnimator animator = ValueAnimator.ofInt(0, targetHeight);
        animator.setDuration(220);
        animator.addUpdateListener(animation -> {
            int value = (Integer) animation.getAnimatedValue();
            if (explanationBody.getLayoutParams() != null) {
                explanationBody.getLayoutParams().height = value;
                explanationBody.requestLayout();
            }
        });
        animator.start();

        // rotate expand icon to indicate open state
        if (expandExplanation != null) expandExplanation.animate().rotation(180f).setDuration(220).start();

        explanationExpanded = true;
    }

    private void collapseExplanation() {
        if (explanationBody == null) return;
        if (!explanationExpanded) return;

        final int initialHeight = explanationBody.getMeasuredHeight();
        if (initialHeight <= 0) { explanationBody.setVisibility(View.GONE); explanationExpanded = false; return; }

        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, 0);
        animator.setDuration(180);
        animator.addUpdateListener(animation -> {
            int value = (Integer) animation.getAnimatedValue();
            if (explanationBody.getLayoutParams() != null) {
                explanationBody.getLayoutParams().height = value;
                explanationBody.requestLayout();
            }
        });
        animator.addListener(new ValueAnimator.AnimatorListener() {
            @Override public void onAnimationStart(@NonNull android.animation.Animator animator) {}
            @Override public void onAnimationEnd(@NonNull android.animation.Animator animator) { explanationBody.setVisibility(View.GONE); }
            @Override public void onAnimationCancel(@NonNull android.animation.Animator animator) {}
            @Override public void onAnimationRepeat(@NonNull android.animation.Animator animator) {}
        });
        animator.start();

        // rotate expand icon back
        if (expandExplanation != null) expandExplanation.animate().rotation(0f).setDuration(180).start();

        explanationExpanded = false;
    }

    private void collapseExplanationQuietly() {
        if (explanationBody == null) return;
        if (explanationBody.getLayoutParams() != null) explanationBody.getLayoutParams().height = 0;
        explanationBody.requestLayout();
        explanationBody.setVisibility(View.GONE);
        if (expandExplanation != null) expandExplanation.setRotation(0f);
        explanationExpanded = false;
    }

    // ---------- OptionAdapter (inner) ----------
    private class OptionAdapter extends RecyclerView.Adapter<OptionAdapter.VH> {
        private final List<String> options = new ArrayList<>();
        private ExamCenter question;
        private int correctIndex = -1;
        private int selectedIndex = -1;
        private boolean enabled = true;

        void setOptions(List<String> items) {
            options.clear();
            if (items != null) options.addAll(items);
            notifyDataSetChanged();
        }

        void setQuestion(ExamCenter q, int correctIndex) {
            this.question = q;
            this.correctIndex = correctIndex;
            this.selectedIndex = -1;
            setOptions(q != null ? q.getOptions() : null);
            this.enabled = true;
        }

        void markAnswered(int selected, int correct, boolean selectedIsCorrect) {
            this.selectedIndex = selected;
            this.correctIndex = correct;
            notifyDataSetChanged();
        }

        void setEnabled(boolean e) { this.enabled = e; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            MaterialCardView card = new MaterialCardView(parent.getContext());
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dpToPxInt(6), 0, dpToPxInt(6));
            card.setLayoutParams(lp);
            card.setRadius(dpToPx(12f));
            card.setCardElevation(dpToPx(2f));
            card.setUseCompatPadding(true);
            card.setClickable(true);
            card.setPreventCornerOverlap(true);
            card.setStrokeWidth(dpToPxInt(1));
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            int pad = dpToPxInt(14);
            tv.setPadding(pad, pad, pad, pad);
            tv.setTextSize(16f);
            tv.setTypeface(Typeface.DEFAULT);
            card.addView(tv);
            return new VH(card);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String text = options.size() > position ? options.get(position) : "";
            TextView tv = holder.getTextView();
            tv.setText(text);
            MaterialCardView card = holder.card;

            card.setCardBackgroundColor(ContextCompat.getColor(card.getContext(), R.color.option_bg));
            card.setStrokeColor(ContextCompat.getColor(card.getContext(), R.color.option_border));
            tv.setTextColor(ContextCompat.getColor(card.getContext(), R.color.black));

            if (selectedIndex >= 0) {
                int strokePx = dpToPxInt(1);
                if (position == selectedIndex) {
                    if (position == correctIndex) {
                        card.setCardBackgroundColor(ContextCompat.getColor(card.getContext(), R.color.duo_green_fill));
                        card.setStrokeColor(ContextCompat.getColor(card.getContext(), R.color.duo_green_outline));
                        card.setStrokeWidth(strokePx);
                        tv.setTextColor(ContextCompat.getColor(card.getContext(), R.color.duo_green_text));
                    } else {
                        card.setCardBackgroundColor(ContextCompat.getColor(card.getContext(), R.color.duo_red_fill));
                        card.setStrokeColor(ContextCompat.getColor(card.getContext(), R.color.duo_red_outline));
                        card.setStrokeWidth(strokePx);
                        tv.setTextColor(ContextCompat.getColor(card.getContext(), R.color.duo_red_text));
                    }
                } else if (position == correctIndex) {
                    card.setCardBackgroundColor(ContextCompat.getColor(card.getContext(), R.color.duo_green_fill));
                    card.setStrokeColor(ContextCompat.getColor(card.getContext(), R.color.duo_green_outline));
                    card.setStrokeWidth(strokePx);
                    tv.setTextColor(ContextCompat.getColor(card.getContext(), R.color.duo_green_text));
                } else {
                    card.setCardBackgroundColor(ContextCompat.getColor(card.getContext(), R.color.option_bg));
                    card.setStrokeColor(ContextCompat.getColor(card.getContext(), R.color.option_border));
                    tv.setTextColor(ContextCompat.getColor(card.getContext(), R.color.black));
                }
            } else {
                card.setStrokeWidth(dpToPxInt(1));
            }

            card.setOnClickListener(v -> {
                if (!enabled) return;
                if (question != null) onOptionClicked(text, position, question);
            });
        }

        @Override
        public int getItemCount() { return options.size(); }

        class VH extends RecyclerView.ViewHolder {
            final MaterialCardView card;
            VH(@NonNull View itemView) {
                super(itemView);
                card = (MaterialCardView) itemView;
            }
            TextView getTextView() { return (TextView) card.getChildAt(0); }
        }
    }

    // dp helpers
    private float dpToPx(float dp) { return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()); }
    private int dpToPxInt(float dp) { return Math.round(dpToPx(dp)); }

    private void updateNavigationState() {
        if (questionList.isEmpty()) {
            previousBtn.setEnabled(false);
            nextBtn.setEnabled(false);
            return;
        }
        previousBtn.setEnabled(currentQuestionIndex > 0);
        if (currentQuestionIndex == questionList.size() - 1) {
            nextBtn.setText(!examSubmitted ? "Submit" : "Next");
        } else {
            nextBtn.setText("Next");
        }
    }



    private String resolveCorrectAnswerText(String correctRaw, List<String> options) {
        if (correctRaw == null) return null;
        for (String opt : options) {
            if (correctRaw.equals(opt) || correctRaw.equalsIgnoreCase(opt)) return opt;
        }
        String cr = correctRaw.trim();
        if (cr.length() == 1 && Character.isLetter(cr.charAt(0))) {
            int idx = Character.toUpperCase(cr.charAt(0)) - 'A';
            if (idx >= 0 && idx < options.size()) return options.get(idx);
        }
        try {
            int idx = Integer.parseInt(cr);
            if (idx >= 0 && idx < options.size()) return options.get(idx);
        } catch (Exception ignored) {}
        return correctRaw;
    }


    private List<String> parseOptions(DataSnapshot optionsNode) {
        List<String> options = new ArrayList<>();
        if (optionsNode == null || !optionsNode.exists()) return options;

        boolean numericKeys = true;
        for (DataSnapshot child : optionsNode.getChildren()) {
            String key = child.getKey();
            try { Integer.parseInt(key); } catch (Exception e) { numericKeys = false; break; }
        }
        if (numericKeys) {
            List<DataSnapshot> children = new ArrayList<>();
            for (DataSnapshot c : optionsNode.getChildren()) children.add(c);
            children.sort((a, b) -> {
                try {
                    return Integer.compare(Integer.parseInt(a.getKey()), Integer.parseInt(b.getKey()));
                } catch (Exception ex) { return 0; }
            });
            for (DataSnapshot c : children) {
                String val = c.getValue(String.class);
                if (val != null) options.add(val);
            }
            return options;
        }

        String[] alphaOrder = {"A","B","C","D","E","F"};
        boolean hasAlpha = false;
        for (String k : alphaOrder) if (optionsNode.hasChild(k)) { hasAlpha = true; break; }
        if (hasAlpha) {
            for (String k : alphaOrder) {
                if (optionsNode.hasChild(k)) {
                    String val = optionsNode.child(k).getValue(String.class);
                    if (val != null) options.add(val);
                }
            }
            return options;
        }

        for (DataSnapshot child : optionsNode.getChildren()) {
            String val = child.getValue(String.class);
            if (val != null) options.add(val);
        }
        return options;
    }
}