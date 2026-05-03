package com.example.database_project;

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

import java.util.ArrayList;

public class TodoDetailActivity extends AppCompatActivity {

    private ImageView btnBack;
//    private ImageView btnSave;
    private EditText etTodoInput;
    private TextView btnAddTodo;
    private RecyclerView rvTodo;

    private View aiContainer;
    private View directContainer;
    private TextView tvTitle;

    private String createMode;

    private final ArrayList<TodoItem> todoList = new ArrayList<>();
    private TodoAdapter todoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_detail);

        createMode = getIntent().getStringExtra("create_mode");

        btnBack = findViewById(R.id.btn_back);
//        btnSave = findViewById(R.id.btn_save);
        etTodoInput = findViewById(R.id.et_todo_input);
        btnAddTodo = findViewById(R.id.btn_add_todo);
        rvTodo = findViewById(R.id.rv_todo);
        aiContainer = findViewById(R.id.ai_container);
        directContainer = findViewById(R.id.direct_container);
        tvTitle = findViewById(R.id.tv_title);

        tvTitle.setText(TodoStorage.getTodayString() + " To-do");

        btnBack.setOnClickListener(v -> finish());

        setupMode();
        setupRecyclerView();
        setupInputActions();
        loadTodoList();
    }

    private void setupMode() {
        if ("ai".equals(createMode)) {
            if (aiContainer != null) aiContainer.setVisibility(View.VISIBLE);
            if (directContainer != null) directContainer.setVisibility(View.GONE);
            Toast.makeText(this, "AI 모드는 나중에 연결할 예정입니다", Toast.LENGTH_SHORT).show();
        } else {
            if (aiContainer != null) aiContainer.setVisibility(View.GONE);
            if (directContainer != null) directContainer.setVisibility(View.VISIBLE);
        }
    }

    private void setupRecyclerView() {
        todoAdapter = new TodoAdapter(todoList, new TodoAdapter.OnTodoChangedListener() {
            @Override
            public void onTodoDeleted() {
                saveTodoList();
            }

            @Override
            public void onTodoUpdated() {
                saveTodoList();
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

//        btnSave.setOnClickListener(v -> {
//            saveTodoList();
//            Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show();
//        });
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

        saveTodoList();
    }

    private void loadTodoList() {
        todoList.clear();
        todoList.addAll(TodoStorage.getTodayTodos(this));
        todoAdapter.notifyDataSetChanged();
    }

    private void saveTodoList() {
        TodoStorage.saveTodayTodos(this, todoList);
    }
}