package com.example.database_project;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private int currentYear, currentMonth;
    private final int[] grassData = {
            0,2,0,1,3,0,1, 4,2,3,1,0,2,0,
            1,0,3,2,1,0,2, 0,1,0,2,1,0,3, 0,1
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH);

        TextView tvDate = view.findViewById(R.id.tv_todo_date);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA);
        tvDate.setText(sdf.format(new Date()) + " To-do");

        // 루틴 아이템
        LinearLayout llRoutine = view.findViewById(R.id.ll_routine_items);
        addRoutineItem(inflater, llRoutine, "기상 알람", "07:00", true);
        addRoutineItem(inflater, llRoutine, "출발 준비", "08:00", false);
        addRoutineItem(inflater, llRoutine, "운동 30분", "21:00", false);

        // 리스트 아이템
        LinearLayout llList = view.findViewById(R.id.ll_list_items);
        addListItem(inflater, llList, "과제 하기", true);
        addListItem(inflater, llList, "답장하기", false);
        addListItem(inflater, llList, "쓰레기 버리기", false);

        // 잔디 달력
        TextView tvMonth = view.findViewById(R.id.tv_month_title);
        LinearLayout llGrass = view.findViewById(R.id.ll_grass_calendar);
        drawGrassCalendar(llGrass, tvMonth);

        view.findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 0) { currentMonth = 11; currentYear--; }
            drawGrassCalendar(llGrass, tvMonth);
        });

        view.findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 11) { currentMonth = 0; currentYear++; }
            drawGrassCalendar(llGrass, tvMonth);
        });

        return view;
    }

    private void addRoutineItem(LayoutInflater inflater, LinearLayout parent,
                                String content, String time, boolean done) {
        View item = inflater.inflate(R.layout.item_todo_routine, parent, false);
        TextView tvContent = item.findViewById(R.id.tv_content);
        TextView tvTime = item.findViewById(R.id.tv_time);
        ImageView ivCheck = item.findViewById(R.id.iv_check);

        tvContent.setText(content);
        tvTime.setText(time);

        if (done) {
            ivCheck.setImageResource(R.drawable.ic_check_circle_done);
            tvContent.setPaintFlags(tvContent.getPaintFlags() |
                    android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            tvContent.setAlpha(0.65f);
        } else {
            ivCheck.setImageResource(R.drawable.ic_check_circle_empty);
        }

        parent.addView(item);
    }

    private void addListItem(LayoutInflater inflater, LinearLayout parent,
                             String content, boolean done) {
        View item = inflater.inflate(R.layout.item_todo_list, parent, false);
        TextView tvContent = item.findViewById(R.id.tv_content);
        ImageView ivCheck = item.findViewById(R.id.iv_check);

        tvContent.setText(content);

        if (done) {
            ivCheck.setImageResource(R.drawable.ic_check_box_done);
            tvContent.setPaintFlags(tvContent.getPaintFlags() |
                    android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            tvContent.setAlpha(0.65f);
        } else {
            ivCheck.setImageResource(R.drawable.ic_check_box_empty);
        }

        parent.addView(item);
    }

    private void drawGrassCalendar(LinearLayout container, TextView tvMonth) {
        container.removeAllViews();
        tvMonth.setText((currentMonth + 1) + "월");

        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);
        int firstDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar today = Calendar.getInstance();
        int todayDay = (currentYear == today.get(Calendar.YEAR) &&
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
                View cell = new View(requireContext());
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dpToPx(28), 1f);
                cp.setMargins(dpToPx(2), 0, dpToPx(2), 0);
                cell.setLayoutParams(cp);

                if (cellDay < 1 || cellDay > daysInMonth) {
                    cell.setBackgroundColor(Color.TRANSPARENT);
                } else if (cellDay == todayDay) {
                    cell.setBackground(ContextCompat.getDrawable(
                            requireContext(), R.drawable.bg_grass_today));
                } else {
                    int lv = (cellDay - 1 < grassData.length) ? grassData[cellDay - 1] : 0;
                    cell.setBackground(ContextCompat.getDrawable(
                            requireContext(), getGrassBg(lv)));
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