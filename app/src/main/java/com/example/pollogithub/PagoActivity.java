package com.example.pollogithub;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pollogithub.data.entity.PagoEntity;
import com.example.pollogithub.data.repository.PosRepository;

import java.util.Locale;

/**
 * Controlador de Vista: PagoActivity (Módulo de Cobranza y Liquidación)
 * 
 * Capa de Presentación / Pasarela de Pago Interna del POS
 * Hereda de: AppCompatActivity
 * 
 * Orquesta el proceso de liquidación y cobranza de una orden de venta.
 * Soporta múltiples modalidades de pago: Efectivo (con calculadora de cambio y vuelto),
 * Tarjeta de débito/crédito, Pago móvil por código QR y Pago Mixto.
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Algoritmo de Cálculo de Cambio Contable:
 *     * vuelto = montoRecibido - totalVenta
 *     * Validación de Precondición de Solvencia: montoRecibido >= totalVenta en transacciones de efectivo.
 * - Transaccionalidad ACID en Persistencia: La confirmación del pago invoca de forma atómica
 *   la inserción de la entidad PagoEntity y la actualización del estado de cobro en PedidoEntity ('pagado').
 * - Experiencia de Usuario Táctil (POS UX): Botones de acceso directo (Quick Cash Buttons) para montos exactos y denominaciones usuales.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
public class PagoActivity extends AppCompatActivity {

    private double totalAmount = 41.40;
    private int selectedMethodIndex = 0; // 0: Efectivo, 1: Tarjeta, 2: QR, 3: Mixto

    private LinearLayout methodEfectivo, methodTarjeta, methodQR, methodMixto;
    private View iconEfectivo, iconTarjeta, iconQR, iconMixto;
    private LinearLayout cashPanel;
    private EditText etAmountReceived;
    private TextView tvChangeDue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pago);

        // Compensación de insets de ventana para barras de sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainPago), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Obtención de parámetros de la orden a liquidar
        totalAmount = getIntent().getDoubleExtra("TOTAL_AMOUNT", 41.40);

        TextView tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvTotalAmount.setText(String.format(Locale.getDefault(), "%.2f", totalAmount));

        // 2. Enlace de vistas para selección de instrumento monetario
        methodEfectivo = findViewById(R.id.methodEfectivo);
        methodTarjeta = findViewById(R.id.methodTarjeta);
        methodQR = findViewById(R.id.methodQR);
        methodMixto = findViewById(R.id.methodMixto);

        iconEfectivo = findViewById(R.id.iconEfectivo);
        iconTarjeta = findViewById(R.id.iconTarjeta);
        iconQR = findViewById(R.id.iconQR);
        iconMixto = findViewById(R.id.iconMixto);

        cashPanel = findViewById(R.id.cashPanel);
        etAmountReceived = findViewById(R.id.etAmountReceived);
        tvChangeDue = findViewById(R.id.tvChangeDue);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 3. Configuración de lógica de interacción y calculadora de vuelto
        setupMethodSelection();
        setupCashCalculator();

        int pedidoId = getIntent().getIntExtra("PEDIDO_ID", 1);
        int orderNumber = getIntent().getIntExtra("ORDER_NUMBER", 231);

        // 4. Confirmación de liquidación financiera
        findViewById(R.id.btnConfirmPayment).setOnClickListener(v -> {
            String methodStr = selectedMethodIndex == 0 ? "Efectivo" :
                               selectedMethodIndex == 1 ? "Tarjeta" :
                               selectedMethodIndex == 2 ? "QR" : "Mixto";

            String inputStr = etAmountReceived.getText().toString().trim();
            double received = 0.0;
            if (selectedMethodIndex == 0) {
                // Validación estricta para pagos en efectivo
                if (inputStr.isEmpty()) {
                    etAmountReceived.setError("Ingresa el monto recibido");
                    return;
                }
                try {
                    received = Double.parseDouble(inputStr);
                } catch (NumberFormatException e) {
                    etAmountReceived.setError("Monto inválido");
                    return;
                }
                if (received < totalAmount) {
                    etAmountReceived.setError(String.format(Locale.getDefault(), "El monto debe ser mínimo Bs. %.2f", totalAmount));
                    return;
                }
            } else {
                // En medios electrónicos (Tarjeta, QR) el importe recibido coincide con el total exigido
                received = totalAmount;
            }

            double change = received - totalAmount;
            if (change < 0) change = 0.0;

            final double finalReceived = received;
            final double finalChange = change;

            // 5. Asentamiento del pago en la base de datos local
            PosRepository.getInstance(PagoActivity.this)
                    .registrarPago(pedidoId, methodStr, totalAmount, finalReceived, finalChange, new PosRepository.Callback<PagoEntity>() {
                        @Override
                        public void onSuccess(PagoEntity result) {
                            // Transición a la pantalla de comprobante / recibo
                            Intent intent = new Intent(PagoActivity.this, ReciboActivity.class);
                            intent.putExtra("PAYMENT_METHOD", methodStr);
                            intent.putExtra("TOTAL_AMOUNT", totalAmount);
                            intent.putExtra("RECEIVED_AMOUNT", selectedMethodIndex == 0 ? finalReceived : totalAmount);
                            intent.putExtra("CHANGE_DUE", selectedMethodIndex == 0 ? finalChange : 0.0);
                            intent.putExtra("PEDIDO_ID", pedidoId);
                            intent.putExtra("ORDER_NUMBER", orderNumber);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(PagoActivity.this, "Error al registrar pago: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    /**
     * Vincula los controladores de eventos a los botones de selección de método de pago.
     */
    private void setupMethodSelection() {
        methodEfectivo.setOnClickListener(v -> selectMethod(0));
        methodTarjeta.setOnClickListener(v -> selectMethod(1));
        methodQR.setOnClickListener(v -> selectMethod(2));
        methodMixto.setOnClickListener(v -> selectMethod(3));
    }

    /**
     * Conmuta los estilos visuales del instrumento seleccionado y gestiona la visibilidad
     * del panel de conteo de efectivo.
     * 
     * @param index Índice del método (0: Efectivo, 1: Tarjeta, 2: QR, 3: Mixto).
     */
    private void selectMethod(int index) {
        selectedMethodIndex = index;

        methodEfectivo.setBackgroundResource(index == 0 ? R.drawable.bg_method_selected : R.drawable.bg_method_unselected);
        iconEfectivo.setBackgroundResource(index == 0 ? R.drawable.bg_method_icon_selected : R.drawable.bg_method_icon_unselected);

        methodTarjeta.setBackgroundResource(index == 1 ? R.drawable.bg_method_selected : R.drawable.bg_method_unselected);
        iconTarjeta.setBackgroundResource(index == 1 ? R.drawable.bg_method_icon_selected : R.drawable.bg_method_icon_unselected);

        methodQR.setBackgroundResource(index == 2 ? R.drawable.bg_method_selected : R.drawable.bg_method_unselected);
        iconQR.setBackgroundResource(index == 2 ? R.drawable.bg_method_icon_selected : R.drawable.bg_method_icon_unselected);

        methodMixto.setBackgroundResource(index == 3 ? R.drawable.bg_method_selected : R.drawable.bg_method_unselected);
        iconMixto.setBackgroundResource(index == 3 ? R.drawable.bg_method_icon_selected : R.drawable.bg_method_icon_unselected);

        // El panel de cálculo de cambio solo es visible en pagos en efectivo
        if (index == 0) {
            cashPanel.setVisibility(View.VISIBLE);
        } else {
            cashPanel.setVisibility(View.GONE);
        }
    }

    /**
     * Configura el listener de texto y los botones rápidos de billetes comunes para agilizar el vuelto.
     */
    private void setupCashCalculator() {
        etAmountReceived.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateChange();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnQuickExact).setOnClickListener(v -> {
            etAmountReceived.setText(String.format(Locale.getDefault(), "%.2f", totalAmount));
        });

        findViewById(R.id.btnQuick45).setOnClickListener(v -> etAmountReceived.setText("45.00"));
        findViewById(R.id.btnQuick50).setOnClickListener(v -> etAmountReceived.setText("50.00"));
        findViewById(R.id.btnQuick100).setOnClickListener(v -> etAmountReceived.setText("100.00"));

        calculateChange();
    }

    /**
     * Computa aritméticamente el diferencial a devolver al cliente:
     * change = max(0, received - totalAmount)
     */
    private void calculateChange() {
        String inputStr = etAmountReceived.getText().toString().trim();
        double received = 0.0;
        try {
            if (!inputStr.isEmpty()) {
                received = Double.parseDouble(inputStr);
            }
        } catch (NumberFormatException ignored) {}

        double change = received - totalAmount;
        if (change < 0) {
            change = 0.0;
        }

        tvChangeDue.setText(String.format(Locale.getDefault(), "Bs. %.2f", change));
    }
}