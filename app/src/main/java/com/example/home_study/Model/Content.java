package com.example.home_study.Model;

public class Content {

    private String contentName, contentSubject, pdfUrl, contentImage;
    public Content() {
    }

    public Content(String contentName, String contentSubject, String pdfUrl, String contentImage) {
        this.contentName = contentName;
        this.contentSubject = contentSubject;
        this.pdfUrl = pdfUrl;
        this.contentImage = contentImage;
    }


    public String getContentImage() {
        return contentImage;
    }

    public void setContentImage(String contentImage) {
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

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }
}
