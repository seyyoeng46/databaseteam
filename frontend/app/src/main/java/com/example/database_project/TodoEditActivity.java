package com.example.database_project;

import android.app.Activity;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TodoEditActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etTodoInput;
    private TextView btnAddTodo;
    private TextView tvSelectedDate;
    private TextView tvTodoCount;
    private RecyclerView rvTodo;

    private String todoDate;
    private String[] originalIds;

    private final ArrayList<TodoItem> todoList = new ArrayList<>();
    private final ArrayList<String> todoIds = new ArrayList<>();
    private TodoAdapter todoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_edit);

        todoDate    = getIntent().getStringExtra("todo_date");
        String[] items = getIntent().getStringArrayExtra("todo_items");
        originalIds = getIntent().getStringArrayExtra("todo_ids");

        btnBack        = findViewById(R.id.btn_back);
        rvTodo         = findViewById(R.id.rv_todo);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        tvTodoCount    = findViewById(R.id.tv_todo_count);
        etTodoInput    = findViewById(R.id.et_todo_input);
        btnAddTodo     = findViewById(R.id.btn_add_todo);

        if (todoDate != null) tvSelectedDate.setText(todoDate);

        // 기존 항목 로드 (타이틀 오름차순 정렬)
        if (items != null) {
            Integer[] idx = new Integer[items.length];
            for (int i = 0; i < items.length; i++) idx[i] = i;
            java.util.Arrays.sort(idx, (a, b) -> items[a].compareTo(items[b]));
            for (int i : idx) {
                todoList.add(new TodoItem(items[i], false));
                todoIds.add(originalIds != null && i < originalIds.length
                        ? originalIds[i] : null);
            }
        }

        setupRecyclerView();
        setupInputActions();

        btnBack.setOnClickListener(v -> {
            setResult(Activity.RESULT_OK);
            finish();
        });

        // 뒤로가기 제스처
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        setResult(Activity.RESULT_OK);
                        finish();
                    }
                });
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

        // 수정 시 DB에도 반영
        todoAdapter.setOnEditListener((position, newText) -> {
            String todoId = todoIds.get(position);
            if (todoId != null) {
                Map<String, String> body = new HashMap<>();
                body.put("title", newText);
                body.put("content", "");

                RetrofitClient.getRoutineApi(this)
                        .patchTodo(todoId, body)
                        .enqueue(new Callback<BasicResponse>() {
                            @Override
                            public void onResponse(Call<BasicResponse> call,
                                                   Response<BasicResponse> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(TodoEditActivity.this,
                                            "수정됐어요!", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<BasicResponse> call, Throwable t) {
                                Toast.makeText(TodoEditActivity.this,
                                        "수정 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        // 삭제 시 DB에서도 삭제
        todoAdapter.setOnDeleteListener((position) -> {
            String todoId = todoIds.get(position);
            if (todoId != null) {
                RetrofitClient.getRoutineApi(this)
                        .deleteTodo(todoId)
                        .enqueue(new Callback<BasicResponse>() {
                            @Override
                            public void onResponse(Call<BasicResponse> call,
                                                   Response<BasicResponse> response) {
                                todoList.remove(position);
                                todoIds.remove(position);
                                todoAdapter.notifyItemRemoved(position);
                                todoAdapter.notifyItemRangeChanged(position, todoList.size());
                                updateTodoCount();
                            }

                            @Override
                            public void onFailure(Call<BasicResponse> call, Throwable t) {
                                Toast.makeText(TodoEditActivity.this,
                                        "삭제 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                todoList.remove(position);
                todoIds.remove(position);
                todoAdapter.notifyItemRemoved(position);
                todoAdapter.notifyItemRangeChanged(position, todoList.size());
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

        String date = todoDate.replace(".", "-");
        Map<String, String> body = new HashMap<>();
        body.put("title", input);
        body.put("content", "");
        body.put("target_date", date);

        etTodoInput.setText("");

        RetrofitClient.getRoutineApi(this)
                .addTodo(body)
                .enqueue(new Callback<BasicResponse>() {
                    @Override
                    public void onResponse(Call<BasicResponse> call,
                                           Response<BasicResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success) {
                            reloadTodos(date);
                        } else {
                            Toast.makeText(TodoEditActivity.this,
                                    "추가 실패", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BasicResponse> call, Throwable t) {
                        Toast.makeText(TodoEditActivity.this,
                                "추가 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void reloadTodos(String date) {
        RetrofitClient.getRoutineApi(this)
                .getTodos(date)
                .enqueue(new Callback<TodoResponse>() {
                    @Override
                    public void onResponse(Call<TodoResponse> call,
                                           Response<TodoResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success) {
                            List<TodoResponse.TodoData> dataList = response.body().data;
                            dataList.sort((a, b) -> a.title.compareTo(b.title));
                            todoList.clear();
                            todoIds.clear();
                            for (TodoResponse.TodoData data : dataList) {
                                todoList.add(new TodoItem(data.title, data.isCompleted));
                                todoIds.add(data.id);
                            }
                            todoAdapter.notifyDataSetChanged();
                            updateTodoCount();
                        }
                    }

                    @Override
                    public void onFailure(Call<TodoResponse> call, Throwable t) {
                        Toast.makeText(TodoEditActivity.this,
                                "불러오기 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateTodoCount() {
        if (tvTodoCount != null) {
            tvTodoCount.setText("등록된 항목 " + todoList.size() + "개");
        }
    }
}
