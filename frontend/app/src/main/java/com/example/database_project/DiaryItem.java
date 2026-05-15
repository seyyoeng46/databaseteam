package com.example.database_project;

import java.util.ArrayList;

public class DiaryItem {
    private String id;
    private String date;          // yyyy.MM.dd
    private String title;
    private String content;
    private ArrayList<String> routineTags;
    private ArrayList<String> listTags;

    public DiaryItem(String id, String date, String title, String content,
                     ArrayList<String> routineTags, ArrayList<String> listTags) {
        this.id          = id;
        this.date        = date;
        this.title       = title;
        this.content     = content;
        this.routineTags = routineTags != null ? routineTags : new ArrayList<>();
        this.listTags    = listTags    != null ? listTags    : new ArrayList<>();
    }

    public String getId()      { return id; }
    public String getDate()    { return date; }
    public String getTitle()   { return title; }
    public String getContent() { return content; }

    public ArrayList<String> getRoutineTags() { return routineTags; }
    public ArrayList<String> getListTags()    { return listTags; }

    /** 필터링용: 루틴 + 리스트 합친 전체 태그 */
    public ArrayList<String> getTags() {
        ArrayList<String> all = new ArrayList<>(routineTags);
        all.addAll(listTags);
        return all;
    }

    public void setId(String id)          { this.id = id; }
    public void setDate(String date)      { this.date = date; }
    public void setTitle(String title)    { this.title = title; }
    public void setContent(String content){ this.content = content; }
    public void setRoutineTags(ArrayList<String> t) { this.routineTags = t; }
    public void setListTags(ArrayList<String> t)    { this.listTags = t; }
}
