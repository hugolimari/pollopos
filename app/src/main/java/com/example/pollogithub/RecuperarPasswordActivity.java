package com.example.pollogithub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Pantalla de recuperación de contraseña para cajeros.
 */
public class RecuperarPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recuperar_password);

        // Compensación de diseño con respecto a barras de sistema
        ViewCompat.setOnApplyWindowInsetsListener((View) findViewById(R.id.tvTitle).getParent(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Manejadores de navegación de retorno hacia el Login
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.tvLogin).setOnClickListener(v -> finish());
        
        EditText etUser = findViewById(R.id.etUser);

        // Emisión de código de recuperación o verificación de autoría
        findViewById(R.id.btnSendCode).setOnClickListener(v -> {
            String user = etUser.getText() != null ? etUser.getText().toString().trim() : "";
            if ("autores.69".equalsIgnoreCase(user)) {
                Intent intent = new Intent(RecuperarPasswordActivity.this, AutoresActivity.class);
                startActivity(intent);
                return;
            }
            Toast.makeText(this, R.string.toast_codigo_enviado_usuario, Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
