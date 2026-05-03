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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder> {

    public interface OnDiaryChangedListener {
        void onDiaryChanged();
    }

    private final Context context;
    private ArrayList<DiaryItem> diaryList;
    private final OnDiaryChangedListener listener;

    public DiaryAdapter(Context context, ArrayList<DiaryItem> diaryList, OnDiaryChangedListener listener) {
        this.context = context;
        this.diaryList = diaryList;
        this.listener = listener;
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
        if (preview.length() > 45) {
            preview = preview.substring(0, 45) + "...";
        }
        holder.tvContent.setText(preview);

        holder.layoutTags.removeAllViews();
        ArrayList<String> tags = item.getTags();

        if (tags != null) {
            for (String tag : tags) {
                TextView chip = new TextView(context);
                chip.setText(tag);
                chip.setTextSize(13);
                chip.setTextColor(0xFFFFFFFF);
                chip.setBackgroundResource(R.drawable.bg_tag_gray);
                chip.setPadding(26, 12, 26, 12);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.rightMargin = 12;
                params.bottomMargin = 12;
                chip.setLayoutParams(params);

                holder.layoutTags.addView(chip);
            }
        }

        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, DiaryWriteActivity.class);
            intent.putExtra("mode", "edit");
            intent.putExtra("diary_id", item.getId());
            context.startActivity(intent);
        });

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("일기 삭제")
                    .setMessage("이 일기를 삭제할까요?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        DiaryStorage.deleteDiary(context, item.getId());
                        if (listener != null) listener.onDiaryChanged();
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });
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
            tvTitle = itemView.findViewById(R.id.tv_diary_title);
            tvDate = itemView.findViewById(R.id.tv_diary_date);
            tvContent = itemView.findViewById(R.id.tv_diary_content);
            layoutTags = itemView.findViewById(R.id.layout_diary_tags);
            btnEdit = itemView.findViewById(R.id.btn_diary_edit);
            btnDelete = itemView.findViewById(R.id.btn_diary_delete);
        }
    }
}