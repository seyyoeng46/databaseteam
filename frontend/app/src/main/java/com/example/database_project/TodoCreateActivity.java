package com.example.database_project;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TodoCreateActivity extends AppCompatActivity {

    private ImageView btnBack;
    private ImageView btnSave;
    private EditText etTodoInput;
    private TextView btnAddTodo;
    private RecyclerView rvTodo;

    private View aiContainer;
    private View directContainer;

    private String createMode;

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

        btnBack = findViewById(R.id.btn_back);
        btnSave = findViewById(R.id.btn_save);
        etTodoInput = findViewById(R.id.et_todo_input);
        btnAddTodo = findViewById(R.id.btn_add_todo);
        rvTodo = findViewById(R.id.rv_todo);
        aiContainer = findViewById(R.id.ai_container);
        directContainer = findViewById(R.id.direct_container);

        resetTodoIfDateChanged();

        btnBack.setOnClickListener(v -> finish());

        setupMode();
        setupRecyclerView();
        setupInputActions();
        loadSampleOrSavedData();
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
                saveTodoCount(todoList.size()); // 직접 계산
            }

            @Override
            public void onTodoUpdated() {
            }
        });

        rvTodo.setLayoutManager(new LinearLayoutManager(this));
        rvTodo.setAdapter(todoAdapter);
    }

    private void setupInputActions() {
        btnAddTodo.setOnClickListener(v -> addTodo());

        etTodoInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND) {
                addTodo();
                return true;
            }
            return false;
        });

        btnSave.setOnClickListener(v ->
                Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show()
        );
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

        saveTodoCount(todoList.size());
    }

    private void loadSampleOrSavedData() {
        if (!"direct".equals(createMode)) return;

        SharedPreferences pref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        int count = pref.getInt(KEY_TODO_COUNT, 0);

        if (count == 0) {
            saveTodoCount(0);
        }
    }

    private void saveTodoCount(int count) {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_TODO_DATE, getTodayString())
                .putInt(KEY_TODO_COUNT, count)
                .apply();
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