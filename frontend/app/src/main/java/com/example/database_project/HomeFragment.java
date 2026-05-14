package com.example.database_project;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private int currentYear, currentMonth;
    private int selectedYear, selectedMonth, selectedDay;
    private LayoutInflater myInflater;
    private LinearLayout llRoutine;
    private LinearLayout llList;
    private TextView tvDate;
    private ScrollView scrollView;
    private int[] monthlyCompletedData = new int[32];

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        myInflater = inflater;

        Calendar cal = Calendar.getInstance();
        currentYear   = cal.get(Calendar.YEAR);
        currentMonth  = cal.get(Calendar.MONTH);
        selectedYear  = currentYear;
        selectedMonth = currentMonth;
        selectedDay   = cal.get(Calendar.DAY_OF_MONTH);

        tvDate     = view.findViewById(R.id.tv_todo_date);
        llRoutine  = view.findViewById(R.id.ll_routine_items);
        llList     = view.findViewById(R.id.ll_list_items);
        scrollView = view.findViewById(R.id.scroll_view);

        updateDateLabel();
        loadTodayData();

        view.findViewById(R.id.btn_prev_day).setOnClickListener(v -> {
            Calendar c = getSelectedCalendar();
            c.add(Calendar.DAY_OF_MONTH, -1);
            selectedYear  = c.get(Calendar.YEAR);
            selectedMonth = c.get(Calendar.MONTH);
            selectedDay   = c.get(Calendar.DAY_OF_MONTH);
            updateDateLabel();
            loadTodayData();
        });

        view.findViewById(R.id.btn_next_day).setOnClickListener(v -> {
            Calendar c = getSelectedCalendar();
            c.add(Calendar.DAY_OF_MONTH, 1);
            selectedYear  = c.get(Calendar.YEAR);
            selectedMonth = c.get(Calendar.MONTH);
            selectedDay   = c.get(Calendar.DAY_OF_MONTH);
            updateDateLabel();
            loadTodayData();
        });

        TextView tvMonth     = view.findViewById(R.id.tv_month_title);
        LinearLayout llGrass = view.findViewById(R.id.ll_grass_calendar);

        loadMonthlyData(llGrass, tvMonth);

        view.findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 0) { currentMonth = 11; currentYear--; }
            loadMonthlyData(llGrass, tvMonth);
        });

        view.findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 11) { currentMonth = 0; currentYear++; }
            loadMonthlyData(llGrass, tvMonth);
        });

        return view;
    }

    private Calendar getSelectedCalendar() {
        Calendar c = Calendar.getInstance();
        c.set(selectedYear, selectedMonth, selectedDay);
        return c;
    }

    private void updateDateLabel() {
        Calendar c = getSelectedCalendar();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA);
        tvDate.setText(sdf.format(c.getTime()) + " To-do");
    }

    private void loadTodayData() {
        Calendar c = getSelectedCalendar();
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK) - 1;
        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(c.getTime());

        loadTodayRoutines(dayOfWeek, dateStr);
        loadTodayTodos(dateStr);
    }

    private void loadTodayRoutines(int todayDayOfWeek, String dateStr) {
        RetrofitClient.getRoutineApi(getContext())
                .getRoutines()
                .enqueue(new Callback<RoutineResponse>() {
                    @Override
                    public void onResponse(Call<RoutineResponse> call,
                                           Response<RoutineResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success) {
                            llRoutine.removeAllViews();
                            for (RoutineResponse.RoutineData data : response.body().data) {
                                if (data.schedules != null
                                        && data.schedules.contains(todayDayOfWeek)) {
                                    loadRoutineItems(data.id, data.routineName, dateStr);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<RoutineResponse> call, Throwable t) {}
                });
    }

    private void loadRoutineItems(String routineId, String routineName, String dateStr) {
        RetrofitClient.getRoutineApi(getContext())
                .getRoutineCompletions(routineId, dateStr)
                .enqueue(new Callback<RoutineCompletionResponse>() {
                    @Override
                    public void onResponse(Call<RoutineCompletionResponse> call,
                                           Response<RoutineCompletionResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().data != null) {

                            TextView tvHeader = new TextView(requireContext());
                            tvHeader.setText(routineName);
                            tvHeader.setTextSize(11f);
                            tvHeader.setTextColor(Color.parseColor("#C8E6C9"));
                            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                            hp.setMargins(0, dpToPx(4), 0, dpToPx(2));
                            tvHeader.setLayoutParams(hp);
                            llRoutine.addView(tvHeader);

                            for (RoutineCompletionResponse.CompletionData item : response.body().data) {
                                String title = item.title;
                                String time  = "";
                                String desc  = title;
                                if (title != null && title.contains(" ")) {
                                    int idx = title.indexOf(" ");
                                    time = title.substring(0, idx);
                                    desc = title.substring(idx + 1);
                                }
                                addRoutineItem(myInflater, llRoutine,
                                        routineId, item.id, desc, time, item.isCompleted, dateStr);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<RoutineCompletionResponse> call, Throwable t) {}
                });
    }

    private void loadTodayTodos(String dateStr) {
        RetrofitClient.getRoutineApi(getContext())
                .getTodos(dateStr)
                .enqueue(new Callback<TodoResponse>() {
                    @Override
                    public void onResponse(Call<TodoResponse> call,
                                           Response<TodoResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success) {
                            llList.removeAllViews();
                            for (TodoResponse.TodoData item : response.body().data) {
                                addListItem(myInflater, llList,
                                        item.id, item.title, item.isCompleted);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<TodoResponse> call, Throwable t) {}
                });
    }

    private void loadMonthlyData(LinearLayout llGrass, TextView tvMonth) {
        RetrofitClient.getRoutineApi(getContext())
                .getMonthlyTodos(currentYear, currentMonth + 1)
                .enqueue(new Callback<MonthlyResponse>() {
                    @Override
                    public void onResponse(Call<MonthlyResponse> call,
                                           Response<MonthlyResponse> response) {
                        monthlyCompletedData = new int[32];
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success) {
                            for (MonthlyResponse.MonthlyData data : response.body().data) {
                                int day = Integer.parseInt(data.day.trim());
                                int completed = data.completed != null
                                        ? Integer.parseInt(data.completed.trim()) : 0;
                                monthlyCompletedData[day] = completed;
                            }
                        }
                        drawGrassCalendar(llGrass, tvMonth);
                    }

                    @Override
                    public void onFailure(Call<MonthlyResponse> call, Throwable t) {
                        drawGrassCalendar(llGrass, tvMonth);
                    }
                });
    }

    private int getGrassLevel(int completed) {
        if (completed >= 12) return 4;
        if (completed >= 9)  return 3;
        if (completed >= 6)  return 2;
        if (completed >= 3)  return 1;
        return 0;
    }

    private void addRoutineItem(LayoutInflater inflater, LinearLayout parent,
                                String routineId, String itemId,
                                String content, String time, boolean done, String dateStr) {
        View item = inflater.inflate(R.layout.item_routine_row, parent, false);
        TextView tvContent = item.findViewById(R.id.tv_content);
        TextView tvTime    = item.findViewById(R.id.tv_time);
        ImageView ivCheck  = item.findViewById(R.id.iv_check);

        tvContent.setText(content);
        tvTime.setText(time);

        final boolean[] isChecked = {done};
        updateRoutineCheckStyle(tvContent, ivCheck, done);

        ivCheck.setOnClickListener(v -> {
            isChecked[0] = !isChecked[0];
            updateRoutineCheckStyle(tvContent, ivCheck, isChecked[0]);

            Map<String, Object> body = new HashMap<>();
            body.put("completed_date", dateStr);
            body.put("is_completed", isChecked[0]);

            RetrofitClient.getRoutineApi(getContext())
                    .completeRoutineItem(routineId, itemId, body)
                    .enqueue(new Callback<BasicResponse>() {
                        @Override
                        public void onResponse(Call<BasicResponse> call,
                                               Response<BasicResponse> response) {
                            if (response.isSuccessful()) {
                                // 달력 새로고침
                                TextView tvMonth     = requireView().findViewById(R.id.tv_month_title);
                                LinearLayout llGrass = requireView().findViewById(R.id.ll_grass_calendar);
                                loadMonthlyData(llGrass, tvMonth);
                            } else {
                                isChecked[0] = !isChecked[0];
                                updateRoutineCheckStyle(tvContent, ivCheck, isChecked[0]);
                            }
                        }

                        @Override
                        public void onFailure(Call<BasicResponse> call, Throwable t) {
                            isChecked[0] = !isChecked[0];
                            updateRoutineCheckStyle(tvContent, ivCheck, isChecked[0]);
                        }
                    });
        });

        parent.addView(item);
    }

    private void updateRoutineCheckStyle(TextView tvContent, ImageView ivCheck, boolean checked) {
        if (checked) {
            ivCheck.setImageResource(R.drawable.ic_check_circle_done);
            tvContent.setPaintFlags(tvContent.getPaintFlags() |
                    android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            tvContent.setAlpha(0.65f);
        } else {
            ivCheck.setImageResource(R.drawable.ic_check_circle_empty);
            tvContent.setPaintFlags(tvContent.getPaintFlags() &
                    ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            tvContent.setAlpha(1.0f);
        }
    }

    private void addListItem(LayoutInflater inflater, LinearLayout parent,
                             String id, String content, boolean done) {
        View item = inflater.inflate(R.layout.item_todo_row, parent, false);
        TextView tvContent = item.findViewById(R.id.tv_content);
        ImageView ivCheck  = item.findViewById(R.id.iv_check);

        tvContent.setText(content);

        final boolean[] isChecked = {done};
        updateCheckStyle(tvContent, ivCheck, done);

        ivCheck.setOnClickListener(v -> {
            isChecked[0] = !isChecked[0];
            updateCheckStyle(tvContent, ivCheck, isChecked[0]);

            Map<String, String> body = new HashMap<>();
            body.put("is_completed", String.valueOf(isChecked[0]));

            RetrofitClient.getRoutineApi(getContext())
                    .patchTodo(id, body)
                    .enqueue(new Callback<BasicResponse>() {
                        @Override
                        public void onResponse(Call<BasicResponse> call,
                                               Response<BasicResponse> response) {
                            if (response.isSuccessful()) {
                                TextView tvMonth     = requireView().findViewById(R.id.tv_month_title);
                                LinearLayout llGrass = requireView().findViewById(R.id.ll_grass_calendar);
                                loadMonthlyData(llGrass, tvMonth);
                            } else {
                                isChecked[0] = !isChecked[0];
                                updateCheckStyle(tvContent, ivCheck, isChecked[0]);
                            }
                        }

                        @Override
                        public void onFailure(Call<BasicResponse> call, Throwable t) {
                            isChecked[0] = !isChecked[0];
                            updateCheckStyle(tvContent, ivCheck, isChecked[0]);
                        }
                    });
        });

        parent.addView(item);
    }

    private void updateCheckStyle(TextView tvContent, ImageView ivCheck, boolean checked) {
        if (checked) {
            ivCheck.setImageResource(R.drawable.ic_check_box_done);
            tvContent.setPaintFlags(tvContent.getPaintFlags() |
                    android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            tvContent.setAlpha(0.65f);
        } else {
            ivCheck.setImageResource(R.drawable.ic_check_box_empty);
            tvContent.setPaintFlags(tvContent.getPaintFlags() &
                    ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            tvContent.setAlpha(1.0f);
        }
    }

    private void drawGrassCalendar(LinearLayout container, TextView tvMonth) {
        container.removeAllViews();
        tvMonth.setText((currentMonth + 1) + "월");

        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);
        int firstDay    = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar today  = Calendar.getInstance();
        int todayDay    = (currentYear == today.get(Calendar.YEAR) &&
                currentMonth == today.get(Calendar.MONTH))
                ? today.get(Calendar.DAY_OF_MONTH) : -1;

        int day = 1;
        for (int row = 0; row < 6; row++) {
            if (day > daysInMonth) break;

            LinearLayout weekRow = new LinearLayout(requireContext());
            weekRow.setOrientation(LinearLayout.HORIZONTAL);
            weekRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, dpToPx(4));
            weekRow.setLayoutParams(rowParams);

            TextView tvLabel = new TextView(requireContext());
            tvLabel.setTextSize(9);
            tvLabel.setTextColor(Color.parseColor("#888780"));
            tvLabel.setMinWidth(dpToPx(24));
            tvLabel.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
            tvLabel.setText(String.valueOf(row == 0 ? 1 : day));
            weekRow.addView(tvLabel);

            View sp = new View(requireContext());
            sp.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(4), 1));
            weekRow.addView(sp);

            for (int col = 0; col < 7; col++) {
                int cellDay = (row == 0) ? (col - firstDay + 1) : (day + col);
                View cell   = new View(requireContext());
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dpToPx(28), 1f);
                cp.setMargins(dpToPx(2), 0, dpToPx(2), 0);
                cell.setLayoutParams(cp);

                if (cellDay < 1 || cellDay > daysInMonth) {
                    cell.setBackgroundColor(Color.TRANSPARENT);
                } else {
                    boolean isToday    = (cellDay == todayDay);
                    boolean isSelected = (currentYear == selectedYear
                            && currentMonth == selectedMonth
                            && cellDay == selectedDay
                            && !isToday);
                    int lv = getGrassLevel(monthlyCompletedData[cellDay]);

                    if (isToday) {
                        if (lv == 0) {
                            cell.setBackground(ContextCompat.getDrawable(
                                    requireContext(), R.drawable.bg_grass_today));
                        } else {
                            android.graphics.drawable.GradientDrawable gd =
                                    new android.graphics.drawable.GradientDrawable();
                            gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                            gd.setCornerRadius(dpToPx(4));
                            gd.setStroke(dpToPx(2), Color.parseColor("#2E7D32"));
                            switch (lv) {
                                case 1: gd.setColor(Color.parseColor("#C8E6C9")); break;
                                case 2: gd.setColor(Color.parseColor("#66BB6A")); break;
                                case 3: gd.setColor(Color.parseColor("#388E3C")); break;
                                case 4: gd.setColor(Color.parseColor("#1B5E20")); break;
                            }
                            cell.setBackground(gd);
                        }
                    } else if (isSelected) {
                        android.graphics.drawable.GradientDrawable gd =
                                new android.graphics.drawable.GradientDrawable();
                        gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                        gd.setCornerRadius(dpToPx(4));
                        gd.setStroke(dpToPx(2), Color.parseColor("#FF8F00"));
                        int fill;
                        switch (lv) {
                            case 1: fill = Color.parseColor("#C8E6C9"); break;
                            case 2: fill = Color.parseColor("#66BB6A"); break;
                            case 3: fill = Color.parseColor("#388E3C"); break;
                            case 4: fill = Color.parseColor("#1B5E20"); break;
                            default: fill = Color.parseColor("#EDEDEA");  break;
                        }
                        gd.setColor(fill);
                        cell.setBackground(gd);
                    } else {
                        cell.setBackground(ContextCompat.getDrawable(
                                requireContext(), getGrassBg(lv)));
                    }

                    final int finalCellDay = cellDay;
                    cell.setClickable(true);
                    cell.setFocusable(true);
                    cell.setOnClickListener(v -> {
                        selectedYear  = currentYear;
                        selectedMonth = currentMonth;
                        selectedDay   = finalCellDay;
                        updateDateLabel();
                        loadTodayData();
                        drawGrassCalendar(container, tvMonth);
                        if (scrollView != null) {
                            scrollView.post(() -> scrollView.smoothScrollTo(0, 0));
                        }
                    });
                }
                weekRow.addView(cell);
            }

            day = (row == 0) ? (1 + 7 - firstDay) : (day + 7);
            container.addView(weekRow);
        }
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