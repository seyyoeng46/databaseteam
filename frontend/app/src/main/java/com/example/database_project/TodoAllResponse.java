package com.example.database_project;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TodoAllResponse {

    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    public List<TodoAllData> data;

    public static class TodoAllData {
        @SerializedName("target_date")
        public String targetDate;

        @SerializedName("titles")
        public List<String> titles;

        @SerializedName("ids")
        public List<String> ids;
    }
}