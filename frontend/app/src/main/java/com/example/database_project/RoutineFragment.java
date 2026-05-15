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

    private static final int REQUEST_CREATE_ROUTINE = 2001;

    private RoutineAdapter adapter;
    private List<RoutineAdapter.RoutineItem> routineList = new ArrayList<>();
    private List<RoutineAdapter.RoutineItem> fullList = new ArrayList<>();
    private TextView tvRoutineCount;
    private TextView tvFilterLabel;
    private int currentFilter = 0; // 0=전체, 1=루틴, 2=리스트

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routine, container, false);

        RecyclerView rv = view.findViewById(R.id.rv_routines);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        tvRoutineCount = view.findViewById(R.id.tv_routine_count);
        tvFilterLabel  = view.findViewById(R.id.tv_filter_label);

        // 1. onCreateView 내부의 adapter 생성 부분 수정
        adapter = new RoutineAdapter(getContext(), routineList,
                item -> { // [수정] () -> 에서 item -> 로 변경 (루틴 삭제 시)
                    // 실제 삭제 확인 다이얼로그 호출
                    showRoutineDeleteConfirmDialog(item);
                },
                item -> { // 루틴 수정 시
                    Intent intent = new Intent(getActivity(), RoutineEditActivity.class);
                    intent.putExtra("routine_id", item.id);
                    intent.putExtra("routine_name", item.name);
                    intent.putExtra("routine_schedules", item.schedules != null ? item.schedules : new int[0]);
                    startActivityForResult(intent, 3001);
                },
                item -> { // 투두 수정 시 (카드 하단 수정 버튼)
                    Intent intent = new Intent(getActivity(), TodoEditActivity.class);
                    intent.putExtra("todo_date", item.name);
                    intent.putExtra("todo_items", item.alarms);
                    intent.putExtra("todo_ids", item.todoIds);
                    startActivityForResult(intent, 5001);
                },
                item -> { // 투두 삭제 시 (카드 하단 삭제 버튼)
                    showTodoDeleteConfirmDialog(item);
                }
        );
        rv.setAdapter(adapter);

        // 필터 버튼
        view.findViewById(R.id.btn_filter).setOnClickListener(v -> showFilterPopup(v));

        loadRoutines();
        loadTodos();

        FloatingActionButton fab = view.findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> showAddBottomSheet());

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CREATE_ROUTINE || requestCode == 3001) {
            loadRoutines();
        } else if (requestCode == 4001 && resultCode == android.app.Activity.RESULT_OK) {
            loadTodos();
        } else if (requestCode == 5001 && resultCode == android.app.Activity.RESULT_OK) {
            loadTodos();
        }
    }

    private void showFilterPopup(View anchor) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), anchor);
        popup.getMenu().add(0, 0, 0, "전체");
        popup.getMenu().add(0, 1, 1, "루틴");
        popup.getMenu().add(0, 2, 2, "리스트");

        popup.setOnMenuItemClickListener(menuItem -> {
            currentFilter = menuItem.getItemId();
            switch (currentFilter) {
                case 0: tvFilterLabel.setText("전체"); break;
                case 1: tvFilterLabel.setText("루틴"); break;
                case 2: tvFilterLabel.setText("리스트"); break;
            }
            applyFilter();
            return true;
        });
        popup.show();
    }

    private void applyFilter() {
        routineList.clear();
        for (RoutineAdapter.RoutineItem item : fullList) {
            if (currentFilter == 0) {
                routineList.add(item);
            } else if (currentFilter == 1 && item.type == RoutineAdapter.TYPE_ROUTINE) {
                routineList.add(item);
            } else if (currentFilter == 2 && item.type == RoutineAdapter.TYPE_TODO) {
                routineList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
        updateCount();
    }

    private void updateCount() {
        long routineCount = fullList.stream()
                .filter(item -> item.type == RoutineAdapter.TYPE_ROUTINE).count();
        long todoCount = fullList.stream()
                .filter(item -> item.type == RoutineAdapter.TYPE_TODO).count();

        if (tvRoutineCount == null) return;

        switch (currentFilter) {
            case 0:
                tvRoutineCount.setText("전체 " + (routineCount + todoCount) + "개");
                break;
            case 1:
                tvRoutineCount.setText("루틴 " + routineCount + "개");
                break;
            case 2:
                tvRoutineCount.setText("리스트 " + todoCount + "개");
                break;
        }
    }

    private void loadRoutines() {
        RetrofitClient.getRoutineApi(getContext())
                .getRoutines()
                .enqueue(new Callback<RoutineResponse>() {
                    @Override
                    public void onResponse(Call<RoutineResponse> call,
                                           Response<RoutineResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success) {

                            fullList.removeIf(item -> item.type == RoutineAdapter.TYPE_ROUTINE);

                            List<RoutineResponse.RoutineData> dataList = response.body().data;
                            int[] loadedCount = {0};
                            int total = dataList.size();

                            if (total == 0) {
                                applyFilter();
                                return;
                            }

                            for (RoutineResponse.RoutineData data : dataList) {
                                String dayText = convertDays(data.schedules);

                                int[] schedulesArr = new int[0];
                                if (data.schedules != null) {
                                    schedulesArr = new int[data.schedules.size()];
                                    for (int i = 0; i < data.schedules.size(); i++) {
                                        schedulesArr[i] = data.schedules.get(i);
                                    }
                                }
                                final int[] finalSchedulesArr = schedulesArr;

                                RetrofitClient.getRoutineApi(getContext())
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

                                                    List<ItemResponse.ItemData> items = response.body().data;
                                                    items.sort((a, b) -> {
                                                        String timeA = a.title != null && a.title.contains(" ")
                                                                ? a.title.substring(0, a.title.indexOf(" ")) : a.title;
                                                        String timeB = b.title != null && b.title.contains(" ")
                                                                ? b.title.substring(0, b.title.indexOf(" ")) : b.title;
                                                        return timeA.compareTo(timeB);
                                                    });

                                                    alarms = new String[items.size()];
                                                    for (int i = 0; i < items.size(); i++) {
                                                        alarms[i] = items.get(i).title;
                                                    }
                                                } else {
                                                    alarms = new String[]{"알람 없음"};
                                                }

                                                synchronized (fullList) {
                                                    fullList.add(new RoutineAdapter.RoutineItem(
                                                            data.routineName, alarms, dayText,
                                                            data.id, finalSchedulesArr));
                                                    loadedCount[0]++;
                                                    if (loadedCount[0] == total) {
                                                        fullList.sort((a, b) ->
                                                                Integer.compare(a.type, b.type));
                                                        applyFilter();
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<ItemResponse> call, Throwable t) {
                                                synchronized (fullList) {
                                                    fullList.add(new RoutineAdapter.RoutineItem(
                                                            data.routineName, new String[]{"알람 없음"},
                                                            dayText, data.id, finalSchedulesArr));
                                                    loadedCount[0]++;
                                                    if (loadedCount[0] == total) {
                                                        fullList.sort((a, b) ->
                                                                Integer.compare(a.type, b.type));
                                                        applyFilter();
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

    private void loadTodos() {
        RetrofitClient.getRoutineApi(getContext())
                .getAllTodos()
                .enqueue(new Callback<TodoAllResponse>() {
                    @Override
                    public void onResponse(Call<TodoAllResponse> call,
                                           Response<TodoAllResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success) {

                            fullList.removeIf(item -> item.type == RoutineAdapter.TYPE_TODO);

                            for (TodoAllResponse.TodoAllData data : response.body().data) {
                                String[] items = data.titles.toArray(new String[0]);
                                String[] ids = data.ids != null
                                        ? data.ids.toArray(new String[0])
                                        : new String[0];

                                String rawDate = data.targetDate.contains("T")
                                        ? data.targetDate.substring(0, data.targetDate.indexOf("T"))
                                        : data.targetDate;
                                String displayDate = rawDate.replace("-", ".");

                                fullList.add(new RoutineAdapter.RoutineItem(
                                        displayDate, items, "todo_" + data.targetDate, ids));
                            }

                            fullList.sort((a, b) -> Integer.compare(a.type, b.type));
                            applyFilter();
                        }
                    }

                    @Override
                    public void onFailure(Call<TodoAllResponse> call, Throwable t) {
                        // 실패해도 루틴 목록 유지
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
                        startActivityForResult(intent, 4001);
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

    private void showTodoDeleteConfirmDialog(RoutineAdapter.RoutineItem item) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("삭제 확인")
                .setMessage(item.name + "의 모든 투두 항목을 삭제할까요?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    if (item.todoIds != null) deleteTodoItems(item.todoIds);
                })
                .setNegativeButton("취소", null).show();
    }

    private void deleteTodoItems(String[] ids) {
        int[] deleteCount = {0};
        for (String id : ids) {
            RetrofitClient.getRoutineApi(getContext()).deleteTodo(id).enqueue(new retrofit2.Callback<BasicResponse>() {
                @Override
                public void onResponse(retrofit2.Call<BasicResponse> call, retrofit2.Response<BasicResponse> response) {
                    deleteCount[0]++;
                    if (deleteCount[0] == ids.length) {
                        Toast.makeText(getContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                        loadTodos(); // 목록 새로고침
                    }
                }
                @Override public void onFailure(retrofit2.Call<BasicResponse> call, Throwable t) {}
            });
        }
    }

    // 루틴 삭제 확인 다이얼로그
    private void showRoutineDeleteConfirmDialog(RoutineAdapter.RoutineItem item) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("루틴 삭제")
                .setMessage("'" + item.name + "' 루틴을 삭제할까요?\n관련된 상세 항목도 모두 삭제됩니다.")
                .setPositiveButton("삭제", (dialog, which) -> {
                    deleteRoutineFromServer(item.id);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // 서버에서 루틴 삭제 실행
    private void deleteRoutineFromServer(String routineId) {
        RetrofitClient.getRoutineApi(getContext())
                .deleteRoutine(routineId) // RoutineApi에 정의된 deleteRoutine 호출
                .enqueue(new Callback<BasicResponse>() {
                    @Override
                    public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "루틴이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                            loadRoutines(); // 목록 새로고침
                        } else {
                            Toast.makeText(getContext(), "삭제 실패", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BasicResponse> call, Throwable t) {
                        Toast.makeText(getContext(), "네트워크 오류", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}