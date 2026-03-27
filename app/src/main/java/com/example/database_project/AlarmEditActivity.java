package com.example.database_project;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AlarmEditActivity extends AppCompatActivity {

    private int selectedHour = 6;
    private int selectedMinute = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_edit);

        TextView tvTime = findViewById(R.id.tv_time);
        EditText etDesc = findViewById(R.id.et_alarm_desc);
        ImageView btnBack = findViewById(R.id.btn_back);
        Button btnDone = findViewById(R.id.btn_done);

        // 이전 화면에서 넘어온 데이터
        String time = getIntent().getStringExtra("time");
        String desc = getIntent().getStringExtra("desc");
        if (time != null) tvTime.setText(time);
        if (desc != null) etDesc.setText(desc);

        // 뒤로가기
        btnBack.setOnClickListener(v -> finish());

        // 시간 클릭 → TimePicker (24시간제)
        tvTime.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        selectedHour = hourOfDay;
                        selectedMinute = minute;
                        tvTime.setText(String.format("%02d:%02d", selectedHour, selectedMinute));
                    },
                    selectedHour,
                    selectedMinute,
                    true // true = 24시간제
            );
            dialog.show();
        });

        // 완료 버튼
        btnDone.setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra("time", tvTime.getText().toString());
            result.putExtra("desc", etDesc.getText().toString());
            setResult(RESULT_OK, result);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}