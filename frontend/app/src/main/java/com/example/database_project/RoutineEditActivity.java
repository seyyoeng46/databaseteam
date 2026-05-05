package com.example.database_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoutineEditActivity extends AppCompatActivity {

    private boolean[] daySelected = {false, false, false, false, false, false, false};
    private TextView[] dayViews;
    private LinearLayout llAlarmList;
    private TextView tvRoutineName;

    private String routineId;
    private String routineName;

    public static final int REQUEST_ALARM_NEW  = 1001;
    public static final int REQUEST_ALARM_EDIT = 1002;
    private View currentEditingRow = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_edit);

        routineId   = getIntent().getStringExtra("routine_id");
        routineName = getIntent().getStringExtra("routine_name");
        int[] routineSchedules = getIntent().getIntArrayExtra("routine_schedules");
        if (routineSchedules == null) {
            routineSchedules = new int[0];
        }

        tvRoutineName = findViewById(R.id.tv_routine_name);
        if (routineName != null) tvRoutineName.setText(routineName);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_register).setOnClickListener(v -> updateRoutine());

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

        // 요일 미리 체크 (index = 서버값 그대로)
        if (routineSchedules != null) {
            for (int serverDay : routineSchedules) {
                if (serverDay >= 0 && serverDay <= 6) {
                    daySelected[serverDay] = true;
                    dayViews[serverDay].setBackground(
                            ContextCompat.getDrawable(this, R.drawable.bg_day_selected));
                    dayViews[serverDay].setTextColor(android.graphics.Color.WHITE);
                }
            }
        }

        llAlarmList = findViewById(R.id.ll_alarm_list);

        findViewById(R.id.btn_add_alarm).setOnClickListener(v -> {
            Intent intent = new Intent(this, AlarmEditActivity.class);
            startActivityForResult(intent, REQUEST_ALARM_NEW);
        });

        if (routineId != null) loadAlarmItems();
    }

    private void loadAlarmItems() {
        RetrofitClient.getRoutineApi(this)
                .getItems(routineId)
                .enqueue(new Callback<ItemResponse>() {
                    @Override
                    public void onResponse(Call<ItemResponse> call,
                                           Response<ItemResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().data != null) {
                            llAlarmList.removeAllViews();
                            for (ItemResponse.ItemData item : response.body().data) {
                                String title = item.title;
                                String time = "";
                                String desc = title;
                                if (title != null && title.contains(" ")) {
                                    int idx = title.indexOf(" ");
                                    time = title.substring(0, idx);
                                    desc = title.substring(idx + 1);
                                }
                                addAlarmRow(item.id, time, desc);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ItemResponse> call, Throwable t) {
                        Toast.makeText(RoutineEditActivity.this,
                                "알람 불러오기 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addAlarmRow(String itemId, String time, String desc) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_alarm_edit, llAlarmList, false);

        TextView tvTime = row.findViewById(R.id.tv_alarm_time);
        TextView tvDesc = row.findViewById(R.id.tv_alarm_desc);
        tvTime.setText(time);
        tvDesc.setText(desc);
        row.setTag(itemId);

        row.findViewById(R.id.tv_alarm_edit).setOnClickListener(v -> {
            currentEditingRow = row;
            Intent intent = new Intent(this, AlarmEditActivity.class);
            intent.putExtra("time", tvTime.getText().toString());
            intent.putExtra("desc", tvDesc.getText().toString());
            startActivityForResult(intent, REQUEST_ALARM_EDIT);
        });

        row.findViewById(R.id.tv_alarm_delete).setOnClickListener(v ->
                llAlarmList.removeView(row));

        llAlarmList.addView(row);
    }

    private void updateRoutine() {
        List<Integer> selectedDays = new ArrayList<>();
        for (int i = 0; i < daySelected.length; i++) {
            if (daySelected[i]) selectedDays.add(i); // index = 서버값 그대로
        }

        Map<String, Object> body = new HashMap<>();
        body.put("routine_name", tvRoutineName.getText().toString());
        body.put("schedules", selectedDays);

        RetrofitClient.getRoutineApi(this)
                .updateRoutine(routineId, body)
                .enqueue(new Callback<BasicResponse>() {
                    @Override
                    public void onResponse(Call<BasicResponse> call,
                                           Response<BasicResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success) {
                            saveAlarmItems();
                        } else {
                            Toast.makeText(RoutineEditActivity.this,
                                    "수정 실패", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BasicResponse> call, Throwable t) {
                        Toast.makeText(RoutineEditActivity.this,
                                "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveAlarmItems() {
        int count = llAlarmList.getChildCount();
        if (count == 0) {
            Toast.makeText(this, "수정 완료!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
            return;
        }

        int[] savedCount = {0};

        for (int i = 0; i < count; i++) {
            View row = llAlarmList.getChildAt(i);
            String time = ((TextView) row.findViewById(R.id.tv_alarm_time)).getText().toString();
            String desc = ((TextView) row.findViewById(R.id.tv_alarm_desc)).getText().toString();
            String itemName = time + " " + desc;
            String itemId = (String) row.getTag();

            Map<String, String> itemBody = new HashMap<>();
            itemBody.put("item_name", itemName);

            Call<BasicResponse> call = (itemId != null && !itemId.isEmpty())
                    ? RetrofitClient.getRoutineApi(this).updateItem(routineId, itemId, itemBody)
                    : RetrofitClient.getRoutineApi(this).addItem(routineId, itemBody);

            call.enqueue(new Callback<BasicResponse>() {
                @Override
                public void onResponse(Call<BasicResponse> call,
                                       Response<BasicResponse> response) {
                    savedCount[0]++;
                    if (savedCount[0] == count) {
                        Toast.makeText(RoutineEditActivity.this,
                                "수정 완료!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<BasicResponse> call, Throwable t) {
                    savedCount[0]++;
                    if (savedCount[0] == count) {
                        Toast.makeText(RoutineEditActivity.this,
                                "수정 완료!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        String time = data.getStringExtra("time");
        String desc = data.getStringExtra("desc");

        if (requestCode == REQUEST_ALARM_NEW) {
            if (time != null && desc != null) {
                addAlarmRow(null, time, desc);
            }
        } else if (requestCode == REQUEST_ALARM_EDIT) {
            if (currentEditingRow != null && time != null && desc != null) {
                ((TextView) currentEditingRow.findViewById(R.id.tv_alarm_time)).setText(time);
                ((TextView) currentEditingRow.findViewById(R.id.tv_alarm_desc)).setText(desc);
                currentEditingRow = null;
            }
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