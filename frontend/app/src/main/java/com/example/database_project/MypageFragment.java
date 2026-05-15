package com.example.database_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

public class MypageFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mypage, container, false);

        ImageView ivProfile = view.findViewById(R.id.iv_profile);
        TextView tvName     = view.findViewById(R.id.tv_name);
        TextView tvEmail    = view.findViewById(R.id.tv_email);
        TextView btnLogout  = view.findViewById(R.id.btn_logout);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user_prefs", requireActivity().MODE_PRIVATE);
        String username = prefs.getString("username", "이름");
        String email    = prefs.getString("email", "");
        String photoUrl = prefs.getString("photo_url", "");

        tvName.setText(username);
        tvEmail.setText(email.isEmpty() ? "-" : email);

        if (!photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(ivProfile);
        }

        btnLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            RetrofitClient.reset();

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken("553427997682-p9dssaiv0ml6c46uibp3adkc3edn0mp8.apps.googleusercontent.com")
                    .requestEmail()
                    .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        });

        return view;
    }
}
