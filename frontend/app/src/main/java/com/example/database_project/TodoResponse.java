package com.example.database_project;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TodoResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("message")
    public String message;

    @SerializedName("data")
    public List<TodoData> data;

    public static class TodoData {
        @SerializedName("id")
        public int id;

        @SerializedName("user_id") // 명세서: user_id
        public String userId;

        @SerializedName("title") // 명세서: title
        public String title;

        @SerializedName("content") // 명세서: content
        public String content;

        @SerializedName("target_date")
        public String targetDate;

        @SerializedName("is_completed") // 명세서: is_completed
        public boolean isCompleted;

        @SerializedName("created_at")
        public String createdAt;
    }
}