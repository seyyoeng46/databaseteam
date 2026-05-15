package com.example.database_project;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DiaryListResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    public List<DiaryData> data;

    public static class DiaryData {
        @SerializedName("id")
        public String id;

        @SerializedName("content")
        public String content;

        @SerializedName("target_date")
        public String targetDate;
    }
}