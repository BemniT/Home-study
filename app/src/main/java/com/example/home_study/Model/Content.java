package com.example.home_study.Model;

public class Content {

    private String contentName, contentSubject, pdfUrl;
    private int  contentImage;
    public Content() {
    }

    public Content(String contentName, String contentSubject, int contentImage, String pdfUrl) {
        this.contentName = contentName;
        this.contentSubject = contentSubject;
        this.contentImage = contentImage;
        this.pdfUrl = pdfUrl;
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
}
