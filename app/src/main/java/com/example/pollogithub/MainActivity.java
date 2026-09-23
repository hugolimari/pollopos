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

import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import com.example.pollogithub.ui.viewmodel.LoginViewModel;

public class MainActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false;
    private LoginViewModel loginViewModel;

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

        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

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

        findViewById(R.id.tvForgotPin).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecuperarPasswordActivity.class);
            startActivity(intent);
        });

        loginViewModel.getLoginSuccess().observe(this, usuario -> {
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            intent.putExtra("USER_NAME", usuario.getNombreCompleto());
            startActivity(intent);
            finish();
        });

        loginViewModel.getLoginError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
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

            loginViewModel.login(user, pass);
        });
    }
}