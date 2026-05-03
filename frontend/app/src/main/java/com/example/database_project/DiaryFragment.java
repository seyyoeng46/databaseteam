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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class DiaryFragment extends Fragment {

    private EditText etSearchDiary;
    private ImageView btnAddDiary;
    private ImageView btnCalendarSearch;
    private RecyclerView recyclerDiary;

    private DiaryAdapter adapter;
    private ArrayList<DiaryItem> fullList = new ArrayList<>();
    private ArrayList<DiaryItem> filteredList = new ArrayList<>();

    // 날짜 검색값 저장
    private String selectedFilterDate = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
                filterDiary();
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

    private void loadDiaryList() {
        fullList = DiaryStorage.getDiaryList(requireContext());
        filterDiary();
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

            if (matchKeyword && matchDate) {
                filteredList.add(item);
            }
        }

        adapter.setDiaryList(filteredList);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.getDefault()).contains(keyword);
    }

    private boolean hasMatchingTag(ArrayList<String> tags, String keyword) {
        if (tags == null) return false;

        for (String tag : tags) {
            if (tag != null && tag.toLowerCase(Locale.getDefault()).contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // 일기 생성용 날짜 선택
    private void showCreateDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = String.format(
                            Locale.getDefault(),
                            "%04d.%02d.%02d",
                            year, month + 1, dayOfMonth
                    );

                    Intent intent = new Intent(requireContext(), DiaryWriteActivity.class);
                    intent.putExtra("mode", "create");
                    intent.putExtra("selected_date", selectedDate);
                    startActivity(intent);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    // 검색용 날짜 선택
    private void showSearchDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedFilterDate = String.format(
                            Locale.getDefault(),
                            "%04d.%02d.%02d",
                            year, month + 1, dayOfMonth
                    );

                    // 검색창에도 같이 보여주고 싶으면 주석 해제
                    // etSearchDiary.setText(selectedFilterDate);

                    filterDiary();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, "초기화", (d, which) -> {
            selectedFilterDate = "";
            filterDiary();
        });

        dialog.show();
    }
}