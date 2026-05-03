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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class DiaryWriteActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvDateTitle;
    private EditText etDiaryTitle;
    private EditText etDiaryContent;
    private LinearLayout layoutTodoTags;
    private Button btnSaveDiary;

    private String mode = "create";
    private String selectedDate = "";
    private String diaryId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_write);

        btnBack = findViewById(R.id.btn_back);
        tvDateTitle = findViewById(R.id.tv_date_title);
        etDiaryTitle = findViewById(R.id.et_diary_title);
        etDiaryContent = findViewById(R.id.et_diary_content);
        layoutTodoTags = findViewById(R.id.layout_todo_tags);
        btnSaveDiary = findViewById(R.id.btn_save_diary);

        btnBack.setOnClickListener(v -> finish());

        mode = getIntent().getStringExtra("mode");

        if ("edit".equals(mode)) {
            diaryId = getIntent().getStringExtra("diary_id");
            loadDiaryForEdit();
        } else {
            selectedDate = getIntent().getStringExtra("selected_date");
            tvDateTitle.setText(selectedDate + " 기록");
            renderTodoTags(getTodoTagsForDate(selectedDate));
        }

        btnSaveDiary.setOnClickListener(v -> saveDiary());
    }

    private void loadDiaryForEdit() {
        ArrayList<DiaryItem> list = DiaryStorage.getDiaryList(this);

        for (DiaryItem item : list) {
            if (item.getId().equals(diaryId)) {
                selectedDate = item.getDate();
                tvDateTitle.setText(selectedDate + " 기록");
                etDiaryTitle.setText(item.getTitle());
                etDiaryContent.setText(item.getContent());
                renderTodoTags(getTodoTagsForDate(selectedDate));
                break;
            }
        }
    }

    private void renderTodoTags(ArrayList<String> tags) {
        layoutTodoTags.removeAllViews();

        if (tags == null || tags.isEmpty()) {
            TextView emptyTag = new TextView(this);
            emptyTag.setText("완료한 투두가 없어요");
            emptyTag.setTextSize(13);
            emptyTag.setTextColor(0xFF8A8A8A);
            layoutTodoTags.addView(emptyTag);
            return;
        }

        for (String tag : tags) {
            TextView chip = new TextView(this);
            chip.setText(tag);
            chip.setTextSize(13);
            chip.setTextColor(0xFFFFFFFF);
            chip.setBackgroundResource(R.drawable.bg_tag_gray);
            chip.setPadding(28, 14, 28, 14);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.rightMargin = 14;
            params.bottomMargin = 14;
            chip.setLayoutParams(params);

            layoutTodoTags.addView(chip);
        }
    }

    private void saveDiary() {
        String title = etDiaryTitle.getText().toString().trim();
        String content = etDiaryContent.getText().toString().trim();
        ArrayList<String> tags = getTodoTagsForDate(selectedDate);

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "일기 제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "일기 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("edit".equals(mode)) {
            DiaryItem item = new DiaryItem(diaryId, selectedDate, title, content, tags);
            DiaryStorage.updateDiary(this, item);
            Toast.makeText(this, "일기가 수정되었습니다.", Toast.LENGTH_SHORT).show();
        } else {
            DiaryItem item = new DiaryItem(
                    UUID.randomUUID().toString(),
                    selectedDate,
                    title,
                    content,
                    tags
            );
            DiaryStorage.addDiary(this, item);
            Toast.makeText(this, "일기가 등록되었습니다.", Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    private ArrayList<String> getTodoTagsForDate(String diaryDate) {
        ArrayList<String> tags = new ArrayList<>();

        String todoDateKey = convertDiaryDateToTodoDateKey(diaryDate);
        ArrayList<TodoItem> todoList = TodoStorage.getTodosByDate(this, todoDateKey);

        for (TodoItem todo : todoList) {
            // 체크된 투두만 태그로 표시
            if (todo.isChecked() && todo.getText() != null && !todo.getText().trim().isEmpty()) {
                tags.add(todo.getText().trim());
            }
        }

        return tags;
    }

    private String convertDiaryDateToTodoDateKey(String diaryDate) {
        try {
            SimpleDateFormat diaryFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);
            SimpleDateFormat todoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);

            Date date = diaryFormat.parse(diaryDate);
            if (date != null) {
                return todoFormat.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return diaryDate;
    }
}