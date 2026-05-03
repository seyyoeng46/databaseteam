package com.example.database_project;

import android.app.AlertDialog;
import android.graphics.Paint;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {

    public interface OnTodoChangedListener {
        void onTodoDeleted();
        void onTodoUpdated();
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

        holder.cbTodo.setOnCheckedChangeListener(null);
        holder.cbTodo.setText(item.getText());
        holder.cbTodo.setChecked(item.isChecked());

        applyCheckedStyle(holder.cbTodo, item.isChecked());

        holder.cbTodo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                todoList.get(currentPosition).setChecked(isChecked);
                applyCheckedStyle(holder.cbTodo, isChecked);
                if (listener != null) listener.onTodoUpdated();
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                todoList.remove(currentPosition);
                notifyItemRemoved(currentPosition);
                notifyItemRangeChanged(currentPosition, todoList.size());

                if (listener != null) listener.onTodoDeleted();
            }
        });

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
                            todoList.get(currentPosition).setText(newText);
                            notifyItemChanged(currentPosition);
                            if (listener != null) listener.onTodoUpdated();
                        }
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });
    }

    private void applyCheckedStyle(CheckBox checkBox, boolean isChecked) {
        if (isChecked) {
            checkBox.setPaintFlags(checkBox.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            checkBox.setAlpha(0.45f);
        } else {
            checkBox.setPaintFlags(checkBox.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            checkBox.setAlpha(1.0f);
        }
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    static class TodoViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbTodo;
        ImageView btnEdit;
        ImageView btnDelete;

        public TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            cbTodo = itemView.findViewById(R.id.cb_todo);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}