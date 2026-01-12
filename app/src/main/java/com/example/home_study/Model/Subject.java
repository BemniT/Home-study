package com.example.home_study.Model;

public class Subject {

    private String courseId;
    private String name;
    private String grade;
    private String section;
    private int iconRes;

    public Subject() {}

    public Subject(String courseId, String name, String grade, String section, int iconRes) {
        this.courseId = courseId;
        this.name = name;
        this.grade = grade;
        this.section = section;
        this.iconRes = iconRes;
    }

    public int getIconRes() {
        return iconRes;
    }

    public String getCourseId() {
        return courseId;
    }

    public String getName() {
        return name;
    }

    public String getGrade() {
        return grade;
    }

    public String getSection() {
        return section;
    }
}
