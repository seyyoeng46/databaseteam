package com.example.database_project;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TodoResponse {

    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    public List<TodoData> data;

    public static class TodoData {
        @SerializedName("id")
        public String id;

        @SerializedName("title")
        public String title;

        @SerializedName("content")
        public String content;

        @SerializedName("target_date")
        public String targetDate;

        @SerializedName("is_completed")
        public boolean isCompleted;
    }
}