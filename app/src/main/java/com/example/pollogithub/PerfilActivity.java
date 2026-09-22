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

public class PerfilActivity extends AppCompatActivity {

    private String cashierName = "Carlos Méndez";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainPerfil), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView tvProfileName = findViewById(R.id.tvProfileName);
        TextView tvProfileAvatarInitial = findViewById(R.id.tvProfileAvatarInitial);

        String userExtra = getIntent().getStringExtra("USER_NAME");
        if (userExtra != null && !userExtra.trim().isEmpty()) {
            cashierName = userExtra.trim();
        }

        tvProfileName.setText(cashierName);
        String initial = cashierName.substring(0, 1).toUpperCase(Locale.getDefault());
        tvProfileAvatarInitial.setText(initial);

        setupSettingsActions();
        setupBottomNav();
    }

    private void setupSettingsActions() {
        findViewById(R.id.btnSettingPrinter).setOnClickListener(v ->
            Toast.makeText(this, "Impresora Epson TM-T20III lista · Estado OK", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.btnSettingCashCut).setOnClickListener(v ->
            Toast.makeText(this, "Generando arqueo de caja (Corte X)...", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.btnSettingSync).setOnClickListener(v ->
            Toast.makeText(this, "Sincronizando pedidos con la nube...", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.btnSettingLock).setOnClickListener(v ->
            Toast.makeText(this, "Bloqueo de terminal activo", Toast.LENGTH_SHORT).show()
        );

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
            // Already on Perfil
        });
    }
}
