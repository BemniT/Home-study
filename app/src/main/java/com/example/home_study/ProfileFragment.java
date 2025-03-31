package com.example.home_study;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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
import java.util.HashMap;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private UserHelper userHelper;
    private String  myUri;
    private Uri uriContent;
    private StorageTask uploadTask;

    private StorageReference profilePictureRef;

    private ImageView profilePic, editBtn;
    private TextView userName, userEmail;


    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ProfileFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }


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
                        Toast.makeText(getContext(), "Crop failed. Try again!", Toast.LENGTH_SHORT).show();
                    }
                } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                    Throwable error = UCrop.getError(result.getData());
                    Toast.makeText(getContext(), "Crop Error: " + (error != null ? error.getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
                }
            });



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        // Inflate the layout for this fragment

        profilePictureRef = FirebaseStorage.getInstance().getReference().child("Profile Pictures");
        profilePic = view.findViewById(R.id.profilePic);
        editBtn = view.findViewById(R.id.editProfile);
        userName = view.findViewById(R.id.userName);
        userEmail = view.findViewById(R.id.userEmail);


        Glide.with(getContext()).load(Continuity.currentOnlineUser.getimageUrl())
                .into(profilePic);
        userName.setText(Continuity.currentOnlineUser.getName());
        userEmail.setText(Continuity.currentOnlineUser.getEmail());

        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                photoLibraryLauncher.launch(intent);
            }
        });
        return view;
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
        Uri destinationUri = Uri.fromFile(new File(getActivity().getCacheDir(), System.currentTimeMillis() + "_cropped.jpg"));

        UCrop.Options options = new UCrop.Options();
        options.setToolbarColor(ContextCompat.getColor(getContext(), R.color.black_low_low));
        options.setStatusBarColor(ContextCompat.getColor(getContext(), R.color.primary_secondary));
        options.setActiveControlsWidgetColor(ContextCompat.getColor(getContext(), R.color.primary_secondary));
        options.setToolbarTitle("Crop Image");

        Intent intent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(512, 512)
                .withOptions(options)
                .getIntent(getContext());

        cropActivityLauncher.launch(intent);
    }

    private void uploadImage() {
        if (uriContent != null) {  // Ensure that uriContent is not null before uploading
            final StorageReference fileRef = profilePictureRef
                    .child(Continuity.currentOnlineUser.getusername() + ".jpg");

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

                        ref.child(Continuity.currentOnlineUser.getusername()).updateChildren(userData)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(getContext(), "Profile Updated", Toast.LENGTH_SHORT).show();
                                            Continuity.currentOnlineUser.setimageUrl(myUri);
                                        } else {
                                            Toast.makeText(getContext(), "Error occurred, try again", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    } else {
                        Toast.makeText(getContext(), "Error uploading image", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            Toast.makeText(getContext(), "Image is not selected", Toast.LENGTH_SHORT).show();
        }
    }


}