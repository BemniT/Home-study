package com.example.home_study.Model;

import android.app.Application;

import com.google.firebase.FirebaseApp;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
