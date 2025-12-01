package com.example.home_study;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import com.example.home_study.Model.Account;
import com.example.home_study.Prevalent.Continuity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import io.paperdb.Paper;

public class LoginActivity extends AppCompatActivity {


    private EditText editTxtUserName, editTxtPassword;
    private Button loginButton;
    private ProgressDialog loadingBar;
    private CheckBox rememberMe;
    private TextView AdminLink,notAdminLink, log_signUp;
    private boolean isFormatting;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        editTxtUserName = (EditText) findViewById(R.id.login_phonenumber);
        editTxtPassword = (EditText) findViewById(R.id.login_password);
        loginButton = (Button) findViewById(R.id.login_btn);

        loadingBar = new ProgressDialog(this);


//        rememberMe = (CheckBox) findViewById(R.id.rememberMe_check);

        loginButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                String studentPassword = editTxtPassword.getText().toString();
                String studentUsername = editTxtUserName.getText().toString();

                if (TextUtils.isEmpty(studentPassword))
                {
                    Toast.makeText(LoginActivity.this, "Insert Password please", Toast.LENGTH_SHORT).show();
                }
                else if (TextUtils.isEmpty(studentUsername))
                {
                    Toast.makeText(LoginActivity.this, "Insert Username please", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    loadingBar.setTitle("Logining");
                    loadingBar.setMessage("Logining in");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    Autenticate(studentUsername, studentPassword);

                }
            }
        });


    }

    private boolean verifyPassword(String inputPassword, String storedPassword)
    {
        return inputPassword.equals(storedPassword);
    }
    //WHEN THE PERSON LOGIN
    private void Autenticate(final String studentUsername, String studentPassword) {
//        if (rememberMe.isChecked())
//        {
//        Paper.init(this);
//        Paper.book().write(Continuity.userNamekey, username);
//        Paper.book().write(Continuity.userPassword, password);
//        }

        final DatabaseReference rootRef;
        rootRef = FirebaseDatabase.getInstance().getReference().child("Users");

        rootRef.orderByChild("username")
                .equalTo(studentUsername)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if(!snapshot.exists()){
                            loadingBar.dismiss();
                            Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        DataSnapshot userSnap = snapshot.getChildren().iterator().next();

                        Account account = userSnap.getValue(Account.class);

                        if(account == null){
                            loadingBar.dismiss();
                            Toast.makeText(LoginActivity.this, "Login Error!!!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (!verifyPassword(studentPassword, account.getPassword())){
                            loadingBar.dismiss();
                            Toast.makeText(LoginActivity.this, "Wrong Password", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        loadingBar.dismiss();
                        Continuity.userId= account.getUserId();
                        Continuity.currentOnlineUser = account;

                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        loadingBar.dismiss();
                        Toast.makeText(LoginActivity.this, "Database Error", Toast.LENGTH_SHORT).show();

                    }
                });

    }
}