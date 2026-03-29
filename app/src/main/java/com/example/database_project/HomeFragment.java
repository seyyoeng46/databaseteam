package com.example.database_project;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private ImageView btnTodoAdd;
    private ImageView btnTodoArrow;
    private TextView tvTodoTitle;
    private LinearLayout layoutTodoPreview;

    private ImageView btnPrevMonth;
    private ImageView btnNextMonth;
    private TextView tvMonthTitle;
    private GridLayout gridCalendar;

    private Calendar currentMonth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        btnTodoAdd = view.findViewById(R.id.btn_todo_add);
        btnTodoArrow = view.findViewById(R.id.btn_todo_arrow);
        tvTodoTitle = view.findViewById(R.id.tv_todo_title);
        layoutTodoPreview = view.findViewById(R.id.layout_todo_preview);

        btnPrevMonth = view.findViewById(R.id.btn_prev_month);
        btnNextMonth = view.findViewById(R.id.btn_next_month);
        tvMonthTitle = view.findViewById(R.id.tv_month_title);
        gridCalendar = view.findViewById(R.id.grid_calendar);

        currentMonth = Calendar.getInstance();

        btnTodoAdd.setOnClickListener(v -> showTodoAddDialog());

        btnTodoArrow.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TodoDetailActivity.class);
            startActivity(intent);
        });

        btnPrevMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            renderCalendar();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            renderCalendar();
        });

        updateHomeTodoUI();
        renderCalendar();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHomeTodoUI();
        renderCalendar();
    }

    private void updateHomeTodoUI() {
        ArrayList<TodoItem> list = TodoStorage.getTodayTodos(requireContext());

        tvTodoTitle.setText(TodoStorage.getTodayString() + " To-do");
        layoutTodoPreview.removeAllViews();

        if (list.isEmpty()) {
            btnTodoAdd.setVisibility(View.VISIBLE);
            btnTodoArrow.setVisibility(View.GONE);

            TextView emptyText = new TextView(requireContext());
            emptyText.setText("오늘의 투두리스트가 없습니다.");
            emptyText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            emptyText.setTextSize(18);
            emptyText.setPadding(0, 10, 0, 0);

            layoutTodoPreview.addView(emptyText);
            return;
        }

// 투두가 있어도 + 버튼은 항상 보이게
        btnTodoAdd.setVisibility(View.VISIBLE);
        btnTodoArrow.setVisibility(View.VISIBLE);

        int max = Math.min(list.size(), 6);

        for (int i = 0; i < max; i++) {
            TodoItem item = list.get(i);

            CheckBox cb = new CheckBox(requireContext());
            cb.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            cb.setText(item.getText());
            cb.setChecked(item.isChecked());
            cb.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            cb.setTextSize(15);
            cb.setPadding(0, 8, 0, 8);

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.setChecked(isChecked);
                TodoStorage.saveTodayTodos(requireContext(), list);
                renderCalendar(); // 체크 상태 바뀌면 달력 색도 반영
            });

            layoutTodoPreview.addView(cb);
        }
    }

    private void renderCalendar() {
        gridCalendar.removeAllViews();

        SimpleDateFormat monthFormat = new SimpleDateFormat("M월", Locale.KOREA);
        tvMonthTitle.setText(monthFormat.format(currentMonth.getTime()));

        Calendar cal = (Calendar) currentMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 일요일=0
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < 42; i++) {
            TextView dayView = new TextView(requireContext());

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dpToPx(48);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
            dayView.setLayoutParams(params);

            dayView.setGravity(Gravity.CENTER);
            dayView.setTextSize(16);
            dayView.setBackgroundResource(android.R.color.transparent);

            if (i >= firstDayOfWeek && i < firstDayOfWeek + daysInMonth) {
                int day = i - firstDayOfWeek + 1;
                Calendar dateCal = (Calendar) currentMonth.clone();
                dateCal.set(Calendar.DAY_OF_MONTH, day);

                String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
                        .format(dateCal.getTime());

                dayView.setText(String.valueOf(day));
                dayView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));

                ArrayList<TodoItem> todos = TodoStorage.getTodosByDate(requireContext(), dateKey);

                if (todos.isEmpty()) {
                    dayView.setBackgroundResource(R.drawable.bg_calendar_empty);
                } else {
                    int colorRes = getCalendarColorRes(todos);
                    dayView.setBackgroundResource(colorRes);

                    dayView.setOnClickListener(v -> showTodoHistoryDialog(dateKey, todos));
                }

            } else {
                dayView.setBackgroundResource(android.R.color.transparent);
            }

            gridCalendar.addView(dayView);
        }
    }

    private int getCalendarColorRes(ArrayList<TodoItem> todos) {
        int total = todos.size();
        int done = 0;

        for (TodoItem item : todos) {
            if (item.isChecked()) done++;
        }

        float ratio = total == 0 ? 0 : (float) done / total;

        if (ratio == 0f) return R.drawable.bg_calendar_level1;
        if (ratio < 0.5f) return R.drawable.bg_calendar_level2;
        if (ratio < 1f) return R.drawable.bg_calendar_level3;
        return R.drawable.bg_calendar_level4;
    }

    private void showTodoHistoryDialog(String dateKey, ArrayList<TodoItem> todos) {
        StringBuilder sb = new StringBuilder();

        for (TodoItem item : todos) {
            sb.append(item.isChecked() ? "☑ " : "☐ ")
                    .append(item.getText())
                    .append("\n");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(dateKey + " 투두리스트")
                .setMessage(sb.toString())
                .setPositiveButton("확인", null)
                .show();
    }

    private void showTodoAddDialog() {
        ArrayList<TodoItem> list = TodoStorage.getTodayTodos(requireContext());

        if (list.isEmpty()) {
            // 투두가 없을 때: AI 추가 + 직접 추가 둘 다 표시
            String[] options = {"AI로 투두리스트 추가하기", "직접 추가하기"};

            new AlertDialog.Builder(requireContext())
                    .setTitle("투두리스트 추가")
                    .setItems(options, (dialog, which) -> {
                        Intent intent = new Intent(requireContext(), TodoDetailActivity.class);

                        if (which == 0) {
                            intent.putExtra("create_mode", "ai");
                        } else {
                            intent.putExtra("create_mode", "direct");
                        }

                        startActivity(intent);
                    })
                    .show();

        } else {
            // 투두가 이미 있을 때: AI 추가만 표시
            String[] options = {"AI로 투두리스트 추가하기"};

            new AlertDialog.Builder(requireContext())
                    .setTitle("투두리스트 추가")
                    .setItems(options, (dialog, which) -> {
                        Intent intent = new Intent(requireContext(), TodoDetailActivity.class);
                        intent.putExtra("create_mode", "ai");
                        startActivity(intent);
                    })
                    .show();
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}