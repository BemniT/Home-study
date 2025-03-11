package com.example.home_study.Model;

public class ExamContent {
    private String contentName, contentSubject;
    private int  contentImage;

    public ExamContent() {
    }

    public ExamContent(String contentName, String contentSubject, int contentImage) {
        this.contentName = contentName;
        this.contentSubject = contentSubject;

        this.contentImage = contentImage;
    }

    public String getContentName() {
        return contentName;
    }

    public void setContentName(String contentName) {
        this.contentName = contentName;
    }

    public String getContentSubject() {
        return contentSubject;
    }

    public void setContentSubject(String contentSubject) {
        this.contentSubject = contentSubject;
    }


    public int getContentImage() {
        return contentImage;
    }

    public void setContentImage(int contentImage) {
        this.contentImage = contentImage;
    }
}
