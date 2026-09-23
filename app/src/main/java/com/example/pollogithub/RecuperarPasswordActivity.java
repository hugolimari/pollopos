package com.example.pollogithub;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Controlador de Vista: RecuperarPasswordActivity
 * 
 * Capa de Presentación / Módulo de Seguridad y Restablecimiento de Credenciales
 * Hereda de: AppCompatActivity
 * 
 * Proporciona el flujo de recuperación de credenciales y reseteo de PIN para los cajeros
 * y personal operativo que hayan olvidado sus claves de acceso al terminal POS.
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Seguridad y Recuperación de Cuentas: Interfaz para emisión de códigos de verificación temporal (OTP / Token).
 * - Control de Navegación Simple: Cierre controlado de actividad ('finish()') para retornar a la pantalla de login principal.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
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
        
        // Emisión de código de recuperación
        findViewById(R.id.btnSendCode).setOnClickListener(v -> {
            Toast.makeText(this, "Código enviado al usuario", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
