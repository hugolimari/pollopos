package com.example.pollogithub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CierreCajaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cierre_caja);

        ViewCompat.setOnApplyWindowInsetsListener((View) findViewById(R.id.tvTitle).getParent(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        android.widget.EditText etConteo = findViewById(R.id.etConteo);
        com.example.pollogithub.data.repository.PosRepository repo = com.example.pollogithub.data.repository.PosRepository.getInstance(this);

        findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            String conteoStr = etConteo != null ? etConteo.getText().toString().trim() : "0";
            double contado = 0.0;
            try {
                contado = Double.parseDouble(conteoStr);
            } catch (Exception ignored) {}

            int turnoId = repo.getSessionManager().getTurnoId();
            repo.cerrarTurno(turnoId, contado, new com.example.pollogithub.data.repository.PosRepository.Callback<com.example.pollogithub.data.entity.TurnoEntity>() {
                @Override
                public void onSuccess(com.example.pollogithub.data.entity.TurnoEntity result) {
                    Toast.makeText(CierreCajaActivity.this, "Turno cerrado exitosamente", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CierreCajaActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(CierreCajaActivity.this, "Turno cerrado", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CierreCajaActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        });
    }
}
