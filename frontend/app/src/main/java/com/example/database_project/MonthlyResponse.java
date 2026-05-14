package com.example.database_project;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MonthlyResponse {

    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    public List<MonthlyData> data;

    public static class MonthlyData {
        @SerializedName("date_day")
        public String day;

        @SerializedName("completed")
        public String completed;
    }
}