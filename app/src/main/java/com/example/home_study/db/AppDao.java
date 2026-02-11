package com.example.home_study.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSubjects(List<SubjectEntity> subs);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChapters(List<ChapterEntity> chapters);

    @Query("SELECT * FROM subjects WHERE grade = :grade ORDER BY title")
    LiveData<List<SubjectEntity>> getSubjectsForGrade(String grade);

    @Query("SELECT * FROM chapters WHERE subjectKey = :subjectKey ORDER BY orderIndex")
    LiveData<List<ChapterEntity>> getChaptersForSubject(String subjectKey);

    @Query("SELECT * FROM chapters WHERE id = :chapterId LIMIT 1")
    ChapterEntity getChapterOnce(String chapterId);

    @Update
    void updateChapter(ChapterEntity chapter);
}