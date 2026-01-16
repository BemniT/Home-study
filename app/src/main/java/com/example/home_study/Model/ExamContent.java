package com.example.home_study.Model;

public class ExamContent {
    private String chapterId;
    private String title;
    private boolean hasExam;

    // exam meta
    private boolean examPublished = false;
    private int durationMinutes = 0;
    private int totalQuestions = 0;
    private int passScore = 0;

    // user result
    private boolean userTaken = false;
    private int userScore = 0;
    private int userTotal = 0;
    private boolean userPassed = false;
    private long takenAt = 0L;

    private String chapterImage;

    public ExamContent() {}

    public ExamContent(String chapterId, String title, boolean hasExam) {
        this.chapterId = chapterId;
        this.title = title;
        this.hasExam = hasExam;
    }

    // getters / setters


    public String getChapterImage() {
        return chapterImage;
    }

    public void setChapterImage(String chapterImage) {
        this.chapterImage = chapterImage;
    }

    public String getChapterId() { return chapterId; }
    public String getTitle() { return title; }
    public boolean hasExam() { return hasExam; }

    public boolean isHasExam() {
        return hasExam;
    }

    public boolean isExamPublished() {
        return examPublished;
    }

    public boolean isUserTaken() {
        return userTaken;
    }

    public int getDurationMinutes() { return durationMinutes; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getPassScore() { return passScore; }

    public int getUserScore() { return userScore; }
    public int getUserTotal() { return userTotal; }
    public boolean isUserPassed() { return userPassed; }
    public long getTakenAt() { return takenAt; }

    public void setHasExam(boolean hasExam) { this.hasExam = hasExam; }
    public void setExamPublished(boolean examPublished) { this.examPublished = examPublished; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public void setPassScore(int passScore) { this.passScore = passScore; }
    public void setUserTaken(boolean userTaken) { this.userTaken = userTaken; }
    public void setUserScore(int userScore) { this.userScore = userScore; }
    public void setUserTotal(int userTotal) { this.userTotal = userTotal; }
    public void setUserPassed(boolean userPassed) { this.userPassed = userPassed; }
    public void setTakenAt(long takenAt) { this.takenAt = takenAt; }

    // convenience
}