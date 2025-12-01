package com.example.home_study.Model;

public class Post {

    private String postId;

    private String adminId;
    private String time;
    private String message;
    private String postImage;
    private int likes;
    private int comments;

    public Post() {
    }

    public Post(String postId, String adminId, String time, String postImage, String message, int likes, int comments) {
        this.postId = postId;
        this.adminId = adminId;
        this.time = time;
        this.postImage = postImage;
        this.message = message;
        this.likes = likes;
        this.comments = comments;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
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

    public String getPostImage() {
        return postImage;
    }

    public void setPostImage(String postImage) {
        this.postImage = postImage;
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
