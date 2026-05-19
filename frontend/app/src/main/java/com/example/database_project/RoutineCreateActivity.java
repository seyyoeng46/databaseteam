package com.example.database_project;

import android.app.DatePickerDialog;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoutineCreateActivity extends AppCompatActivity
        implements ChatAdapter.OnRegisterListener {

    private static final int MODE_ROUTINE = 0;
    private static final int MODE_LIST    = 1;

    private int    currentMode = MODE_ROUTINE;
    private String todoDate    = "";   // 리스트 모드에서 사용할 날짜 (YYYY.MM.DD)

    private RecyclerView      rvChat;
    private ChatAdapter       adapter;
    private List<ChatMessage> messages = new ArrayList<>();

    private EditText     etInput;
    private LinearLayout layoutImagePreview;
    private View         dividerPreview;
    private ImageView    ivThumbnail;

    private TextView     tvSubtitle;
    private TextView     chipRoutine;
    private TextView     chipList;
    private LinearLayout layoutTodoDate;
    private TextView     tvTodoDate;

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
        tvSubtitle         = findViewById(R.id.tv_subtitle);
        chipRoutine        = findViewById(R.id.chip_routine);
        chipList           = findViewById(R.id.chip_list);
        layoutTodoDate     = findViewById(R.id.layout_todo_date);
        tvTodoDate         = findViewById(R.id.tv_todo_date);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        rvChat.setLayoutManager(llm);
        adapter = new ChatAdapter(this, messages, this);
        rvChat.setAdapter(adapter);

        // 오늘 날짜 기본값
        todoDate = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(new Date());
        tvTodoDate.setText(todoDate);

        // 모드 토글
        chipRoutine.setOnClickListener(v -> setMode(MODE_ROUTINE));
        chipList.setOnClickListener(v -> setMode(MODE_LIST));

        // 날짜 선택
        layoutTodoDate.setOnClickListener(v -> showDatePicker());

        // 기타 버튼
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_plus).setOnClickListener(v -> openGallery());
        findViewById(R.id.btn_send).setOnClickListener(v -> sendMessage());
        findViewById(R.id.btn_remove_image).setOnClickListener(v -> clearPendingImage());

        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); return true; }
            return false;
        });

        setMode(MODE_ROUTINE);
    }

    // ── 모드 전환 ──────────────────────────────────────────────

    private void setMode(int mode) {
        currentMode = mode;
        if (mode == MODE_ROUTINE) {
            chipRoutine.setBackgroundResource(R.drawable.bg_chip_selected);
            chipRoutine.setTextColor(0xFFFFFFFF);
            chipList.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipList.setTextColor(0xFF888780);
            layoutTodoDate.setVisibility(View.GONE);
            tvSubtitle.setText("원하는 루틴을 입력하거나 사진을 첨부하세요");
            etInput.setHint("원하는 루틴을 입력하세요");
        } else {
            chipList.setBackgroundResource(R.drawable.bg_chip_selected);
            chipList.setTextColor(0xFFFFFFFF);
            chipRoutine.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipRoutine.setTextColor(0xFF888780);
            layoutTodoDate.setVisibility(View.VISIBLE);
            tvSubtitle.setText("원하는 할 일을 입력하거나 사진을 첨부하세요");
            etInput.setHint("오늘 해야 할 일을 입력하세요");
        }
    }

    private void showDatePicker() {
        String[] parts = todoDate.split("\\.");
        int y = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]) - 1;
        int d = Integer.parseInt(parts[2]);
        new DatePickerDialog(this, (view, year, month, day) -> {
            todoDate = String.format(Locale.getDefault(), "%04d.%02d.%02d", year, month + 1, day);
            tvTodoDate.setText(todoDate);
        }, y, m, d).show();
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

            InputStream is       = getContentResolver().openInputStream(uri);
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

        messages.add(new ChatMessage(text, pendingImageUri));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();

        etInput.setText("");
        String sentBase64   = pendingImageBase64;
        String sentMimeType = pendingMimeType;
        clearPendingImage();

        int loadingIndex = messages.size();
        messages.add(new ChatMessage());
        adapter.notifyItemInserted(loadingIndex);
        scrollToBottom();

        Map<String, String> body = new HashMap<>();
        if (!text.isEmpty())    body.put("userMessage", text);
        if (sentBase64 != null) { body.put("imageBase64", sentBase64); body.put("mimeType", sentMimeType); }

        if (currentMode == MODE_ROUTINE) {
            sendRoutineRequest(body, loadingIndex);
        } else {
            sendTodoRequest(body, loadingIndex);
        }
    }

    private void sendRoutineRequest(Map<String, String> body, int loadingIndex) {
        RetrofitClient.getRoutineApi(this)
                .createRoutineFromImage(body)
                .enqueue(new Callback<AiRoutineResponse>() {
                    @Override
                    public void onResponse(Call<AiRoutineResponse> call, Response<AiRoutineResponse> response) {
                        removeLoading(loadingIndex);
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success && response.body().data != null) {
                            AiRoutineResponse.PreviewData data = response.body().data;
                            List<String> itemContents = new ArrayList<>();
                            if (data.items != null)
                                for (AiRoutineResponse.ItemData item : data.items)
                                    if (item.content != null) itemContents.add(item.content);
                            List<Integer> schedules = (data.schedules != null && !data.schedules.isEmpty())
                                    ? data.schedules : Arrays.asList(0, 1, 2, 3, 4, 5, 6);
                            messages.add(new ChatMessage(data.routineTitle, itemContents, schedules));
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

    private void sendTodoRequest(Map<String, String> body, int loadingIndex) {
        final String date = todoDate; // 전송 시점의 날짜 캡처
        RetrofitClient.getRoutineApi(this)
                .createTodosFromAi(body)
                .enqueue(new Callback<AiTodoResponse>() {
                    @Override
                    public void onResponse(Call<AiTodoResponse> call, Response<AiTodoResponse> response) {
                        removeLoading(loadingIndex);
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success && response.body().data != null) {
                            List<String> titles = new ArrayList<>();
                            if (response.body().data.items != null)
                                for (AiTodoResponse.ItemData item : response.body().data.items)
                                    if (item.title != null) titles.add(item.title);
                            messages.add(new ChatMessage(titles, date));
                            adapter.notifyItemInserted(messages.size() - 1);
                            scrollToBottom();
                        } else {
                            Toast.makeText(RoutineCreateActivity.this,
                                    "리스트 생성 실패. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<AiTodoResponse> call, Throwable t) {
                        removeLoading(loadingIndex);
                        Toast.makeText(RoutineCreateActivity.this,
                                "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeLoading(int index) {
        if (index < messages.size() && messages.get(index).type == ChatMessage.TYPE_LOADING) {
            messages.remove(index);
            adapter.notifyItemRemoved(index);
        }
    }

    // ── 등록하기 버튼 클릭 ────────────────────────────────────

    @Override
    public void onRegister(ChatMessage msg, int position) {
        if (msg.registered) return;
        if (msg.type == ChatMessage.TYPE_TODO_RESULT) {
            saveTodos(msg, position);
        } else {
            saveRoutine(msg, position);
        }
    }

    // 루틴 저장
    private void saveRoutine(ChatMessage msg, int position) {
        Map<String, Object> routineBody = new HashMap<>();
        routineBody.put("routine_name", msg.routineTitle);
        routineBody.put("description",  "");
        routineBody.put("schedules",    msg.schedules != null ? msg.schedules : Arrays.asList(0,1,2,3,4,5,6));

        RetrofitClient.getRoutineApi(this)
                .createRoutine(routineBody)
                .enqueue(new Callback<BasicResponse>() {
                    @Override
                    public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                        if (!response.isSuccessful() || response.body() == null || !response.body().success) {
                            Toast.makeText(RoutineCreateActivity.this, "등록 실패", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        saveItems(response.body().routineId, msg, position);
                    }
                    @Override
                    public void onFailure(Call<BasicResponse> call, Throwable t) {
                        Toast.makeText(RoutineCreateActivity.this, "등록 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
            itemBody.put("item_name", content);
            RetrofitClient.getRoutineApi(this)
                    .addItem(routineId, itemBody)
                    .enqueue(new Callback<BasicResponse>() {
                        @Override public void onResponse(Call<BasicResponse> c, Response<BasicResponse> r) {
                            if (++done[0] == total) markRegistered(msg, position);
                        }
                        @Override public void onFailure(Call<BasicResponse> c, Throwable t) {
                            if (++done[0] == total) markRegistered(msg, position);
                        }
                    });
        }
    }

    // 투두 저장
    private void saveTodos(ChatMessage msg, int position) {
        if (msg.routineItems == null || msg.routineItems.isEmpty()) {
            markRegistered(msg, position);
            return;
        }
        // "YYYY.MM.DD" → "YYYY-MM-DD"
        String date = msg.todoDate != null ? msg.todoDate.replace(".", "-") : "";
        int total = msg.routineItems.size();
        int[] done = {0};
        for (String title : msg.routineItems) {
            Map<String, String> body = new HashMap<>();
            body.put("title", title);
            body.put("content", "");
            body.put("target_date", date);
            RetrofitClient.getRoutineApi(this)
                    .addTodo(body)
                    .enqueue(new Callback<BasicResponse>() {
                        @Override public void onResponse(Call<BasicResponse> c, Response<BasicResponse> r) {
                            if (++done[0] == total) markRegistered(msg, position);
                        }
                        @Override public void onFailure(Call<BasicResponse> c, Throwable t) {
                            if (++done[0] == total) markRegistered(msg, position);
                        }
                    });
        }
    }

    private void markRegistered(ChatMessage msg, int position) {
        runOnUiThread(() -> {
            msg.registered = true;
            adapter.notifyItemChanged(position);
            setResult(RESULT_OK);
            String label = msg.type == ChatMessage.TYPE_TODO_RESULT ? "리스트가" : "루틴이";
            Toast.makeText(this, label + " 등록되었습니다.", Toast.LENGTH_SHORT).show();
        });
    }

    private void scrollToBottom() { rvChat.scrollToPosition(messages.size() - 1); }

    @Override
    public void onBackPressed() { super.onBackPressed(); finish(); }
}
