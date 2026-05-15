package com.example.database_project;

import com.google.gson.annotations.SerializedName;

public class DiaryRequest {

    @SerializedName("title")
    public String title;

    @SerializedName("content")
    public String content;

    @SerializedName("mood")
    public String mood;

    @SerializedName("target_date")
    public String targetDate;

    public DiaryRequest(String title, String content, String mood, String targetDate) {
        this.title = title;
        this.content = content;
        this.mood = mood;
        this.targetDate = targetDate;
    }
}