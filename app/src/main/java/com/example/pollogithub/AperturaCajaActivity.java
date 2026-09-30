package com.example.pollogithub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pollogithub.data.entity.TurnoEntity;
import com.example.pollogithub.data.repository.PosRepository;

import java.util.Locale;

/**
 * Registro de fondo inicial para apertura de turno.
 */
public class AperturaCajaActivity extends AppCompatActivity {

    private EditText etFondoInicial;
    private PosRepository repository;

    private int userId;
    private int sucursalId;
    private String userName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_apertura_caja);

        // Compensación de diseño respecto a la barra de estado y navegación del dispositivo
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainApertura), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = PosRepository.getInstance(this);

        userId = getIntent().getIntExtra("USER_ID", repository.getSessionManager().getUserId());
        sucursalId = getIntent().getIntExtra("SUCURSAL_ID", repository.getSessionManager().getSucursalId());
        userName = getIntent().getStringExtra("USER_NAME");
        if (userName == null || userName.isEmpty()) {
            userName = repository.getSessionManager().getUserName();
        }

        TextView tvWelcome = findViewById(R.id.tvAperturaWelcome);
        if (userName != null && !userName.isEmpty()) {
            tvWelcome.setText(String.format("Bienvenido, %s", userName));
        }

        etFondoInicial = findViewById(R.id.etFondoInicial);

        // Atajos de montos rápidos
        findViewById(R.id.btnQuickFondo50).setOnClickListener(v -> etFondoInicial.setText("50.00"));
        findViewById(R.id.btnQuickFondo100).setOnClickListener(v -> etFondoInicial.setText("100.00"));
        findViewById(R.id.btnQuickFondo200).setOnClickListener(v -> etFondoInicial.setText("200.00"));
        findViewById(R.id.btnQuickFondo300).setOnClickListener(v -> etFondoInicial.setText("300.00"));

        findViewById(R.id.btnAbrirTurno).setOnClickListener(v -> {
            String montoStr = etFondoInicial.getText().toString().trim();
            if (montoStr.isEmpty()) {
                etFondoInicial.setError(getString(R.string.error_ingresar_fondo_inicial));
                return;
            }

            double fondo;
            try {
                fondo = Double.parseDouble(montoStr);
                if (fondo < 0) {
                    etFondoInicial.setError(getString(R.string.error_monto_negativo));
                    return;
                }
            } catch (NumberFormatException e) {
                etFondoInicial.setError(getString(R.string.error_monto_invalido));
                return;
            }

            // 6. Invocación asíncrona del Repositorio para asentar el turno en SQLite
            final double finalFondo = fondo;
            repository.abrirTurno(finalFondo, userId, sucursalId, new PosRepository.Callback<TurnoEntity>() {
                @Override
                public void onSuccess(TurnoEntity turno) {
                    // Actualización de la sesión compartida con el nuevo ID de turno activo
                    repository.getSessionManager().setTurnoId(turno.getId());
                    Toast.makeText(AperturaCajaActivity.this, R.string.toast_turno_abierto_exito, Toast.LENGTH_SHORT).show();

                    // Navegación hacia el dashboard principal
                    Intent intent = new Intent(AperturaCajaActivity.this, HomeActivity.class);
                    intent.putExtra("USER_NAME", userName);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(AperturaCajaActivity.this, getString(R.string.toast_error_abrir_turno, error), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
