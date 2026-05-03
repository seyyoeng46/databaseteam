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

        // 뒤로가기
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        llAlarmList = findViewById(R.id.ll_alarm_list);

        // 알람 추가 버튼
        findViewById(R.id.btn_add_alarm).setOnClickListener(v -> showTimePicker());

        // 요일 선택
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

        // 등록 버튼
        findViewById(R.id.btn_register).setOnClickListener(v -> registerRoutine());
    }

    private void showTimePicker() {
        Intent intent = new Intent(this, AlarmEditActivity.class);
        startActivityForResult(intent, 1001);
    }

    private View currentEditingRow = null; // 수정 중인 행 저장

    private void addAlarmRow(String time, String desc) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_alarm_edit, llAlarmList, false);

        TextView tvTime = row.findViewById(R.id.tv_alarm_time);
        TextView tvDesc = row.findViewById(R.id.tv_alarm_desc);
        tvTime.setText(time);
        tvDesc.setText(desc);

        // 수정 - requestCode 1002 사용
        row.findViewById(R.id.tv_alarm_edit).setOnClickListener(v -> {
            currentEditingRow = row; // 수정 중인 행 저장
            Intent intent = new Intent(this, AlarmEditActivity.class);
            intent.putExtra("time", tvTime.getText().toString());
            intent.putExtra("desc", tvDesc.getText().toString());
            startActivityForResult(intent, 1002);
        });

        // 삭제
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
            // 새 알람 추가
            if (time != null && desc != null) {
                addAlarmRow(time, desc);
            }
        } else if (requestCode == 1002) {
            // 기존 알람 수정
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

        // 선택된 요일 수집
        List<Integer> selectedDays = new ArrayList<>();
        for (int i = 0; i < daySelected.length; i++) {
            if (daySelected[i]) selectedDays.add(i); // 0=일, 1=월 ... 6=토
        }

        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "요일을 최소 1개 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // DB에 저장
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", "17c6f527-dad9-4e23-bbcd-7343b2cca698");
        body.put("routine_name", routineName);
        body.put("description", "");
        body.put("schedules", selectedDays);

        RetrofitClient.getRoutineApi()
                .createRoutine(body)
                .enqueue(new Callback<BasicResponse>() {
                    @Override
                    public void onResponse(Call<BasicResponse> call,
                                           Response<BasicResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success) {

                            // 루틴 생성 성공 후 아이템 저장
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

            RetrofitClient.getRoutineApi()
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