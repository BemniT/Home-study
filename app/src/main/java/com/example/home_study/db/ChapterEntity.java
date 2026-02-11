package com.example.home_study.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Index;

@Entity(tableName = "chapters", indices = {@Index("subjectKey")})
public class ChapterEntity {
    @PrimaryKey
    @NonNull
    public String id = ""; // e.g., "chapter_01" or DB key

    public String subjectKey; // foreign link to SubjectEntity.key
    public String title;
    public int orderIndex; // numeric order (1-based)
    public String pdfUrl; // remote URL
    public String localPath; // local file path if downloaded
    public boolean hasExam;
    public long updatedAt;

    public ChapterEntity() { }

    // optional convenience constructor
    public ChapterEntity(@NonNull String id, String subjectKey, String title, int orderIndex, String pdfUrl, String localPath, boolean hasExam, long updatedAt) {
        this.id = id;
        this.subjectKey = subjectKey;
        this.title = title;
        this.orderIndex = orderIndex;
        this.pdfUrl = pdfUrl;
        this.localPath = localPath;
        this.hasExam = hasExam;
        this.updatedAt = updatedAt;
    }
}