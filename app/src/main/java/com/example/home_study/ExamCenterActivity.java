package com.example.home_study;

import android.animation.Animator;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.home_study.Prevalent.Continuity;
import com.google.firebase.database.ServerValue;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.home_study.Model.ExamCenter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ExamCenterActivity — card-based option UI with collapsible explanation and result persistence.
 *
 * Notes:
 * - Layout must contain the explanation CardView and child IDs:
 *   explanationCard, explanationHeader, explanationHeaderText, explanationChevron, explanationBody
 * - The old single TextView id `explanation` must not be used anywhere in this file.
 */
public class ExamCenterActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView questionTv, questionCounterTv, timerText;

    // explanation UI (collapsible)
    private MaterialCardView explanationCard;
    private View explanationHeader;
    private TextView explanationHeaderText;
    private ImageView explanationChevron;
    private TextView explanationBody;
    private boolean explanationExpanded = false;

    private LinearLayout optionsContainer;
    private CircularProgressIndicator loadingIndicator;
    private MaterialButton nextBtn, previousBtn;
    private CircularProgressIndicator progressCircular;

    private final List<ExamCenter> questionList = new ArrayList<>();
    private final List<MaterialCardView> optionCards = new ArrayList<>();
    private int currentQuestionIndex = 0;

    private DatabaseReference examsRef;
    private DatabaseReference questionsRef;
    private String examKey;
    private String selectedSubject, selectedChapter;
    private String chapterTitleExtra;
    private final Map<String, String> userAnswers = new HashMap<>();

    // exam metadata (from intent)
    private int examPassScore = 0;   // pass threshold
    private int examTotalQuestions = 0;
    private int examDurationMinutes = 0;

    private boolean optionsLocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_center);

        // UI bindings
        toolbar = findViewById(R.id.examCenterToolBar);
        questionCounterTv = findViewById(R.id.questionCounter);
        questionTv = findViewById(R.id.question);
        optionsContainer = findViewById(R.id.optionsContainer);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        nextBtn = findViewById(R.id.nextBtn);
        previousBtn = findViewById(R.id.previousBtn);
        progressCircular = findViewById(R.id.progressCircular);
        timerText = findViewById(R.id.timerText);

        // explanation bindings (collapsible)
        explanationCard = findViewById(R.id.explanationCard);
        explanationHeader = findViewById(R.id.explanationHeader);
        explanationHeaderText = findViewById(R.id.explanationHeaderText);
        explanationChevron = findViewById(R.id.explanationChevron);
        explanationBody = findViewById(R.id.explanationBody);

        // start hidden
        if (explanationCard != null) explanationCard.setVisibility(View.GONE);
        if (explanationBody != null) {
            if (explanationBody.getLayoutParams() != null) explanationBody.getLayoutParams().height = 0;
            explanationBody.setVisibility(View.GONE);
        }
        if (explanationChevron != null) explanationChevron.setRotation(0f);

        // header toggles expand/collapse
        if (explanationHeader != null) {
            explanationHeader.setOnClickListener(v -> {
                if (explanationExpanded) collapseExplanation();
                else expandExplanation();
            });
        }

        // Toolbar navigation
        toolbar.setNavigationOnClickListener(v -> finish());

        // Firebase root ref
        examsRef = FirebaseDatabase.getInstance().getReference("Exams");

        // Read extras (may be null)
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

        // Set toolbar title early (if provided)
        if (chapterTitleExtra != null && !chapterTitleExtra.trim().isEmpty()) {
            toolbar.setTitle(chapterTitleExtra.trim());
        }

        // Validate we have at least one valid way to resolve questions
        if ((examKey == null || examKey.trim().isEmpty()) &&
                (selectedSubject == null || selectedSubject.trim().isEmpty() ||
                        selectedChapter == null || selectedChapter.trim().isEmpty())) {
            Toast.makeText(this, "Missing exam information. Please open the exam from the course screen.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // If toolbar title not set yet, use selectedChapter (fallback)
        if ((toolbar.getTitle() == null || toolbar.getTitle().toString().isEmpty()) && selectedChapter != null) {
            toolbar.setTitle(selectedChapter);
        }

        // Start resolution / fetch
        resolveAndFetchQuestions();

        // Navigation listeners
        nextBtn.setOnClickListener(v -> {
            if (currentQuestionIndex < questionList.size() - 1) {
                currentQuestionIndex++;
                displayQuestion();
            } else {
                // last question -> submit
                confirmAndSubmitExam();
            }
        });

        previousBtn.setOnClickListener(v -> {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--;
                displayQuestion();
            }
        });
    }

    private void resolveAndFetchQuestions() {
        loadingIndicator.setVisibility(View.VISIBLE);

        if (examKey != null && !examKey.trim().isEmpty()) {
            final String key = examKey.trim();
            DatabaseReference candidate = examsRef.child(key).child("Questions");
            candidate.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    questionsRef = candidate;
                    fetchQuestionsFrom(questionsRef);
                } else {
                    DatabaseReference candidate2 = examsRef.child(key);
                    candidate2.get().addOnSuccessListener(snap2 -> {
                        if (snap2.exists()) {
                            boolean looksLikeQuestions = false;
                            for (DataSnapshot child : snap2.getChildren()) {
                                if (child.child("question").exists() || child.child("options").exists()) {
                                    looksLikeQuestions = true;
                                    break;
                                }
                            }
                            if (looksLikeQuestions) {
                                questionsRef = candidate2;
                                fetchQuestionsFrom(questionsRef);
                            } else {
                                loadingIndicator.setVisibility(View.GONE);
                                showNoQuestions("No questions found for examKey=" + key);
                            }
                        } else {
                            loadingIndicator.setVisibility(View.GONE);
                            showNoQuestions("No exam found for " + key);
                        }
                    }).addOnFailureListener(e -> {
                        loadingIndicator.setVisibility(View.GONE);
                        showNoQuestions("Failed to resolve exam node");
                    });
                }
            }).addOnFailureListener(e -> {
                loadingIndicator.setVisibility(View.GONE);
                showNoQuestions("Failed to lookup exam questions");
            });
            return;
        }

        if (selectedSubject != null && selectedChapter != null) {
            final String key = selectedSubject.trim() + "_" + selectedChapter.trim();
            DatabaseReference candidate = examsRef.child(key).child("Questions");
            candidate.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    questionsRef = candidate;
                    fetchQuestionsFrom(questionsRef);
                } else {
                    DatabaseReference candidate2 = examsRef.child(selectedSubject.trim()).child(selectedChapter.trim());
                    candidate2.get().addOnSuccessListener(snap2 -> {
                        if (snap2.exists()) {
                            questionsRef = candidate2;
                            fetchQuestionsFrom(questionsRef);
                        } else {
                            loadingIndicator.setVisibility(View.GONE);
                            showNoQuestions("No questions found for given parameters");
                        }
                    }).addOnFailureListener(e -> {
                        loadingIndicator.setVisibility(View.GONE);
                        showNoQuestions("Failed to resolve questions");
                    });
                }
            }).addOnFailureListener(e -> {
                loadingIndicator.setVisibility(View.GONE);
                showNoQuestions("Failed to lookup questions");
            });
            return;
        }

        loadingIndicator.setVisibility(View.GONE);
        showNoQuestions("Missing exam key or chapter info");
    }

    private void fetchQuestionsFrom(DatabaseReference nodeRef) {
        loadingIndicator.setVisibility(View.VISIBLE);
        nodeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                questionList.clear();
                loadingIndicator.setVisibility(View.GONE);

                if (!snapshot.exists()) {
                    showNoQuestions("No questions available");
                    return;
                }

                for (DataSnapshot child : snapshot.getChildren()) {
                    String qText = child.child("question").getValue(String.class);
                    String explanation = child.child("explanation").getValue(String.class);
                    String correctRaw = child.child("correct").getValue(String.class);
                    if (correctRaw == null) correctRaw = child.child("correctAnswer").getValue(String.class);
                    List<String> options = parseOptions(child.child("options"));
                    if (options.isEmpty()) options = parseOptions(child);

                    String correct = resolveCorrectAnswerText(correctRaw, options);

                    ExamCenter q = new ExamCenter();
                    q.setId(child.getKey());
                    q.setQuestion(qText != null ? qText : "");
                    q.setExplanation(explanation != null ? explanation : "");
                    q.setOptions(options != null ? options : new ArrayList<>());
                    q.setCorrectAnswer(correct != null ? correct : "");

                    questionList.add(q);
                }

                if (!questionList.isEmpty()) {
                    currentQuestionIndex = 0;
                    displayQuestion();
                } else {
                    showNoQuestions("No question available");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadingIndicator.setVisibility(View.GONE);
                showNoQuestions("Failed to load questions");
            }
        });
    }

    private void displayQuestion() {
        if (questionList.isEmpty()) return;

        optionsLocked = false;
        optionCards.clear();
        optionsContainer.removeAllViews();

        // reset explanation UI for the new question
        collapseExplanationQuietly();
        if (explanationBody != null) explanationBody.setText("");
        if (explanationHeaderText != null) explanationHeaderText.setText("Explanation");

        // require answer before moving to next by default
        nextBtn.setEnabled(false);

        ExamCenter q = questionList.get(currentQuestionIndex);
        questionTv.setText((currentQuestionIndex + 1) + ". " + (q.getQuestion() != null ? q.getQuestion() : ""));
        questionCounterTv.setText("Question " + (currentQuestionIndex + 1) + " of " + questionList.size());

        int percent = (int) (((currentQuestionIndex + 1) / (float) questionList.size()) * 100);
        progressCircular.setProgress(percent);

        List<String> options = q.getOptions();
        if (options == null) options = Collections.emptyList();

        int correctIndex = resolveCorrectIndex(q.getCorrectAnswer(), options);

        for (int i = 0; i < options.size(); i++) {
            String opt = options.get(i);
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dpToPxInt(6), 0, dpToPxInt(6));
            card.setLayoutParams(lp);

            // runtime card styling
            card.setRadius(dpToPx(12f));
            card.setCardElevation(dpToPx(2f));
            card.setUseCompatPadding(true);
            card.setClickable(true);
            card.setFocusable(true);
            card.setPreventCornerOverlap(true);
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.option_bg));
            card.setStrokeWidth(dpToPxInt(1));
            card.setStrokeColor(ContextCompat.getColor(this, R.color.option_border));

            TextView tv = new TextView(this);
            LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tv.setLayoutParams(tvLp);
            int pad = dpToPxInt(14);
            tv.setPadding(pad, pad, pad, pad);
            tv.setText(opt);
            tv.setTextSize(16f);
            tv.setTypeface(Typeface.DEFAULT);
            tv.setTextColor(ContextCompat.getColor(this, R.color.black));

            card.addView(tv);
            final int idx = i;
            optionCards.add(card);

            card.setOnClickListener(v -> {
                if (optionsLocked) return;
                optionsLocked = true;

                // store user's selection for this question
                String qId = q.getId();
                if (qId != null) {
                    userAnswers.put(qId, opt);
                }
                // allow moving forward
                nextBtn.setEnabled(true);

                // press animation
                card.animate().scaleX(0.98f).scaleY(0.98f).setDuration(120)
                        .withEndAction(() -> card.animate().scaleX(1f).scaleY(1f).setDuration(120)).start();

                int strokePx = dpToPxInt(5);

                TextView optionTv = null;
                if (card.getChildCount() > 0 && card.getChildAt(0) instanceof TextView) {
                    optionTv = (TextView) card.getChildAt(0);
                }

                String explanationText = q.getExplanation() != null ? q.getExplanation() : "No explanation available.";

                if (idx == correctIndex) {
                    // correct
                    card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.duo_green_fill));
                    card.setStrokeColor(ContextCompat.getColor(this, R.color.duo_green_outline));
                    card.setStrokeWidth(strokePx);
                    if (optionTv != null) optionTv.setTextColor(ContextCompat.getColor(this, R.color.duo_green_text));

                    if (explanationHeaderText != null) explanationHeaderText.setTextColor(ContextCompat.getColor(this, R.color.duo_green_text));
                    if (explanationBody != null) {
                        explanationBody.setText(explanationText);
                        explanationBody.setTextColor(ContextCompat.getColor(this, R.color.black_low));
                    }
                    if (explanationCard != null && explanationCard.getVisibility() != View.VISIBLE) explanationCard.setVisibility(View.VISIBLE);
                    expandExplanation();
                } else {
                    // wrong
                    card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.duo_red_fill));
                    card.setStrokeColor(ContextCompat.getColor(this, R.color.duo_red_outline));
                    card.setStrokeWidth(strokePx);
                    if (optionTv != null) optionTv.setTextColor(ContextCompat.getColor(this, R.color.duo_red_text));

                    if (explanationHeaderText != null) explanationHeaderText.setTextColor(ContextCompat.getColor(this, R.color.duo_red_text));
                    if (explanationBody != null) {
                        explanationBody.setText(explanationText);
                        explanationBody.setTextColor(ContextCompat.getColor(this, R.color.black_low));
                    }
                    if (explanationCard != null && explanationCard.getVisibility() != View.VISIBLE) explanationCard.setVisibility(View.VISIBLE);
                    expandExplanation();

                    // reveal correct card
                    if (correctIndex >= 0 && correctIndex < optionCards.size()) {
                        MaterialCardView correctCard = optionCards.get(correctIndex);
                        correctCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.duo_green_fill));
                        correctCard.setStrokeColor(ContextCompat.getColor(this, R.color.duo_green_outline));
                        correctCard.setStrokeWidth(strokePx);
                        if (correctCard.getChildCount() > 0 && correctCard.getChildAt(0) instanceof TextView) {
                            ((TextView) correctCard.getChildAt(0)).setTextColor(ContextCompat.getColor(this, R.color.duo_green_text));
                        }
                        correctCard.animate().scaleX(1.02f).scaleY(1.02f).setDuration(150)
                                .withEndAction(() -> correctCard.animate().scaleX(1f).scaleY(1f).setDuration(150)).start();
                    }
                }

                // lock further interaction with options for this question
                setOptionsEnabled(false);
            });

            optionsContainer.addView(card);
        }

        findViewById(R.id.questionScroll).scrollTo(0, 0);
        updateNavigationState();
    }

    private int resolveCorrectIndex(String correctRaw, List<String> options) {
        if (correctRaw == null || options == null || options.isEmpty()) return -1;
        for (int i = 0; i < options.size(); i++) {
            if (correctRaw.equals(options.get(i)) || correctRaw.equalsIgnoreCase(options.get(i))) return i;
        }
        String cr = correctRaw.trim();
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

    private void setOptionsEnabled(boolean enabled) {
        for (MaterialCardView c : optionCards) {
            c.setClickable(enabled);
            c.setEnabled(enabled);
        }
    }

    private void updateNavigationState() {
        if (questionList.isEmpty()) {
            previousBtn.setEnabled(false);
            nextBtn.setEnabled(false);
            return;
        }
        previousBtn.setEnabled(currentQuestionIndex > 0);
        if (currentQuestionIndex == questionList.size() - 1) {
            nextBtn.setText("Submit");
        } else {
            nextBtn.setText("Next");
        }
    }

    private void showNoQuestions(String reason) {
        Log.w("ExamCenterActivity", reason);
        questionTv.setText("No questions available");
        optionsContainer.removeAllViews();
        collapseExplanationQuietly();
        questionCounterTv.setText("Question 0 of 0");
        nextBtn.setEnabled(false);
        previousBtn.setEnabled(false);
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
            String qId = q.getId();
            int points = 1;
            try {
                points = q.getPoints() > 0 ? q.getPoints() : 1;
            } catch (Exception ignored) {}

            totalPoints += points;

            String selected = null;
            if (qId != null) selected = userAnswers.get(qId);

            String correctText = q.getCorrectAnswer();

            boolean isCorrect = false;
            if (selected != null && correctText != null) {
                isCorrect = selected.equals(correctText) || selected.equalsIgnoreCase(correctText);
            }

            if (isCorrect) { earnedPoints += points; }
        }

        boolean passed;
        if (examPassScore > 0) {
            passed = earnedPoints >= examPassScore;
        } else {
            passed = totalPoints > 0 && (earnedPoints * 100.0 / totalPoints) >= 50.0;
        }

        int percent = totalPoints > 0 ? (int) Math.round((earnedPoints * 100.0) / totalPoints) : 0;

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
                            if (task.isSuccessful()) {
                                onResultSavedSuccessfully(earnedFinal, totalFinal, passedFinal, percentFinal);
                            } else {
                                Toast.makeText(this, "Failed to save result", Toast.LENGTH_SHORT).show();
                            }
                        }))
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                root.updateChildren(updates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        onResultSavedSuccessfully(earnedFinal, totalFinal, passedFinal, percentFinal);
                    } else {
                        Toast.makeText(this, "Failed to save result", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to check existing results", Toast.LENGTH_SHORT).show();
        });
    }

    private void onResultSavedSuccessfully(int earned, int total, boolean passed, int percent) {
        new AlertDialog.Builder(this)
                .setTitle("Exam submitted")
                .setMessage("Score: " + earned + " / " + total + "\n" + (passed ? "Passed" : "Failed") + "\n" + percent + "%")
                .setPositiveButton("OK", (d, w) -> {
                    setOptionsEnabled(false);
                    nextBtn.setEnabled(false);
                    previousBtn.setEnabled(false);
                })
                .show();
    }

    private List<String> parseOptions(DataSnapshot optionsNode) {
        List<String> options = new ArrayList<>();
        if (optionsNode == null || !optionsNode.exists()) return options;

        boolean numericKeys = true;
        for (DataSnapshot child : optionsNode.getChildren()) {
            String key = child.getKey();
            try {
                Integer.parseInt(key);
            } catch (Exception e) {
                numericKeys = false;
                break;
            }
        }
        if (numericKeys) {
            List<DataSnapshot> children = new ArrayList<>();
            for (DataSnapshot c : optionsNode.getChildren()) children.add(c);
            children.sort((a, b) -> {
                try {
                    return Integer.compare(Integer.parseInt(a.getKey()), Integer.parseInt(b.getKey()));
                } catch (Exception ex) {
                    return 0;
                }
            });
            for (DataSnapshot c : children) {
                String val = c.getValue(String.class);
                if (val != null) options.add(val);
            }
            return options;
        }

        String[] alphaOrder = {"A", "B", "C", "D", "E", "F"};
        boolean hasAlpha = false;
        for (String k : alphaOrder) {
            if (optionsNode.hasChild(k)) { hasAlpha = true; break; }
        }
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

    // dp helpers
    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
    private int dpToPxInt(float dp) {
        return Math.round(dpToPx(dp));
    }

    private void expandExplanation() {
        if (explanationBody == null) return;
        if (explanationExpanded) return;

        explanationBody.setVisibility(View.VISIBLE);

        explanationBody.measure(
                View.MeasureSpec.makeMeasureSpec(explanationBody.getWidth() > 0 ? explanationBody.getWidth() : getResources().getDisplayMetrics().widthPixels, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.UNSPECIFIED);
        final int targetHeight = explanationBody.getMeasuredHeight();

        if (targetHeight <= 0) {
            explanationBody.post(() -> {
                if (!explanationExpanded) expandExplanation();
            });
            return;
        }

        if (explanationBody.getLayoutParams() != null) {
            explanationBody.getLayoutParams().height = 0;
            explanationBody.requestLayout();
        }

        ValueAnimator animator = ValueAnimator.ofInt(0, targetHeight);
        animator.setDuration(240);
        animator.addUpdateListener(animation -> {
            Integer value = (Integer) animation.getAnimatedValue();
            if (explanationBody.getLayoutParams() != null) {
                explanationBody.getLayoutParams().height = value;
                explanationBody.requestLayout();
            }
        });
        animator.start();

        if (explanationChevron != null) explanationChevron.animate().rotation(180f).setDuration(240).start();
        explanationExpanded = true;
    }

    private void collapseExplanation() {
        if (explanationBody == null) return;
        if (!explanationExpanded) return;

        final int initialHeight = explanationBody.getMeasuredHeight();

        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, 0);
        animator.setDuration(200);
        animator.addUpdateListener(animation -> {
            Integer value = (Integer) animation.getAnimatedValue();
            if (explanationBody.getLayoutParams() != null) {
                explanationBody.getLayoutParams().height = value;
                explanationBody.requestLayout();
            }
        });
        animator.addListener(new ValueAnimator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animator) {

            }

            @Override
            public void onAnimationEnd(@NonNull Animator animator) {
                explanationBody.setVisibility(View.GONE);

            }

            @Override
            public void onAnimationCancel(@NonNull Animator animator) {

            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animator) {

            }

        });
        animator.start();

        if (explanationChevron != null) explanationChevron.animate().rotation(0f).setDuration(200).start();
        explanationExpanded = false;
    }

    private void collapseExplanationQuietly() {
        if (explanationBody == null) return;
        if (explanationBody.getLayoutParams() != null) explanationBody.getLayoutParams().height = 0;
        explanationBody.requestLayout();
        explanationBody.setVisibility(View.GONE);
        if (explanationChevron != null) explanationChevron.setRotation(0f);
        explanationExpanded = false;
    }
}