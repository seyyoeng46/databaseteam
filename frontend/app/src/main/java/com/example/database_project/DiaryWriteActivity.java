package com.example.database_project;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiaryWriteActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvDateTitle;
    private EditText etDiaryTitle;
    private EditText etDiaryContent;
    private LinearLayout layoutTodoTags;
    private Button btnSaveDiary;

    private String mode = "create";
    private String selectedDate = ""; // yyyy.MM.dd 형식
    private String diaryId = "";      // 수정을 위한 ID 저장용

    private ApiService apiService;
    private ArrayList<String> completedTodoTags = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_write);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        btnBack = findViewById(R.id.btn_back);
        tvDateTitle = findViewById(R.id.tv_date_title);
        etDiaryTitle = findViewById(R.id.et_diary_title);
        etDiaryContent = findViewById(R.id.et_diary_content);
        layoutTodoTags = findViewById(R.id.layout_todo_tags);
        btnSaveDiary = findViewById(R.id.btn_save_diary);

        btnBack.setOnClickListener(v -> finish());

        mode = getIntent().getStringExtra("mode");
        diaryId = getIntent().getStringExtra("diary_id");

        if ("edit".equals(mode)) {
            // 상세조회 API 대신 "날짜조회" API를 사용하기 위해 날짜를 가져옴
            String diaryDate = getIntent().getStringExtra("diary_date");
            if (diaryDate != null) {
                selectedDate = diaryDate;
                loadDiaryByDateFromServer(diaryDate);
            }
        } else {
            selectedDate = getIntent().getStringExtra("selected_date");
            tvDateTitle.setText(selectedDate + " 기록");
            fetchTodosAndRenderTags(selectedDate);
        }

        btnSaveDiary.setOnClickListener(v -> saveDiaryToServer());
    }

    // [핵심] 백엔드 수정 없이 날짜 조회를 통해 상세 데이터를 가져옴
    private void loadDiaryByDateFromServer(String dateStr) {
        String serverDate = convertDiaryDateToTodoDateKey(dateStr); // "yyyy-MM-dd"

        apiService.getDiaryByDate(serverDate).enqueue(new Callback<DiaryResponse>() {
            @Override
            public void onResponse(Call<DiaryResponse> call, Response<DiaryResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    DiaryResponse.DiaryData diary = response.body().data;

                    tvDateTitle.setText(dateStr + " 기록");

                    // 제목|내용 분리하여 입력창에 채우기
                    String raw = diary.content;
                    if (raw != null && raw.contains("|")) {
                        String[] parts = raw.split("\\|", 2);
                        etDiaryTitle.setText(parts[0]);
                        etDiaryContent.setText(parts[1]);
                    } else {
                        etDiaryTitle.setText("");
                        etDiaryContent.setText(raw != null ? raw : "");
                    }

                    // 해당 날짜의 완료된 투두 태그들도 함께 로드
                    fetchTodosAndRenderTags(dateStr);
                }
            }
            @Override
            public void onFailure(Call<DiaryResponse> call, Throwable t) {
                Log.e("DIARY_TEST", "로드 실패: " + t.getMessage());
            }
        });
    }

    private void fetchTodosAndRenderTags(String dateStr) {
        String serverDate = convertDiaryDateToTodoDateKey(dateStr);
        apiService.getTodosByDate(serverDate).enqueue(new Callback<TodoResponse>() {
            @Override
            public void onResponse(Call<TodoResponse> call, Response<TodoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    completedTodoTags.clear();
                    List<TodoResponse.TodoData> todos = response.body().data;
                    if (todos != null) {
                        for (TodoResponse.TodoData todo : todos) {
                            if (todo.isCompleted) completedTodoTags.add(todo.title);
                        }
                    }
                    renderTodoTags(completedTodoTags);
                }
            }
            @Override
            public void onFailure(Call<TodoResponse> call, Throwable t) {
                renderTodoTags(new ArrayList<>());
            }
        });
    }

    private void renderTodoTags(ArrayList<String> tags) {
        layoutTodoTags.removeAllViews();
        if (tags == null || tags.isEmpty()) {
            TextView emptyTag = new TextView(this);
            emptyTag.setText("완료한 투두가 없어요");
            emptyTag.setTextColor(0xFF8A8A8A);
            layoutTodoTags.addView(emptyTag);
            return;
        }
        for (String tag : tags) {
            TextView chip = new TextView(this);
            chip.setText("#" + tag);
            chip.setBackgroundResource(R.drawable.bg_tag_gray);
            chip.setPadding(35, 15, 35, 15);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
            params.rightMargin = 20;
            chip.setLayoutParams(params);
            layoutTodoTags.addView(chip);
        }
    }

    private void saveDiaryToServer() {
        String title = etDiaryTitle.getText().toString().trim();
        String content = etDiaryContent.getText().toString().trim();
        String serverDate = convertDiaryDateToTodoDateKey(selectedDate);

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            Toast.makeText(this, "제목과 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String combinedData = title + "|" + content;
        DiaryRequest request = new DiaryRequest("", combinedData, "happy", serverDate);

        if ("edit".equals(mode)) {
            // 수정 시에는 원래 받았던 ID를 사용합니다 (PATCH /api/diary/:id)
            apiService.updateDiary(diaryId, request).enqueue(new Callback<DiaryResponse>() {
                @Override
                public void onResponse(Call<DiaryResponse> call, Response<DiaryResponse> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(DiaryWriteActivity.this, "수정 완료", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                @Override
                public void onFailure(Call<DiaryResponse> call, Throwable t) {}
            });
        } else {
            apiService.createDiary(request).enqueue(new Callback<DiaryResponse>() {
                @Override
                public void onResponse(Call<DiaryResponse> call, Response<DiaryResponse> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(DiaryWriteActivity.this, "등록 완료", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                @Override
                public void onFailure(Call<DiaryResponse> call, Throwable t) {}
            });
        }
    }

    private String convertDiaryDateToTodoDateKey(String diaryDate) {
        try {
            SimpleDateFormat diaryFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);
            SimpleDateFormat todoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            Date date = diaryFormat.parse(diaryDate);
            return (date != null) ? todoFormat.format(date) : diaryDate;
        } catch (ParseException e) { return diaryDate; }
    }

    private String formatServerDate(String serverDate) {
        if (serverDate == null || serverDate.isEmpty()) return "";
        try {
            SimpleDateFormat inputFormat = serverDate.contains("T") ?
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA) :
                    new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            if (serverDate.contains("T")) inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(serverDate);
            return new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(date);
        } catch (Exception e) { return serverDate.split("T")[0].replace("-", "."); }
    }
}