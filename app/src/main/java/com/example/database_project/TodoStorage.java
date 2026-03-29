package com.example.database_project;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TodoStorage {

    private static final String PREF_NAME = "todo_pref";
    private static final String KEY_TODO_DATE = "todo_date";
    private static final String KEY_TODO_LIST = "todo_list";
    private static final String KEY_TODO_HISTORY = "todo_history";

    public static void resetIfDateChanged(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        String savedDate = pref.getString(KEY_TODO_DATE, "");
        String today = getTodayKey();

        if (!today.equals(savedDate)) {
            // 날짜가 바뀌면 오늘 리스트를 history 기준으로 새로 불러옴
            ArrayList<TodoItem> todayList = getTodosByDate(context, today);

            pref.edit()
                    .putString(KEY_TODO_DATE, today)
                    .putString(KEY_TODO_LIST, new Gson().toJson(todayList))
                    .apply();
        }
    }

    public static ArrayList<TodoItem> getTodayTodos(Context context) {
        resetIfDateChanged(context);

        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = pref.getString(KEY_TODO_LIST, "[]");

        Type type = new TypeToken<ArrayList<TodoItem>>() {}.getType();
        ArrayList<TodoItem> list = new Gson().fromJson(json, type);

        return list != null ? list : new ArrayList<>();
    }

    public static void saveTodayTodos(Context context, ArrayList<TodoItem> list) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String today = getTodayKey();
        String json = new Gson().toJson(list);

        pref.edit()
                .putString(KEY_TODO_DATE, today)
                .putString(KEY_TODO_LIST, json)
                .apply();

        saveTodosByDate(context, today, list);
    }

    public static void saveTodosByDate(Context context, String dateKey, ArrayList<TodoItem> list) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        Map<String, ArrayList<TodoItem>> history = getAllTodoHistory(context);
        history.put(dateKey, list);

        String json = new Gson().toJson(history);
        pref.edit().putString(KEY_TODO_HISTORY, json).apply();
    }

    public static ArrayList<TodoItem> getTodosByDate(Context context, String dateKey) {
        Map<String, ArrayList<TodoItem>> history = getAllTodoHistory(context);
        ArrayList<TodoItem> list = history.get(dateKey);
        return list != null ? list : new ArrayList<>();
    }

    public static Map<String, ArrayList<TodoItem>> getAllTodoHistory(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = pref.getString(KEY_TODO_HISTORY, "{}");

        Type type = new TypeToken<HashMap<String, ArrayList<TodoItem>>>() {}.getType();
        Map<String, ArrayList<TodoItem>> history = new Gson().fromJson(json, type);

        return history != null ? history : new HashMap<>();
    }

    public static String getTodayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
    }

    public static String getTodayString() {
        return new SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA).format(new Date());
    }
}