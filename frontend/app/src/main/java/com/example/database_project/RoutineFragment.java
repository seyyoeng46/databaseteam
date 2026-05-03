package com.example.database_project;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoutineFragment extends Fragment {

    private static final String USER_ID = "17c6f527-dad9-4e23-bbcd-7343b2cca698";
    private static final int REQUEST_CREATE_ROUTINE = 2001;

    private RoutineAdapter adapter;
    private List<RoutineAdapter.RoutineItem> routineList = new ArrayList<>();
    private TextView tvCount;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routine, container, false);

        RecyclerView rv = view.findViewById(R.id.rv_routines);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        tvCount = view.findViewById(R.id.tv_routine_count);

        adapter = new RoutineAdapter(getContext(), routineList);
        rv.setAdapter(adapter);

        loadRoutines();

        FloatingActionButton fab = view.findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> showAddBottomSheet());

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CREATE_ROUTINE) {
            loadRoutines();
        }
    }

    private void loadRoutines() {
        RetrofitClient.getRoutineApi()
                .getRoutines(USER_ID)
                .enqueue(new Callback<RoutineResponse>() {
                    @Override
                    public void onResponse(Call<RoutineResponse> call,
                                           Response<RoutineResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success) {

                            routineList.clear();

                            List<RoutineResponse.RoutineData> dataList = response.body().data;
                            int[] loadedCount = {0};
                            int total = dataList.size();

                            if (total == 0) {
                                adapter.notifyDataSetChanged();
                                if (tvCount != null) tvCount.setText("등록된 루틴 0개");
                                return;
                            }

                            for (RoutineResponse.RoutineData data : dataList) {
                                String dayText = convertDays(data.schedules);

                                RetrofitClient.getRoutineApi()
                                        .getItems(data.id)
                                        .enqueue(new Callback<ItemResponse>() {
                                            @Override
                                            public void onResponse(Call<ItemResponse> call,
                                                                   Response<ItemResponse> response) {
                                                String[] alarms;
                                                if (response.isSuccessful()
                                                        && response.body() != null
                                                        && response.body().data != null
                                                        && !response.body().data.isEmpty()) {
                                                    alarms = new String[response.body().data.size()];
                                                    for (int i = 0; i < response.body().data.size(); i++) {
                                                        alarms[i] = response.body().data.get(i).title;
                                                    }
                                                } else {
                                                    alarms = new String[]{"알람 없음"};
                                                }

                                                synchronized (routineList) {
                                                    routineList.add(new RoutineAdapter.RoutineItem(
                                                            data.routineName, alarms, dayText, data.id));
                                                    loadedCount[0]++;
                                                    if (loadedCount[0] == total) {
                                                        adapter.notifyDataSetChanged();
                                                        if (tvCount != null)
                                                            tvCount.setText("등록된 루틴 " + routineList.size() + "개");
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<ItemResponse> call, Throwable t) {
                                                synchronized (routineList) {
                                                    routineList.add(new RoutineAdapter.RoutineItem(
                                                            data.routineName, new String[]{"알람 없음"}, dayText, data.id));
                                                    loadedCount[0]++;
                                                    if (loadedCount[0] == total) {
                                                        adapter.notifyDataSetChanged();
                                                        if (tvCount != null)
                                                            tvCount.setText("등록된 루틴 " + routineList.size() + "개");
                                                    }
                                                }
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(getContext(), "루틴을 불러오지 못했어요", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<RoutineResponse> call, Throwable t) {
                        Toast.makeText(getContext(), "서버 연결 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String convertDays(List<Integer> schedules) {
        if (schedules == null || schedules.isEmpty()) return "없음";
        if (schedules.size() == 7) return "매일";
        String[] dayNames = {"일", "월", "화", "수", "목", "금", "토"};
        StringBuilder sb = new StringBuilder();
        for (int day : schedules) {
            if (day >= 0 && day < 7) sb.append(dayNames[day]);
        }
        return sb.toString();
    }

    private void showTypeSelectBottomSheet() {
        BottomSheetDialog dialog2 = new BottomSheetDialog(requireContext());
        View sheetView2 = LayoutInflater.from(getContext())
                .inflate(R.layout.bottom_sheet_type_select, (ViewGroup) getView(), false);

        sheetView2.findViewById(R.id.option_routine).setOnClickListener(v -> {
            dialog2.dismiss();
            startActivityForResult(
                    new Intent(getActivity(), RoutineCreateManualActivity.class),
                    REQUEST_CREATE_ROUTINE);
        });

        sheetView2.findViewById(R.id.option_list).setOnClickListener(v -> {
            dialog2.dismiss();
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
        if (dialog2.getWindow() != null)
            dialog2.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog2.show();
    }

    private void showAddBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext())
                .inflate(R.layout.bottom_sheet_routine, (ViewGroup) getView(), false);

        sheetView.findViewById(R.id.option_ai).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(getActivity(), RoutineCreateActivity.class));
        });

        sheetView.findViewById(R.id.option_manual).setOnClickListener(v -> {
            dialog.dismiss();
            showTypeSelectBottomSheet();
        });

        dialog.setContentView(sheetView);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }
}