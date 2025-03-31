package com.example.home_study.Model;
public class Account
{
    private String name, password, username, imageUrl, address, email;

    public Account()
    {
    }

    public Account(String name, String password, String username, String imageUrl, String address, String email) {
        this.name = name;
        this.password = password;
        this.username = username;
        this.imageUrl = imageUrl;
        this.address = address;
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getusername() {
        return username;
    }

    public void setusername(String username) {
        this.username = username;
    }

    public String getimageUrl() {
        return imageUrl;
    }

    public void setimageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
