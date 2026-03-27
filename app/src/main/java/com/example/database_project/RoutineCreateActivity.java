package com.example.database_project;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class RoutineCreateActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_create);

        // 뒤로가기 버튼
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView rvChat = findViewById(R.id.rv_chat);
        rvChat.setLayoutManager(new LinearLayoutManager(this));

        EditText etInput = findViewById(R.id.et_input);

        // 갤러리에서 사진 선택 결과 처리
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        // TODO: 선택된 이미지 채팅창에 표시
                        Toast.makeText(this, "사진이 선택됐습니다", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // + 버튼 클릭 시 갤러리 열기
        TextView btnPlus = findViewById(R.id.btn_plus);
        btnPlus.setOnClickListener(v -> openGallery());

        etInput.setOnEditorActionListener((v, actionId, event) -> {
            String input = etInput.getText().toString().trim();
            if (!input.isEmpty()) {
                // TODO: AI에게 루틴 생성 요청
                etInput.setText("");
            }
            return true;
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}