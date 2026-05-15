package com.example.database_project;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RoutineItemResponse {
    public boolean success;
    public String message;
    public List<RoutineItemData> data;

    public static class RoutineItemData {

        @SerializedName("id")
        public String id;

        @SerializedName("title")
        public String title;

//        @SerializedName("item_name")
//        public String itemName;

        @SerializedName("list_order")
        public int listOrder;

        @SerializedName("start_time")
        public String startTime;

        @SerializedName("routine_id")
        public String routineId;

        public String getDisplayName() {
//            if (itemName != null && !itemName.isEmpty()) {
//                return itemName;
//            }

            if (title != null && !title.isEmpty()) {
                return title;
            }

            return "";
        }
    }
}