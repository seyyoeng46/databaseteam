package com.example.database_project;

import com.google.gson.annotations.SerializedName;

public class DiaryResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    public DiaryData data;

    public static class DiaryData {
        // 서버 DB의 id가 숫자면 String으로 선언해도 GSON이 자동 변환해줍니다.
        @SerializedName("id")
        public String id;

        @SerializedName("user_id")
        public String userId;

        @SerializedName("content")
        public String content;

        @SerializedName("target_date")
        public String targetDate;
    }
}