package com.example.database_project;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RoutineAdapter extends RecyclerView.Adapter<RoutineAdapter.RoutineViewHolder> {

    public static class RoutineItem {
        String name;
        String[] alarms;
        String day;

        public RoutineItem(String name, String[] alarms, String day) {
            this.name = name;
            this.alarms = alarms;
            this.day = day;
        }
    }

    private final Context context;
    private final List<RoutineItem> routineList;

    public RoutineAdapter(Context context, List<RoutineItem> routineList) {
        this.context = context;
        this.routineList = routineList;
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

        // 알람 목록 동적 추가
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

        // 요일 태그 동적 추가
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
            Intent intent = new Intent(context, RoutineEditActivity.class);
            intent.putExtra("routine_name", item.name);
            context.startActivity(intent);
        });

        // 삭제 버튼
        holder.tvDelete.setOnClickListener(v -> {
            routineList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, routineList.size());
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