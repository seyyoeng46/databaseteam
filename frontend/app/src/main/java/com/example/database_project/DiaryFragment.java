package com.example.database_project;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast; // 추가

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List; // 추가
import java.util.Locale;

import retrofit2.Call; // 추가
import retrofit2.Callback; // 추가
import retrofit2.Response; // 추가

public class DiaryFragment extends Fragment {

    private EditText etSearchDiary;
    private ImageView btnAddDiary;
    private ImageView btnCalendarSearch;
    private RecyclerView recyclerDiary;

    private DiaryAdapter adapter;
    private ArrayList<DiaryItem> fullList = new ArrayList<>();
    private ArrayList<DiaryItem> filteredList = new ArrayList<>();

    private String selectedFilterDate = "";

    // 추가: apiService 선언
    private ApiService apiService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 추가: apiService 초기화
        apiService = ApiClient.getClient(requireContext()).create(ApiService.class);

        etSearchDiary = view.findViewById(R.id.et_search_diary);
        btnAddDiary = view.findViewById(R.id.btn_add_diary);
        btnCalendarSearch = view.findViewById(R.id.btn_calendar_search);
        recyclerDiary = view.findViewById(R.id.recycler_diary);

        adapter = new DiaryAdapter(requireContext(), filteredList, this::loadDiaryList);
        recyclerDiary.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerDiary.setAdapter(adapter);

        loadDiaryList();

        btnAddDiary.setOnClickListener(v -> showCreateDatePicker());
        btnCalendarSearch.setOnClickListener(v -> showSearchDatePicker());

        etSearchDiary.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadDiaryList(); // 검색어 입력 시 서버에서 다시 검색
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDiaryList();
    }

    // DiaryFragment.java의 loadDiaryList와 추가 메서드들

    // DiaryFragment.java 내의 loadDiaryList 메서드 수정

    private void loadDiaryList() {
        String keyword = etSearchDiary.getText().toString().trim();
        String finalKeyword = keyword.isEmpty() ? "%" : keyword; // 전체 조회를 위해 % 사용

        apiService.searchDiaries(finalKeyword).enqueue(new Callback<DiaryListResponse>() {
            @Override
            public void onResponse(Call<DiaryListResponse> call, Response<DiaryListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullList.clear();
                    List<DiaryListResponse.DiaryData> diaries = response.body().data;

                    if (diaries != null) {
                        for (DiaryListResponse.DiaryData data : diaries) {
                            // 제목|내용 분리
                            String raw = data.content;
                            String title = "제목 없음";
                            String content = (raw != null) ? raw : "";
                            if (raw != null && raw.contains("|")) {
                                String[] parts = raw.split("\\|", 2);
                                title = parts[0];
                                content = parts[1];
                            }

                            DiaryItem item = new DiaryItem(
                                    data.id,
                                    formatServerDate(data.targetDate),
                                    title,
                                    content,
                                    new ArrayList<>()
                            );
                            fullList.add(item);

                            // 여기서 태그를 가져옵니다.
                            fetchTagsFromServer(item);
                        }
                    }
                    updateUI();
                }
            }
            @Override
            public void onFailure(Call<DiaryListResponse> call, Throwable t) {
                Log.e("DiaryFragment", "로드 실패");
            }
        });
    }

    // DiaryFragment.java 수정
    private void fetchTagsFromServer(DiaryItem item) {
        String dateQuery = item.getDate().replace(".", "-");

        apiService.getTodosByDate(dateQuery).enqueue(new Callback<TodoResponse>() {
            @Override
            public void onResponse(Call<TodoResponse> call, Response<TodoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ArrayList<String> tags = new ArrayList<>();
                    List<TodoResponse.TodoData> todos = response.body().data;
                    if (todos != null) {
                        for (TodoResponse.TodoData todo : todos) {
                            // [수정] 완료 안 된 투두도 카드에 뜨는지 확인하기 위해 if문 제거
                            tags.add(todo.title);
                            Log.d("TAG_TEST", "찾은 투두: " + todo.title + " (날짜: " + dateQuery + ")");
                        }
                    }
                    item.setTags(tags);
                    adapter.notifyDataSetChanged(); // 이 명령어가 있어야 화면에 태그가 "짠" 하고 나타납니다.
                }
            }
            @Override
            public void onFailure(Call<TodoResponse> call, Throwable t) { }
        });
    }

    private void updateUI() {
        filteredList.clear();
        for (DiaryItem item : fullList) {
            boolean matchDate = selectedFilterDate.isEmpty() || selectedFilterDate.equals(item.getDate());
            if (matchDate) {
                filteredList.add(item);
            }
        }
        adapter.setDiaryList(filteredList);
    }

    private String formatServerDate(String serverDate) {
        if (serverDate == null || serverDate.isEmpty()) return "";

        try {
            // 서버에서 오는 UTC 형식을 파싱 (소수점 초 단위(.000Z)까지 포함될 수 있으므로 유연하게 처리)
            // 백엔드에서 주는 ISO 8601 형식 대응
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            if (serverDate.contains("T")) {
                sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                sdfInput.setTimeZone(TimeZone.getTimeZone("UTC"));
            }

            Date date = sdfInput.parse(serverDate);

            // 사용자의 폰 시간대(KST)로 변환하여 출력
            SimpleDateFormat sdfOutput = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
            sdfOutput.setTimeZone(TimeZone.getDefault());

            return sdfOutput.format(date);
        } catch (Exception e) {
            // 실패 시 문자열만이라도 변환
            return serverDate.split("T")[0].replace("-", ".");
        }
    }

    private void showCreateDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%04d.%02d.%02d", year, month + 1, dayOfMonth);
                    Intent intent = new Intent(requireContext(), DiaryWriteActivity.class);
                    intent.putExtra("mode", "create");
                    intent.putExtra("selected_date", selectedDate);
                    startActivity(intent);
                },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void showSearchDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedFilterDate = String.format(Locale.getDefault(), "%04d.%02d.%02d", year, month + 1, dayOfMonth);
                    updateUI();
                },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, "초기화", (d, which) -> {
            selectedFilterDate = "";
            updateUI();
        });
        dialog.show();
    }
}