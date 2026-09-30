package com.example.pollogithub;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pollogithub.data.entity.SucursalEntity;
import com.example.pollogithub.data.entity.TurnoEntity;
import com.example.pollogithub.data.repository.PosRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Pantalla de cierre de turno y arqueo de caja.
 */
public class CierreCajaActivity extends AppCompatActivity {

    private PosRepository repo;
    private double efectivoEsperado = 0.0;
    private int turnoId = 0;

    // Componentes para desglose de métricas contables
    private TextView tvSubtitle;
    private TextView tvResumenSucursalFecha;
    private TextView tvResumenEfectivo;
    private TextView tvResumenTarjeta;
    private TextView tvResumenQr;
    private TextView tvResumenTotalVendido;
    private TextView tvResumenFondoInicial;
    private TextView tvResumenIngresosExtra;
    private TextView tvResumenEgresosGastos;
    private TextView tvResumenEsperado;

    // Componentes de retroalimentación de arqueo físico
    private CardView cardAlert;
    private TextView tvAlertTitulo;
    private TextView tvAlertMonto;
    private TextView tvAlertDescripcion;
    private EditText etConteo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cierre_caja);

        // Ajuste de insets de ventana para barras del sistema usando el id del contenedor raíz
        View mainView = findViewById(R.id.mainCierre);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        repo = PosRepository.getInstance(this);

        // Control de navegación hacia atrás
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Enlace de vistas de métricas contables
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvResumenSucursalFecha = findViewById(R.id.tvResumenSucursalFecha);
        tvResumenEfectivo = findViewById(R.id.tvResumenEfectivo);
        tvResumenTarjeta = findViewById(R.id.tvResumenTarjeta);
        tvResumenQr = findViewById(R.id.tvResumenQr);
        tvResumenTotalVendido = findViewById(R.id.tvResumenTotalVendido);
        tvResumenFondoInicial = findViewById(R.id.tvResumenFondoInicial);
        tvResumenIngresosExtra = findViewById(R.id.tvResumenIngresosExtra);
        tvResumenEgresosGastos = findViewById(R.id.tvResumenEgresosGastos);
        tvResumenEsperado = findViewById(R.id.tvResumenEsperado);

        cardAlert = findViewById(R.id.cardAlert);
        tvAlertTitulo = findViewById(R.id.tvAlertTitulo);
        tvAlertMonto = findViewById(R.id.tvAlertMonto);
        tvAlertDescripcion = findViewById(R.id.tvAlertDescripcion);
        etConteo = findViewById(R.id.etConteo);

        // Restricción a máximo 2 decimales en el arqueo físico
        etConteo.setFilters(new InputFilter[]{new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                String replacement = source.subSequence(start, end).toString();
                String newVal = dest.subSequence(0, dstart).toString() + replacement + dest.subSequence(dend, dest.length()).toString();
                if (newVal.isEmpty()) return null;
                // Permite enteros o números con hasta 2 cifras decimales (usando punto o coma)
                if (!newVal.matches("^\\d*([.,]\\d{0,2})?$")) {
                    return "";
                }
                return null;
            }
        }});

        // Formateo de fecha del reporte
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String fecha = sdf.format(new Date());

        // 1. Sucursal dinámica desde base de datos
        repo.getSucursalActiva(new PosRepository.Callback<SucursalEntity>() {
            @Override
            public void onSuccess(SucursalEntity s) {
                String nombreSucursal = (s != null && s.getNombre() != null && !s.getNombre().isEmpty())
                        ? s.getNombre()
                        : getString(R.string.etiqueta_sucursal_centro);
                tvResumenSucursalFecha.setText(String.format("%s · %s", nombreSucursal, fecha));
            }

            @Override
            public void onError(String error) {
                tvResumenSucursalFecha.setText(String.format("%s · %s", getString(R.string.etiqueta_sucursal_centro), fecha));
            }
        });

        // 2. Subtítulo dinámico con nombre del cajero y horario del turno
        tvSubtitle = findViewById(R.id.tvSubtitle);
        String currentUserName = repo.getSessionManager().getUserName();
        final String displayName = (currentUserName != null && !currentUserName.isEmpty()) ? currentUserName : "Cajero";
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

        turnoId = repo.getSessionManager().getTurnoId();

        // 3. Resolución reactiva del turno abierto desde la base de datos
        repo.getTurnoActivo(new PosRepository.Callback<TurnoEntity>() {
            @Override
            public void onSuccess(TurnoEntity turno) {
                if (turno != null) {
                    turnoId = turno.getId();
                    repo.getSessionManager().setTurnoId(turnoId);

                    String horaApertura = (turno.getAbiertoEn() > 0)
                            ? timeFormat.format(new Date(turno.getAbiertoEn()))
                            : timeFormat.format(new Date());
                    String horaActual = timeFormat.format(new Date());
                    if (tvSubtitle != null) {
                        tvSubtitle.setText(String.format("Turno de %s · %s - %s", displayName, horaApertura, horaActual));
                    }
                    cargarResumenTurno(turnoId);
                } else if (turnoId > 0) {
                    if (tvSubtitle != null) {
                        tvSubtitle.setText(String.format("Turno de %s", displayName));
                    }
                    cargarResumenTurno(turnoId);
                } else {
                    if (tvSubtitle != null) {
                        tvSubtitle.setText(String.format("Turno de %s · Sin turno activo", displayName));
                    }
                    Toast.makeText(CierreCajaActivity.this, "No se detectó ningún turno de caja abierto", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                if (tvSubtitle != null) {
                    tvSubtitle.setText(String.format("Turno de %s", displayName));
                }
                if (turnoId > 0) {
                    cargarResumenTurno(turnoId);
                }
            }
        });

        // 4. Observador en tiempo real de digitación para el arqueo físico
        etConteo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                actualizarDiferencia();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 5. Confirmación formal y persistencia del cierre de turno
        findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            if (turnoId <= 0) {
                Toast.makeText(CierreCajaActivity.this, R.string.error_turno_no_encontrado, Toast.LENGTH_SHORT).show();
                return;
            }
            String conteoStr = etConteo.getText().toString().trim().replace(',', '.');
            double contado = 0.0;
            try {
                if (!conteoStr.isEmpty()) {
                    contado = Double.parseDouble(conteoStr);
                }
            } catch (Exception ignored) {}

            final double finalContado = contado;
            repo.cerrarTurno(turnoId, finalContado, new PosRepository.Callback<TurnoEntity>() {
                @Override
                public void onSuccess(TurnoEntity result) {
                    // Reseteo del turno activo en almacenamiento local
                    repo.getSessionManager().setTurnoId(0);
                    Toast.makeText(CierreCajaActivity.this, R.string.toast_turno_cerrado_exito, Toast.LENGTH_SHORT).show();

                    // Reenrutamiento a la pantalla inicial limpiando el historial de navegación
                    Intent intent = new Intent(CierreCajaActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(CierreCajaActivity.this, getString(R.string.toast_error_cerrar_turno, error != null ? error : ""), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Carga asíncrona de los datos contables del turno desde la base de datos local.
     */
    private void cargarResumenTurno(int id) {
        if (id <= 0) return;
        repo.getResumenTurno(id, new PosRepository.Callback<PosRepository.ResumenTurno>() {
            @Override
            public void onSuccess(PosRepository.ResumenTurno r) {
                efectivoEsperado = r.esperado;

                tvResumenEfectivo.setText(String.format(Locale.getDefault(), "Bs. %.2f", r.totalEfectivo));
                tvResumenTarjeta.setText(String.format(Locale.getDefault(), "Bs. %.2f", r.totalTarjeta));
                tvResumenQr.setText(String.format(Locale.getDefault(), "Bs. %.2f", r.totalQr));
                tvResumenTotalVendido.setText(String.format(Locale.getDefault(), "Bs. %.2f", r.totalVentas));
                tvResumenFondoInicial.setText(String.format(Locale.getDefault(), "Bs. %.2f", r.fondoInicial));
                if (tvResumenIngresosExtra != null) {
                    tvResumenIngresosExtra.setText(String.format(Locale.getDefault(), "Bs. %.2f", r.totalIngresosExtra));
                }
                if (tvResumenEgresosGastos != null) {
                    tvResumenEgresosGastos.setText(String.format(Locale.getDefault(), "Bs. %.2f", r.totalEgresosGastos));
                }
                tvResumenEsperado.setText(String.format(Locale.getDefault(), "Bs. %.2f", r.esperado));

                actualizarDiferencia();
            }

            @Override
            public void onError(String error) {}
        });
    }

    /**
     * Algoritmo de evaluación de arqueo contable.
     * Compara el efectivo físico ingresado contra el monto esperado del sistema,
     * adaptando visualmente la tarjeta de alerta informativa.
     */
    private void actualizarDiferencia() {
        String inputStr = etConteo.getText().toString().trim().replace(',', '.');
        double contado = 0.0;
        try {
            if (!inputStr.isEmpty()) {
                contado = Double.parseDouble(inputStr);
            }
        } catch (NumberFormatException ignored) {}

        double diff = contado - efectivoEsperado;

        if (diff < -0.01) {
            // Caso: Faltante de dinero en gaveta (Alerta crítica - Rojo)
            cardAlert.setCardBackgroundColor(Color.parseColor("#FFCDD2"));
            tvAlertTitulo.setText(R.string.cierre_alerta_faltante_titulo);
            tvAlertTitulo.setTextColor(Color.parseColor("#D32F2F"));
            String montoStr = String.format(Locale.getDefault(), "%.2f", Math.abs(diff));
            tvAlertMonto.setText(String.format(Locale.getDefault(), "- Bs. %s", montoStr));
            tvAlertDescripcion.setText(getString(R.string.cierre_alerta_faltante_desc, montoStr));
        } else if (diff > 0.01) {
            // Caso: Sobrante de dinero en gaveta (Alerta informativa - Verde)
            cardAlert.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            tvAlertTitulo.setText(R.string.cierre_alerta_sobrante_titulo);
            tvAlertTitulo.setTextColor(ContextCompat.getColor(this, R.color.ok_600));
            String montoStr = String.format(Locale.getDefault(), "%.2f", diff);
            tvAlertMonto.setText(String.format(Locale.getDefault(), "+ Bs. %s", montoStr));
            tvAlertDescripcion.setText(getString(R.string.cierre_alerta_sobrante_desc, montoStr));
        } else {
            // Caso: Conciliación perfecta (Caja Cuadrada)
            cardAlert.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            tvAlertTitulo.setText(R.string.cierre_alerta_exacta_titulo);
            tvAlertTitulo.setTextColor(ContextCompat.getColor(this, R.color.ok_600));
            tvAlertMonto.setText(R.string.cero_moneda);
            tvAlertDescripcion.setText(R.string.cierre_alerta_exacta_desc);
        }
    }
}
