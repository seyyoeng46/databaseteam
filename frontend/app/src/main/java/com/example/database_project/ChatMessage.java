package com.example.database_project;

import android.net.Uri;
import java.util.List;

public class ChatMessage {

    public static final int TYPE_USER        = 0;
    public static final int TYPE_LOADING     = 1;
    public static final int TYPE_RESULT      = 2;  // 루틴 결과
    public static final int TYPE_TODO_RESULT = 3;  // 리스트 결과

    public final int type;

    // USER
    public String text;
    public Uri    imageUri;

    // RESULT (루틴)
    public String        routineTitle;
    public List<String>  routineItems;
    public List<Integer> schedules;

    // TODO_RESULT (리스트)
    public String        todoDate;    // "YYYY-MM-DD"

    public boolean       registered = false; // 등록 완료 여부

    /** 사용자 메시지 */
    public ChatMessage(String text, Uri imageUri) {
        this.type     = TYPE_USER;
        this.text     = text;
        this.imageUri = imageUri;
    }

    /** 로딩 메시지 */
    public ChatMessage() {
        this.type = TYPE_LOADING;
    }

    /** AI 루틴 결과 메시지 */
    public ChatMessage(String routineTitle, List<String> routineItems, List<Integer> schedules) {
        this.type         = TYPE_RESULT;
        this.routineTitle = routineTitle;
        this.routineItems = routineItems;
        this.schedules    = schedules;
    }

    /** AI 리스트(투두) 결과 메시지 */
    public ChatMessage(List<String> todoItems, String todoDate) {
        this.type         = TYPE_TODO_RESULT;
        this.routineItems = todoItems; // 필드 재활용
        this.todoDate     = todoDate;
    }
}
