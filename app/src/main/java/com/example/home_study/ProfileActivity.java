package com.example.home_study;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.home_study.Prevalent.Continuity;
import com.example.home_study.Prevalent.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * ProfileActivity with polished entrance animations and tactile touch effects on cards.
 *
 * - Staggered entrance animation for header, stats and cards.
 * - Press (touch) animation: subtle scale down & lift (translationZ) on ACTION_DOWN, revert on ACTION_UP/CANCEL.
 * - Uses efficient property animations (ViewPropertyAnimator) to preserve performance.
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    // Header (top)
    private TextView headerFullName;
    private TextView headerUsername;

    // Main UI
    private ImageView backArrow;
    private CircleImageView profileImage;
    private ImageView changeProfileBtn;
    private TextView username;    // personal info card
    private TextView fullName;    // personal info card
    private TextView email;
    private TextView phoneNumber;
    private TextView scoreValue;
    private TextView gradeValue;
    private TextView sectionValue;
    private CircularProgressIndicator uploadProgressBar;

    // Firebase
    private StorageReference profileImageRef;
    private Uri croppedImageUri;

    // Image picker
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri sourceUri = result.getData().getData();
                    if (sourceUri != null) startCrop(sourceUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile); // your layout

        initViews();
        initFirebase();
        populateUserData();   // public so other components (bottomsheet) can call it
        initClickListeners();

        // Entrance animations
        animateInitialEntrance();

        // Attach touch/tactile effects to interactive cards (improves perceived responsiveness)
        // in onCreate() after initViews()
        attachTouchEffectsToCards(
                R.id.statCard1,
                R.id.statCard2,
                R.id.statCard3,
                R.id.personalInfoContainer,
                R.id.editProfile,
                R.id.shareBtn,
                R.id.termAndPrivacy,
                R.id.logoutBtn,
                R.id.contactCard,
                R.id.telegramRow,
                R.id.linkedinRow,
                R.id.emailRow
        );
    }

    private void initViews() {
        // Header (optional; ensure your layout has these ids if used)
        headerFullName = findViewById(R.id.headerFullName);
        headerUsername = findViewById(R.id.headerUsername);

        backArrow = findViewById(R.id.profileBackArrow);
        profileImage = findViewById(R.id.profileImage);
        changeProfileBtn = findViewById(R.id.changeProfileBtn);
        uploadProgressBar = findViewById(R.id.uploadProgress);

        username = findViewById(R.id.username);
        fullName = findViewById(R.id.fullName);
        email = findViewById(R.id.email);
        phoneNumber = findViewById(R.id.phoneNumber);

        scoreValue = findViewById(R.id.scoreValue);
        gradeValue = findViewById(R.id.gradeValue);
        sectionValue = findViewById(R.id.sectionValue);
    }

    private void initFirebase() {
        profileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Pictures");
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateUserData();
    }

    /**
     * Public so bottomsheet or other components can refresh UI after edits.
     */
    public void populateUserData() {
        if (Continuity.currentOnlineUser == null) return;

        String name = safe(Continuity.currentOnlineUser.getName());
        String uname = "@" + safe(Continuity.currentOnlineUser.getUsername());

        if (headerFullName != null) headerFullName.setText(name);
        if (headerUsername != null) headerUsername.setText(uname);

        if (fullName != null) fullName.setText(name);
        if (username != null) username.setText(uname);

        if (email != null) email.setText(safe(Continuity.currentOnlineUser.getEmail()));
        if (phoneNumber != null) phoneNumber.setText(safe(Continuity.currentOnlineUser.getPhone()));

        if (uploadProgressBar != null) uploadProgressBar.setVisibility(View.GONE);

        Glide.with(this)
                .load(Continuity.currentOnlineUser.getProfileImage())
                .placeholder(R.drawable.profile_image)
                .error(R.drawable.profile_image)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .circleCrop()
                .into(profileImage);

        loadStudentData();
        loadUserProgress();
    }

    private void initClickListeners() {

        // --- Contact rows (Telegram / LinkedIn / Email) ---
        View telegramRow = findViewById(R.id.telegramRow);
        View linkedinRow = findViewById(R.id.linkedinRow);
        View emailRow = findViewById(R.id.emailRow);

// Replace these with your real contact values
        final String telegramHandle = "your_telegram_handle";           // without @
        final String supportEmail = "support@yourapp.com";
        final String linkedInUrl = "https://www.linkedin.com/company/your-company"; // public profile/company URL

// Telegram: try app, then web
        if (telegramRow != null) {
            telegramRow.setOnClickListener(v -> {
                // try Telegram deep link
                try {
                    Uri tgUri = Uri.parse("tg://resolve?domain=" + telegramHandle);
                    Intent tgIntent = new Intent(Intent.ACTION_VIEW, tgUri);
                    if (tgIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(tgIntent);
                        return;
                    }
                } catch (Exception ignored) { }

                // fallback to web
                try {
                    Uri web = Uri.parse("https://t.me/" + telegramHandle);
                    Intent webIntent = new Intent(Intent.ACTION_VIEW, web);
                    startActivity(webIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "No app available to open Telegram link", Toast.LENGTH_SHORT).show();
                }
            });
        }

// LinkedIn: prefer app, fallback to browser
        if (linkedinRow != null) {
            linkedinRow.setOnClickListener(v -> {
                // try open in LinkedIn app via package + URL
                try {
                    Intent linkedInIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkedInUrl));
                    linkedInIntent.setPackage("com.linkedin.android");
                    if (linkedInIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(linkedInIntent);
                        return;
                    }
                } catch (Exception ignored) { }

                // fallback to browser
                try {
                    Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(linkedInUrl));
                    startActivity(browser);
                } catch (Exception e) {
                    Toast.makeText(this, "No app available to open LinkedIn", Toast.LENGTH_SHORT).show();
                }
            });
        }

// Email: open mail client with subject and optional body
        if (emailRow != null) {
            emailRow.setOnClickListener(v -> {
                try {
                    Intent mail = new Intent(Intent.ACTION_SENDTO);
                    mail.setData(Uri.parse("mailto:" + supportEmail));
                    mail.putExtra(Intent.EXTRA_SUBJECT, "HomeStudy Support");
                    mail.putExtra(Intent.EXTRA_TEXT, "Hi team,\n\nI would like to report...");
                    if (mail.resolveActivity(getPackageManager()) != null) {
                        startActivity(mail);
                    } else {
                        Toast.makeText(this, "No email client found", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to open email client", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (backArrow != null) backArrow.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
            finish();
                });

        if (changeProfileBtn != null) changeProfileBtn.setOnClickListener(v -> {
            changeProfileBtn.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100)
                    .withEndAction(() -> changeProfileBtn.animate().scaleX(1f).scaleY(1f).setDuration(100)).start();
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        View edit = findViewById(R.id.editProfile);
        if (edit != null) edit.setOnClickListener(v ->{
                new EditProfileBottomSheet().show(getSupportFragmentManager(), "EditProfile");
        });


        View share = findViewById(R.id.shareBtn);
        if (share != null) share.setOnClickListener(v -> {
//            animateClickFeedback(share);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out my profile on Home Study app!");
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        View terms = findViewById(R.id.termAndPrivacy);
        if (terms != null) terms.setOnClickListener(v ->{
//            animateClickFeedback(terms);
                Toast.makeText(this, "Open Terms & Privacy", Toast.LENGTH_SHORT).show();

        });

        View logout = findViewById(R.id.logoutBtn);
        if (logout != null) logout.setOnClickListener(v -> {
            // clear in-memory user
            Continuity.currentOnlineUser = null;
            Continuity.userId = null;

            // clear persisted session
            new SessionManager(ProfileActivity.this).clearSession();

            // go to login
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    // -------------------- ANIMATIONS --------------------

    /**
     * Entrance animation: stagger fade/translate for important views.
     * Keeps animations short and hardware-accelerated for good performance.
     */


    /**
     * Use this for programmatic click feedback (e.g., when you call an action and want the same visual).
     * It performs a quick press/release animation and returns immediately.
     */
    private void animateClickFeedback(View v) {
        if (v == null) return;
        final float PRESSED_SCALE = 0.95f;
        final long PRESS_DURATION = 90L;
        final long RELEASE_DURATION = 220L;
        final float LIFT_DP = 6f;
        final float liftPx = getResources().getDisplayMetrics().density * LIFT_DP;
        final DecelerateInterpolator decel = new DecelerateInterpolator();
        final OvershootInterpolator overshoot = new OvershootInterpolator(1.05f);

        v.animate().cancel();
        v.bringToFront();
        v.animate()
                .scaleX(PRESSED_SCALE)
                .scaleY(PRESSED_SCALE)
                .translationZ(liftPx)
                .setDuration(PRESS_DURATION)
                .setInterpolator(decel)
                .withEndAction(() -> {
                    v.animate().cancel();
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .translationZ(0f)
                            .setDuration(RELEASE_DURATION)
                            .setInterpolator(overshoot)
                            .start();
                })
                .start();
    }
    private void animateInitialEntrance() {
        final int baseDelay = 80;
        final int itemDuration = 420;
        final DecelerateInterpolator decel = new DecelerateInterpolator();

        // profile avatar
        if (profileImage != null) {
            profileImage.setTranslationY(-6f);
            profileImage.setAlpha(0f);
            profileImage.animate()
                    .translationY(0f).alpha(1f)
                    .setStartDelay(60)
                    .setDuration(itemDuration)
                    .setInterpolator(new OvershootInterpolator(1.0f))
                    .start();
        }

        // small header texts
        View[] headerItems = new View[] {
                headerFullName != null ? headerFullName : null,
                headerUsername != null ? headerUsername : null
        };
        int index = 0;
        for (View v : headerItems) {
            if (v == null) continue;
            v.setTranslationY(20f);
            v.setAlpha(0f);
            v.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setStartDelay(120 + index * baseDelay)
                    .setDuration(itemDuration)
                    .setInterpolator(decel)
                    .start();
            index++;
        }

        // stats cards and content
        int[] entranceIds = new int[] {
                R.id.statCard1, R.id.statCard2, R.id.statCard3,
                R.id.personalInfoContainer, R.id.editProfile, R.id.shareBtn, R.id.termAndPrivacy, R.id.logoutBtn
        };

        for (int i = 0; i < entranceIds.length; i++) {
            View item = findViewById(entranceIds[i]);
            if (item == null) continue;
            item.setTranslationY(28f);
            item.setAlpha(0f);
            item.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setStartDelay(200 + i * baseDelay)
                    .setDuration(itemDuration)
                    .setInterpolator(decel)
                    .start();
        }
    }

    /**
     * Attach tactile touch effect (scale + lift) to MaterialCardView or any view.
     * Keeps effect lightweight: quick scale and translationZ, restored on release/cancel.
     */
    private void attachTouchEffectsToCards(int... viewIds) {
        final float PRESSED_SCALE = 0.97f;          // scale when pressed
        final long PRESS_DURATION = 90L;            // ms
        final long RELEASE_DURATION = 220L;         // ms
        final float LIFT_DP = 6f;                   // dp of translationZ when pressed
        final float liftPx = getResources().getDisplayMetrics().density * LIFT_DP;
        final DecelerateInterpolator decel = new DecelerateInterpolator();
        final OvershootInterpolator overshoot = new OvershootInterpolator(1.05f);

        View.OnTouchListener touch = (v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    // cancel any running animators cleanly
                    v.animate().cancel();
                    // ensure we draw above siblings during press (optional)
                    v.bringToFront();

                    // press animation: scale down + lift quickly
                    v.animate()
                            .scaleX(PRESSED_SCALE)
                            .scaleY(PRESSED_SCALE)
                            .translationZ(liftPx)
                            .setDuration(PRESS_DURATION)
                            .setInterpolator(decel)
                            .start();

                    // set pressed for ripple feedback
                    v.setPressed(true);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // cancel again and animate back with a pleasant overshoot
                    v.animate().cancel();

                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .translationZ(0f)
                            .setDuration(RELEASE_DURATION)
                            .setInterpolator(overshoot)
                            .start();

                    v.setPressed(false);
                    break;
            }
            // Return false so the normal click handling still occurs.
            return false;
        };

        for (int id : viewIds) {
            View v = findViewById(id);
            if (v == null) continue;
            v.setClickable(true);
            v.setFocusable(true);
            // attach the touch listener
            v.setOnTouchListener(touch);
            // optionally ensure ripple is visible by setting foreground (if using CardView/MaterialCardView)
            // v.setForeground(ContextCompat.getDrawable(this, R.attr.selectableItemBackgroundBorderless));
        }
    }


    // -------------------- IMAGE PICK & CROP --------------------

    private void startCrop(Uri sourceUri) {
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), System.currentTimeMillis() + "_crop.jpg"));
        UCrop.Options options = new UCrop.Options();
        options.setToolbarColor(getColorSafe(R.color.primary_secondary));
        options.setStatusBarColor(getColorSafe(R.color.primary_secondary));
        options.setActiveControlsWidgetColor(getColorSafe(R.color.primary_secondary));
        UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(1024, 1024)
                .withOptions(options)
                .start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // UCrop returns here
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            croppedImageUri = UCrop.getOutput(data);
            if (croppedImageUri != null) {
                profileImage.setImageURI(croppedImageUri);
                uploadProfileImage();
            }
        }
    }

    // -------------------- UPLOAD --------------------

    private void uploadProfileImage() {
        if (croppedImageUri == null) return;
        if (uploadProgressBar != null) uploadProgressBar.setVisibility(View.VISIBLE);

        StorageReference fileRef = profileImageRef.child(Continuity.currentOnlineUser.getUserId() + ".jpg");

        fileRef.putFile(croppedImageUri)
                .addOnProgressListener(snapshot -> {
                    if (uploadProgressBar != null) uploadProgressBar.setVisibility(View.VISIBLE);
                    long bytesTransferred = snapshot.getBytesTransferred();
                    long totalBytes = snapshot.getTotalByteCount();
                    int progress = totalBytes > 0 ? (int) ((200.0 * bytesTransferred) / totalBytes) : 0;
                    if (uploadProgressBar != null) uploadProgressBar.setProgress(progress);
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return fileRef.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    Map<String, Object> update = new HashMap<>();
                    update.put("profileImage", imageUrl);
                    FirebaseDatabase.getInstance().getReference("Users")
                            .child(Continuity.currentOnlineUser.getUserId())
                            .updateChildren(update)
                            .addOnSuccessListener(unused -> {
                                Continuity.currentOnlineUser.setProfileImage(imageUrl);
                                if (uploadProgressBar != null) uploadProgressBar.setVisibility(View.GONE);
                                Glide.with(ProfileActivity.this)
                                        .load(imageUrl)
                                        .skipMemoryCache(true)
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                                        .circleCrop()
                                        .into(profileImage);
                                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (uploadProgressBar != null) uploadProgressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // -------------------- STUDENT DATA LOADING --------------------

    private interface StudentSnapshotHandler {
        void handle(DataSnapshot ds);
    }

    private void loadStudentData() {
        if (Continuity.currentOnlineUser == null) {
            if (gradeValue != null) gradeValue.setText("--");
            if (sectionValue != null) sectionValue.setText("--");
            return;
        }

        String uid = Continuity.currentOnlineUser.getUserId();
        String studentId = Continuity.currentOnlineUser.getStudentId();

        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("Students");

        StudentSnapshotHandler applySnapshot = ds -> {
            Log.d(TAG, "Student snapshot key=" + ds.getKey() + " value=" + ds.getValue());
            String grade = null;
            String section = null;

            Object gObj = ds.child("grade").getValue();
            if (gObj != null) grade = String.valueOf(gObj);

            Object sObj = ds.child("section").getValue();
            if (sObj != null) section = String.valueOf(sObj);

            if (section == null) {
                DataSnapshot parents = ds.child("parents");
                if (parents.exists()) {
                    Object sec = parents.child("section").getValue();
                    if (sec != null) section = String.valueOf(sec);
                }
            }
            if (section == null) {
                DataSnapshot parent = ds.child("parent");
                if (parent.exists()) {
                    Object sec = parent.child("section").getValue();
                    if (sec != null) section = String.valueOf(sec);
                }
            }

            if (grade == null) {
                Object alt = ds.child("gradeLevel").getValue();
                if (alt != null) grade = String.valueOf(alt);
                else {
                    alt = ds.child("class").getValue();
                    if (alt != null) grade = String.valueOf(alt);
                }
            }

            final String finalGrade = grade != null ? grade : "--";
            final String finalSection = section != null ? section : "--";

            runOnUiThread(() -> {
                if (gradeValue != null) gradeValue.setText(finalGrade);
                if (sectionValue != null) sectionValue.setText(finalSection);
                Log.d(TAG, "Applied grade=" + finalGrade + " section=" + finalSection);
            });
        };

        if (studentId != null && !studentId.trim().isEmpty()) {
            studentsRef.child(studentId).get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    applySnapshot.handle(snapshot);
                } else {
                    Log.w(TAG, "No Students/" + studentId + " node found, falling back to query by userId");
                    queryStudentsByUserId(studentsRef, uid, applySnapshot);
                }
            }).addOnFailureListener(e -> {
                Log.w(TAG, "Failed reading Students/" + studentId + " : " + e.getMessage());
                queryStudentsByUserId(studentsRef, uid, applySnapshot);
            });
        } else {
            queryStudentsByUserId(studentsRef, uid, applySnapshot);
        }
    }

    private void queryStudentsByUserId(DatabaseReference studentsRef, String uid, StudentSnapshotHandler handler) {
        if (uid == null || uid.trim().isEmpty()) {
            Log.w(TAG, "userId is null or empty, cannot query Students");
            if (gradeValue != null) gradeValue.setText("--");
            if (sectionValue != null) sectionValue.setText("--");
            return;
        }

        studentsRef.orderByChild("userId").equalTo(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        Log.w(TAG, "No Students entry found for userId=" + uid);
                        if (gradeValue != null) gradeValue.setText("--");
                        if (sectionValue != null) sectionValue.setText("--");
                        return;
                    }
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        handler.handle(ds);
                        break;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed querying Students by userId: " + e.getMessage(), e);
                    if (gradeValue != null) gradeValue.setText("--");
                    if (sectionValue != null) sectionValue.setText("--");
                });
    }

    // -------------------- USER PROGRESS --------------------

    private void loadUserProgress() {
        if (Continuity.userId == null || Continuity.userId.isEmpty()) {
            if (scoreValue != null) scoreValue.setText("--");
            return;
        }

        DatabaseReference resultsRef = FirebaseDatabase.getInstance().getReference("ExamResults").child(Continuity.userId);
        resultsRef.get().addOnSuccessListener(snapshot -> {
            long sumScore = 0;
            long sumTotal = 0;
            if (snapshot.exists()) {
                for (DataSnapshot examSnap : snapshot.getChildren()) {
                    Long score = examSnap.child("score").getValue(Long.class);
                    Long total = examSnap.child("total").getValue(Long.class);
                    if (score == null) {
                        Integer sInt = examSnap.child("score").getValue(Integer.class);
                        if (sInt != null) score = sInt.longValue();
                    }
                    if (total == null) {
                        Integer tInt = examSnap.child("total").getValue(Integer.class);
                        if (tInt != null) total = tInt.longValue();
                    }
                    if (score != null && total != null && total > 0) {
                        sumScore += score;
                        sumTotal += total;
                    }
                }
            }

            if (sumTotal > 0) {
                int percent = (int) Math.round((sumScore * 100.0) / sumTotal);
                if (scoreValue != null) scoreValue.setText(percent + "%");
            } else {
                if (scoreValue != null) scoreValue.setText("--");
            }
        }).addOnFailureListener(e -> {
            if (scoreValue != null) scoreValue.setText("--");
        });
    }

    // helpers
    private String safe(String s) { return s == null ? "" : s; }
    private int getColorSafe(int id) { return ContextCompat.getColor(this, id); }
}