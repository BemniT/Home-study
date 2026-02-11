package com.example.home_study;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import android.util.Log;

import com.example.home_study.Model.Account;
import com.example.home_study.Prevalent.Continuity;
import com.example.home_study.Prevalent.SessionManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * LoginActivity with short-lived session (auto-login).
 */
public class LoginActivity extends AppCompatActivity {

    private EditText editTxtUserName, editTxtPassword;
    private Button loginButton;
    private AlertDialog loadingDialog;
    private TextView loadingMessageView;

    private SessionManager sessionManager;

    // session duration in milliseconds (2 days). Change to 1 day: 24L*60*60*1000
    private static final long SESSION_DURATION_MS = 2L * 24L * 60L * 60L * 1000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        FirebaseApp.initializeApp(this);

        sessionManager = new SessionManager(this);

        // If a valid session exists, restore user and skip login
        if (sessionManager.hasValidSession()) {
            String savedUid = sessionManager.getUserId();
            if (savedUid != null) {
                // fetch user from DB and proceed to home
                showLoading("Restoring session...");
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(savedUid);
                userRef.get().addOnSuccessListener(snapshot -> {
                    hideLoading();
                    if (snapshot.exists()) {
                        Account account = snapshot.getValue(Account.class);
                        if (account != null) {
                            Continuity.userId = account.getUserId();
                            Continuity.currentOnlineUser = account;
                            startHomeAndFinish();
                            return;
                        }
                    }
                    // session invalid (user removed) -> clear and continue to login screen
                    sessionManager.clearSession();
                }).addOnFailureListener(e -> {
                    hideLoading();
                    // any error -> clear session and continue to login screen
                    sessionManager.clearSession();
                });
            }
        }

        editTxtUserName = findViewById(R.id.login_phonenumber);
        editTxtPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_btn);

        prepareLoadingDialog();

        loginButton.setOnClickListener(v -> {
            String studentPassword = editTxtPassword.getText().toString().trim();
            String studentUsername = editTxtUserName.getText().toString().trim();

            if (TextUtils.isEmpty(studentPassword)) {
                Toast.makeText(LoginActivity.this, "Insert Password please", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(studentUsername)) {
                Toast.makeText(LoginActivity.this, "Insert Username please", Toast.LENGTH_SHORT).show();
                return;
            }

            showLoading("Signing in...");
            Autenticate(studentUsername, studentPassword);
        });
    }

    private void prepareLoadingDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog);
        LayoutInflater inflater = LayoutInflater.from(this);
        View content = inflater.inflate(R.layout.dialog_loading, null, false);
        loadingMessageView = content.findViewById(R.id.loadingMessage);
        b.setView(content);
        b.setCancelable(false);
        loadingDialog = b.create();
        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void showLoading(String message) {
        if (loadingDialog == null) prepareLoadingDialog();
        if (loadingMessageView != null) loadingMessageView.setText(message);
        if (!loadingDialog.isShowing()) loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }

    private boolean verifyPassword(String inputPassword, String storedPassword) {
        return inputPassword.equals(storedPassword);
    }

    private void Autenticate(final String studentUsername, String studentPassword) {

        final DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference().child("Users");

        rootRef.orderByChild("username")
                .equalTo(studentUsername)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            hideLoading();
                            Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        DataSnapshot userSnap = snapshot.getChildren().iterator().next();

                        String role = userSnap.child("role").getValue(String.class);
                        if (!"student".equals(role)) {
                            hideLoading();
                            Toast.makeText(LoginActivity.this, "You're not a student", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Account account = userSnap.getValue(Account.class);

                        if (account == null) {
                            hideLoading();
                            Toast.makeText(LoginActivity.this, "Login Error!!!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (!verifyPassword(studentPassword, account.getPassword())) {
                            hideLoading();
                            Toast.makeText(LoginActivity.this, "Wrong Password", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // success: create session and go to home
                        hideLoading();
                        Continuity.userId = account.getUserId();
                        Continuity.currentOnlineUser = account;

                        // create a short-lived session (2 days). Adjust SESSION_DURATION_MS as desired.
                        sessionManager.createSession(account.getUserId(), SESSION_DURATION_MS);

                        startHomeAndFinish();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        hideLoading();
                        Toast.makeText(LoginActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startHomeAndFinish() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideLoading();
    }
}