package com.example.database_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import java.util.Calendar;
import java.util.Locale;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class RoutineFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routine, container, false);

        RecyclerView rv = view.findViewById(R.id.rv_routines);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        List<RoutineAdapter.RoutineItem> dummyList = new ArrayList<>();
        dummyList.add(new RoutineAdapter.RoutineItem(
                "외출 준비 루틴",
                new String[]{"15:10  냉장고/주방 확인", "15:20  구매 목록 작성", "15:30  출발 준비"},
                "월"));
        dummyList.add(new RoutineAdapter.RoutineItem(
                "저녁 루틴",
                new String[]{"21:00  운동 30분", "22:00  독서"},
                "매일"));

        RoutineAdapter adapter = new RoutineAdapter(getContext(), dummyList);
        rv.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> showAddBottomSheet());

        return view;
    }

    private void showTypeSelectBottomSheet() {
        BottomSheetDialog dialog2 = new BottomSheetDialog(requireContext());
        View sheetView2 = LayoutInflater.from(getContext())
                .inflate(R.layout.bottom_sheet_type_select, (ViewGroup) getView(), false);

        // 루틴 만들기
        sheetView2.findViewById(R.id.option_routine).setOnClickListener(v -> {
            dialog2.dismiss();
            startActivity(new Intent(getActivity(), RoutineCreateManualActivity.class));
        });

        // 리스트 만들기
        sheetView2.findViewById(R.id.option_list).setOnClickListener(v -> {
            dialog2.dismiss();
            // 날짜 먼저 선택
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        String selectedDate = String.format(Locale.getDefault(),
                                "%04d.%02d.%02d", year, month + 1, dayOfMonth);
                        Intent intent = new Intent(getActivity(), TodoCreateActivity.class);
                        intent.putExtra("create_mode", "direct");
                        intent.putExtra("selected_date", selectedDate);
                        startActivity(intent);
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        dialog2.setContentView(sheetView2);
        if (dialog2.getWindow() != null) {
            dialog2.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog2.show();
    }

    private void showAddBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext())
                .inflate(R.layout.bottom_sheet_routine, (ViewGroup) getView(), false);

        // AI에게 요청
        sheetView.findViewById(R.id.option_ai).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(getActivity(), RoutineCreateActivity.class));
        });

        // 직접 입력 → 두 번째 바텀시트
        sheetView.findViewById(R.id.option_manual).setOnClickListener(v -> {
            dialog.dismiss();
            showTypeSelectBottomSheet();
        });

        dialog.setContentView(sheetView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }
}