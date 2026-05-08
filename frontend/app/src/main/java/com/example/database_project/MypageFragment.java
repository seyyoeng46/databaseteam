package com.example.database_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

public class MypageFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mypage, container, false);

        TextView tvName    = view.findViewById(R.id.tv_name);
        TextView tvEmail   = view.findViewById(R.id.tv_email);
        TextView btnLogout = view.findViewById(R.id.btn_logout);

        // SharedPreferences에서 유저 정보 불러오기
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user_prefs", requireActivity().MODE_PRIVATE);
        String username = prefs.getString("username", "이름");
        String email    = prefs.getString("email", "이메일");

        tvName.setText(username);
        tvEmail.setText(email);

        // 로그아웃
        btnLogout.setOnClickListener(v -> {
            // JWT 토큰 및 유저 정보 삭제
            prefs.edit().clear().apply();

            // Retrofit 초기화
            RetrofitClient.reset();

            // 구글 로그아웃
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken("553427997682-p9dssaiv0ml6c46uibp3adkc3edn0mp8.apps.googleusercontent.com")
                    .requestEmail()
                    .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                // 로그인 화면으로 이동
                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        });

        return view;
    }
}