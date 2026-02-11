package com.example.home_study.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "subjects")
public class SubjectEntity {
    @PrimaryKey
    @NonNull
    public String key = ""; // normalized key, e.g., "mathematics"

    public String title; // display title
    public String grade; // e.g., "grade_7"
    public String imageUrl; // optional remote url
    public long updatedAt; // optional timestamp for sync

    public SubjectEntity() { }

    // optional convenience constructor
    public SubjectEntity(@NonNull String key, String title, String grade, String imageUrl, long updatedAt) {
        this.key = key;
        this.title = title;
        this.grade = grade;
        this.imageUrl = imageUrl;
        this.updatedAt = updatedAt;
    }
}