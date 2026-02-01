package com.example.home_study;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.progressindicator.CircularProgressIndicator;

/**
 * Updated LoginActivity:
 * - Replaces deprecated ProgressDialog with a modern, styled loading dialog (center card + CircularProgressIndicator)
 * - Keeps original logic and method names (Autenticate) for minimal changes
 */
public class LoginActivity extends AppCompatActivity {

    private EditText editTxtUserName, editTxtPassword;
    private Button loginButton;
    // Replaces ProgressDialog
    private AlertDialog loadingDialog;
    private TextView loadingMessageView;

    private CheckBox rememberMe;
    private TextView AdminLink, notAdminLink, log_signUp;
    private boolean isFormatting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // initialize firebase (if needed)
        FirebaseApp.initializeApp(this);

        editTxtUserName = findViewById(R.id.login_phonenumber);
        editTxtPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_btn);

        prepareLoadingDialog(); // create the nicer loading UI

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String studentPassword = editTxtPassword.getText().toString().trim();
                String studentUsername = editTxtUserName.getText().toString().trim();

                if (TextUtils.isEmpty(studentPassword)) {
                    Toast.makeText(LoginActivity.this, "Insert Password please", Toast.LENGTH_SHORT).show();
                } else if (TextUtils.isEmpty(studentUsername)) {
                    Toast.makeText(LoginActivity.this, "Insert Username please", Toast.LENGTH_SHORT).show();
                } else {
                    showLoading("Signing in...");
                    Autenticate(studentUsername, studentPassword);
                }
            }
        });
    }

    private void prepareLoadingDialog() {
        // Inflate custom loading view
        AlertDialog.Builder b = new AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog);
        LayoutInflater inflater = LayoutInflater.from(this);
        View content = inflater.inflate(R.layout.dialog_loading, null, false);

        loadingMessageView = content.findViewById(R.id.loadingMessage);
        // Use builder to create dialog (non cancelable)
        b.setView(content);
        b.setCancelable(false);
        loadingDialog = b.create();
        if (loadingDialog.getWindow() != null) {
            // make the dialog's window background transparent to show rounded card properly
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

    // WHEN THE PERSON LOGIN
    private void Autenticate(final String studentUsername, String studentPassword) {

        final DatabaseReference rootRef;
        rootRef = FirebaseDatabase.getInstance().getReference().child("Users");

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

                        // checking the role of the user
                        String role = userSnap.child("role").getValue(String.class);
                        if (!"student".equals(role)) {
                            hideLoading();
                            Toast.makeText(LoginActivity.this, "You're not a student", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // mapping the users attributes to the account class
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

                        // success
                        hideLoading();
                        Continuity.userId = account.getUserId();
                        Continuity.currentOnlineUser = account;

                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        hideLoading();
                        Toast.makeText(LoginActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideLoading();
    }
}