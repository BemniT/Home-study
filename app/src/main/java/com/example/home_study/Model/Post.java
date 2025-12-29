package com.example.home_study.Model;

public class Post {

    private String postId;

    private String adminId;
    private String time;
    private String message;
    private String postUrl;
    private int likes;
    private int likeCount;
//    private int comments;

    public Post() {
    }

    public Post(String postId, String adminId, String time, String postUrl, String message,int likeCount, int likes) {
        this.postId = postId;
        this.adminId = adminId;
        this.time = time;
        this.postUrl = postUrl;
        this.message = message;
        this.likeCount = likeCount;
//        this.comments = comments;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
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


    public String getPostUrl() {
        return postUrl;
    }

    public void setPostUrl(String postUrl) {
        this.postUrl = postUrl;
    }
}
