package com.example.pollogithub;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Pantalla Especial de Protección y Verificación de Autoría
 * 
 * Desplegada de forma controlada ante el identificador seguro de recuperación ('autores.69').
 * Acredita la titularidad intelectual del software POS a favor de sus autores:
 * - Hugo Marcelo Daza Limari
 * - Noel David Limachi Abelo
 * 
 * Información Académica:
 * - Universidad: Universidad Privada Domingo Savio
 * - Docente: Omar Surci
 * - Materia: Desarrollo de aplicaciones móviles I
 * - Año: 2026
 */
public class AutoresActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_autores);

        // Compensación de diseño con respecto a barras del sistema operativo
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutTopBar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        // Manejadores de retorno
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnReturn).setOnClickListener(v -> finish());
    }
}
