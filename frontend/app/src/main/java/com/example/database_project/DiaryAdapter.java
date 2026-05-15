package com.example.database_project;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder> {

    public interface OnDiaryChangedListener {
        void onDiaryChanged();
    }

    private final Context context;
    private ArrayList<DiaryItem> diaryList;
    private final OnDiaryChangedListener listener;

    public DiaryAdapter(Context context, ArrayList<DiaryItem> diaryList,
                        OnDiaryChangedListener listener) {
        this.context   = context;
        this.diaryList = diaryList;
        this.listener  = listener;
    }

    public void setDiaryList(ArrayList<DiaryItem> diaryList) {
        this.diaryList = diaryList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DiaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_diary_card, parent, false);
        return new DiaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiaryViewHolder holder, int position) {
        DiaryItem item = diaryList.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvDate.setText(item.getDate());

        String preview = item.getContent();
        if (preview.length() > 45) preview = preview.substring(0, 45) + "...";
        holder.tvContent.setText(preview);

        holder.layoutTags.removeAllViews();
        addTagRow(holder.layoutTags, "루틴",   item.getRoutineTags(), false);
        addTagRow(holder.layoutTags, "리스트", item.getListTags(),    !item.getRoutineTags().isEmpty());

        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, DiaryWriteActivity.class);
            intent.putExtra("mode", "edit");
            intent.putExtra("diary_id", item.getId());
            intent.putExtra("diary_date", item.getDate());
            intent.putExtra("diary_title", item.getTitle());
            intent.putExtra("diary_content", item.getContent());
            intent.putStringArrayListExtra("diary_routine_tags", item.getRoutineTags());
            intent.putStringArrayListExtra("diary_list_tags", item.getListTags());
            context.startActivity(intent);
        });

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            new AlertDialog.Builder(context)
                    .setTitle("일기 삭제")
                    .setMessage("이 일기를 삭제할까요?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        RetrofitClient.getRoutineApi(context)
                                .deleteDiary(item.getId())
                                .enqueue(new Callback<BasicResponse>() {
                                    @Override
                                    public void onResponse(Call<BasicResponse> call,
                                                           Response<BasicResponse> response) {
                                        if (response.isSuccessful() && response.body() != null
                                                && response.body().success) {
                                            if (listener != null) listener.onDiaryChanged();
                                        } else {
                                            Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<BasicResponse> call, Throwable t) {
                                        Toast.makeText(context,
                                                "삭제 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });
    }

    /** 초록색 태그 텍스트 한 줄을 parent에 추가 (라벨 없음) */
    private void addTagRow(LinearLayout parent, String label,
                           List<String> tags, boolean addTopMargin) {
        if (tags == null || tags.isEmpty()) return;

        TextView tvTags = new TextView(context);
        tvTags.setText(android.text.TextUtils.join("  ", tags));
        tvTags.setTextSize(13);
        tvTags.setTextColor(0xFF2ECC71);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        if (addTopMargin) lp.topMargin = dp(4);
        tvTags.setLayoutParams(lp);
        parent.addView(tvTags);
    }

    private int dp(int v) {
        return Math.round(v * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return diaryList.size();
    }

    static class DiaryViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvContent;
        LinearLayout layoutTags;
        Button btnEdit, btnDelete;

        public DiaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle    = itemView.findViewById(R.id.tv_diary_title);
            tvDate     = itemView.findViewById(R.id.tv_diary_date);
            tvContent  = itemView.findViewById(R.id.tv_diary_content);
            layoutTags = itemView.findViewById(R.id.layout_diary_tags);
            btnEdit    = itemView.findViewById(R.id.btn_diary_edit);
            btnDelete  = itemView.findViewById(R.id.btn_diary_delete);
        }
    }
}
