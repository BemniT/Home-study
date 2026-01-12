package com.example.home_study;

import static org.bouncycastle.asn1.x509.ReasonFlags.unused;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.home_study.Prevalent.Continuity;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
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
    private ImageView  backArrow;
    private CircleImageView profileImage, changeProfileBtn;
    private TextView username, fullName, email, phoneNumber;
    private TextView grade, section;
    private CircularProgressIndicator uploadProgressBar;

    // Firebase
    private StorageReference profileImageRef;
    private Uri croppedImageUri;

    @Override
    protected void onResume() {
        super.onResume();
        populateUserData();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        findViewById(R.id.profile_main)
                .startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_slide_up));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initFirebase();
        populateUserData();
        initClickListeners();
    }



    // -------------------- INIT --------------------

    private void initViews() {
        profileImage = findViewById(R.id.profileImage);
        changeProfileBtn = findViewById(R.id.changeProfileBtn);
        backArrow = findViewById(R.id.profileBackArrow);
        uploadProgressBar = findViewById(R.id.uploadProgress);

        username = findViewById(R.id.username);
        fullName = findViewById(R.id.fullName);
        email = findViewById(R.id.email);
        phoneNumber = findViewById(R.id.phoneNumber);

        grade = findViewById(R.id.grade);
        section = findViewById(R.id.section);
    }

    private void initFirebase() {
        profileImageRef = FirebaseStorage.getInstance()
                .getReference()
                .child("Profile Pictures");
    }

    // -------------------- DATA --------------------

    public void populateUserData() {
        if (Continuity.currentOnlineUser == null) return;

        username.setText(Continuity.currentOnlineUser.getUsername());
        fullName.setText(Continuity.currentOnlineUser.getName());
        email.setText(Continuity.currentOnlineUser.getEmail());
        phoneNumber.setText(Continuity.currentOnlineUser.getPhone());


        Glide.with(this)
                .load(Continuity.currentOnlineUser.getProfileImage())
                .placeholder(R.drawable.profile_image)
                .into(profileImage);
        loadStudentData();

    }

    // -------------------- CLICKS --------------------

    private void initClickListeners() {

        backArrow.setOnClickListener(v -> finish());

        changeProfileBtn.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            );
            imagePickerLauncher.launch(intent);
        });

        findViewById(R.id.editProfile).setOnClickListener(v ->

                new EditProfileBottomSheet().show(getSupportFragmentManager(), "EditProfile")
        );

        findViewById(R.id.logoutBtn).setOnClickListener(v -> {
//            Continuity.logout(this);
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
        Uri destinationUri = Uri.fromFile(
                new File(getCacheDir(), System.currentTimeMillis() + "_crop.jpg")
        );

        UCrop.Options options = new UCrop.Options();
        options.setToolbarColor(ContextCompat.getColor(this, R.color.black_low_low));
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.primary_secondary));
        options.setActiveControlsWidgetColor(ContextCompat.getColor(this, R.color.primary_secondary));

        UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(512, 512)
                .withOptions(options)
                .start(this);
    }


    // -------------------- UPLOAD --------------------

    private void uploadProfileImage() {
        if (croppedImageUri == null) return;

        uploadProgressBar.setVisibility(View.VISIBLE);
        StorageReference fileRef = profileImageRef
                .child(Continuity.currentOnlineUser.getUserId() + ".jpg");

        fileRef.putFile(croppedImageUri)
                .addOnProgressListener(snapshot -> {

                        uploadProgressBar.setVisibility(View.VISIBLE);
                        long bytesTransfered = snapshot.getBytesTransferred();
                        long totalBytes = snapshot.getTotalByteCount();

                        int progress = (int) ((200.0 * bytesTransfered) / totalBytes);
                        uploadProgressBar.setProgress(progress);
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw  task.getException();
                    }
                      return  fileRef.getDownloadUrl();
                }).addOnSuccessListener(task -> {


                    String imageUrl = task.toString();

                    HashMap<String, Object> update = new HashMap<>();
                    update.put("profileImage", imageUrl);

                    FirebaseDatabase.getInstance()
                            .getReference("Users")
                            .child(Continuity.currentOnlineUser.getUserId())
                            .updateChildren(update)
                            .addOnSuccessListener(unused -> {
                                Continuity.currentOnlineUser.setProfileImage(imageUrl);

                                uploadProgressBar.setVisibility(View.GONE);

                                Glide.with(ProfileActivity.this)
                                                .load(imageUrl)
                                                        .skipMemoryCache(true)
                                                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                                        .into(profileImage);
                                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                            });
                }).addOnFailureListener(e -> {
                   uploadProgressBar.setVisibility(View.GONE);

                   Snackbar.make(profileImage, "Upload failed", Snackbar.LENGTH_INDEFINITE)
                           .setAction("Retry", v -> uploadProfileImage())
                           .show();
                });

    }

    private void loadStudentData() {
        FirebaseDatabase.getInstance()
                .getReference("Students")
                .orderByChild("userId")
                .equalTo(Continuity.currentOnlineUser.getUserId())
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.exists()){
                        grade.setText("--");
                        section.setText("--");
                        return;
                    }
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        grade.setText(String.valueOf(ds.child("grade").getValue()));
                        section.setText(String.valueOf(ds.child("section").getValue()));

                        Log.e("grade",String.valueOf(ds.child("grade").getValue()));
                        Log.e("grade",String.valueOf(ds.child("section").getValue()));
                        break;
                    }
                });
    }
}