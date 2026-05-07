package com.example.database_project;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {

    public interface OnTodoChangedListener {
        void onTodoDeleted();
        void onTodoUpdated();
    }

    public interface OnDeleteListener {
        void onDelete(int position);
    }

    public interface OnEditListener {
        void onEdit(int position, String newText);
    }

    private OnDeleteListener onDeleteListener;
    private OnEditListener onEditListener;

    public void setOnDeleteListener(OnDeleteListener listener) {
        this.onDeleteListener = listener;
    }

    public void setOnEditListener(OnEditListener listener) {
        this.onEditListener = listener;
    }

    private final ArrayList<TodoItem> todoList;
    private final OnTodoChangedListener listener;

    public TodoAdapter(ArrayList<TodoItem> todoList, OnTodoChangedListener listener) {
        this.todoList = todoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_todo, parent, false);
        return new TodoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        TodoItem item = todoList.get(position);

        holder.tvTodo.setText(item.getText());

        // 수정 버튼
        holder.btnEdit.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;

            EditText editText = new EditText(v.getContext());
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
            editText.setText(todoList.get(currentPosition).getText());
            editText.setSelection(editText.getText().length());

            new AlertDialog.Builder(v.getContext())
                    .setTitle("할 일 수정")
                    .setView(editText)
                    .setPositiveButton("저장", (dialog, which) -> {
                        String newText = editText.getText().toString().trim();
                        if (!newText.isEmpty()) {
                            // 로컬 업데이트
                            todoList.get(currentPosition).setText(newText);
                            notifyItemChanged(currentPosition);
                            if (listener != null) listener.onTodoUpdated();
                            // DB 업데이트 콜백
                            if (onEditListener != null) {
                                onEditListener.onEdit(currentPosition, newText);
                            }
                        }
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });

        // 삭제 버튼
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (onDeleteListener != null) {
                onDeleteListener.onDelete(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    static class TodoViewHolder extends RecyclerView.ViewHolder {
        TextView tvTodo;
        TextView btnEdit;
        TextView btnDelete;

        public TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTodo    = itemView.findViewById(R.id.cb_todo);
            btnEdit   = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}