package com.example.home_study.Model;

public class Book {

    private String bookTitle;
    private String bookGrade;
    private int bookImage;

    // New: subject key (e.g., "mathematics") to pass to ContentActivity / repository
    private String subjectKey;

    public Book() {
    }

    // Existing constructor (kept)
    public Book(String bookTitle, String bookGrade, int bookImage) {
        this.bookTitle = bookTitle;
        this.bookGrade = bookGrade;
        this.bookImage = bookImage;
    }

    // New constructor that accepts subjectKey
    public Book(String bookTitle, String bookGrade, int bookImage, String subjectKey) {
        this.bookTitle = bookTitle;
        this.bookGrade = bookGrade;
        this.bookImage = bookImage;
        this.subjectKey = subjectKey;
    }

    // Getters / setters
    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBookGrade() {
        return bookGrade;
    }

    public void setBookGrade(String bookGrade) {
        this.bookGrade = bookGrade;
    }

    public int getBookImage() {
        return bookImage;
    }

    public void setBookImage(int bookImage) {
        this.bookImage = bookImage;
    }

    public String getSubjectKey() {
        return subjectKey;
    }

    public void setSubjectKey(String subjectKey) {
        this.subjectKey = subjectKey;
    }
}