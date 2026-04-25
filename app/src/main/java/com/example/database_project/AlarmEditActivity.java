package com.example.database_project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import androidx.appcompat.app.AppCompatActivity;

public class AlarmEditActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_edit);

        NumberPicker pickerHour = findViewById(R.id.picker_hour);
        NumberPicker pickerMinute = findViewById(R.id.picker_minute);
        EditText etDesc = findViewById(R.id.et_alarm_desc);
        ImageView btnBack = findViewById(R.id.btn_back);
        Button btnDone = findViewById(R.id.btn_done);

        // 시: 0~23
        pickerHour.setMinValue(0);
        pickerHour.setMaxValue(23);
        pickerHour.setFormatter(value -> String.format("%02d", value));

        // 분: 0~59
        pickerMinute.setMinValue(0);
        pickerMinute.setMaxValue(59);
        pickerMinute.setFormatter(value -> String.format("%02d", value));

        // 이전 화면에서 넘어온 데이터 세팅
        String time = getIntent().getStringExtra("time");
        String desc = getIntent().getStringExtra("desc");

        if (time != null && time.contains(":")) {
            String[] parts = time.split(":");
            try {
                pickerHour.setValue(Integer.parseInt(parts[0]));
                pickerMinute.setValue(Integer.parseInt(parts[1]));
            } catch (NumberFormatException e) {
                pickerHour.setValue(6);
                pickerMinute.setValue(0);
            }
        } else {
            pickerHour.setValue(6);
            pickerMinute.setValue(0);
        }

        if (desc != null) etDesc.setText(desc);

        // 뒤로가기
        btnBack.setOnClickListener(v -> finish());

        // 완료 버튼
        btnDone.setOnClickListener(v -> {
            String selectedTime = String.format("%02d:%02d",
                    pickerHour.getValue(), pickerMinute.getValue());
            Intent result = new Intent();
            result.putExtra("time", selectedTime);
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