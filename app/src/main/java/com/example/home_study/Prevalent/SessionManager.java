package com.example.home_study.Prevalent;


import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "home_study_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EXPIRES_AT = "expires_at";

    private final SharedPreferences prefs;

    public SessionManager(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void createSession(String userId, long durationMillis) {
        long expiresAt = System.currentTimeMillis() + durationMillis;
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putLong(KEY_EXPIRES_AT, expiresAt)
                .apply();
    }

    public boolean hasValidSession() {
        String uid = prefs.getString(KEY_USER_ID, null);
        if (uid == null) return false;
        long expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L);
        return System.currentTimeMillis() <= expiresAt;
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }
}