package com.example.database_project;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoutineCreateActivity extends AppCompatActivity
        implements ChatAdapter.OnRegisterListener {

    private RecyclerView      rvChat;
    private ChatAdapter       adapter;
    private List<ChatMessage> messages = new ArrayList<>();

    private EditText     etInput;
    private LinearLayout layoutImagePreview;
    private View         dividerPreview;
    private ImageView    ivThumbnail;

    private Uri    pendingImageUri    = null;
    private String pendingImageBase64 = null;
    private String pendingMimeType    = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null
                                && result.getData().getData() != null) {
                            handleImageSelected(result.getData().getData());
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_create);

        rvChat             = findViewById(R.id.rv_chat);
        etInput            = findViewById(R.id.et_input);
        layoutImagePreview = findViewById(R.id.layout_image_preview);
        dividerPreview     = findViewById(R.id.divider_preview);
        ivThumbnail        = findViewById(R.id.iv_thumbnail);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        rvChat.setLayoutManager(llm);
        adapter = new ChatAdapter(this, messages, this);
        rvChat.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_plus).setOnClickListener(v -> openGallery());
        findViewById(R.id.btn_send).setOnClickListener(v -> sendMessage());
        findViewById(R.id.btn_remove_image).setOnClickListener(v -> clearPendingImage());

        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    // ── 갤러리 ────────────────────────────────────────────────

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void handleImageSelected(Uri uri) {
        try {
            ivThumbnail.setImageURI(uri);
            layoutImagePreview.setVisibility(View.VISIBLE);
            dividerPreview.setVisibility(View.VISIBLE);

            InputStream is      = getContentResolver().openInputStream(uri);
            Bitmap      original = BitmapFactory.decodeStream(is);
            Bitmap      resized  = resizeBitmap(original, 1024);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            pendingImageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            pendingMimeType    = "image/jpeg";
            pendingImageUri    = uri;
        } catch (Exception e) {
            Toast.makeText(this, "이미지를 불러올 수 없어요", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearPendingImage() {
        pendingImageUri    = null;
        pendingImageBase64 = null;
        pendingMimeType    = null;
        layoutImagePreview.setVisibility(View.GONE);
        dividerPreview.setVisibility(View.GONE);
    }

    private Bitmap resizeBitmap(Bitmap src, int maxSize) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= maxSize && h <= maxSize) return src;
        float ratio = (float) maxSize / Math.max(w, h);
        return Bitmap.createScaledBitmap(src, (int)(w * ratio), (int)(h * ratio), true);
    }

    // ── 메시지 전송 (AI 미리보기 요청) ───────────────────────

    private void sendMessage() {
        String text = etInput.getText().toString().trim();

        if (text.isEmpty() && pendingImageUri == null) {
            Toast.makeText(this, "내용을 입력하거나 사진을 첨부해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 사용자 메시지 추가
        messages.add(new ChatMessage(text, pendingImageUri));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();

        etInput.setText("");
        String sentBase64   = pendingImageBase64;
        String sentMimeType = pendingMimeType;
        clearPendingImage();

        // 로딩 추가
        int loadingIndex = messages.size();
        messages.add(new ChatMessage());
        adapter.notifyItemInserted(loadingIndex);
        scrollToBottom();

        // AI 미리보기 요청
        Map<String, String> body = new HashMap<>();
        if (!text.isEmpty())    body.put("userMessage", text);
        if (sentBase64 != null) {
            body.put("imageBase64", sentBase64);
            body.put("mimeType",    sentMimeType);
        }

        RetrofitClient.getRoutineApi(this)
                .createRoutineFromImage(body)
                .enqueue(new Callback<AiRoutineResponse>() {
                    @Override
                    public void onResponse(Call<AiRoutineResponse> call,
                                           Response<AiRoutineResponse> response) {
                        removeLoading(loadingIndex);

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().success
                                && response.body().data != null) {

                            AiRoutineResponse.PreviewData data = response.body().data;
                            List<String> itemContents = new ArrayList<>();
                            if (data.items != null) {
                                for (AiRoutineResponse.ItemData item : data.items) {
                                    if (item.content != null) itemContents.add(item.content);
                                }
                            }
                            messages.add(new ChatMessage(data.routineTitle, itemContents));
                            adapter.notifyItemInserted(messages.size() - 1);
                            scrollToBottom();
                        } else {
                            Toast.makeText(RoutineCreateActivity.this,
                                    "루틴 생성 실패. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<AiRoutineResponse> call, Throwable t) {
                        removeLoading(loadingIndex);
                        Toast.makeText(RoutineCreateActivity.this,
                                "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeLoading(int index) {
        if (index < messages.size()
                && messages.get(index).type == ChatMessage.TYPE_LOADING) {
            messages.remove(index);
            adapter.notifyItemRemoved(index);
        }
    }

    // ── 등록하기 버튼 클릭 → DB 저장 ─────────────────────────

    @Override
    public void onRegister(ChatMessage msg, int position) {
        if (msg.registered) return;

        // 1. 루틴 생성
        Map<String, Object> routineBody = new HashMap<>();
        routineBody.put("routine_name", msg.routineTitle);
        routineBody.put("description",  "");
        routineBody.put("schedules",    Arrays.asList(0,1,2,3,4,5,6)); // 기본: 매일

        RetrofitClient.getRoutineApi(this)
                .createRoutine(routineBody)
                .enqueue(new Callback<BasicResponse>() {
                    @Override
                    public void onResponse(Call<BasicResponse> call,
                                           Response<BasicResponse> response) {
                        if (!response.isSuccessful()
                                || response.body() == null
                                || !response.body().success) {
                            Toast.makeText(RoutineCreateActivity.this,
                                    "등록 실패", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String routineId = response.body().routineId;
                        saveItems(routineId, msg, position);
                    }

                    @Override
                    public void onFailure(Call<BasicResponse> call, Throwable t) {
                        Toast.makeText(RoutineCreateActivity.this,
                                "등록 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveItems(String routineId, ChatMessage msg, int position) {
        if (msg.routineItems == null || msg.routineItems.isEmpty()) {
            markRegistered(msg, position);
            return;
        }

        int total = msg.routineItems.size();
        int[] done = {0};

        for (String content : msg.routineItems) {
            Map<String, String> itemBody = new HashMap<>();
            itemBody.put("item_name", content); // "HH:MM 행동설명" 그대로 저장

            RetrofitClient.getRoutineApi(this)
                    .addItem(routineId, itemBody)
                    .enqueue(new Callback<BasicResponse>() {
                        @Override
                        public void onResponse(Call<BasicResponse> call,
                                               Response<BasicResponse> response) {
                            done[0]++;
                            if (done[0] == total) markRegistered(msg, position);
                        }

                        @Override
                        public void onFailure(Call<BasicResponse> call, Throwable t) {
                            done[0]++;
                            if (done[0] == total) markRegistered(msg, position);
                        }
                    });
        }
    }

    private void markRegistered(ChatMessage msg, int position) {
        runOnUiThread(() -> {
            msg.registered = true;
            adapter.notifyItemChanged(position);
            setResult(RESULT_OK); // 루틴 목록 새로고침 신호
            Toast.makeText(this, "루틴이 등록되었습니다.", Toast.LENGTH_SHORT).show();
        });
    }

    private void scrollToBottom() {
        rvChat.scrollToPosition(messages.size() - 1);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
