package com.example.database_project;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoutineAdapter extends RecyclerView.Adapter<RoutineAdapter.RoutineViewHolder> {

    public interface OnRoutineDeletedListener {
        void onDeleted();
    }

    public interface OnRoutineEditListener {
        void onEdit(RoutineItem item);
    }

    public static class RoutineItem {
        String name;
        String[] alarms;
        String day;
        String id;
        int[] schedules;

        public RoutineItem(String name, String[] alarms, String day, String id,int[] schedules) {
            this.name = name;
            this.alarms = alarms;
            this.day = day;
            this.id = id;
            this.schedules = schedules;
        }
    }

    private final Context context;
    private final List<RoutineItem> routineList;
    private final OnRoutineDeletedListener deleteListener;
    private final OnRoutineEditListener editListener;

    public RoutineAdapter(Context context, List<RoutineItem> routineList,
                          OnRoutineDeletedListener deleteListener,
                          OnRoutineEditListener editListener) {
        this.context = context;
        this.routineList = routineList;
        this.deleteListener = deleteListener;
        this.editListener = editListener;
    }

    @NonNull
    @Override
    public RoutineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_routine, parent, false);
        return new RoutineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoutineViewHolder holder, int position) {
        RoutineItem item = routineList.get(position);

        holder.tvName.setText(item.name);

        // 알람 목록
        holder.llAlarms.removeAllViews();
        for (String alarm : item.alarms) {
            TextView tv = new TextView(context);
            tv.setText(alarm);
            tv.setTextSize(13f);
            tv.setTextColor(0xFF5F5E5A);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = 4;
            tv.setLayoutParams(params);
            holder.llAlarms.addView(tv);
        }

        // 요일 태그
        holder.llDays.removeAllViews();
        TextView dayChip = new TextView(context);
        dayChip.setText(item.day);
        dayChip.setTextSize(11f);
        dayChip.setTextColor(0xFF3B6D11);
        dayChip.setBackgroundResource(R.drawable.bg_day_chip);
        int pad = dpToPx(6);
        dayChip.setPadding(pad, dpToPx(3), pad, dpToPx(3));
        holder.llDays.addView(dayChip);

        // 수정 버튼
        holder.tvEdit.setOnClickListener(v -> {
            if (editListener != null) editListener.onEdit(item);
        });

        // 삭제 버튼
        holder.tvDelete.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;

            new AlertDialog.Builder(context)
                    .setTitle("루틴 삭제")
                    .setMessage("'" + item.name + "' 루틴을 삭제할까요?")
                    .setPositiveButton("삭제", (dialog, which) ->
                            deleteRoutine(item, currentPosition))
                    .setNegativeButton("취소", null)
                    .show();
        });
    }

    private void deleteRoutine(RoutineItem item, int position) {
        RetrofitClient.getRoutineApi(context)
                .deleteRoutine(item.id)
                .enqueue(new Callback<BasicResponse>() {
                    @Override
                    public void onResponse(Call<BasicResponse> call,
                                           Response<BasicResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success) {
                            routineList.remove(item);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, routineList.size());
                            Toast.makeText(context, "루틴이 삭제됐어요", Toast.LENGTH_SHORT).show();
                            if (deleteListener != null) deleteListener.onDeleted();
                        } else {
                            Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BasicResponse> call, Throwable t) {
                        Toast.makeText(context, "서버 오류: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public int getItemCount() {
        return routineList.size();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    static class RoutineViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEdit, tvDelete;
        LinearLayout llAlarms, llDays;

        public RoutineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName   = itemView.findViewById(R.id.tv_routine_name);
            llAlarms = itemView.findViewById(R.id.ll_alarms);
            llDays   = itemView.findViewById(R.id.ll_days);
            tvEdit   = itemView.findViewById(R.id.tv_edit);
            tvDelete = itemView.findViewById(R.id.tv_delete);
        }
    }
}