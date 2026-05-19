package com.example.database_project;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AiRoutineResponse {
    @SerializedName("success") public boolean success;
    @SerializedName("message") public String  message;
    @SerializedName("data")    public PreviewData data;

    public static class PreviewData {
        @SerializedName("routine_title") public String         routineTitle;
        @SerializedName("items")         public List<ItemData> items;
    }

    public static class ItemData {
        /** AI가 반환하는 "HH:MM 행동설명" 형식 문자열 */
        @SerializedName("content") public String content;
    }
}
