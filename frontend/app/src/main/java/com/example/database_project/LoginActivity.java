package com.example.database_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "LoginActivity";
    private static final String WEB_CLIENT_ID = "409230075283-rjd9h77off3p09slc00a9tq7ukj7c3ko.apps.googleusercontent.com";
    private static final String SERVER_URL =
            "http://10.0.2.2:3000/api/auth/google";
//    private static final String SERVER_URL =
//        "https://planner-backend-hgty.onrender.com/api/auth/google";
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 이미 로그인된 경우 바로 메인으로
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String savedToken = prefs.getString("jwt_token", null);
        if (savedToken != null) {
            goToMain();
            return;
        }

        // 구글 로그인 설정
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(WEB_CLIENT_ID)
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        LinearLayout btnGoogleLogin = findViewById(R.id.btn_google_login);
        btnGoogleLogin.setOnClickListener(v -> signIn());
    }

    private void signIn() {
        // 기존 캐시 강제 초기화 후 로그인
        googleSignInClient.signOut().addOnCompleteListener(requireActivity -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                String idToken = account.getIdToken();
                Log.d(TAG, "idToken: " + idToken);
                sendTokenToServer(idToken);
            } catch (ApiException e) {
                Log.e(TAG, "구글 로그인 실패: " + e.getStatusCode());
                Toast.makeText(this, "구글 로그인 실패: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendTokenToServer(String idToken) {
        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject json = new JSONObject();
            json.put("idToken", idToken);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(SERVER_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this,
                                    "서버 연결 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        boolean success = jsonResponse.getBoolean("success");

                        if (success) {
                            JSONObject data = jsonResponse.getJSONObject("data");
                            String token = data.getString("token");
                            JSONObject user = data.getJSONObject("user");
                            String userId = user.getString("id");
                            String username = user.getString("username");

                            // SharedPreferences에 저장
                            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                            prefs.edit()
                                    .putString("jwt_token", token)
                                    .putString("user_id", userId)
                                    .putString("username", username)
                                    .apply();

                            RetrofitClient.reset();
                            runOnUiThread(() -> goToMain());
                        } else {
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this,
                                            "로그인 실패", Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this,
                                        "응답 파싱 실패", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "요청 생성 실패", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}