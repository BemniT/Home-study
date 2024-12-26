package com.example.home_study.Model;

public class Book {

    private String bookTitle, bookGrade, bookImage;
    public Book() {
    }

    public Book(String bookTitle, String bookGrade, String bookImage) {
        this.bookTitle = bookTitle;
        this.bookGrade = bookGrade;
        this.bookImage = bookImage;
    }

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

    public String getBookImage() {
        return bookImage;
    }

    public void setBookImage(String bookImage) {
        this.bookImage = bookImage;
    }
}
