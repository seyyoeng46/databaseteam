package com.example.database_project;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RoutineCompletionResponse {

    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    public List<CompletionData> data;

    public static class CompletionData {
        @SerializedName("id")
        public String id;

        @SerializedName("title")
        public String title;

        @SerializedName("is_completed")
        public boolean isCompleted;
    }
}