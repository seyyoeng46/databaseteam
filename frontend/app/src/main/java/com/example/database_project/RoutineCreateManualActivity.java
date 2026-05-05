package com.example.database_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoutineCreateManualActivity extends AppCompatActivity {

    private LinearLayout llAlarmList;
    private boolean[] daySelected = {false, false, false, false, false, false, false};
    private TextView[] dayViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_create_manual);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        llAlarmList = findViewById(R.id.ll_alarm_list);

        findViewById(R.id.btn_add_alarm).setOnClickListener(v -> showTimePicker());

        // 순서: 일월화수목금토 (서버랑 동일)
        dayViews = new TextView[]{
                findViewById(R.id.tv_sun), // index 0 = 서버 0(일)
                findViewById(R.id.tv_mon), // index 1 = 서버 1(월)
                findViewById(R.id.tv_tue), // index 2 = 서버 2(화)
                findViewById(R.id.tv_wed), // index 3 = 서버 3(수)
                findViewById(R.id.tv_thu), // index 4 = 서버 4(목)
                findViewById(R.id.tv_fri), // index 5 = 서버 5(금)
                findViewById(R.id.tv_sat)  // index 6 = 서버 6(토)
        };

        for (int i = 0; i < dayViews.length; i++) {
            final int index = i;
            dayViews[i].setOnClickListener(v -> toggleDay(index));
        }

        findViewById(R.id.btn_register).setOnClickListener(v -> registerRoutine());
    }

    private void showTimePicker() {
        Intent intent = new Intent(this, AlarmEditActivity.class);
        startActivityForResult(intent, 1001);
    }

    private View currentEditingRow = null;

    private void addAlarmRow(String time, String desc) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_alarm_edit, llAlarmList, false);

        TextView tvTime = row.findViewById(R.id.tv_alarm_time);
        TextView tvDesc = row.findViewById(R.id.tv_alarm_desc);
        tvTime.setText(time);
        tvDesc.setText(desc);

        row.findViewById(R.id.tv_alarm_edit).setOnClickListener(v -> {
            currentEditingRow = row;
            Intent intent = new Intent(this, AlarmEditActivity.class);
            intent.putExtra("time", tvTime.getText().toString());
            intent.putExtra("desc", tvDesc.getText().toString());
            startActivityForResult(intent, 1002);
        });

        row.findViewById(R.id.tv_alarm_delete).setOnClickListener(v ->
                llAlarmList.removeView(row));

        llAlarmList.addView(row);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        String time = data.getStringExtra("time");
        String desc = data.getStringExtra("desc");

        if (requestCode == 1001) {
            if (time != null && desc != null) {
                addAlarmRow(time, desc);
            }
        } else if (requestCode == 1002) {
            if (currentEditingRow != null && time != null && desc != null) {
                ((TextView) currentEditingRow.findViewById(R.id.tv_alarm_time)).setText(time);
                ((TextView) currentEditingRow.findViewById(R.id.tv_alarm_desc)).setText(desc);
                currentEditingRow = null;
            }
        }
    }

    private void registerRoutine() {
        EditText etName = findViewById(R.id.et_routine_name);
        String routineName = etName.getText().toString().trim();

        if (routineName.isEmpty()) {
            Toast.makeText(this, "루틴 이름을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (llAlarmList.getChildCount() == 0) {
            Toast.makeText(this, "알람을 최소 1개 추가해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // index = 서버값 그대로
        List<Integer> selectedDays = new ArrayList<>();
        for (int i = 0; i < daySelected.length; i++) {
            if (daySelected[i]) selectedDays.add(i);
        }

        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "요일을 최소 1개 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("routine_name", routineName);
        body.put("description", "");
        body.put("schedules", selectedDays);

        RetrofitClient.getRoutineApi(this)
                .createRoutine(body)
                .enqueue(new Callback<BasicResponse>() {
                    @Override
                    public void onResponse(Call<BasicResponse> call,
                                           Response<BasicResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success) {
                            String routineId = response.body().routineId;
                            saveAlarmItems(routineId);
                        } else {
                            Toast.makeText(RoutineCreateManualActivity.this,
                                    "등록 실패", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BasicResponse> call, Throwable t) {
                        Toast.makeText(RoutineCreateManualActivity.this,
                                "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveAlarmItems(String routineId) {
        int count = llAlarmList.getChildCount();
        int[] savedCount = {0};

        for (int i = 0; i < count; i++) {
            View row = llAlarmList.getChildAt(i);
            String time = ((TextView) row.findViewById(R.id.tv_alarm_time)).getText().toString();
            String desc = ((TextView) row.findViewById(R.id.tv_alarm_desc)).getText().toString();
            String itemName = time + " " + desc;

            Map<String, String> itemBody = new HashMap<>();
            itemBody.put("item_name", itemName);

            RetrofitClient.getRoutineApi(this)
                    .addItem(routineId, itemBody)
                    .enqueue(new Callback<BasicResponse>() {
                        @Override
                        public void onResponse(Call<BasicResponse> call,
                                               Response<BasicResponse> response) {
                            savedCount[0]++;
                            if (savedCount[0] == count) {
                                Toast.makeText(RoutineCreateManualActivity.this,
                                        "루틴이 등록됐습니다!", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            }
                        }

                        @Override
                        public void onFailure(Call<BasicResponse> call, Throwable t) {
                            savedCount[0]++;
                            if (savedCount[0] == count) {
                                Toast.makeText(RoutineCreateManualActivity.this,
                                        "루틴이 등록됐습니다!", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            }
                        }
                    });
        }
    }

    private void toggleDay(int index) {
        daySelected[index] = !daySelected[index];
        if (daySelected[index]) {
            dayViews[index].setBackground(
                    ContextCompat.getDrawable(this, R.drawable.bg_day_selected));
            dayViews[index].setTextColor(android.graphics.Color.WHITE);
        } else {
            dayViews[index].setBackground(
                    ContextCompat.getDrawable(this, R.drawable.bg_day_unselected));
            dayViews[index].setTextColor(
                    ContextCompat.getColor(this, R.color.nav_inactive));
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}