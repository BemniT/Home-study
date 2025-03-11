package com.example.home_study.Model;

import java.util.List;

public class Exam {
    private String bookTitle, bookGrade;
    private int bookImage;

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

    public Exam(String bookTitle, String bookGrade, int bookImage) {
        this.bookTitle = bookTitle;
        this.bookGrade = bookGrade;
        this.bookImage = bookImage;
    }
}
