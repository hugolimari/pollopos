package com.example.pollogithub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

/**
 * Controlador de Vista: PerfilActivity (Configuración de Terminal y Operador)
 * 
 * Capa de Presentación / Módulo de Perfil y Utilidades de Terminal POS
 * Hereda de: AppCompatActivity
 * 
 * Gestiona la información de identidad del cajero u operador autenticado,
 * además de proporcionar accesos directos a utilidades de hardware y periféricos
 * del punto de venta (impresora térmica de tickets, sincronización remota con la nube,
 * arqueos intermedios / Corte X y cierre definitivo de turno).
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Patrón Confirmación Modal (Defensive UI): Implementación de AlertDialog para confirmar
 *   la intención de cerrar turno antes de finalizar la sesión operativa.
 * - Limpieza de Pila de Actividades (Task Stack Purge): Empleo de 'FLAG_ACTIVITY_CLEAR_TASK'
 *   para garantizar que no persistan estados volátiles de sesión en el historial de Android tras el logout.
 */
public class PerfilActivity extends AppCompatActivity {

    private String cashierName = "Carlos Méndez";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil);

        // Compensación de diseño con respecto a barras de sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainPerfil), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView tvProfileName = findViewById(R.id.tvProfileName);
        TextView tvProfileAvatarInitial = findViewById(R.id.tvProfileAvatarInitial);

        // Recuperación del nombre del usuario activo
        String userExtra = getIntent().getStringExtra("USER_NAME");
        if (userExtra != null && !userExtra.trim().isEmpty()) {
            cashierName = userExtra.trim();
        }

        tvProfileName.setText(cashierName);
        String initial = cashierName.substring(0, 1).toUpperCase(Locale.getDefault());
        tvProfileAvatarInitial.setText(initial);

        // Inicialización de opciones de configuración y navegación
        setupSettingsActions();
        setupBottomNav();
    }

    /**
     * Asocia los manejadores de eventos a los botones de configuración de periféricos y sesión.
     */
    private void setupSettingsActions() {
        // Diagnóstico de conectividad con impresora térmica
        findViewById(R.id.btnSettingPrinter).setOnClickListener(v ->
            Toast.makeText(this, "Impresora Epson TM-T20III lista · Estado OK", Toast.LENGTH_SHORT).show()
        );

        // Generación de arqueo parcial o preliminar (Corte X)
        findViewById(R.id.btnSettingCashCut).setOnClickListener(v ->
            Toast.makeText(this, "Generando arqueo de caja (Corte X)...", Toast.LENGTH_SHORT).show()
        );

        // Disparo manual de sincronización con API / Backend en la nube
        findViewById(R.id.btnSettingSync).setOnClickListener(v ->
            Toast.makeText(this, "Sincronizando pedidos con la nube...", Toast.LENGTH_SHORT).show()
        );

        // Bloqueo temporal de pantalla del terminal para seguridad física
        findViewById(R.id.btnSettingLock).setOnClickListener(v ->
            Toast.makeText(this, "Bloqueo de terminal activo", Toast.LENGTH_SHORT).show()
        );

        // Diálogo de confirmación para cierre definitivo de sesión y turno
        findViewById(R.id.btnEndShift).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Cerrar turno")
                .setMessage("¿Estás seguro de que deseas cerrar tu turno y salir?")
                .setPositiveButton("Cerrar turno", (dialog, which) -> {
                    Toast.makeText(this, "Turno cerrado exitosamente", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(PerfilActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
        });
    }

    /**
     * Enrutamiento hacia las demás pantallas del sistema mediante la barra inferior.
     */
    private void setupBottomNav() {
        findViewById(R.id.navItemVenta).setOnClickListener(v -> {
            Intent intent = new Intent(PerfilActivity.this, VentaActivity.class);
            intent.putExtra("USER_NAME", cashierName);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });

        findViewById(R.id.navItemPedidos).setOnClickListener(v -> {
            Intent intent = new Intent(PerfilActivity.this, PedidosActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });

        findViewById(R.id.navItemReportes).setOnClickListener(v ->
            Toast.makeText(this, "Sección Reportes", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.navItemPerfil).setOnClickListener(v -> {
            // Ya se encuentra en la pantalla de perfil
        });
    }
}
