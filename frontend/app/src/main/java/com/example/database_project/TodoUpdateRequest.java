package com.example.database_project;

import com.google.gson.annotations.SerializedName;

public class TodoUpdateRequest {
    public boolean is_completed;

    public TodoUpdateRequest(boolean isCompleted) {
        this.is_completed = isCompleted;
    }
}