package com.example.database_project;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class DiaryStorage {

    private static final String PREF_NAME = "diary_pref";
    private static final String KEY_DIARY_LIST = "diary_list";

    public static ArrayList<DiaryItem> getDiaryList(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = pref.getString(KEY_DIARY_LIST, "[]");

        Type type = new TypeToken<ArrayList<DiaryItem>>() {}.getType();
        ArrayList<DiaryItem> list = new Gson().fromJson(json, type);

        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public static void saveDiaryList(Context context, ArrayList<DiaryItem> list) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = new Gson().toJson(list);
        pref.edit().putString(KEY_DIARY_LIST, json).apply();
    }

    public static void addDiary(Context context, DiaryItem item) {
        ArrayList<DiaryItem> list = getDiaryList(context);
        list.add(0, item); // 최신순 위로
        saveDiaryList(context, list);
    }

    public static void updateDiary(Context context, DiaryItem newItem) {
        ArrayList<DiaryItem> list = getDiaryList(context);

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(newItem.getId())) {
                list.set(i, newItem);
                break;
            }
        }

        saveDiaryList(context, list);
    }

    public static void deleteDiary(Context context, String diaryId) {
        ArrayList<DiaryItem> list = getDiaryList(context);

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(diaryId)) {
                list.remove(i);
                break;
            }
        }

        saveDiaryList(context, list);
    }
}