package com.example.database_project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnRegisterListener {
        /** 사용자가 "루틴 등록하기" 버튼을 눌렀을 때 */
        void onRegister(ChatMessage msg, int position);
    }

    private final Context            context;
    private final List<ChatMessage>  messages;
    private final OnRegisterListener registerListener;

    public ChatAdapter(Context context, List<ChatMessage> messages,
                       OnRegisterListener registerListener) {
        this.context          = context;
        this.messages         = messages;
        this.registerListener = registerListener;
    }

    @Override public int getItemViewType(int position) { return messages.get(position).type; }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(context);
        switch (viewType) {
            case ChatMessage.TYPE_USER:
                return new UserVH(inf.inflate(R.layout.item_chat_user, parent, false));
            case ChatMessage.TYPE_LOADING:
                return new LoadingVH(inf.inflate(R.layout.item_chat_loading, parent, false));
            default:
                return new ResultVH(inf.inflate(R.layout.item_chat_ai_result, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);

        if (holder instanceof UserVH) {
            UserVH vh = (UserVH) holder;
            if (msg.text != null && !msg.text.isEmpty()) {
                vh.tvMessage.setText(msg.text);
                vh.tvMessage.setVisibility(View.VISIBLE);
            } else {
                vh.tvMessage.setVisibility(View.GONE);
            }
            if (msg.imageUri != null) {
                vh.ivImage.setImageURI(msg.imageUri);
                vh.cardImage.setVisibility(View.VISIBLE);
            } else {
                vh.cardImage.setVisibility(View.GONE);
            }

        } else if (holder instanceof ResultVH) {
            ResultVH vh = (ResultVH) holder;
            vh.tvTitle.setText(msg.routineTitle);
            vh.layoutItems.removeAllViews();

            if (msg.routineItems != null) {
                for (String item : msg.routineItems) {
                    TextView tv = new TextView(context);
                    tv.setText(item);
                    tv.setTextSize(13f);
                    tv.setTextColor(0xFF2C2C2A);
                    tv.setLineSpacing(0, 1.3f);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.bottomMargin = dp(5);
                    tv.setLayoutParams(lp);
                    vh.layoutItems.addView(tv);
                }
            }

            // 등록하기 버튼 상태
            if (msg.registered) {
                vh.btnRegister.setText("등록 완료 ✓");
                vh.btnRegister.setEnabled(false);
                vh.btnRegister.setAlpha(0.5f);
            } else {
                vh.btnRegister.setText("루틴 등록하기");
                vh.btnRegister.setEnabled(true);
                vh.btnRegister.setAlpha(1.0f);
                vh.btnRegister.setOnClickListener(v -> {
                    if (registerListener != null)
                        registerListener.onRegister(msg, holder.getAdapterPosition());
                });
            }
        }
    }

    @Override public int getItemCount() { return messages.size(); }

    private int dp(int v) {
        return Math.round(v * context.getResources().getDisplayMetrics().density);
    }

    // ── ViewHolders ───────────────────────────────────────────

    static class UserVH extends RecyclerView.ViewHolder {
        TextView  tvMessage;
        CardView  cardImage;
        ImageView ivImage;
        UserVH(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tv_message);
            cardImage = v.findViewById(R.id.card_image);
            ivImage   = v.findViewById(R.id.iv_image);
        }
    }

    static class LoadingVH extends RecyclerView.ViewHolder {
        LoadingVH(View v) { super(v); }
    }

    static class ResultVH extends RecyclerView.ViewHolder {
        TextView     tvTitle;
        LinearLayout layoutItems;
        Button       btnRegister;
        ResultVH(View v) {
            super(v);
            tvTitle     = v.findViewById(R.id.tv_routine_title);
            layoutItems = v.findViewById(R.id.layout_items);
            btnRegister = v.findViewById(R.id.btn_register);
        }
    }
}
