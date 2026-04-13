package com.example.routercontrol;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword, etHttpAddress1, etTaskRepeatPeriod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Инициализация Views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etHttpAddress1 = findViewById(R.id.etHttpAddress1);
        etTaskRepeatPeriod = findViewById(R.id.etTaskRepeatPeriod);

        // Настройка Toolbar (кнопка "Назад")
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Загрузка сохранённых данных
        loadSavedSettings();

        // Обработчики кнопок
        findViewById(R.id.btnSaveSettings).setOnClickListener(this::saveSettings);
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
    }

    private void loadSavedSettings() {
        etUsername.setText(RouterState.getName());
        etPassword.setText(RouterState.getPassword());
        etHttpAddress1.setText(RouterState.getMainHttpAddress());
        etTaskRepeatPeriod.setText(String.valueOf(RouterState.getTaskRepeatPeriod()));
    }

    private void saveSettings(View view) {
        RouterState.setName(Objects.requireNonNull(etUsername.getText()).toString().trim());
        RouterState.setPassword(Objects.requireNonNull(etPassword.getText()).toString().trim());
        RouterState.setMainHttpAddress(Objects.requireNonNull(etHttpAddress1.getText()).toString().trim());
        RouterState.setTaskRepeatPeriod(Integer.parseInt(Objects.requireNonNull(etTaskRepeatPeriod.getText()).toString()));
        Toast.makeText(this, "Настройки успешно сохранены", Toast.LENGTH_SHORT).show();
        finish(); // закрываем Activity после сохранения
    }
}