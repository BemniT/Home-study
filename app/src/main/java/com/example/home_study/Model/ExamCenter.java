package com.example.home_study.Model;

import java.util.List;

public class ExamCenter {



    private String question, correctAnswer, explanation,id;
    private int points;
    private List<String> options;

    public ExamCenter() {
    }

    public ExamCenter(String question, String correctAnswer, String explanation, List<String> options) {
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.options = options;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}
