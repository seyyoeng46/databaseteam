package com.example.database_project;

import com.google.gson.annotations.SerializedName;

public class DiaryDeleteResponse {
    @SerializedName("success")
    public boolean success;
    @SerializedName("message")
    public String message;
}