package com.example.home_study;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import com.example.home_study.Model.Account;
import com.example.home_study.Prevalent.Continuity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import io.paperdb.Paper;

public class LoginActivity extends AppCompatActivity {


    private Button loginButton;
    private EditText loginUserName, login_Password;
    private ProgressDialog loadingBar;
    private CheckBox rememberMe;
    private TextView AdminLink,notAdminLink, log_signUp;
    private boolean isFormatting;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginUserName = (EditText) findViewById(R.id.login_phonenumber);
        login_Password = (EditText) findViewById(R.id.login_password);
        loginButton = (Button) findViewById(R.id.login_btn);

        loadingBar = new ProgressDialog(this);


//        rememberMe = (CheckBox) findViewById(R.id.rememberMe_check);

        loginButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                String password = login_Password.getText().toString();
                String username = loginUserName.getText().toString();

                if (TextUtils.isEmpty(password))
                {
                    Toast.makeText(LoginActivity.this, "Password Missing!", Toast.LENGTH_SHORT).show();
                }
                else if (TextUtils.isEmpty(username))
                {
                    Toast.makeText(LoginActivity.this, "Phone Number Missing!", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    loadingBar.setTitle("Logining");
                    loadingBar.setMessage("Logining in");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    Autenticate(username, password);

                }
            }
        });


    }

    private boolean verifyPassword(String inputPassword, String storedPassword)
    {
        return inputPassword.equals(storedPassword);
    }
    //WHEN THE PERSON LOGIN
    private void Autenticate(final String username, String password)
    {
//        if (rememberMe.isChecked())
//        {
        Paper.init(this);
        Paper.book().write(Continuity.userNamekey, username);
        Paper.book().write(Continuity.userPassword, password);
//        }

        final DatabaseReference Rootref;
        Rootref = FirebaseDatabase.getInstance().getReference().child("Users");

        Rootref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                if (snapshot.child(username).exists())
                {
                    Account userData = snapshot.child(username).getValue(Account.class);

                    if (userData.getusername().equals(username))
                    {
                        if (userData.getPassword().equals(password))
                        {
                            Toast.makeText(LoginActivity.this, "Logged in Successfully..", Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            Continuity.currentOnlineUser = userData;
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                        else
                        {
                            Toast.makeText(LoginActivity.this, "Wrong Password!", Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    }
                }
                else
                {
                    Toast.makeText(LoginActivity.this, "You are not Registered!", Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }


            }
            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {

            }
        });

    }
}