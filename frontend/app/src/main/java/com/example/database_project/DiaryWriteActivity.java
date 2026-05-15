package com.example.database_project;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiaryWriteActivity extends AppCompatActivity {

    private ImageView    btnBack;
    private TextView     tvDateTitle;
    private EditText     etDiaryTitle;
    private EditText     etDiaryContent;
    private LinearLayout layoutTodoTags;
    private Button       btnSaveDiary;

    private String mode         = "create";
    private String selectedDate = "";
    private String diaryId      = "";

    // 저장용
    private ArrayList<String> currentRoutineTags = new ArrayList<>();
    private ArrayList<String> currentListTags    = new ArrayList<>();

    private final Object tagLock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_write);

        btnBack        = findViewById(R.id.btn_back);
        tvDateTitle    = findViewById(R.id.tv_date_title);
        etDiaryTitle   = findViewById(R.id.et_diary_title);
        etDiaryContent = findViewById(R.id.et_diary_content);
        layoutTodoTags = findViewById(R.id.layout_todo_tags);
        btnSaveDiary   = findViewById(R.id.btn_save_diary);

        btnBack.setOnClickListener(v -> finish());

        mode = getIntent().getStringExtra("mode");

        if ("edit".equals(mode)) {
            diaryId      = getIntent().getStringExtra("diary_id");
            selectedDate = getIntent().getStringExtra("diary_date");
            tvDateTitle.setText(selectedDate + " 기록");
            etDiaryTitle.setText(getIntent().getStringExtra("diary_title"));
            etDiaryContent.setText(getIntent().getStringExtra("diary_content"));
        } else {
            selectedDate = getIntent().getStringExtra("selected_date");
            tvDateTitle.setText(selectedDate + " 기록");
        }

        // 해당 날짜의 완료된 루틴 + 리스트 투두 로드
        loadTodoTags(selectedDate);

        btnSaveDiary.setOnClickListener(v -> saveDiary());
    }

    // 루틴 완료 아이템과 리스트 완료 투두를 병렬로 불러와 각각 정렬 후 표시
    private void loadTodoTags(String date) {
        String apiDate   = date.replace(".", "-");
        int    dayOfWeek = getDayOfWeekFromDate(apiDate);

        ArrayList<String> routineTags = new ArrayList<>();
        ArrayList<String> listTags    = new ArrayList<>();
        int[]             pending     = {2}; // 투두 + 루틴 두 가지

        // ① 완료된 리스트 투두 (알파벳/가나다 오름차순)
        RetrofitClient.getRoutineApi(this)
                .getTodos(apiDate)
                .enqueue(new Callback<TodoResponse>() {
                    @Override
                    public void onResponse(Call<TodoResponse> call,
                                           Response<TodoResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success && response.body().data != null) {
                            for (TodoResponse.TodoData todo : response.body().data) {
                                if (todo.isCompleted && todo.title != null
                                        && !todo.title.trim().isEmpty()) {
                                    synchronized (tagLock) { listTags.add(todo.title.trim()); }
                                }
                            }
                        }
                        synchronized (tagLock) { Collections.sort(listTags); }
                        decrementAndApply(pending, routineTags, listTags);
                    }

                    @Override
                    public void onFailure(Call<TodoResponse> call, Throwable t) {
                        decrementAndApply(pending, routineTags, listTags);
                    }
                });

        // ② 완료된 루틴 아이템 (알파벳/가나다 오름차순)
        RetrofitClient.getRoutineApi(this)
                .getRoutines()
                .enqueue(new Callback<RoutineResponse>() {
                    @Override
                    public void onResponse(Call<RoutineResponse> call,
                                           Response<RoutineResponse> response) {
                        if (!response.isSuccessful() || response.body() == null
                                || !response.body().success || response.body().data == null) {
                            decrementAndApply(pending, routineTags, listTags);
                            return;
                        }

                        // 오늘 요일 루틴만 필터
                        List<RoutineResponse.RoutineData> todayRoutines = new ArrayList<>();
                        for (RoutineResponse.RoutineData r : response.body().data) {
                            if (r.schedules != null && r.schedules.contains(dayOfWeek))
                                todayRoutines.add(r);
                        }

                        if (todayRoutines.isEmpty()) {
                            decrementAndApply(pending, routineTags, listTags);
                            return;
                        }

                        int[] innerPending = {todayRoutines.size()};

                        for (RoutineResponse.RoutineData routine : todayRoutines) {
                            RetrofitClient.getRoutineApi(DiaryWriteActivity.this)
                                    .getRoutineCompletions(routine.id, apiDate)
                                    .enqueue(new Callback<RoutineCompletionResponse>() {
                                        @Override
                                        public void onResponse(
                                                Call<RoutineCompletionResponse> call,
                                                Response<RoutineCompletionResponse> response) {
                                            if (response.isSuccessful()
                                                    && response.body() != null
                                                    && response.body().data != null) {
                                                for (RoutineCompletionResponse.CompletionData item
                                                        : response.body().data) {
                                                    if (item.isCompleted && item.title != null
                                                            && !item.title.trim().isEmpty()) {
                                                        // 시간 prefix 제거: "07:00 스트레칭" → "스트레칭"
                                                        String t    = item.title.trim();
                                                        String desc = t.contains(" ")
                                                                ? t.substring(t.indexOf(" ") + 1).trim()
                                                                : t;
                                                        synchronized (tagLock) { routineTags.add(desc); }
                                                    }
                                                }
                                            }
                                            innerDecrement(innerPending, pending,
                                                    routineTags, listTags);
                                        }

                                        @Override
                                        public void onFailure(
                                                Call<RoutineCompletionResponse> call,
                                                Throwable t) {
                                            innerDecrement(innerPending, pending,
                                                    routineTags, listTags);
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onFailure(Call<RoutineResponse> call, Throwable t) {
                        decrementAndApply(pending, routineTags, listTags);
                    }
                });
    }

    private void innerDecrement(int[] innerPending, int[] outerPending,
                                ArrayList<String> routineTags, ArrayList<String> listTags) {
        boolean outerDone = false;
        synchronized (tagLock) {
            innerPending[0]--;
            if (innerPending[0] == 0) {
                Collections.sort(routineTags);
                outerPending[0]--;
                if (outerPending[0] == 0) outerDone = true;
            }
        }
        if (outerDone) applyTags(routineTags, listTags);
    }

    private void decrementAndApply(int[] pending,
                                   ArrayList<String> routineTags, ArrayList<String> listTags) {
        boolean done = false;
        synchronized (tagLock) {
            pending[0]--;
            if (pending[0] == 0) done = true;
        }
        if (done) applyTags(routineTags, listTags);
    }

    private void applyTags(ArrayList<String> routineTags, ArrayList<String> listTags) {
        runOnUiThread(() -> {
            currentRoutineTags = new ArrayList<>(routineTags);
            currentListTags    = new ArrayList<>(listTags);
            renderTodoTags(routineTags, listTags);
        });
    }

    // 루틴 태그 → 리스트 태그 순으로 2줄 표시
    private void renderTodoTags(ArrayList<String> routineTags, ArrayList<String> listTags) {
        layoutTodoTags.removeAllViews();

        boolean hasRoutine = routineTags != null && !routineTags.isEmpty();
        boolean hasList    = listTags    != null && !listTags.isEmpty();

        if (!hasRoutine && !hasList) {
            TextView empty = new TextView(this);
            empty.setText("완료한 투두가 없어요");
            empty.setTextSize(13);
            empty.setTextColor(0xFF8A8A8A);
            layoutTodoTags.addView(empty);
            return;
        }

        if (hasRoutine) addTagSection("루틴", routineTags, false);
        if (hasList)    addTagSection("리스트", listTags, hasRoutine);
    }

    private void addTagSection(String label, ArrayList<String> tags, boolean addTopMargin) {
        // 초록색 태그 텍스트 (라벨 없음)
        TextView tvTags = new TextView(this);
        tvTags.setText(android.text.TextUtils.join("  ", tags));
        tvTags.setTextSize(13);
        tvTags.setTextColor(0xFF2ECC71);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        if (addTopMargin) lp.topMargin = dpToPx(6);
        tvTags.setLayoutParams(lp);
        layoutTodoTags.addView(tvTags);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private int getDayOfWeekFromDate(String apiDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = sdf.parse(apiDate);
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            return cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=일 ~ 6=토
        } catch (Exception e) {
            return -1;
        }
    }

    private void saveDiary() {
        String title   = etDiaryTitle.getText().toString().trim();
        String content = etDiaryContent.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "일기 제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "일기 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("title",       title);
        body.put("content",     content);
        body.put("target_date", selectedDate.replace(".", "-"));
        body.put("routineTags", currentRoutineTags);
        body.put("listTags",    currentListTags);

        if ("edit".equals(mode)) {
            RetrofitClient.getRoutineApi(this)
                    .updateDiary(diaryId, body)
                    .enqueue(new Callback<BasicResponse>() {
                        @Override
                        public void onResponse(Call<BasicResponse> call,
                                               Response<BasicResponse> response) {
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().success) {
                                Toast.makeText(DiaryWriteActivity.this,
                                        "일기가 수정되었습니다.", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(DiaryWriteActivity.this,
                                        "수정 실패", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<BasicResponse> call, Throwable t) {
                            Toast.makeText(DiaryWriteActivity.this,
                                    "수정 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            RetrofitClient.getRoutineApi(this)
                    .createDiary(body)
                    .enqueue(new Callback<BasicResponse>() {
                        @Override
                        public void onResponse(Call<BasicResponse> call,
                                               Response<BasicResponse> response) {
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().success) {
                                Toast.makeText(DiaryWriteActivity.this,
                                        "일기가 등록되었습니다.", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                String detail = "";
                                try {
                                    if (response.errorBody() != null)
                                        detail = " [" + response.code() + "] " + response.errorBody().string();
                                    else if (response.body() != null && response.body().message != null)
                                        detail = ": " + response.body().message;
                                } catch (IOException ignored) {}
                                Toast.makeText(DiaryWriteActivity.this,
                                        "등록 실패" + detail, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<BasicResponse> call, Throwable t) {
                            Toast.makeText(DiaryWriteActivity.this,
                                    "등록 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }
}
