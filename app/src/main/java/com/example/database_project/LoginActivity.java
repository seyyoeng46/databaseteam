package com.example.database_project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        LinearLayout btnGoogleLogin = findViewById(R.id.btn_google_login);

        btnGoogleLogin.setOnClickListener(v -> {
            // TODO: 구글 로그인 구현 (Firebase Auth 등 연동 예정)
            // 임시: 버튼 누르면 바로 MainActivity 로 이동
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // 뒤로가기 시 로그인 화면으로 돌아오지 않도록
        });
    }
}