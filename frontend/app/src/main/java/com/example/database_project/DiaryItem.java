package com.example.database_project;

import java.util.ArrayList;

public class DiaryItem {
    private String id;
    private String date;       // yyyy.MM.dd
    private String title;
    private String content;
    private ArrayList<String> tags;

    public DiaryItem(String id, String date, String title, String content, ArrayList<String> tags) {
        this.id = id;
        this.date = date;
        this.title = title;
        this.content = content;
        this.tags = tags;
    }

    public String getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }
}