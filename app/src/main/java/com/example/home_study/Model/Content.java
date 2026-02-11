package com.example.home_study.Model;

public class Content {

    private String contentName;
    private String contentSubject;
    private String pdfUrl;
    private int contentImage;

    // New metadata fields used by ContentActivity
    private String metaId;     // database key like "chapter_01"
    private int orderIndex;    // numeric order (1-based or 0-based as used)
    private boolean hasExam;   // whether chapter has an exam

    public Content() { }

    public Content(String contentName, String contentSubject, int contentImage, String pdfUrl) {
        this.contentName = contentName;
        this.contentSubject = contentSubject;
        this.contentImage = contentImage;
        this.pdfUrl = pdfUrl;
        this.metaId = "";
        this.orderIndex = 0;
        this.hasExam = false;
    }

    // Getters / setters

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

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public int getContentImage() {
        return contentImage;
    }

    public void setContentImage(int contentImage) {
        this.contentImage = contentImage;
    }

    // New metadata accessors

    public String getMetaId() {
        return metaId;
    }

    public void setMetaId(String metaId) {
        this.metaId = metaId;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public boolean isHasExam() {
        return hasExam;
    }

    public void setHasExam(boolean hasExam) {
        this.hasExam = hasExam;
    }
}