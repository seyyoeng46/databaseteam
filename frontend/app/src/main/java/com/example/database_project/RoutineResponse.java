package com.example.database_project;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RoutineResponse {

    @SerializedName("success")
    public boolean success;

    @SerializedName("message")
    public String message;

    @SerializedName("data")
    public List<RoutineData> data;

    public static class RoutineData {
        @SerializedName("id")
        public String id;

        @SerializedName("routine_name")
        public String routineName;

        @SerializedName("description")
        public String description;

        @SerializedName("schedules")
        public List<Integer> schedules;

        @SerializedName("created_at")
        public String createdAt;

        @SerializedName("is_completed")
        public boolean isCompleted;
    }
}