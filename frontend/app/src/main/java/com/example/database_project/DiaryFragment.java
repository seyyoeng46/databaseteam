package com.example.database_project;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiaryFragment extends Fragment {

    private EditText etSearchDiary;
    private ImageView btnAddDiary;
    private ImageView btnCalendarSearch;
    private RecyclerView recyclerDiary;

    private DiaryAdapter adapter;
    private final ArrayList<DiaryItem> fullList = new ArrayList<>();
    private final ArrayList<DiaryItem> filteredList = new ArrayList<>();

    private String selectedFilterDate = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearchDiary      = view.findViewById(R.id.et_search_diary);
        btnAddDiary        = view.findViewById(R.id.btn_add_diary);
        btnCalendarSearch  = view.findViewById(R.id.btn_calendar_search);
        recyclerDiary      = view.findViewById(R.id.recycler_diary);

        adapter = new DiaryAdapter(requireContext(), filteredList, this::loadDiaryList);
        recyclerDiary.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerDiary.setAdapter(adapter);

        loadDiaryList();

        btnAddDiary.setOnClickListener(v -> showCreateDatePicker());
        btnCalendarSearch.setOnClickListener(v -> showSearchDatePicker());

        etSearchDiary.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterDiary(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDiaryList();
    }

    private void loadDiaryList() {
        RetrofitClient.getRoutineApi(requireContext())
                .getAllDiaries()
                .enqueue(new Callback<DiaryListResponse>() {
                    @Override
                    public void onResponse(Call<DiaryListResponse> call,
                                           Response<DiaryListResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success) {
                            fullList.clear();
                            List<DiaryListResponse.DiaryData> dataList = response.body().data;
                            if (dataList != null) {
                                for (DiaryListResponse.DiaryData data : dataList) {
                                    String raw = data.target_date != null && data.target_date.contains("T")
                                            ? data.target_date.substring(0, 10)
                                            : data.target_date;
                                    String displayDate = raw != null ? raw.replace("-", ".") : "";

                                    ArrayList<String> routineTags = data.routineTags != null
                                            ? new ArrayList<>(data.routineTags) : new ArrayList<>();
                                    ArrayList<String> listTags = data.listTags != null
                                            ? new ArrayList<>(data.listTags) : new ArrayList<>();

                                    fullList.add(new DiaryItem(
                                            data.id,
                                            displayDate,
                                            data.title != null ? data.title : "",
                                            data.content != null ? data.content : "",
                                            routineTags,
                                            listTags));
                                }
                            }
                            filterDiary();
                        } else {
                            // 응답 파싱 실패 또는 서버 오류 시 원인 표시
                            String detail = "body=" + (response.body() == null ? "null" : "success=" + response.body().success);
                            try {
                                if (!response.isSuccessful() && response.errorBody() != null)
                                    detail = "[" + response.code() + "] " + response.errorBody().string();
                            } catch (Exception ignored) {}
                            if (getContext() != null)
                                Toast.makeText(getContext(), "목록 로드 실패: " + detail, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<DiaryListResponse> call, Throwable t) {
                        if (getContext() != null)
                            Toast.makeText(getContext(), "일기를 불러오지 못했어요", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterDiary() {
        filteredList.clear();
        String keyword = etSearchDiary.getText().toString().trim().toLowerCase(Locale.getDefault());

        for (DiaryItem item : fullList) {
            boolean matchKeyword = keyword.isEmpty()
                    || containsIgnoreCase(item.getTitle(), keyword)
                    || containsIgnoreCase(item.getContent(), keyword)
                    || containsIgnoreCase(item.getDate(), keyword)
                    || hasMatchingTag(item.getTags(), keyword);

            boolean matchDate = selectedFilterDate.isEmpty()
                    || selectedFilterDate.equals(item.getDate());

            if (matchKeyword && matchDate) filteredList.add(item);
        }

        adapter.setDiaryList(filteredList);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.getDefault()).contains(keyword);
    }

    private boolean hasMatchingTag(ArrayList<String> tags, String keyword) {
        if (tags == null) return false;
        for (String tag : tags) {
            if (tag != null && tag.toLowerCase(Locale.getDefault()).contains(keyword)) return true;
        }
        return false;
    }

    private void showCreateDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(requireContext(),
                (view, year, month, day) -> {
                    String selectedDate = String.format(Locale.getDefault(),
                            "%04d.%02d.%02d", year, month + 1, day);
                    Intent intent = new Intent(requireContext(), DiaryWriteActivity.class);
                    intent.putExtra("mode", "create");
                    intent.putExtra("selected_date", selectedDate);
                    startActivity(intent);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showSearchDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (view, year, month, day) -> {
                    selectedFilterDate = String.format(Locale.getDefault(),
                            "%04d.%02d.%02d", year, month + 1, day);
                    filterDiary();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, "초기화", (d, which) -> {
            selectedFilterDate = "";
            filterDiary();
        });
        dialog.show();
    }
}
