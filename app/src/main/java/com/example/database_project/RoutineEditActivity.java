package com.example.database_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class RoutineEditActivity extends AppCompatActivity {

    private boolean[] daySelected = {false, false, false, false, false, false, false};
    private TextView[] dayViews;
    private LinearLayout llAlarmList;
    public static final int REQUEST_ALARM_EDIT = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_edit);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_register).setOnClickListener(v -> finish());

        dayViews = new TextView[]{
                findViewById(R.id.tv_mon), findViewById(R.id.tv_tue),
                findViewById(R.id.tv_wed), findViewById(R.id.tv_thu),
                findViewById(R.id.tv_fri), findViewById(R.id.tv_sat),
                findViewById(R.id.tv_sun)
        };
        for (int i = 0; i < dayViews.length; i++) {
            final int index = i;
            dayViews[i].setOnClickListener(v -> toggleDay(index));
        }

        llAlarmList = findViewById(R.id.ll_alarm_list);
        addAlarmRow("15:10", "냉장고/주방 확인");
        addAlarmRow("15:20", "구매 목록 작성");
        addAlarmRow("15:30", "출발 준비");
    }

    private void addAlarmRow(String time, String desc) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_alarm_edit, llAlarmList, false);
        TextView tvTime = row.findViewById(R.id.tv_alarm_time);
        TextView tvDesc = row.findViewById(R.id.tv_alarm_desc);
        tvTime.setText(time);
        tvDesc.setText(desc);

        row.findViewById(R.id.tv_alarm_edit).setOnClickListener(v -> {
            Intent intent = new Intent(this, AlarmEditActivity.class);
            intent.putExtra("time", tvTime.getText().toString());
            intent.putExtra("desc", tvDesc.getText().toString());
            startActivityForResult(intent, REQUEST_ALARM_EDIT);
        });

        row.findViewById(R.id.tv_alarm_delete).setOnClickListener(v ->
                llAlarmList.removeView(row));

        llAlarmList.addView(row);
    }

    private void toggleDay(int index) {
        daySelected[index] = !daySelected[index];
        if (daySelected[index]) {
            dayViews[index].setBackground(ContextCompat.getDrawable(this, R.drawable.bg_day_selected));
            dayViews[index].setTextColor(android.graphics.Color.WHITE);
        } else {
            dayViews[index].setBackground(ContextCompat.getDrawable(this, R.drawable.bg_day_unselected));
            dayViews[index].setTextColor(ContextCompat.getColor(this, R.color.nav_inactive));
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}