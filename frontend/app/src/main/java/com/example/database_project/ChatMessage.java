package com.example.database_project;

import android.net.Uri;
import java.util.List;

public class ChatMessage {

    public static final int TYPE_USER    = 0;
    public static final int TYPE_LOADING = 1;
    public static final int TYPE_RESULT  = 2;

    public final int type;

    // USER
    public String text;
    public Uri    imageUri;

    // RESULT
    public String        routineTitle;
    public List<String>  routineItems;
    public List<Integer> schedules;
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

    /** AI 미리보기 결과 메시지 */
    public ChatMessage(String routineTitle, List<String> routineItems, List<Integer> schedules) {
        this.type         = TYPE_RESULT;
        this.routineTitle = routineTitle;
        this.routineItems = routineItems;
        this.schedules    = schedules;
    }
}
