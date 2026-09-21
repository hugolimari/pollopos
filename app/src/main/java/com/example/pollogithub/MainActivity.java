package com.example.pollogithub;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText etUser = findViewById(R.id.etUser);
        EditText etPassword = findViewById(R.id.etPassword);
        ImageButton btnToggleEye = findViewById(R.id.btnToggleEye);

        btnToggleEye.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnToggleEye.setImageResource(R.drawable.ic_eye);
                isPasswordVisible = false;
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnToggleEye.setImageResource(R.drawable.ic_eye_off);
                isPasswordVisible = true;
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        findViewById(R.id.btnStartShift).setOnClickListener(v -> {
            String user = etUser.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (user.isEmpty()) {
                etUser.setError("Usuario requerido");
                return;
            }
            if (pass.isEmpty()) {
                etPassword.setError("Contraseña requerida");
                return;
            }

            Intent intent = new Intent(MainActivity.this, VentaActivity.class);
            intent.putExtra("USER_NAME", user);
            startActivity(intent);
        });

        findViewById(R.id.btnQuickUserC).setOnClickListener(v -> {
            etUser.setText("carlos.caja1");
            etPassword.requestFocus();
        });

        findViewById(R.id.btnQuickUserM).setOnClickListener(v -> {
            etUser.setText("maria.caja2");
            etPassword.requestFocus();
        });

        findViewById(R.id.btnQuickUserJ).setOnClickListener(v -> {
            etUser.setText("juan.caja3");
            etPassword.requestFocus();
        });
    }
}