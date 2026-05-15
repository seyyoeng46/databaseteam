package com.example.database_project;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RoutineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int TYPE_ROUTINE = 0;
    public static final int TYPE_TODO    = 1;

    public interface OnRoutineDeleteListener { void onRoutineDelete(RoutineItem item); }
    public interface OnRoutineEditListener { void onEdit(RoutineItem item); }
    public interface OnTodoEditListener { void onTodoEdit(RoutineItem item); }
    public interface OnTodoDeleteListener { void onTodoDelete(RoutineItem item); }

    public static class RoutineItem {
        public String name, id, day;
        public String[] alarms, todoIds;
        public int[] schedules;
        public int type;

        public RoutineItem(String name, String[] alarms, String day, String id, int[] schedules) {
            this.name = name; this.alarms = alarms; this.day = day; this.id = id;
            this.schedules = schedules; this.type = TYPE_ROUTINE;
        }

        public RoutineItem(String date, String[] items, String id, String[] todoIds) {
            this.name = date; this.alarms = items; this.id = id;
            this.todoIds = todoIds; this.type = TYPE_TODO;
        }
    }

    private final Context context;
    private final List<RoutineItem> routineList;
    private final OnRoutineEditListener editListener;
    private final OnRoutineDeleteListener deleteListener;
    private final OnTodoEditListener todoEditListener;
    private final OnTodoDeleteListener todoDeleteListener;

    public RoutineAdapter(Context context, List<RoutineItem> routineList,
                          OnRoutineDeleteListener dl, OnRoutineEditListener el,
                          OnTodoEditListener tel, OnTodoDeleteListener tdl) {
        this.context = context;
        this.routineList = routineList;
        this.deleteListener = dl;
        this.editListener = el;
        this.todoEditListener = tel;
        this.todoDeleteListener = tdl;
    }

    @Override public int getItemViewType(int position) { return routineList.get(position).type; }

    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_TODO) {
            return new TodoViewHolder(LayoutInflater.from(context).inflate(R.layout.item_todo_list, parent, false));
        }
        return new RoutineViewHolder(LayoutInflater.from(context).inflate(R.layout.item_routine, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RoutineItem item = routineList.get(position);
        if (holder instanceof TodoViewHolder) bindTodoViewHolder((TodoViewHolder) holder, item);
        else bindRoutineViewHolder((RoutineViewHolder) holder, item);
    }

    private void bindRoutineViewHolder(RoutineViewHolder holder, RoutineItem item) {
        holder.tvName.setText(item.name);

        // 알람 목록 초기화 및 추가
        holder.llAlarms.removeAllViews();
        if (item.alarms != null) {
            for (String alarm : item.alarms) {
                TextView tv = new TextView(context);
                tv.setText(alarm);
                tv.setTextSize(13f);
                tv.setTextColor(0xFF5F5E5A);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.bottomMargin = 4; // 항목 간 간격
                tv.setLayoutParams(params);
                holder.llAlarms.addView(tv);
            }
        }

        // [수정된 부분] 요일 태그 표시 (배경색과 디자인 적용)
        holder.llDays.removeAllViews();
        if (item.day != null && !item.day.isEmpty()) {
            TextView dayChip = new TextView(context);
            dayChip.setText(item.day);
            dayChip.setTextSize(11f);
            dayChip.setTextColor(0xFF3B6D11); // 진한 초록색 글자

            // 중요: 배경 리소스와 패딩 설정
            dayChip.setBackgroundResource(R.drawable.bg_day_chip);
            int horizontalPad = dpToPx(8); // 좌우 여백
            int verticalPad = dpToPx(3);   // 상하 여백
            dayChip.setPadding(horizontalPad, verticalPad, horizontalPad, verticalPad);

            holder.llDays.addView(dayChip);
        }

        // 수정/삭제 버튼 클릭 리스너
        holder.tvEdit.setOnClickListener(v -> {
            if (editListener != null) editListener.onEdit(item);
        });
        holder.tvDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onRoutineDelete(item);
        });
    }

    // [추가] 패딩 계산을 위한 helper 메서드 (클래스 안에 넣어주세요)
    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    private void bindTodoViewHolder(TodoViewHolder holder, RoutineItem item) {
        if (holder.tvDate != null) holder.tvDate.setText(item.name);
        if (holder.llTodoItems != null) {
            holder.llTodoItems.removeAllViews();
            if (item.alarms != null) {
                for (String todoTitle : item.alarms) {
                    View v = LayoutInflater.from(context).inflate(R.layout.item_todo, holder.llTodoItems, false);
                    TextView tv = v.findViewById(R.id.cb_todo);
                    if (tv != null) tv.setText(todoTitle);
                    if (v.findViewById(R.id.btn_edit) != null) v.findViewById(R.id.btn_edit).setVisibility(View.GONE);
                    if (v.findViewById(R.id.btn_delete) != null) v.findViewById(R.id.btn_delete).setVisibility(View.GONE);
                    holder.llTodoItems.addView(v);
                }
            }
        }
        if (holder.tvEdit != null) holder.tvEdit.setOnClickListener(v -> todoEditListener.onTodoEdit(item));
        if (holder.tvDelete != null) holder.tvDelete.setOnClickListener(v -> todoDeleteListener.onTodoDelete(item));
    }

    @Override public int getItemCount() { return routineList.size(); }

    static class RoutineViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEdit, tvDelete;
        LinearLayout llAlarms, llDays; // [추가] llDays 추가
        public RoutineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_routine_name);
            tvEdit = itemView.findViewById(R.id.tv_edit);
            tvDelete = itemView.findViewById(R.id.tv_delete);
            llAlarms = itemView.findViewById(R.id.ll_alarms);
            llDays = itemView.findViewById(R.id.ll_days);
        }
    }

    static class TodoViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvEdit, tvDelete; LinearLayout llTodoItems;
        public TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_todo_date);
            tvEdit = itemView.findViewById(R.id.tv_todo_edit);
            tvDelete = itemView.findViewById(R.id.tv_todo_delete);
            llTodoItems = itemView.findViewById(R.id.ll_todo_items);
        }
    }
}