package com.example.home_study.repo;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.home_study.db.AppDao;
import com.example.home_study.db.AppDatabase;
import com.example.home_study.db.ChapterEntity;
import com.example.home_study.db.SubjectEntity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ContentRepository {
    private final AppDao dao;
    private final Executor io = Executors.newSingleThreadExecutor();
    private final Context ctx;

    public ContentRepository(Context ctx) {
        AppDatabase db = AppDatabase.get(ctx);
        this.dao = db.dao();
        this.ctx = ctx.getApplicationContext();
    }

    public LiveData<List<SubjectEntity>> getSubjectsForGradeLive(String grade) {
        return dao.getSubjectsForGrade(grade);
    }

    public LiveData<List<ChapterEntity>> getChaptersForSubjectLive(String subjectKey) {
        return dao.getChaptersForSubject(subjectKey);
    }

    // Remote sync: read Curriculum/grade_x/<subject> and store Subjects + Chapters
    public void syncSubjectsForGrade(String gradeKey) {
        // gradeKey e.g., "grade_7"
        FirebaseDatabase.getInstance().getReference("Curriculum").child(gradeKey)
                .get().addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;
                    List<SubjectEntity> subs = new ArrayList<>();
                    List<ChapterEntity> chapters = new ArrayList<>();

                    for (DataSnapshot subjectSnap : snapshot.getChildren()) {
                        String subjectKey = subjectSnap.getKey();
                        String title = subjectSnap.child("subjectName").getValue(String.class);
                        SubjectEntity s = new SubjectEntity();
                        s.key = subjectKey;
                        s.title = title != null ? title : subjectKey;
                        s.grade = gradeKey;
                        s.updatedAt = System.currentTimeMillis();
                        subs.add(s);

                        DataSnapshot chaps = subjectSnap.child("chapters");
                        if (chaps != null && chaps.exists()) {
                            for (DataSnapshot ch : chaps.getChildren()) {
                                ChapterEntity ce = new ChapterEntity();
                                ce.id = ch.getKey();
                                ce.subjectKey = subjectKey;
                                ce.title = ch.child("title").getValue(String.class);
                                Object orderObj = ch.child("order").getValue();
                                ce.orderIndex = orderObj instanceof Long ? ((Long)orderObj).intValue() :
                                        (orderObj instanceof Integer ? (Integer)orderObj : 0);
                                ce.pdfUrl = ch.child("contentUrl").getValue(String.class);
                                ce.hasExam = ch.child("hasExam").getValue(Boolean.class) != null ? ch.child("hasExam").getValue(Boolean.class) : false;
                                ce.updatedAt = System.currentTimeMillis();
                                chapters.add(ce);
                            }
                        }
                    }

                    // store on IO thread
                    io.execute(() -> {
                        dao.insertSubjects(subs);
                        dao.insertChapters(chapters);
                    });
                }).addOnFailureListener(e -> Log.w("ContentRepo", "sync failed", e));
    }

    // sync a single subject's chapters (TextBooks fallback)
    public void syncTextBookChapters(String bookTitle, String subjectKey) {
        FirebaseDatabase.getInstance().getReference("TextBooks").child(bookTitle).child("Content")
                .get().addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;
                    List<ChapterEntity> chapters = new ArrayList<>();
                    for (DataSnapshot ch : snapshot.getChildren()) {
                        ChapterEntity ce = new ChapterEntity();
                        ce.id = ch.getKey();
                        ce.subjectKey = subjectKey;
                        ce.title = ch.child("title").getValue(String.class);
                        Object orderObj = ch.child("order").getValue();
                        ce.orderIndex = orderObj instanceof Long ? ((Long)orderObj).intValue() : 0;
                        ce.pdfUrl = ch.child("pdfUrl").getValue(String.class);
                        ce.updatedAt = System.currentTimeMillis();
                        chapters.add(ce);
                    }
                    io.execute(() -> dao.insertChapters(chapters));
                });
    }

    // in ContentRepository
    public void updateChapterLocalPathFromId(String chapterId, String localPath) {
        io.execute(() -> {
            ChapterEntity ce = dao.getChapterOnce(chapterId);
            if (ce != null) {
                ce.localPath = localPath;
                dao.updateChapter(ce);
            }
        });
    }
}