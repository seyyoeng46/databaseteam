package com.example.database_project;

import com.google.gson.annotations.SerializedName;

public class BasicResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("message")
    public String message;

    @SerializedName("routine_id")
    public String routineId;

    @SerializedName("id")
    public String todoId;
}