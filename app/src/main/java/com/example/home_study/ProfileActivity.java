package com.example.home_study;

import android.annotation.SuppressLint;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.home_study.Prevalent.Continuity;
import com.example.home_study.Prevalent.SessionManager;
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
 * ProfileActivity - refined touch animations and click-feedback handling.
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
        setContentView(R.layout.activity_profile);

        initViews();
        initFirebase();
        populateUserData();
        initClickListeners();

        // Entrance animations
        animateInitialEntrance();

        // Attach tactile effects for interactive cards/rows (use IDs present in layout)
        attachTouchEffectsToCards(
                R.id.statCard1,
                R.id.statCard2,
                R.id.statCard3,
                R.id.personalInfoContainer,
                R.id.editProfile,
                R.id.accountDataInfo,
                R.id.termAndPrivacy,
                R.id.logoutBtn,
                R.id.contactCard,
                R.id.telegramRow,
                R.id.emailRow
        );
    }

    private void initViews() {
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
        final String telegramHandle = "gojo_edu"; // without @
        final String supportEmail = "gojo.education1@gmail.com";
        final String accountDataInfoLink = "https://ephrem-t.github.io/Gojo-Account-Data-Info/";
        final String termsAndPrivacy = "https://sites.google.com/view/gojo-parent-privacy";

        View telegramRow = findViewById(R.id.telegramRow);
        View emailRow = findViewById(R.id.emailRow);
        View accountDataInfo = findViewById(R.id.accountDataInfo);
        View terms = findViewById(R.id.termAndPrivacy);
        View logout = findViewById(R.id.logoutBtn);
        View edit = findViewById(R.id.editProfile);

        // Telegram
        if (telegramRow != null) telegramRow.setOnClickListener(v -> {
            animateClickFeedback(v);
            v.postDelayed(() -> {
                try {
                    Uri tgUri = Uri.parse("tg://resolve?domain=" + telegramHandle);
                    Intent tgIntent = new Intent(Intent.ACTION_VIEW, tgUri);
                    if (tgIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(tgIntent);
                        return;
                    }
                } catch (Exception ignored) { }
                try {
                    Uri web = Uri.parse("https://t.me/" + telegramHandle);
                    startActivity(new Intent(Intent.ACTION_VIEW, web));
                } catch (Exception e) {
                    Toast.makeText(this, "No app/browser available to open Telegram", Toast.LENGTH_SHORT).show();
                }
            }, 120);
        });

        // Email
        if (emailRow != null) emailRow.setOnClickListener(v -> {
            animateClickFeedback(v);
            v.postDelayed(() -> {
                String subject = "GojoStudy Support";
                String body = "Hi team,\n\nI would like to report...";
                try {
                    // 1) Preferred: ACTION_SENDTO with a properly encoded mailto: URI
                    String uriText = "mailto:" + Uri.encode(supportEmail) +
                            "?subject=" + Uri.encode(subject) +
                            "&body=" + Uri.encode(body);
                    Uri mailUri = Uri.parse(uriText);
                    Intent mailIntent = new Intent(Intent.ACTION_SENDTO, mailUri);

                    if (mailIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mailIntent);
                        return;
                    }

                    // 2) Fallback: ACTION_SEND with chooser (covers more apps)
                    Intent fallback = new Intent(Intent.ACTION_SEND);
                    fallback.setType("message/rfc822");
                    fallback.putExtra(Intent.EXTRA_EMAIL, new String[]{ supportEmail });
                    fallback.putExtra(Intent.EXTRA_SUBJECT, subject);
                    fallback.putExtra(Intent.EXTRA_TEXT, body);
                    startActivity(Intent.createChooser(fallback, "Send email"));
                } catch (android.content.ActivityNotFoundException ex) {
                    // No email clients installed
                    Toast.makeText(this, "No email client found on this device", Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    Toast.makeText(this, "Unable to open email client: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }, 100); // small delay so the touch animation is visible
        });
        // Account data info link
        if (accountDataInfo != null) accountDataInfo.setOnClickListener(v -> {
            animateClickFeedback(v);
            v.postDelayed(() -> {
                try {
                    Uri web = Uri.parse(accountDataInfoLink);
                    startActivity(new Intent(Intent.ACTION_VIEW, web));
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to open the link", Toast.LENGTH_SHORT).show();
                }
            }, 120);
        });

        // Terms
        if (terms != null) terms.setOnClickListener(v -> {
            animateClickFeedback(v);
            v.postDelayed(() -> {
                try {
                    Uri web = Uri.parse(termsAndPrivacy);
                    startActivity(new Intent(Intent.ACTION_VIEW, web));
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to open the link", Toast.LENGTH_SHORT).show();
                }
            }, 120);
        });

        // Edit Profile (bottom sheet)
        if (edit != null) edit.setOnClickListener(v -> {
            animateClickFeedback(v);
            v.postDelayed(() -> new EditProfileBottomSheet().show(getSupportFragmentManager(), "EditProfile"), 120);
        });

        // Logout
        if (logout != null) logout.setOnClickListener(v -> {
            animateClickFeedback(v);
            v.postDelayed(() -> {
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
            }, 120);
        });

        // Back arrow
        if (backArrow != null) backArrow.setOnClickListener(v -> {
            animateClickFeedback(v);
            v.postDelayed(() -> {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            }, 80);
        });

        // Change profile image
        if (changeProfileBtn != null) changeProfileBtn.setOnClickListener(v -> {
            animateClickFeedback(v);
            v.postDelayed(() -> imagePickerLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)), 120);
        });
    }

    // -------------------- ANIMATIONS --------------------

    /**
     * Use this for programmatic click feedback.
     * Removed bringToFront() to avoid layout re-orders that cause stutter.
     */
    // Replaces your current animateClickFeedback & attachTouchEffectsToCards
    private void animateClickFeedback(View v) {
        if (v == null) return;
        final float PRESSED_SCALE = 0.96f;
        final long PRESS_DURATION = 100L;      // visible press time
        final long RELEASE_DURATION = 180L;    // smooth release
        final float LIFT_DP = 4f;
        final float liftPx = getResources().getDisplayMetrics().density * LIFT_DP;
        final DecelerateInterpolator decel = new DecelerateInterpolator();
        final OvershootInterpolator overshoot = new OvershootInterpolator(1.02f);

        v.animate().cancel();
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

    private void attachTouchEffectsToCards(int... viewIds) {
        final float PRESSED_SCALE = 0.97f;
        final long PRESS_DURATION = 90L;
        final long RELEASE_DURATION = 160L;
        final float LIFT_DP = 4f;
        final float liftPx = getResources().getDisplayMetrics().density * LIFT_DP;
        final DecelerateInterpolator decel = new DecelerateInterpolator();
        final OvershootInterpolator overshoot = new OvershootInterpolator(1.02f);

        @SuppressLint("ClickableViewAccessibility") View.OnTouchListener touch = (v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().cancel();
                    v.animate()
                            .scaleX(PRESSED_SCALE)
                            .scaleY(PRESSED_SCALE)
                            .translationZ(liftPx)
                            .setDuration(PRESS_DURATION)
                            .setInterpolator(decel)
                            .start();
                    // keep ripple visible
                    v.setPressed(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
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
            // Return false so ripple and click are handled by the system
            return false;
        };

        for (int id : viewIds) {
            View v = findViewById(id);
            if (v == null) continue;
            v.setClickable(true);
            v.setFocusable(true);
            v.setOnTouchListener(touch);
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
    private interface StudentSnapshotHandler { void handle(DataSnapshot ds); }

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
                R.id.personalInfoContainer, R.id.editProfile, R.id.accountDataInfo, R.id.termAndPrivacy, R.id.logoutBtn
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

    // -------------------- USER PROGRESS --------------------
    @SuppressLint("SetTextI18n")
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