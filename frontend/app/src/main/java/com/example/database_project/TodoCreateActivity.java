package com.example.database_project;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_create);

        createMode = getIntent().getStringExtra("create_mode");
        selectedDate = getIntent().getStringExtra("selected_date");

        btnBack        = findViewById(R.id.btn_back);
        etTodoInput    = findViewById(R.id.et_todo_input);
        btnAddTodo     = findViewById(R.id.btn_add_todo);
        rvTodo         = findViewById(R.id.rv_todo);
        aiContainer    = findViewById(R.id.ai_container);
        directContainer = findViewById(R.id.direct_container);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        tvTodoCount    = findViewById(R.id.tv_todo_count);
        dateSection    = findViewById(R.id.date_section);

        // 날짜 초기 세팅
        if (selectedDate != null && !selectedDate.isEmpty()) {
            tvSelectedDate.setText(selectedDate);
        } else {
            // 기본값: 오늘 날짜
            Calendar cal = Calendar.getInstance();
            selectedDate = String.format(Locale.getDefault(), "%04d.%02d.%02d",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH));
            tvSelectedDate.setText(selectedDate);
        }

        // 날짜 클릭 시 DatePicker
        dateSection.setOnClickListener(v -> showDatePicker());

        resetTodoIfDateChanged();
        btnBack.setOnClickListener(v -> finish());

        setupMode();
        setupRecyclerView();
        setupInputActions();
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