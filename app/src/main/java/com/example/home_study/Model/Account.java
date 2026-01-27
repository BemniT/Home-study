package com.example.home_study.Model;
public class Account
{
    private String userId ,username, name, password, profileImage, role, gender, email, phone
            ,grade, section, studentId;

    private int age;
    private boolean isActive;

    public Account()
    {
    }

    public Account(String userId, String username, String profileImage, String password,
                   String role, boolean isActive, String name, String gender,
                   String email,String phone, String grade, String section, int age, String studentId) {
        this.userId = userId;
        this.studentId = studentId;
        this.username = username;
        this.profileImage = profileImage;
        this.password = password;
        this.role = role;
        this.gender = gender;
        this.isActive = isActive;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.grade = grade;
        this.section = section;
        this.age = age;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }
}
