package com.example.database_project;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiTodoActivity extends AppCompatActivity {

    // ── 메시지 타입 ────────────────────────────────────────────
    private static final int MSG_USER    = 0;
    private static final int MSG_LOADING = 1;
    private static final int MSG_RESULT  = 2;

    private static class TMsg {
        int           type;
        String        text;
        Uri           imageUri;
        List<String>  items;
        boolean       saved = false;

        TMsg(String text, Uri imageUri) {
            this.type = MSG_USER; this.text = text; this.imageUri = imageUri;
        }
        TMsg() { this.type = MSG_LOADING; }
        TMsg(List<String> items) { this.type = MSG_RESULT; this.items = items; }
    }

    // ── 필드 ───────────────────────────────────────────────────
    private RecyclerView   rvChat;
    private TAdapter       adapter;
    private List<TMsg>     messages = new ArrayList<>();

    private EditText       etInput;
    private LinearLayout   layoutImagePreview;
    private View           dividerPreview;
    private ImageView      ivThumbnail;

    private String selectedDate = "";

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
        setContentView(R.layout.activity_ai_todo);

        selectedDate = getIntent().getStringExtra("selected_date");
        if (selectedDate == null) selectedDate = "";

        rvChat             = findViewById(R.id.rv_chat);
        etInput            = findViewById(R.id.et_input);
        layoutImagePreview = findViewById(R.id.layout_image_preview);
        dividerPreview     = findViewById(R.id.divider_preview);
        ivThumbnail        = findViewById(R.id.iv_thumbnail);

        TextView tvSubtitle = findViewById(R.id.tv_date_subtitle);
        if (!selectedDate.isEmpty()) tvSubtitle.setText(selectedDate + " 할 일 목록");
        else tvSubtitle.setText("원하는 할 일을 입력하거나 사진을 첨부하세요");

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        rvChat.setLayoutManager(llm);
        adapter = new TAdapter();
        rvChat.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_plus).setOnClickListener(v -> openGallery());
        findViewById(R.id.btn_send).setOnClickListener(v -> sendMessage());
        findViewById(R.id.btn_remove_image).setOnClickListener(v -> clearPendingImage());

        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); return true; }
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
        pendingImageUri = null; pendingImageBase64 = null; pendingMimeType = null;
        layoutImagePreview.setVisibility(View.GONE);
        dividerPreview.setVisibility(View.GONE);
    }

    private Bitmap resizeBitmap(Bitmap src, int maxSize) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= maxSize && h <= maxSize) return src;
        float ratio = (float) maxSize / Math.max(w, h);
        return Bitmap.createScaledBitmap(src, (int)(w * ratio), (int)(h * ratio), true);
    }

    // ── 메시지 전송 ───────────────────────────────────────────

    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty() && pendingImageUri == null) {
            Toast.makeText(this, "내용을 입력하거나 사진을 첨부해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        messages.add(new TMsg(text, pendingImageUri));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();

        etInput.setText("");
        String sentBase64   = pendingImageBase64;
        String sentMimeType = pendingMimeType;
        clearPendingImage();

        int loadingIndex = messages.size();
        messages.add(new TMsg());
        adapter.notifyItemInserted(loadingIndex);
        scrollToBottom();

        Map<String, String> body = new HashMap<>();
        if (!text.isEmpty())    body.put("userMessage", text);
        if (sentBase64 != null) { body.put("imageBase64", sentBase64); body.put("mimeType", sentMimeType); }

        RetrofitClient.getRoutineApi(this)
                .createTodosFromAi(body)
                .enqueue(new Callback<AiTodoResponse>() {
                    @Override
                    public void onResponse(Call<AiTodoResponse> call, Response<AiTodoResponse> response) {
                        removeLoading(loadingIndex);
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success && response.body().data != null) {
                            List<String> titles = new ArrayList<>();
                            if (response.body().data.items != null) {
                                for (AiTodoResponse.ItemData item : response.body().data.items) {
                                    if (item.title != null) titles.add(item.title);
                                }
                            }
                            messages.add(new TMsg(titles));
                            adapter.notifyItemInserted(messages.size() - 1);
                            scrollToBottom();
                        } else {
                            Toast.makeText(AiTodoActivity.this,
                                    "리스트 생성 실패. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<AiTodoResponse> call, Throwable t) {
                        removeLoading(loadingIndex);
                        Toast.makeText(AiTodoActivity.this,
                                "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeLoading(int index) {
        if (index < messages.size() && messages.get(index).type == MSG_LOADING) {
            messages.remove(index);
            adapter.notifyItemRemoved(index);
        }
    }

    // ── 등록하기 → DB 저장 ────────────────────────────────────

    private void onRegister(TMsg msg, int position) {
        if (msg.saved || msg.items == null || msg.items.isEmpty()) return;

        // 날짜 형식 변환: 2026.05.19 → 2026-05-19
        String date = selectedDate.replace(".", "-");
        int total = msg.items.size();
        int[] done = {0};

        for (String title : msg.items) {
            Map<String, String> body = new HashMap<>();
            body.put("title", title);
            body.put("content", "");
            body.put("target_date", date);

            RetrofitClient.getRoutineApi(this)
                    .addTodo(body)
                    .enqueue(new Callback<BasicResponse>() {
                        @Override
                        public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                            done[0]++;
                            if (done[0] == total) markSaved(msg, position);
                        }

                        @Override
                        public void onFailure(Call<BasicResponse> call, Throwable t) {
                            done[0]++;
                            if (done[0] == total) markSaved(msg, position);
                        }
                    });
        }
    }

    private void markSaved(TMsg msg, int position) {
        runOnUiThread(() -> {
            msg.saved = true;
            adapter.notifyItemChanged(position);
            setResult(RESULT_OK);
            Toast.makeText(this, "리스트가 등록되었습니다.", Toast.LENGTH_SHORT).show();
        });
    }

    private void scrollToBottom() { rvChat.scrollToPosition(messages.size() - 1); }

    // ── 어댑터 (내부 클래스) ──────────────────────────────────

    private class TAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override public int getItemViewType(int position) { return messages.get(position).type; }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(AiTodoActivity.this);
            switch (viewType) {
                case MSG_USER:
                    return new UserVH(inf.inflate(R.layout.item_chat_user, parent, false));
                case MSG_LOADING:
                    return new LoadVH(inf.inflate(R.layout.item_chat_loading, parent, false));
                default:
                    return new ResultVH(inf.inflate(R.layout.item_chat_ai_todo_result, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TMsg msg = messages.get(position);

            if (holder instanceof UserVH) {
                UserVH vh = (UserVH) holder;
                if (msg.text != null && !msg.text.isEmpty()) {
                    vh.tvMessage.setText(msg.text);
                    vh.tvMessage.setVisibility(View.VISIBLE);
                } else {
                    vh.tvMessage.setVisibility(View.GONE);
                }
                if (msg.imageUri != null) {
                    vh.ivImage.setImageURI(msg.imageUri);
                    vh.cardImage.setVisibility(View.VISIBLE);
                } else {
                    vh.cardImage.setVisibility(View.GONE);
                }

            } else if (holder instanceof ResultVH) {
                ResultVH vh = (ResultVH) holder;
                vh.layoutItems.removeAllViews();

                if (msg.items != null) {
                    for (String title : msg.items) {
                        TextView tv = new TextView(AiTodoActivity.this);
                        tv.setText("• " + title);
                        tv.setTextSize(13f);
                        tv.setTextColor(0xFF2C2C2A);
                        tv.setLineSpacing(0, 1.3f);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp.bottomMargin = dp(6);
                        tv.setLayoutParams(lp);
                        vh.layoutItems.addView(tv);
                    }
                }

                if (msg.saved) {
                    vh.btnRegister.setText("등록 완료 ✓");
                    vh.btnRegister.setEnabled(false);
                    vh.btnRegister.setAlpha(0.5f);
                } else {
                    vh.btnRegister.setText("리스트 등록하기");
                    vh.btnRegister.setEnabled(true);
                    vh.btnRegister.setAlpha(1.0f);
                    vh.btnRegister.setOnClickListener(v ->
                            onRegister(msg, holder.getAdapterPosition()));
                }
            }
        }

        @Override public int getItemCount() { return messages.size(); }
    }

    // ── ViewHolders ───────────────────────────────────────────

    static class UserVH extends RecyclerView.ViewHolder {
        TextView tvMessage; androidx.cardview.widget.CardView cardImage; ImageView ivImage;
        UserVH(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tv_message);
            cardImage = v.findViewById(R.id.card_image);
            ivImage   = v.findViewById(R.id.iv_image);
        }
    }

    static class LoadVH extends RecyclerView.ViewHolder {
        LoadVH(View v) { super(v); }
    }

    static class ResultVH extends RecyclerView.ViewHolder {
        LinearLayout layoutItems; Button btnRegister;
        ResultVH(View v) {
            super(v);
            layoutItems = v.findViewById(R.id.layout_todo_items);
            btnRegister = v.findViewById(R.id.btn_register);
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
