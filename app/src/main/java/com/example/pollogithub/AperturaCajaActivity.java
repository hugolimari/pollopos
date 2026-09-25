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
 * Controlador de Vista: AperturaCajaActivity
 * 
 * Capa de Presentación / Módulo de Arqueo y Control Operativo
 * Hereda de: AppCompatActivity
 * 
 * Implementa el protocolo de inicio de operaciones del cajero.
 * Exige el registro formal del fondo inicial o cambio base de caja antes de habilitar
 * el terminal de ventas, garantizando la trazabilidad contable para el posterior arqueo.
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Validación Defensiva de Entradas: Verificación de no nulidad, formato numérico de punto flotante
 *   y restricción de dominio para valores no negativos (fondo >= 0).
 * - Persistencia de Contexto Operativo: Actualización atómica del identificador de turno ('turnoId')
 *   en el SessionManager tras la respuesta asíncrona favorable del Repositorio.
 * - Limpieza de Pila de Actividades (Back Stack Management): Utilización de banderas 'FLAG_ACTIVITY_CLEAR_TOP'
 *   para evitar que el usuario regrese a la pantalla de apertura mediante el botón atrás del sistema operativo.
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

        // 1. Obtención de la instancia del Repositorio Central
        repository = PosRepository.getInstance(this);

        // 2. Recuperación de parámetros de sesión pasados por Intent o persistidos en SessionManager
        userId = getIntent().getIntExtra("USER_ID", repository.getSessionManager().getUserId());
        sucursalId = getIntent().getIntExtra("SUCURSAL_ID", repository.getSessionManager().getSucursalId());
        userName = getIntent().getStringExtra("USER_NAME");
        if (userName == null || userName.isEmpty()) {
            userName = repository.getSessionManager().getUserName();
        }

        // 3. Renderizado del saludo personalizado al cajero
        TextView tvWelcome = findViewById(R.id.tvAperturaWelcome);
        if (userName != null && !userName.isEmpty()) {
            tvWelcome.setText(String.format("Bienvenido, %s", userName));
        }

        etFondoInicial = findViewById(R.id.etFondoInicial);

        // 4. Atajos de Montos Rápidos (Facilidad de uso y UX en pantallas táctiles POS)
        findViewById(R.id.btnQuickFondo50).setOnClickListener(v -> etFondoInicial.setText("50.00"));
        findViewById(R.id.btnQuickFondo100).setOnClickListener(v -> etFondoInicial.setText("100.00"));
        findViewById(R.id.btnQuickFondo200).setOnClickListener(v -> etFondoInicial.setText("200.00"));
        findViewById(R.id.btnQuickFondo300).setOnClickListener(v -> etFondoInicial.setText("300.00"));

        // 5. Botón de Confirmación y Validación de Apertura
        findViewById(R.id.btnAbrirTurno).setOnClickListener(v -> {
            String montoStr = etFondoInicial.getText().toString().trim();
            if (montoStr.isEmpty()) {
                etFondoInicial.setError("Ingresa el fondo inicial");
                return;
            }

            double fondo;
            try {
                fondo = Double.parseDouble(montoStr);
                if (fondo < 0) {
                    etFondoInicial.setError("El monto no puede ser negativo");
                    return;
                }
            } catch (NumberFormatException e) {
                etFondoInicial.setError("Monto inválido");
                return;
            }

            // 6. Invocación asíncrona del Repositorio para asentar el turno en SQLite
            final double finalFondo = fondo;
            repository.abrirTurno(finalFondo, userId, sucursalId, new PosRepository.Callback<TurnoEntity>() {
                @Override
                public void onSuccess(TurnoEntity turno) {
                    // Actualización de la sesión compartida con el nuevo ID de turno activo
                    repository.getSessionManager().setTurnoId(turno.getId());
                    Toast.makeText(AperturaCajaActivity.this, "Turno abierto exitosamente", Toast.LENGTH_SHORT).show();

                    // Navegación hacia el dashboard principal
                    Intent intent = new Intent(AperturaCajaActivity.this, HomeActivity.class);
                    intent.putExtra("USER_NAME", userName);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(AperturaCajaActivity.this, "Error al abrir turno: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
