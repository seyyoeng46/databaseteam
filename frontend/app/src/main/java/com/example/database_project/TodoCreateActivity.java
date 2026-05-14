package com.example.database_project;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TodoCreateActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etTodoInput;
    private TextView btnAddTodo;
    private TextView tvSelectedDate;
    private TextView tvTodoCount;
    private LinearLayout dateSection;
    private RecyclerView rvTodo;

    private View aiContainer;
    private View directContainer;

    private String createMode;
    private String selectedDate = "";

    private final ArrayList<TodoItem> todoList = new ArrayList<>();
    private TodoAdapter todoAdapter;

    private static final String PREF_NAME = "todo_pref";
    private static final String KEY_TODO_COUNT = "todo_count";
    private static final String KEY_TODO_DATE = "todo_date";

    private boolean isSaving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_create);

        createMode   = getIntent().getStringExtra("create_mode");
        selectedDate = getIntent().getStringExtra("selected_date");

        btnBack         = findViewById(R.id.btn_back);
        etTodoInput     = findViewById(R.id.et_todo_input);
        btnAddTodo      = findViewById(R.id.btn_add_todo);
        rvTodo          = findViewById(R.id.rv_todo);
        aiContainer     = findViewById(R.id.ai_container);
        directContainer = findViewById(R.id.direct_container);
        tvSelectedDate  = findViewById(R.id.tv_selected_date);
        tvTodoCount     = findViewById(R.id.tv_todo_count);
        dateSection     = findViewById(R.id.date_section);

        // 날짜 초기 세팅
        if (selectedDate != null && !selectedDate.isEmpty()) {
            tvSelectedDate.setText(selectedDate);
        } else {
            Calendar cal = Calendar.getInstance();
            selectedDate = String.format(Locale.getDefault(), "%04d.%02d.%02d",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH));
            tvSelectedDate.setText(selectedDate);
        }

        dateSection.setOnClickListener(v -> showDatePicker());

        resetTodoIfDateChanged();

        // 뒤로가기 버튼
        btnBack.setOnClickListener(v -> handleBack());

        // 뒤로가기 제스처 처리
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        handleBack();
                    }
                });

        setupMode();
        setupRecyclerView();
        setupInputActions();
    }

    private void handleBack() {
        if (isSaving) return;
        if (todoList.isEmpty()) {
            finish();
            return;
        }

        isSaving = true;
        // 날짜 형식 변환 2026.05.01 → 2026-05-01
        String date = tvSelectedDate.getText().toString().replace(".", "-");

        saveTodosToServer(date);
    }

    private void saveTodosToServer(String date) {
        saveTodosSequential(date, 0);
    }

    // 순차 저장: 앞 항목이 먼저 INSERT되어 ID 순서 = 생성 순서 보장
    private void saveTodosSequential(String date, int index) {
        if (index >= todoList.size()) {
            String displayDate = date.replace("-", ".");
            String[] items = new String[todoList.size()];
            for (int j = 0; j < todoList.size(); j++) {
                items[j] = todoList.get(j).getText();
            }
            Intent result = new Intent();
            result.putExtra("todo_date", displayDate);
            result.putExtra("todo_items", items);
            setResult(Activity.RESULT_OK, result);
            finish();
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("title", todoList.get(index).getText());
        body.put("content", "");
        body.put("target_date", date);

        RetrofitClient.getRoutineApi(this)
                .addTodo(body)
                .enqueue(new Callback<BasicResponse>() {
                    @Override
                    public void onResponse(Call<BasicResponse> call,
                                           Response<BasicResponse> response) {
                        saveTodosSequential(date, index + 1);
                    }

                    @Override
                    public void onFailure(Call<BasicResponse> call, Throwable t) {
                        Toast.makeText(TodoCreateActivity.this,
                                "저장 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        saveTodosSequential(date, index + 1);
                    }
                });
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = String.format(Locale.getDefault(),
                            "%04d.%02d.%02d", year, month + 1, dayOfMonth);
                    tvSelectedDate.setText(selectedDate);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void setupMode() {
        if ("ai".equals(createMode)) {
            if (aiContainer != null) aiContainer.setVisibility(View.VISIBLE);
            if (directContainer != null) directContainer.setVisibility(View.GONE);
            Toast.makeText(this, "AI 추후 구현", Toast.LENGTH_SHORT).show();
        } else {
            if (aiContainer != null) aiContainer.setVisibility(View.GONE);
            if (directContainer != null) directContainer.setVisibility(View.VISIBLE);
        }
    }

    private void setupRecyclerView() {
        todoAdapter = new TodoAdapter(todoList, new TodoAdapter.OnTodoChangedListener() {
            @Override
            public void onTodoDeleted() {
                updateTodoCount();
            }

            @Override
            public void onTodoUpdated() {
                updateTodoCount();
            }
        });

        rvTodo.setLayoutManager(new LinearLayoutManager(this));
        rvTodo.setAdapter(todoAdapter);
        updateTodoCount();
    }

    private void setupInputActions() {
        btnAddTodo.setOnClickListener(v -> addTodo());

        etTodoInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_SEND) {
                addTodo();
                return true;
            }
            return false;
        });
    }

    private void addTodo() {
        String input = etTodoInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "할 일을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        todoList.add(new TodoItem(input, false));
        todoAdapter.notifyItemInserted(todoList.size() - 1);
        rvTodo.scrollToPosition(todoList.size() - 1);
        etTodoInput.setText("");
        updateTodoCount();
    }

    private void updateTodoCount() {
        if (tvTodoCount != null) {
            tvTodoCount.setText("등록된 항목 " + todoList.size() + "개");
        }
    }

    private void resetTodoIfDateChanged() {
        SharedPreferences pref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedDate = pref.getString(KEY_TODO_DATE, "");
        String today = getTodayString();
        if (!today.equals(savedDate)) {
            pref.edit()
                    .putString(KEY_TODO_DATE, today)
                    .putInt(KEY_TODO_COUNT, 0)
                    .apply();
        }
    }

    private String getTodayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }
}