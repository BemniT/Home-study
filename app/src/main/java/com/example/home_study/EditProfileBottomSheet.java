package com.example.home_study;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.home_study.Prevalent.Continuity;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class EditProfileBottomSheet extends BottomSheetDialogFragment {

    private EditText etEmail, etUsername, etCurrentPass, etNewPass, etConfirmPass;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.bottomsheet_edit_profile, container, false);

        etEmail = v.findViewById(R.id.etEmail);
        etUsername = v.findViewById(R.id.etUsername);
        etCurrentPass = v.findViewById(R.id.etCurrentPassword);
        etNewPass = v.findViewById(R.id.etNewPassword);
        etConfirmPass = v.findViewById(R.id.etConfirmPassword);

        v.findViewById(R.id.btnSave).setOnClickListener(view -> saveChanges());

        preloadData();
        return v;
    }

    private void preloadData() {
        etEmail.setText(Continuity.currentOnlineUser.getEmail());
        etUsername.setText(Continuity.currentOnlineUser.getUsername());
    }

    private void saveChanges() {
        String email = etEmail.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String current = etCurrentPass.getText().toString();
        String newPass = etNewPass.getText().toString();
        String confirm = etConfirmPass.getText().toString();

        if (!current.isEmpty() && !current.equals(Continuity.currentOnlineUser.getPassword())){
            etCurrentPass.setError("Passwords do not match");
            return;
        }
        if (!newPass.isEmpty() && !newPass.equals(confirm)) {
            etConfirmPass.setError("Passwords do not match");
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(Continuity.currentOnlineUser.getUserId());

        Map<String, Object> update = new HashMap<>();
        update.put("email", email);
        update.put("username", username);

        if (!newPass.isEmpty()) {
            update.put("password", newPass); // replace with hashing later
        }

        userRef.updateChildren(update).addOnSuccessListener(unused -> {
            Continuity.currentOnlineUser.setEmail(email);
            Continuity.currentOnlineUser.setUsername(username);
            requireActivity().runOnUiThread(() -> {
                if (getActivity() instanceof ProfileActivity) {
                    ((ProfileActivity) getActivity()).populateUserData();
                }
            });
            dismiss();
        });
    }
}
