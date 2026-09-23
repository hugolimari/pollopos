package com.example.pollogithub;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
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

import com.example.pollogithub.data.entity.TurnoEntity;
import com.example.pollogithub.data.repository.PosRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CierreCajaActivity extends AppCompatActivity {

    private PosRepository repo;
    private double efectivoEsperado = 0.0;

    private TextView tvResumenSucursalFecha;
    private TextView tvResumenEfectivo;
    private TextView tvResumenTarjeta;
    private TextView tvResumenQr;
    private TextView tvResumenTotalVendido;
    private TextView tvResumenFondoInicial;
    private TextView tvResumenEsperado;

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

        ViewCompat.setOnApplyWindowInsetsListener((View) findViewById(R.id.tvTitle).getParent(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repo = PosRepository.getInstance(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tvResumenSucursalFecha = findViewById(R.id.tvResumenSucursalFecha);
        tvResumenEfectivo = findViewById(R.id.tvResumenEfectivo);
        tvResumenTarjeta = findViewById(R.id.tvResumenTarjeta);
        tvResumenQr = findViewById(R.id.tvResumenQr);
        tvResumenTotalVendido = findViewById(R.id.tvResumenTotalVendido);
        tvResumenFondoInicial = findViewById(R.id.tvResumenFondoInicial);
        tvResumenEsperado = findViewById(R.id.tvResumenEsperado);

        cardAlert = findViewById(R.id.cardAlert);
        tvAlertTitulo = findViewById(R.id.tvAlertTitulo);
        tvAlertMonto = findViewById(R.id.tvAlertMonto);
        tvAlertDescripcion = findViewById(R.id.tvAlertDescripcion);
        etConteo = findViewById(R.id.etConteo);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String fecha = sdf.format(new Date());
        tvResumenSucursalFecha.setText(String.format("Sucursal Centro · %s", fecha));

        int turnoId = repo.getSessionManager().getTurnoId();

        repo.getResumenTurno(turnoId, new PosRepository.Callback<PosRepository.ResumenTurno>() {
            @Override
            public void onSuccess(PosRepository.ResumenTurno r) {
                efectivoEsperado = r.esperado;

                tvResumenEfectivo.setText(String.format(Locale.getDefault(), "Bs. %.2f", r.totalEfectivo));
                tvResumenTarjeta.setText(String.format(Locale.getDefault(), "Bs. %.2f", r.totalTarjeta));
                tvResumenQr.setText(String.format(Locale.getDefault(), "Bs. %.2f", r.totalQr));
                tvResumenTotalVendido.setText(String.format(Locale.getDefault(), "Bs. %.2f", r.totalVentas));
                tvResumenFondoInicial.setText(String.format(Locale.getDefault(), "Bs. %.2f", r.fondoInicial));
                tvResumenEsperado.setText(String.format(Locale.getDefault(), "Bs. %.2f", r.esperado));

                actualizarDiferencia();
            }

            @Override
            public void onError(String error) {}
        });

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

        findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            String conteoStr = etConteo.getText().toString().trim();
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
                    repo.getSessionManager().setTurnoId(0);
                    Toast.makeText(CierreCajaActivity.this, "Turno cerrado exitosamente", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CierreCajaActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(CierreCajaActivity.this, "Error al cerrar turno: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void actualizarDiferencia() {
        String inputStr = etConteo.getText().toString().trim();
        double contado = 0.0;
        try {
            if (!inputStr.isEmpty()) {
                contado = Double.parseDouble(inputStr);
            }
        } catch (NumberFormatException ignored) {}

        double diff = contado - efectivoEsperado;

        if (diff < -0.01) {
            // Faltante (Rojo)
            cardAlert.setCardBackgroundColor(Color.parseColor("#FFCDD2"));
            tvAlertTitulo.setText("Faltante en caja");
            tvAlertTitulo.setTextColor(Color.parseColor("#D32F2F"));
            tvAlertMonto.setText(String.format(Locale.getDefault(), "- Bs. %.2f", Math.abs(diff)));
            tvAlertDescripcion.setText(String.format(Locale.getDefault(), "Hay Bs. %.2f menos de lo esperado. Revisa si hubo algún vuelto mal entregado antes de cerrar.", Math.abs(diff)));
        } else if (diff > 0.01) {
            // Sobrante (Verde)
            cardAlert.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            tvAlertTitulo.setText("Sobrante en caja");
            tvAlertTitulo.setTextColor(ContextCompat.getColor(this, R.color.ok_600));
            tvAlertMonto.setText(String.format(Locale.getDefault(), "+ Bs. %.2f", diff));
            tvAlertDescripcion.setText(String.format(Locale.getDefault(), "Hay Bs. %.2f más de lo esperado en la gaveta.", diff));
        } else {
            // Cuadrado
            cardAlert.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            tvAlertTitulo.setText("Caja cuadrada");
            tvAlertTitulo.setTextColor(ContextCompat.getColor(this, R.color.ok_600));
            tvAlertMonto.setText("Bs. 0.00");
            tvAlertDescripcion.setText("El efectivo contado coincide con el monto esperado.");
        }
    }
}
