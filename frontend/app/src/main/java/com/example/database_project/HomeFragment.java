package com.example.database_project;

import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private int currentYear, currentMonth;
    private ApiService apiService;

    private LinearLayout llGrass;
    private TextView tvMonth;

    private LinearLayout llRoutine;
    private LinearLayout llList;
    private LayoutInflater savedInflater;

    private final List<TodoResponse.TodoData> todayTodos = new ArrayList<>();
    private final Map<String, DayTodoSummary> todoHistoryMap = new HashMap<>();

    static class DayTodoSummary {
        int totalCount;
        int completedCount;
        List<TodoResponse.TodoData> todos;

        DayTodoSummary(int totalCount, int completedCount, List<TodoResponse.TodoData> todos) {
            this.totalCount = totalCount;
            this.completedCount = completedCount;
            this.todos = todos;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        savedInflater = inflater;
        apiService = ApiClient.getClient(requireContext()).create(ApiService.class);

        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH);

        TextView tvDate = view.findViewById(R.id.tv_todo_date);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA);
        tvDate.setText(sdf.format(new Date()) + " To-do");

        llRoutine = view.findViewById(R.id.ll_routine_items);
        llList = view.findViewById(R.id.ll_list_items);
        tvMonth = view.findViewById(R.id.tv_month_title);
        llGrass = view.findViewById(R.id.ll_grass_calendar);

        ImageView btnTodoArrow = view.findViewById(R.id.btn_todo_arrow);
        btnTodoArrow.setOnClickListener(v -> {
            String selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
            Intent intent = new Intent(getActivity(), TodoDetailActivity.class);
            intent.putExtra("selected_date", selectedDate);
            startActivity(intent);
        });

        loadTodos(savedInflater, llList);

        view.findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 0) {
                currentMonth = 11;
                currentYear--;
            }
            loadMonthTodos();
        });

        view.findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 11) {
                currentMonth = 0;
                currentYear++;
            }
            loadMonthTodos();
        });

        loadMonthTodos();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 상세 페이지에서 변경된 내용을 반영하기 위해 화면으로 돌아올 때마다 데이터를 다시 불러옵니다.
        if (llList != null && savedInflater != null) {
            // 오늘 할 일 목록(최대 5개, 미완료 항목) 갱신
            loadTodos(savedInflater, llList);
            // 하단 잔디(캘린더) 상태 갱신
            loadMonthTodos();
        }
    }

    private void loadTodos(LayoutInflater inflater, LinearLayout parent) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());

        apiService.getTodosByDate(today).enqueue(new Callback<TodoResponse>() {
            @Override
            public void onResponse(Call<TodoResponse> call, Response<TodoResponse> response) {
                if (response.isSuccessful()) {
                    parent.removeAllViews();
                    todayTodos.clear();
                    if (response.body() != null && response.body().data != null) {
                        todayTodos.addAll(response.body().data);
                        redrawTodoListOnly(); // 여기서 필터링된 리스트를 그림
                    }
                } else {
                    addListItem(inflater, parent, -1, "서버 내부 오류 (500)", false);
                }
                loadRoutines(savedInflater, llRoutine);
            }

            @Override
            public void onFailure(Call<TodoResponse> call, Throwable t) {
                parent.removeAllViews();
                todayTodos.clear();
                addListItem(inflater, parent, -1, "Todo 불러오기 실패", false);
                loadRoutines(savedInflater, llRoutine);
            }
        });
    }

    // [수정 핵심] 홈 화면 전용 투두 리스트 갱신 로직
    private void redrawTodoListOnly() {
        if (!isAdded() || llList == null || savedInflater == null) return;
        llList.removeAllViews();

        // 1. 체크 안 된(미완료) 할 일만 필터링
        List<TodoResponse.TodoData> uncompletedTodos = new ArrayList<>();
        for (TodoResponse.TodoData todo : todayTodos) {
            if (todo != null && !todo.isCompleted) {
                uncompletedTodos.add(todo);
            }
        }

        // 2. 오늘 할 일이 아예 없거나, 모두 완료한 경우 처리
        if (uncompletedTodos.isEmpty()) {
            String msg = todayTodos.isEmpty() ? "오늘 할 일이 없습니다" : "오늘의 할 일을 모두 마쳤습니다! ✨";
            addListItem(savedInflater, llList, -1, msg, false);
            return;
        }

        // 3. 최대 5개까지만 리스트에 추가
        int limit = Math.min(uncompletedTodos.size(), 5);
        for (int i = 0; i < limit; i++) {
            TodoResponse.TodoData todo = uncompletedTodos.get(i);
            addListItem(
                    savedInflater,
                    llList,
                    todo.id,
                    getTodoDisplayText(todo),
                    todo.isCompleted
            );
        }
    }

    private void loadRoutines(LayoutInflater inflater, LinearLayout parent) {
        apiService.getRoutines().enqueue(new Callback<RoutineResponse>() {
            @Override
            public void onResponse(Call<RoutineResponse> call, Response<RoutineResponse> response) {
                parent.removeAllViews();
                if (response.isSuccessful() && response.body() != null && response.body().data != null && !response.body().data.isEmpty()) {
                    for (RoutineResponse.RoutineData routine : response.body().data) {
                        loadRoutineItemsAndAdd(inflater, parent, routine);
                    }
                } else {
                    addRoutineItem(inflater, parent, "등록된 루틴이 없습니다", "", false, new ArrayList<>());
                }
            }
            @Override
            public void onFailure(Call<RoutineResponse> call, Throwable t) {
                parent.removeAllViews();
                addRoutineItem(inflater, parent, "루틴 불러오기 실패", "", false, new ArrayList<>());
            }
        });
    }

    private void loadRoutineItemsAndAdd(LayoutInflater inflater, LinearLayout parent, RoutineResponse.RoutineData routine) {
        apiService.getRoutineItems(routine.id).enqueue(new Callback<RoutineItemResponse>() {
            @Override
            public void onResponse(Call<RoutineItemResponse> call, Response<RoutineItemResponse> response) {
                List<RoutineItemResponse.RoutineItemData> items = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    items.addAll(response.body().data);
                }
                addRoutineItem(inflater, parent, routine.routineName, "", false, items);
            }
            @Override
            public void onFailure(Call<RoutineItemResponse> call, Throwable t) {
                addRoutineItem(inflater, parent, routine.routineName, "", false, new ArrayList<>());
            }
        });
    }

    private void addRoutineItem(LayoutInflater inflater, LinearLayout parent, String content, String time, boolean done, List<RoutineItemResponse.RoutineItemData> routineItems) {
        if (content == null || content.trim().isEmpty() || content.equals("등록된 루틴이 없습니다") || content.equals("루틴 불러오기 실패")) {
            TextView emptyText = new TextView(requireContext());
            emptyText.setText(content);
            emptyText.setTextColor(Color.parseColor("#FFFFFF"));
            emptyText.setTextSize(14);
            parent.addView(emptyText);
            return;
        }

        View item = inflater.inflate(R.layout.item_todo_routine, parent, false);
        TextView tvContent = item.findViewById(R.id.tv_content);
        TextView tvTime = item.findViewById(R.id.tv_time);
        ImageView ivCheck = item.findViewById(R.id.iv_check);

        if (ivCheck != null) ivCheck.setVisibility(View.GONE);
        tvContent.setText(content);

        StringBuilder subItems = new StringBuilder();
        if (routineItems != null && !routineItems.isEmpty()) {
            for (RoutineItemResponse.RoutineItemData routineItem : routineItems) {
                subItems.append("• ").append(routineItem.getDisplayName()).append("\n");
            }
        }

        if (subItems.length() > 0) {
            tvTime.setVisibility(View.VISIBLE);
            tvTime.setText(subItems.toString().trim());
        } else {
            tvTime.setVisibility(View.GONE);
        }

        tvContent.setPaintFlags(tvContent.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
        tvContent.setAlpha(1f);
        parent.addView(item);
    }

    private void addListItem(LayoutInflater inflater, LinearLayout parent, int todoId, String content, boolean done) {
        if (content == null || content.trim().isEmpty() || content.equals("오늘 할 일이 없습니다") || content.equals("Todo 불러오기 실패") || content.equals("오늘의 할 일을 모두 마쳤습니다! ✨")) {
            TextView emptyText = new TextView(requireContext());
            emptyText.setText(content);
            emptyText.setTextColor(Color.parseColor("#FFFFFF"));
            emptyText.setTextSize(14);
            parent.addView(emptyText);
            return;
        }

        View item = inflater.inflate(R.layout.item_todo_row, parent, false);
        TextView tvContent = item.findViewById(R.id.tv_content);
        ImageView ivCheck = item.findViewById(R.id.iv_check);

        tvContent.setText(content);
        final boolean[] checked = {done};

        Runnable applyState = () -> {
            if (checked[0]) {
                ivCheck.setImageResource(R.drawable.ic_check_box_done);
                tvContent.setPaintFlags(tvContent.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                tvContent.setAlpha(0.65f);
            } else {
                ivCheck.setImageResource(R.drawable.ic_check_box_empty);
                tvContent.setPaintFlags(tvContent.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                tvContent.setAlpha(1f);
            }
        };

        applyState.run();

        View.OnClickListener clickListener = v -> {
            if (todoId == -1) return;

            boolean targetDone = !checked[0];
            checked[0] = targetDone;
            applyState.run();

            // 1. 메모리 내의 리스트 데이터 상태 먼저 업데이트 (즉각적인 반응)
            for (TodoResponse.TodoData todo : todayTodos) {
                if (todo.id == todoId) {
                    todo.isCompleted = targetDone;
                    break;
                }
            }

            // 2. 홈 화면 상단 리스트 갱신 (체크된 항목 제거 등)
            redrawTodoListOnly();

            // 3. 서버 업데이트 시도
            apiService.updateTodo(todoId, new TodoUpdateRequest(targetDone))
                    .enqueue(new Callback<BasicResponse>() {
                        @Override
                        public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                            if (response.isSuccessful()) {
                                // 4. [핵심] 서버 저장이 확인된 후 잔디(달력) 데이터를 다시 불러와서 새로 그림
                                loadMonthTodos();
                            }
                        }
                        @Override
                        public void onFailure(Call<BasicResponse> call, Throwable t) {
                            Log.e("TODO_UPDATE", "수정 실패", t);
                        }
                    });
        };

        item.setOnClickListener(clickListener);
        ivCheck.setOnClickListener(clickListener);
        parent.addView(item);
    }

    private void updateTodoOnServer(int todoId, boolean targetDone) {
        apiService.updateTodo(todoId, new TodoUpdateRequest(targetDone))
                .enqueue(new Callback<BasicResponse>() {
                    @Override
                    public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().success) {
                            Log.d("TODO_UPDATE", "TODO 수정 성공 id = " + todoId);
                        }
                    }
                    @Override
                    public void onFailure(Call<BasicResponse> call, Throwable t) {
                        Log.e("TODO_UPDATE", "TODO 수정 API 호출 실패", t);
                    }
                });
    }

    private void loadMonthTodos() {
        todoHistoryMap.clear();
        if (tvMonth != null) tvMonth.setText((currentMonth + 1) + "월");
        drawGrassCalendar();

        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int day = 1; day <= daysInMonth; day++) {
            String dateKey = String.format(Locale.KOREA, "%04d-%02d-%02d", currentYear, currentMonth + 1, day);
            apiService.getTodosByDate(dateKey).enqueue(new Callback<TodoResponse>() {
                @Override
                public void onResponse(Call<TodoResponse> call, Response<TodoResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                        List<TodoResponse.TodoData> todos = response.body().data;
                        int total = todos.size();
                        int completed = 0;
                        for (TodoResponse.TodoData todo : todos) {
                            if (todo.isCompleted) completed++;
                        }
                        todoHistoryMap.put(dateKey, new DayTodoSummary(total, completed, todos));
                    } else {
                        todoHistoryMap.put(dateKey, new DayTodoSummary(0, 0, new ArrayList<>()));
                    }
                    drawGrassCalendar();
                }
                @Override
                public void onFailure(Call<TodoResponse> call, Throwable t) {
                    todoHistoryMap.put(dateKey, new DayTodoSummary(0, 0, new ArrayList<>()));
                    drawGrassCalendar();
                }
            });
        }
    }

    private void drawGrassCalendar() {
        Context context = getContext();
        if (context == null || !isAdded() || llGrass == null || tvMonth == null) return;

        llGrass.removeAllViews();
        tvMonth.setText((currentMonth + 1) + "월");

        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);
        int firstDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar today = Calendar.getInstance();

        int todayDay = currentYear == today.get(Calendar.YEAR) && currentMonth == today.get(Calendar.MONTH) ? today.get(Calendar.DAY_OF_MONTH) : -1;
        int day = 1;

        for (int row = 0; row < 6; row++) {
            if (day > daysInMonth) break;
            LinearLayout weekRow = new LinearLayout(context);
            weekRow.setOrientation(LinearLayout.HORIZONTAL);
            weekRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, dpToPx(4));
            weekRow.setLayoutParams(rowParams);

            TextView tvLabel = new TextView(context);
            tvLabel.setTextSize(9);
            tvLabel.setTextColor(Color.parseColor("#888780"));
            tvLabel.setMinWidth(dpToPx(24));
            tvLabel.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            tvLabel.setText(String.valueOf(row == 0 ? 1 : day));
            weekRow.addView(tvLabel);

            View sp = new View(context);
            sp.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(4), 1));
            weekRow.addView(sp);

            for (int col = 0; col < 7; col++) {
                int cellDay = row == 0 ? col - firstDay + 1 : day + col;
                View cell = new View(context);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dpToPx(28), 1f);
                cp.setMargins(dpToPx(2), 0, dpToPx(2), 0);
                cell.setLayoutParams(cp);

                if (cellDay < 1 || cellDay > daysInMonth) {
                    cell.setBackgroundColor(Color.TRANSPARENT);
                } else {
                    String dateKey = String.format(Locale.KOREA, "%04d-%02d-%02d", currentYear, currentMonth + 1, cellDay);
                    DayTodoSummary summary = todoHistoryMap.get(dateKey);

                    // 1. 달성률에 따른 기본 배경 레벨 계산
                    int level = summary == null ? 0 : getTodoLevel(summary.completedCount, summary.totalCount);

                    // 2. 배경 설정 (레이어를 겹치기 위해 LayerDrawable 활용 가능)
                    if (cellDay == todayDay) {
                        // [수정 핵심] 오늘 날짜인 경우: 달성률 배경과 오늘 테두리 배경을 겹쳐서 적용
                        android.graphics.drawable.Drawable baseBg = ContextCompat.getDrawable(context, getGrassBg(level));
                        android.graphics.drawable.Drawable todayStroke = ContextCompat.getDrawable(context, R.drawable.bg_grass_today);

                        android.graphics.drawable.Drawable[] layers = {baseBg, todayStroke};
                        cell.setBackground(new android.graphics.drawable.LayerDrawable(layers));
                    } else {
                        // 오늘이 아닌 경우: 일반 달성률 배경만 적용
                        cell.setBackground(ContextCompat.getDrawable(context, getGrassBg(level)));
                    }

                    cell.setOnClickListener(v -> {
                        DayTodoSummary latestSummary = todoHistoryMap.get(dateKey);
                        showTodoHistoryDialog(dateKey, latestSummary);
                    });
                }
                weekRow.addView(cell);
            }
            day = row == 0 ? 1 + 7 - firstDay : day + 7;
            llGrass.addView(weekRow);
        }
    }

    private int getTodoLevel(int completed, int total) {
        if (total == 0) return 0;
        double ratio = (double) completed / total;
        if (ratio <= 0.25) return 1;
        if (ratio <= 0.50) return 2;
        if (ratio <= 0.75) return 3;
        return 4;
    }

    private void showTodoHistoryDialog(String date, DayTodoSummary summary) {
        StringBuilder sb = new StringBuilder();
        if (summary == null || summary.totalCount == 0) {
            sb.append("이 날 등록된 TODO가 없습니다.");
        } else {
            sb.append("완료 ").append(summary.completedCount).append(" / ").append(summary.totalCount).append("\n\n");
            for (TodoResponse.TodoData todo : summary.todos) {
                sb.append(todo.isCompleted ? "✅ " : "⬜ ").append(getTodoDisplayText(todo)).append("\n");
            }
        }
        new AlertDialog.Builder(requireContext()).setTitle(date + " TODO 이력").setMessage(sb.toString()).setPositiveButton("확인", null).show();
    }

    private String getTodoDisplayText(TodoResponse.TodoData todo) {
        if (todo.title != null && !todo.title.trim().isEmpty()) return todo.title;
        if (todo.content != null && !todo.content.trim().isEmpty()) return todo.content;
        return "제목 없는 TODO";
    }

    private int getGrassBg(int level) {
        switch (level) {
            case 1: return R.drawable.bg_calendar_level1;
            case 2: return R.drawable.bg_calendar_level2;
            case 3: return R.drawable.bg_calendar_level3;
            case 4: return R.drawable.bg_calendar_level4;
            default: return R.drawable.bg_calendar_empty;
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}