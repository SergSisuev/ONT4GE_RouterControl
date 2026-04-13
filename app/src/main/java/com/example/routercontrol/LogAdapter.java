package com.example.routercontrol;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    private List<LogEntry> logs;

    public LogAdapter(List<LogEntry> logs) {
        this.logs = logs;
    }

    public void updateLogs(List<LogEntry> newLogs) {
        this.logs = newLogs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        LogEntry entry = logs.get(position);
        holder.tvTime.setText(entry.getFormattedTime());
        holder.tvStatus.setText(entry.getStatus());
        holder.tvMessage.setText(entry.getMessage());

        // Цвет статуса
        int color = entry.getStatus().equals("SUCCESS") ? 0xFF4CAF50 :
                entry.getStatus().equals("FAILED") ? 0xFFF44336 : 0xFFFF9800;
        holder.tvStatus.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvStatus, tvMessage;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvMessage = itemView.findViewById(R.id.tv_message);
        }
    }
}