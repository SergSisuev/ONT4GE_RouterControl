package com.example.routercontrol;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LogActivity extends AppCompatActivity {

    private LogAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        setTitle("Лог операций");

        RecyclerView recyclerView = findViewById(R.id.recycler_logs);
        Button btnClear = findViewById(R.id.btn_clear_logs);
        Button btnReturn = findViewById(R.id.btn_return);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<LogEntry> logs = AppLogger.getAllLogs(this);
        adapter = new LogAdapter(logs);
        recyclerView.setAdapter(adapter);

        btnClear.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Очистить лог")
                    .setMessage("Вы уверены, что хотите удалить всю историю?")
                    .setPositiveButton("Да", (dialog, which) -> {
                        AppLogger.clearLogs(this);
                        adapter.updateLogs(new ArrayList<>());
                        Toast.makeText(this, "Лог очищен", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });
        btnReturn.setOnClickListener(v -> {
                    startActivity(new Intent(LogActivity.this, MainActivity.class));
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем список при возвращении на экран
        if (adapter != null) {
            adapter.updateLogs(AppLogger.getAllLogs(this));
        }
    }
}