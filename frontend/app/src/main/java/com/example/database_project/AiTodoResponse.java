package com.example.database_project;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AiTodoResponse {
    @SerializedName("success") public boolean success;
    @SerializedName("message") public String  message;
    @SerializedName("data")    public AiTodoData data;

    public static class AiTodoData {
        @SerializedName("items") public List<ItemData> items;
    }

    public static class ItemData {
        @SerializedName("title") public String title;
    }
}
