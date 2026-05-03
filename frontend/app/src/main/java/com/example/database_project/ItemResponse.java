package com.example.database_project;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ItemResponse {

    @SerializedName("success")
    public boolean success;

    @SerializedName("message")
    public String message;

    @SerializedName("data")
    public List<ItemData> data;

    public static class ItemData {
        @SerializedName("id")
        public String id;

        @SerializedName("title")
        public String title;
    }
}