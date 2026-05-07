package com.example.database_project;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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

public class RoutineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_ROUTINE = 0;
    public static final int TYPE_TODO    = 1;

    public interface OnRoutineDeletedListener {
        void onDeleted();
    }

    public interface OnRoutineEditListener {
        void onEdit(RoutineItem item);
    }

    public interface OnTodoEditListener {
        void onTodoEdit(RoutineItem item);
    }

    public static class RoutineItem {
        String name;
        String[] alarms;
        String day;
        String id;
        int[] schedules;
        int type;
        String[] todoIds;

        // 루틴용 생성자
        public RoutineItem(String name, String[] alarms, String day, String id, int[] schedules) {
            this.name = name;
            this.alarms = alarms;
            this.day = day;
            this.id = id;
            this.schedules = schedules;
            this.type = TYPE_ROUTINE;
            this.todoIds = null;
        }

        // 투두용 생성자
        public RoutineItem(String date, String[] items, String id, String[] todoIds) {
            this.name = date;
            this.alarms = items;
            this.day = "";
            this.id = id;
            this.schedules = new int[0];
            this.type = TYPE_TODO;
            this.todoIds = todoIds;
        }
    }

    private final Context context;
    private final List<RoutineItem> routineList;
    private final OnRoutineDeletedListener deleteListener;
    private final OnRoutineEditListener editListener;
    private final OnTodoEditListener todoEditListener;

    public RoutineAdapter(Context context, List<RoutineItem> routineList,
                          OnRoutineDeletedListener deleteListener,
                          OnRoutineEditListener editListener,
                          OnTodoEditListener todoEditListener) {
        this.context = context;
        this.routineList = routineList;
        this.deleteListener = deleteListener;
        this.editListener = editListener;
        this.todoEditListener = todoEditListener;
    }

    @Override
    public int getItemViewType(int position) {
        return routineList.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_TODO) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_todo_list, parent, false);
            return new TodoViewHolder(view);
        } else {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_routine, parent, false);
            return new RoutineViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RoutineItem item = routineList.get(position);
        if (holder instanceof TodoViewHolder) {
            bindTodoViewHolder((TodoViewHolder) holder, item, position);
        } else {
            bindRoutineViewHolder((RoutineViewHolder) holder, item, position);
        }
    }

    private void bindRoutineViewHolder(RoutineViewHolder holder, RoutineItem item, int position) {
        holder.tvName.setText(item.name);

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

        holder.llDays.removeAllViews();
        TextView dayChip = new TextView(context);
        dayChip.setText(item.day);
        dayChip.setTextSize(11f);
        dayChip.setTextColor(0xFF3B6D11);
        dayChip.setBackgroundResource(R.drawable.bg_day_chip);
        int pad = dpToPx(6);
        dayChip.setPadding(pad, dpToPx(3), pad, dpToPx(3));
        holder.llDays.addView(dayChip);

        // 루틴 수정 버튼 → editListener
        holder.tvEdit.setOnClickListener(v -> {
            if (editListener != null) editListener.onEdit(item);
        });

        // 루틴 삭제 버튼
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

    private void bindTodoViewHolder(TodoViewHolder holder, RoutineItem item, int position) {
        holder.tvDate.setText(item.name);

        holder.llTodoItems.removeAllViews();
        for (String todoItem : item.alarms) {
            TextView tv = new TextView(context);
            tv.setText(todoItem);
            tv.setTextSize(13f);
            tv.setTextColor(0xFF5F5E5A);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = 4;
            tv.setLayoutParams(params);
            holder.llTodoItems.addView(tv);
        }

        // 투두 수정 버튼 → todoEditListener
        holder.tvEdit.setOnClickListener(v -> {
            if (todoEditListener != null) todoEditListener.onTodoEdit(item);
        });

        // 투두 삭제 버튼
        holder.tvDelete.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;
            new AlertDialog.Builder(context)
                    .setTitle("리스트 삭제")
                    .setMessage("이 리스트를 삭제할까요?")
                    .setPositiveButton("삭제", (dialog, which) ->
                            deleteTodos(item, currentPosition))
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

    private void deleteTodos(RoutineItem item, int position) {
        if (item.todoIds == null || item.todoIds.length == 0) {
            routineList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, routineList.size());
            if (deleteListener != null) deleteListener.onDeleted();
            return;
        }

        int[] deletedCount = {0};
        int total = item.todoIds.length;

        for (String todoId : item.todoIds) {
            RetrofitClient.getRoutineApi(context)
                    .deleteTodo(todoId)
                    .enqueue(new Callback<BasicResponse>() {
                        @Override
                        public void onResponse(Call<BasicResponse> call,
                                               Response<BasicResponse> response) {
                            deletedCount[0]++;
                            if (deletedCount[0] == total) {
                                routineList.remove(item);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, routineList.size());
                                Toast.makeText(context, "리스트가 삭제됐어요", Toast.LENGTH_SHORT).show();
                                if (deleteListener != null) deleteListener.onDeleted();
                            }
                        }

                        @Override
                        public void onFailure(Call<BasicResponse> call, Throwable t) {
                            Toast.makeText(context, "삭제 실패: " + t.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
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

    static class TodoViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvEdit, tvDelete;
        LinearLayout llTodoItems;

        public TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate      = itemView.findViewById(R.id.tv_todo_date);
            llTodoItems = itemView.findViewById(R.id.ll_todo_items);
            tvEdit      = itemView.findViewById(R.id.tv_todo_edit);
            tvDelete    = itemView.findViewById(R.id.tv_todo_delete);
        }
    }
}