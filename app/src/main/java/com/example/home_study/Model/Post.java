package com.example.home_study.Model;

public class Post {

    private String postId;
    private String authorProfile;
    private String author;
    private String time;
    private String message;
    private String imageUrl;
    private int likes;
    private int comments;

    public Post() {
    }



    public Post(String author, String time, String message, String imageUrl, int likes, int comments, String authorProfile, String postId) {
        this.author = author;
        this.time = time;
        this.message = message;
        this.imageUrl = imageUrl;
        this.likes = likes;
        this.comments = comments;
        this.authorProfile = authorProfile;
        this.postId = postId;
    }
    public String getAuthorProfile() {
        return authorProfile;
    }

    public void setAuthorProfile(String authorProfile) {
        this.authorProfile = authorProfile;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getComments() {
        return comments;
    }

    public void setComments(int comments) {
        this.comments = comments;
    }
}
