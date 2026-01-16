package com.example.home_study;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.home_study.Prevalent.Continuity;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    // UI
    private ImageView backArrow, changeProfileBtn;
    private CircleImageView profileImage;
    private TextView username, fullName, email, phoneNumber;
    private TextView gradeValue, sectionValue;
    private CircularProgressIndicator uploadProgressBar;

    // Firebase
    private StorageReference profileImageRef;
    private Uri croppedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        initFirebase();
        populateUserData();
        initClickListeners();

        // entrance animations for content -- subtle and performant
        profileImage.setScaleX(0.92f);
        profileImage.setScaleY(0.92f);
        profileImage.setAlpha(0f);
        profileImage.animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(420).setInterpolator(new DecelerateInterpolator()).start();

        // slight stagger for info
        ViewCompat.animate(findViewById(R.id.personalInfoContainer))
                .translationY(18f).alpha(0f)
                .setDuration(0).start();
        findViewById(R.id.personalInfoContainer).postDelayed(() ->
                        ViewCompat.animate(findViewById(R.id.personalInfoContainer))
                                .translationY(0f).alpha(1f).setDuration(420).setInterpolator(new DecelerateInterpolator()).start()
                , 140);
    }

    private void initViews() {
        profileImage = findViewById(R.id.profileImage);
        changeProfileBtn = findViewById(R.id.changeProfileBtn);
        backArrow = findViewById(R.id.profileBackArrow);
        uploadProgressBar = findViewById(R.id.uploadProgress);

        username = findViewById(R.id.username);
        fullName = findViewById(R.id.fullName);
        email = findViewById(R.id.email);
        phoneNumber = findViewById(R.id.phoneNumber);

        gradeValue = findViewById(R.id.gradeValue);
        sectionValue = findViewById(R.id.sectionValue);
    }

    private void initFirebase() {
        profileImageRef = FirebaseStorage.getInstance()
                .getReference()
                .child("Profile Pictures");
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateUserData();
    }

    public void populateUserData() {
        if (Continuity.currentOnlineUser == null) return;

        username.setText("@" + safe(Continuity.currentOnlineUser.getUsername()));
        fullName.setText(safe(Continuity.currentOnlineUser.getName()));
        email.setText(safe(Continuity.currentOnlineUser.getEmail()));
        phoneNumber.setText(safe(Continuity.currentOnlineUser.getPhone()));

        // Glide cross-fade for avatar (fast and efficient)
        Glide.with(this)
                .load(Continuity.currentOnlineUser.getProfileImage())
                .placeholder(R.drawable.profile_image)
                .error(R.drawable.profile_image)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .circleCrop()
                .into(profileImage);

        // load student-specific details from Realtime DB
        loadStudentData();
    }

    private void initClickListeners() {
        backArrow.setOnClickListener(v -> finish());

        changeProfileBtn.setOnClickListener(v -> {
            // small tactile animation
            changeProfileBtn.animate().scaleX(0.92f).scaleY(0.92f).setDuration(120)
                    .withEndAction(() -> changeProfileBtn.animate().scaleX(1f).scaleY(1f).setDuration(120)).start();

            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        findViewById(R.id.editProfile).setOnClickListener(v ->
                new EditProfileBottomSheet().show(getSupportFragmentManager(), "EditProfile"));

        findViewById(R.id.logoutBtn).setOnClickListener(v -> {
            // replace with actual logout flow; for now finish
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    // -------------------- IMAGE PICK & CROP --------------------

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri sourceUri = result.getData().getData();
                    if (sourceUri != null) startCrop(sourceUri);
                }
            });

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // UCrop uses startActivityForResult API; keep this override for compatibility
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            croppedImageUri = UCrop.getOutput(data);
            if (croppedImageUri != null) {
                profileImage.setImageURI(croppedImageUri);
                uploadProfileImage();
            }
        }
    }

    private void startCrop(Uri sourceUri) {
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), System.currentTimeMillis() + "_crop.jpg"));
        UCrop.Options options = new UCrop.Options();
        options.setToolbarColor(getColorSafe(R.color.primary_secondary));
        options.setStatusBarColor(getColorSafe(R.color.primary_secondary));
        options.setActiveControlsWidgetColor(getColorSafe(R.color.primary_secondary));
        UCrop.of(sourceUri, destinationUri).withAspectRatio(1, 1).withMaxResultSize(512, 512).withOptions(options).start(this);
    }

    // -------------------- UPLOAD --------------------

    private void uploadProfileImage() {
        if (croppedImageUri == null) return;

        uploadProgressBar.setVisibility(View.VISIBLE);
        StorageReference fileRef = profileImageRef.child(Continuity.currentOnlineUser.getUserId() + ".jpg");

        fileRef.putFile(croppedImageUri)
                .addOnProgressListener(snapshot -> {
                    uploadProgressBar.setVisibility(View.VISIBLE);
                    long bytesTransfered = snapshot.getBytesTransferred();
                    long totalBytes = snapshot.getTotalByteCount();
                    int progress = (int) ((200.0 * bytesTransfered) / totalBytes);
                    uploadProgressBar.setProgress(progress);
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return fileRef.getDownloadUrl();
                }).addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    HashMap<String, Object> update = new HashMap<>();
                    update.put("profileImage", imageUrl);
                    FirebaseDatabase.getInstance().getReference("Users")
                            .child(Continuity.currentOnlineUser.getUserId())
                            .updateChildren(update)
                            .addOnSuccessListener(unused -> {
                                Continuity.currentOnlineUser.setProfileImage(imageUrl);
                                uploadProgressBar.setVisibility(View.GONE);
                                Glide.with(ProfileActivity.this).load(imageUrl)
                                        .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                                        .circleCrop().into(profileImage);
                                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                            });
                }).addOnFailureListener(e -> {
                    uploadProgressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadStudentData() {
        FirebaseDatabase.getInstance().getReference("Students")
                .orderByChild("userId")
                .equalTo(Continuity.currentOnlineUser.getUserId())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        gradeValue.setText("--");
                        sectionValue.setText("--");
                        return;
                    }
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Object g = ds.child("grade").getValue();
                        Object s = ds.child("section").getValue();
                        gradeValue.setText(g != null ? String.valueOf(g) : "--");
                        sectionValue.setText(s != null ? String.valueOf(s) : "--");
                        break;
                    }
                })
                .addOnFailureListener(e -> {
                    gradeValue.setText("--");
                    sectionValue.setText("--");
                });
    }

    // small helpers
    private String safe(String s) { return s == null ? "" : s; }
    private int getColorSafe(int id) { return ContextCompat.getColor(this, id); }
}