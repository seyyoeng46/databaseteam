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
import android.widget.Toast; // 추가

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import retrofit2.Call; // 추가
import retrofit2.Callback; // 추가
import retrofit2.Response; // 추가

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
        holder.tvContent.setText(item.getContent());

        // 태그 그리기
        holder.layoutTags.removeAllViews();
        ArrayList<String> tags = item.getTags();

        if (tags != null && !tags.isEmpty()) {
            holder.layoutTags.setVisibility(View.VISIBLE);
            for (String tag : tags) {
                TextView chip = new TextView(context);
                chip.setText("#" + tag); // 태그임을 알 수 있게 # 추가
                chip.setTextSize(11);
                chip.setTextColor(0xFFFFFFFF); // 흰색 글자
                chip.setBackgroundResource(R.drawable.bg_tag_gray); // 회색 배경
                chip.setPadding(20, 10, 20, 10);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.rightMargin = 12;
                chip.setLayoutParams(params);
                holder.layoutTags.addView(chip);
            }
        } else {
            // 태그가 없으면 공간을 숨김
            holder.layoutTags.setVisibility(View.GONE);
        }

        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, DiaryWriteActivity.class);
            intent.putExtra("mode", "edit");
            intent.putExtra("diary_id", item.getId());     // 저장/수정용 ID
            intent.putExtra("diary_date", item.getDate()); // 조회용 날짜 (yyyy.MM.dd)
            context.startActivity(intent);
        });

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("일기 삭제")
                    .setMessage("이 일기를 삭제할까요?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        // 서버 API 호출로 삭제
                        ApiService apiService = ApiClient.getClient(context).create(ApiService.class);
                        apiService.deleteDiary(Integer.parseInt(item.getId())).enqueue(new Callback<DiaryDeleteResponse>() {
                            @Override
                            public void onResponse(Call<DiaryDeleteResponse> call, Response<DiaryDeleteResponse> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                    if (listener != null) listener.onDiaryChanged();
                                }
                            }
                            @Override
                            public void onFailure(Call<DiaryDeleteResponse> call, Throwable t) {
                                Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show();
                            }
                        });
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