package com.example.home_study;

import static java.security.AccessController.getContext;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.home_study.Adapter.ExamContentAdapter;
import com.example.home_study.Model.ExamContent;
import com.example.home_study.Model.UserHelper;
import com.example.home_study.Prevalent.Continuity;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private UserHelper userHelper;
    private String  myUri;
    private Uri uriContent;
    private StorageTask uploadTask;

    private StorageReference profilePictureRef;

    private ImageView profilePic, editBtn;
    private TextView userName, userEmail, attendance, classPoint ;

    private RecyclerView listRecycler;
    private List course;
    private boolean isClassPointSelected = true;
    private ExamContentAdapter examContentAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        profilePictureRef = FirebaseStorage.getInstance().getReference().child("Profile Pictures");
        profilePic = findViewById(R.id.profilePic);
        editBtn = findViewById(R.id.editProfile);
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        listRecycler = findViewById(R.id.subjectList);
//        attendance = findViewById(R.id.attendance);
        classPoint = findViewById(R.id.classPointTxt);

        course = new ArrayList();
        examContentAdapter = new ExamContentAdapter(course,this::onExamContentSelected , this);

        listRecycler.setLayoutManager(new LinearLayoutManager(this));
        listRecycler.setHasFixedSize(true);
        listRecycler.setAdapter(examContentAdapter);
        loadCourse();





//            if (!isClassPointSelected){
//                attendance.setBackgroundColor(ContextCompat.getColor(this,R.color.white));
//                classPoint.setBackgroundColor(ContextCompat.getColor(this, R.color.black_low_low));
//                isClassPointSelected = true;
                loadCourse();
//            }


//        attendance.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (isClassPointSelected){
//                    attendance.setBackgroundColor(ContextCompat.getColor(ProfileActivity.this,R.color.black_low_low));
//                    classPoint.setBackgroundColor(ContextCompat.getColor(ProfileActivity.this, R.color.white));
//                    isClassPointSelected = false;
////                    loadCourseAttendance();
//                }
//            }
//        });





//        Glide.with(getContext()).load(Continuity.currentOnlineUser.getimageUrl())
//                .into(profilePic);
//        userName.setText(Continuity.currentOnlineUser.getName());
//        userEmail.setText(Continuity.currentOnlineUser.getEmail());

        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                photoLibraryLauncher.launch(intent);
            }
        });
    }

    private final ActivityResultLauncher<Intent> cropActivityLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri resultUri = UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        uriContent = resultUri;  // Store cropped image URI
                        profilePic.setImageURI(resultUri);  // Directly set the cropped image
                        Glide.with(this)
                                .load(resultUri)
                                .placeholder(R.drawable.profile)
                                .skipMemoryCache(true)
                                .into(profilePic);
                        uploadImage();  // Upload image to Firebase
                    } else {
                        Toast.makeText(this, "Crop failed. Try again!", Toast.LENGTH_SHORT).show();
                    }
                } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                    Throwable error = UCrop.getError(result.getData());
                    Toast.makeText(this, "Crop Error: " + (error != null ? error.getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
                }
            });


    private void onExamContentSelected(ExamContent examContent) {

        Intent intent;
        if (isClassPointSelected) {
            intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            Toast.makeText(this, "this is class point", Toast.LENGTH_SHORT).show();

        } else {
            intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            Toast.makeText(this, "this is attendance", Toast.LENGTH_SHORT).show();
        }
        intent.putExtra("course_name", examContent.getContentSubject());
        startActivity(intent);
    }

    private void loadCourse()
    {
        course.clear();
        course.add(new ExamContent("English", "Score", R.drawable.english));
        course.add(new ExamContent("Mathematics", "Score", R.drawable.math));
        course.add(new ExamContent("Physics", "Score", R.drawable.physics));
        course.add(new ExamContent("Biology", "Score", R.drawable.biology));
        course.add(new ExamContent("Chemistry", "Score", R.drawable.chemistry));
        course.add(new ExamContent("Geography", "Score", R.drawable.geography));
        course.add(new ExamContent("History", "Score", R.drawable.history));
        examContentAdapter.notifyDataSetChanged();

    }

    private final ActivityResultLauncher<Intent> photoLibraryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        startCropActivity(selectedImageUri);
                    }
                }
            });

    private void startCropActivity(Uri sourceUri) {
        Uri destinationUri = Uri.fromFile(new File(this.getCacheDir(), System.currentTimeMillis() + "_cropped.jpg"));

        UCrop.Options options = new UCrop.Options();
        options.setToolbarColor(ContextCompat.getColor(this, R.color.black_low_low));
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.primary_secondary));
        options.setActiveControlsWidgetColor(ContextCompat.getColor(this, R.color.primary_secondary));
        options.setToolbarTitle("Crop Image");

        Intent intent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(512, 512)
                .withOptions(options)
                .getIntent(this);

        cropActivityLauncher.launch(intent);
    }

    private void uploadImage() {
        if (uriContent != null) {  // Ensure that uriContent is not null before uploading
            final StorageReference fileRef = profilePictureRef
                    .child(Continuity.currentOnlineUser.getUsername() + ".jpg");

            uploadTask = fileRef.putFile(uriContent);

            uploadTask.continueWithTask(new Continuation<Task<Uri>, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<Task<Uri>> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return fileRef.getDownloadUrl();  // Get the download URL after uploading
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        myUri = downloadUri.toString();

                        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users");

                        HashMap<String, Object> userData = new HashMap<>();
                        userData.put("imageUrl", myUri);

                        ref.child(Continuity.currentOnlineUser.getUsername()).updateChildren(userData)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(ProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                                            Continuity.currentOnlineUser.setImageUrl(myUri);
                                        } else {
                                            Toast.makeText(ProfileActivity.this, "Error occurred, try again", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    } else {
                        Toast.makeText(ProfileActivity.this, "Error uploading image", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            Toast.makeText(this, "Image is not selected", Toast.LENGTH_SHORT).show();
        }
    }

}