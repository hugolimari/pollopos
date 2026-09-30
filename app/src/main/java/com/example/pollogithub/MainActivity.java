package com.example.pollogithub;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.pollogithub.ui.viewmodel.LoginViewModel;

/**
 * Pantalla de inicio de sesión y autenticación de cajeros.
 */
public class MainActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false;
    private LoginViewModel loginViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Si ya hay una sesión activa (< 4 horas), redirigir directamente
        com.example.pollogithub.data.SessionManager sessionManager = new com.example.pollogithub.data.SessionManager(this);
        if (sessionManager.isSessionActive()) {
            int turnoId = sessionManager.getTurnoId();
            if (turnoId > 0) {
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                intent.putExtra("USER_NAME", sessionManager.getUserName());
                startActivity(intent);
                finish();
                return;
            } else {
                Intent intent = new Intent(MainActivity.this, AperturaCajaActivity.class);
                intent.putExtra("USER_NAME", sessionManager.getUserName());
                intent.putExtra("USER_ID", sessionManager.getUserId());
                intent.putExtra("SUCURSAL_ID", sessionManager.getSucursalId());
                startActivity(intent);
                finish();
                return;
            }
        }

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

        // Alternar visibilidad de contraseña
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

        // Observadores del login
        loginViewModel.getLoginSuccess().observe(this, usuario -> {
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            intent.putExtra("USER_NAME", usuario.getNombreCompleto());
            startActivity(intent);
            finish();
        });

        loginViewModel.getNeedsTurnoApertura().observe(this, needs -> {
            if (Boolean.TRUE.equals(needs)) {
                Intent intent = new Intent(MainActivity.this, AperturaCajaActivity.class);
                if (loginViewModel.getLastUsuario() != null) {
                    intent.putExtra("USER_NAME", loginViewModel.getLastUsuario().getNombreCompleto());
                    intent.putExtra("USER_ID", loginViewModel.getLastUsuario().getId());
                    intent.putExtra("SUCURSAL_ID", loginViewModel.getLastUsuario().getSucursalId());
                }
                startActivity(intent);
                finish();
            }
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
                etUser.setError(getString(R.string.error_usuario_requerido));
                return;
            }
            if (pass.isEmpty()) {
                etPassword.setError(getString(R.string.error_contrasena_requerida));
                return;
            }

            loginViewModel.login(user, pass);
        });
    }
}