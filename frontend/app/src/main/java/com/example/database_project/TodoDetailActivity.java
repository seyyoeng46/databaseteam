package com.example.database_project;

import android.widget.ImageView;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TodoDetailActivity extends AppCompatActivity {

    private ApiService apiService;
    private String selectedDate;
    private TextView tvDateTitle;
    private LinearLayout llRoutineContainer, llTodoContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_detail);

        // 1. 초기화 및 데이터 수신
        apiService = ApiClient.getClient(this).create(ApiService.class);
        selectedDate = getIntent().getStringExtra("selected_date");

        tvDateTitle = findViewById(R.id.tv_detail_date);
        llRoutineContainer = findViewById(R.id.ll_detail_routines);
        llTodoContainer = findViewById(R.id.ll_detail_todos);

        tvDateTitle.setText(selectedDate + " 상세 보기");

        // 2. 데이터 로드
        loadRoutines();
        loadTodos();

        // 뒤로가기 버튼
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    // --- 루틴 불러오기 (체크 기능 없음) ---
    private void loadRoutines() {
        apiService.getRoutines().enqueue(new Callback<RoutineResponse>() {
            @Override
            public void onResponse(Call<RoutineResponse> call, Response<RoutineResponse> response) {
                llRoutineContainer.removeAllViews();
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    for (RoutineResponse.RoutineData routine : response.body().data) {
                        fetchRoutineItems(routine);
                    }
                }
            }
            @Override
            public void onFailure(Call<RoutineResponse> call, Throwable t) {
                Log.e("DETAIL_ROUTINE", "실패", t);
            }
        });
    }

    private void fetchRoutineItems(RoutineResponse.RoutineData routine) {
        apiService.getRoutineItems(routine.id).enqueue(new Callback<RoutineItemResponse>() {
            @Override
            public void onResponse(Call<RoutineItemResponse> call, Response<RoutineItemResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    addRoutineView(routine.routineName, response.body().data);
                }
            }
            @Override
            public void onFailure(Call<RoutineItemResponse> call, Throwable t) {}
        });
    }

    private void addRoutineView(String name, List<RoutineItemResponse.RoutineItemData> items) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_todo_routine, llRoutineContainer, false);
        TextView tvName = v.findViewById(R.id.tv_content);
        TextView tvItems = v.findViewById(R.id.tv_time);
        v.findViewById(R.id.iv_check).setVisibility(View.GONE); // 체크박스 숨김

        tvName.setTextColor(android.graphics.Color.parseColor("#2C2C2A"));
        tvItems.setTextColor(android.graphics.Color.parseColor("#666666"));

        tvName.setText(name);
        StringBuilder sb = new StringBuilder();
        if (items != null) {
            for (RoutineItemResponse.RoutineItemData item : items) {
                sb.append("• ").append(item.getDisplayName()).append("\n");
            }
        }
        tvItems.setText(sb.toString().trim());
        llRoutineContainer.addView(v);
    }

    // --- 투두 불러오기 (체크 기능 포함) ---
    private void loadTodos() {
        apiService.getTodosByDate(selectedDate).enqueue(new Callback<TodoResponse>() {
            @Override
            public void onResponse(Call<TodoResponse> call, Response<TodoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // [로그 추가] 서버가 보내준 데이터의 실제 개수 확인
                    Log.d("DEBUG_COUNT", "서버에서 받은 투두 개수: " + response.body().data.size());

                    llTodoContainer.removeAllViews();
                    for (TodoResponse.TodoData todo : response.body().data) {
                        addTodoView(todo);
                    }
                }
            }
            @Override
            public void onFailure(Call<TodoResponse> call, Throwable t) {}
        });
    }

    private void addTodoView(TodoResponse.TodoData todo) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_todo_row, llTodoContainer, false);
        TextView tvTitle = v.findViewById(R.id.tv_content);
        ImageView ivCheck = v.findViewById(R.id.iv_check);

        tvTitle.setTextColor(android.graphics.Color.parseColor("#2C2C2A"));
        tvTitle.setText(todo.title != null ? todo.title : todo.content);

        // 상태 업데이트 로직
        Runnable updateUI = () -> {
            if (todo.isCompleted) {
                ivCheck.setImageResource(R.drawable.ic_check_box_done);
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                tvTitle.setAlpha(0.5f);
            } else {
                ivCheck.setImageResource(R.drawable.ic_check_box_empty);
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                tvTitle.setAlpha(1f);
            }
        };

        updateUI.run();

        View.OnClickListener listener = view -> {
            todo.isCompleted = !todo.isCompleted;
            updateUI.run();
            // 서버에 상태 전송
            apiService.updateTodo(todo.id, new TodoUpdateRequest(todo.isCompleted)).enqueue(new Callback<BasicResponse>() {
                @Override public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {}
                @Override public void onFailure(Call<BasicResponse> call, Throwable t) {}
            });
        };

        v.setOnClickListener(listener);
        ivCheck.setOnClickListener(listener);
        llTodoContainer.addView(v);
    }
}