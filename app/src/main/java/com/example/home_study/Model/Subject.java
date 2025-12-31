package com.example.home_study.Model;

public class Subject {

    private String courseId;
    private String name;
    private String grade;
    private String section;

    public Subject() {}

    public Subject(String courseId, String name, String grade, String section) {
        this.courseId = courseId;
        this.name = name;
        this.grade = grade;
        this.section = section;
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
