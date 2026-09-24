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
import androidx.core.content.ContextCompat;
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
 * Orquesta el proceso de liquidación y cobranza de una orden de venta con:
 * - Motor de descuentos y promociones en vivo (0%, 5%, 10%, 15%, 20%, Cortesía).
 * - Pago en efectivo con calculadora inteligente de cambio y botones de billetes.
 * - Pago mixto real con desglose editable de parte en efectivo y parte digital (QR/Tarjeta).
 * - Pago con Tarjeta y QR bancario.
 */
public class PagoActivity extends AppCompatActivity {

    private double originalTotal = 41.40;
    private double totalAmount = 41.40;
    private double discountAmount = 0.0;
    private int selectedMethodIndex = 0; // 0: Efectivo, 1: Tarjeta, 2: QR, 3: Mixto

    private TextView tvTotalAmount;
    private TextView tvOriginalSubtotal;

    // Métodos de pago
    private LinearLayout methodEfectivo, methodTarjeta, methodQR, methodMixto;
    private View iconEfectivo, iconTarjeta, iconQR, iconMixto;

    // Panel de Efectivo
    private LinearLayout cashPanel;
    private EditText etAmountReceived;
    private TextView tvChangeDue;

    // Panel Mixto
    private LinearLayout panelMixto;
    private EditText etMixtoEfectivo;
    private EditText etMixtoDigital;
    private TextView tvMixtoStatus;

    // Chips de Descuento
    private TextView[] discountChips;

    private int pedidoId;
    private int orderNumber;
    private String tipoEntrega;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pago);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainPago), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Obtención de parámetros del pedido
        pedidoId = getIntent().getIntExtra("PEDIDO_ID", 1);
        orderNumber = getIntent().getIntExtra("ORDER_NUMBER", 231);
        originalTotal = getIntent().getDoubleExtra("TOTAL_AMOUNT", 41.40);
        totalAmount = originalTotal;
        tipoEntrega = getIntent().getStringExtra("TIPO_ENTREGA");

        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvOriginalSubtotal = findViewById(R.id.tvOriginalSubtotal);
        actualizarPantallaTotal();

        // 2. Enlace de vistas
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

        panelMixto = findViewById(R.id.panelMixto);
        etMixtoEfectivo = findViewById(R.id.etMixtoEfectivo);
        etMixtoDigital = findViewById(R.id.etMixtoDigital);
        tvMixtoStatus = findViewById(R.id.tvMixtoStatus);

        TextView tvPaymentSubtitle = findViewById(R.id.tvPaymentSubtitle);
        if (tvPaymentSubtitle != null) {
            boolean isLocal = tipoEntrega == null || "mesa".equalsIgnoreCase(tipoEntrega) || "local".equalsIgnoreCase(tipoEntrega);
            tvPaymentSubtitle.setText(String.format(Locale.getDefault(), "Pedido #%04d · %s", orderNumber, isLocal ? "En el local" : "Para llevar"));
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 3. Inicialización de componentes funcionales
        setupDiscountChips();
        setupMethodSelection();
        setupCashCalculator();
        setupMixtoCalculator();

        // 4. Confirmación de Pago
        findViewById(R.id.btnConfirmPayment).setOnClickListener(v -> procesarCobro());
    }

    /**
     * Actualiza la etiqueta de precio y la visualización de subtotal/descuento.
     */
    private void actualizarPantallaTotal() {
        if (tvTotalAmount != null) {
            tvTotalAmount.setText(String.format(Locale.getDefault(), "%.2f", totalAmount));
        }
        if (tvOriginalSubtotal != null) {
            if (discountAmount > 0) {
                tvOriginalSubtotal.setVisibility(View.VISIBLE);
                tvOriginalSubtotal.setText(String.format(Locale.getDefault(), "Subtotal: Bs. %.2f  (Descuento: -Bs. %.2f)", originalTotal, discountAmount));
            } else {
                tvOriginalSubtotal.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Configuración del selector de descuentos y promociones en vivo.
     */
    private void setupDiscountChips() {
        TextView chipDesc0 = findViewById(R.id.chipDesc0);
        TextView chipDesc5 = findViewById(R.id.chipDesc5);
        TextView chipDesc10 = findViewById(R.id.chipDesc10);
        TextView chipDesc15 = findViewById(R.id.chipDesc15);
        TextView chipDesc20 = findViewById(R.id.chipDesc20);
        TextView chipDescCortesia = findViewById(R.id.chipDescCortesia);

        discountChips = new TextView[]{chipDesc0, chipDesc5, chipDesc10, chipDesc15, chipDesc20, chipDescCortesia};
        final double[] percentValues = new double[]{0.0, 0.05, 0.10, 0.15, 0.20, 1.00};

        for (int i = 0; i < discountChips.length; i++) {
            final int index = i;
            discountChips[index].setOnClickListener(v -> {
                for (int j = 0; j < discountChips.length; j++) {
                    if (j == index) {
                        discountChips[j].setBackgroundResource(R.drawable.bg_chip_selected);
                        discountChips[j].setTextColor(getColor(R.color.white));
                    } else {
                        discountChips[j].setBackgroundResource(R.drawable.bg_chip_unselected);
                        discountChips[j].setTextColor(getColor(R.color.char_700));
                    }
                }

                double rate = percentValues[index];
                discountAmount = originalTotal * rate;
                totalAmount = Math.max(0.0, originalTotal - discountAmount);

                actualizarPantallaTotal();
                calculateChange();
                recalcularMixtoPorDefecto();
            });
        }
    }

    /**
     * Alterna entre instrumentos monetarios (Efectivo, Tarjeta, QR, Mixto).
     */
    private void setupMethodSelection() {
        methodEfectivo.setOnClickListener(v -> selectMethod(0));
        methodTarjeta.setOnClickListener(v -> selectMethod(1));
        methodQR.setOnClickListener(v -> selectMethod(2));
        methodMixto.setOnClickListener(v -> selectMethod(3));
    }

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

        // Control de visibilidad de paneles especializados
        if (index == 0) {
            cashPanel.setVisibility(View.VISIBLE);
            panelMixto.setVisibility(View.GONE);
            calculateChange();
        } else if (index == 3) {
            cashPanel.setVisibility(View.GONE);
            panelMixto.setVisibility(View.VISIBLE);
            recalcularMixtoPorDefecto();
        } else {
            cashPanel.setVisibility(View.GONE);
            panelMixto.setVisibility(View.GONE);
        }
    }

    /**
     * Calculadora interactiva de efectivo y vuelto.
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

        findViewById(R.id.btnQuick20).setOnClickListener(v -> sumarBillete(20));
        findViewById(R.id.btnQuick50).setOnClickListener(v -> sumarBillete(50));
        findViewById(R.id.btnQuick100).setOnClickListener(v -> sumarBillete(100));
        findViewById(R.id.btnQuick200).setOnClickListener(v -> sumarBillete(200));

        etAmountReceived.setText(String.format(Locale.getDefault(), "%.2f", totalAmount));
        calculateChange();
    }

    private void sumarBillete(double billete) {
        String inputStr = etAmountReceived.getText().toString().trim();
        double actual = 0.0;
        try {
            if (!inputStr.isEmpty()) actual = Double.parseDouble(inputStr);
        } catch (NumberFormatException ignored) {}

        if (actual < totalAmount) {
            etAmountReceived.setText(String.format(Locale.getDefault(), "%.2f", billete));
        } else {
            etAmountReceived.setText(String.format(Locale.getDefault(), "%.2f", actual + billete));
        }
    }

    private void calculateChange() {
        String inputStr = etAmountReceived.getText().toString().trim();
        double received = 0.0;
        try {
            if (!inputStr.isEmpty()) received = Double.parseDouble(inputStr);
        } catch (NumberFormatException ignored) {}

        double change = received - totalAmount;
        if (change < 0) {
            tvChangeDue.setText(String.format(Locale.getDefault(), "Faltan Bs. %.2f", Math.abs(change)));
            tvChangeDue.setTextColor(ContextCompat.getColor(this, R.color.ember_600));
        } else {
            tvChangeDue.setText(String.format(Locale.getDefault(), "Bs. %.2f", change));
            tvChangeDue.setTextColor(ContextCompat.getColor(this, R.color.ok_600));
        }
    }

    /**
     * Calculadora y validador de Cobro Mixto.
     */
    private void setupMixtoCalculator() {
        TextWatcher mixtoWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validarMixto();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etMixtoEfectivo.addTextChangedListener(mixtoWatcher);
        etMixtoDigital.addTextChangedListener(mixtoWatcher);
    }

    private void recalcularMixtoPorDefecto() {
        if (totalAmount <= 0) {
            etMixtoEfectivo.setText("0.00");
            etMixtoDigital.setText("0.00");
            validarMixto();
            return;
        }

        double mitadEfectivo = Math.floor(totalAmount / 2.0);
        double restoDigital = totalAmount - mitadEfectivo;

        etMixtoEfectivo.setText(String.format(Locale.getDefault(), "%.2f", mitadEfectivo));
        etMixtoDigital.setText(String.format(Locale.getDefault(), "%.2f", restoDigital));
        validarMixto();
    }

    private boolean validarMixto() {
        double ef = 0.0;
        double dig = 0.0;
        try {
            String sEf = etMixtoEfectivo.getText().toString().trim();
            if (!sEf.isEmpty()) ef = Double.parseDouble(sEf);
        } catch (NumberFormatException ignored) {}

        try {
            String sDig = etMixtoDigital.getText().toString().trim();
            if (!sDig.isEmpty()) dig = Double.parseDouble(sDig);
        } catch (NumberFormatException ignored) {}

        double suma = ef + dig;
        double diff = suma - totalAmount;

        if (Math.abs(diff) < 0.01) {
            tvMixtoStatus.setText(String.format(Locale.getDefault(), "Total cubierto con éxito (Bs. %.2f)", suma));
            tvMixtoStatus.setTextColor(ContextCompat.getColor(this, R.color.ok_600));
            return true;
        } else if (diff < 0) {
            tvMixtoStatus.setText(String.format(Locale.getDefault(), "Faltan Bs. %.2f para cubrir el total", Math.abs(diff)));
            tvMixtoStatus.setTextColor(ContextCompat.getColor(this, R.color.ember_600));
            return false;
        } else {
            tvMixtoStatus.setText(String.format(Locale.getDefault(), "Excede por Bs. %.2f", diff));
            tvMixtoStatus.setTextColor(ContextCompat.getColor(this, R.color.char_700));
            return true;
        }
    }

    /**
     * Valida y procesa la transacción financiera definitiva.
     */
    private void procesarCobro() {
        String methodStr = selectedMethodIndex == 0 ? "Efectivo" :
                           selectedMethodIndex == 1 ? "Tarjeta" :
                           selectedMethodIndex == 2 ? "QR" : "Mixto";

        double received = 0.0;
        double change = 0.0;
        String referencia = "";

        if (selectedMethodIndex == 0) { // Efectivo
            String inputStr = etAmountReceived.getText().toString().trim();
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
            change = Math.max(0.0, received - totalAmount);

        } else if (selectedMethodIndex == 3) { // Mixto
            if (!validarMixto()) {
                Toast.makeText(this, "El desglose de pago mixto no cubre el total de la orden", Toast.LENGTH_SHORT).show();
                return;
            }
            double ef = 0.0, dig = 0.0;
            try {
                ef = Double.parseDouble(etMixtoEfectivo.getText().toString().trim());
            } catch (Exception ignored) {}
            try {
                dig = Double.parseDouble(etMixtoDigital.getText().toString().trim());
            } catch (Exception ignored) {}

            received = ef + dig;
            change = Math.max(0.0, received - totalAmount);
            referencia = String.format(Locale.getDefault(), "Efectivo: Bs. %.2f | Digital: Bs. %.2f", ef, dig);

        } else { // Tarjeta o QR
            received = totalAmount;
            change = 0.0;
        }

        final double finalReceived = received;
        final double finalChange = change;
        final String finalReferencia = referencia;

        PosRepository repo = PosRepository.getInstance(PagoActivity.this);

        // Si se aplicó descuento, persistirlo en PedidoEntity
        if (discountAmount > 0) {
            repo.aplicarDescuentoPedido(pedidoId, discountAmount, null);
        }

        // Asentar pago en la base de datos
        repo.registrarPago(pedidoId, methodStr, totalAmount, finalReceived, finalChange, finalReferencia, new PosRepository.Callback<PagoEntity>() {
            @Override
            public void onSuccess(PagoEntity result) {
                Intent intent = new Intent(PagoActivity.this, ReciboActivity.class);
                intent.putExtra("PAYMENT_METHOD", methodStr);
                intent.putExtra("TOTAL_AMOUNT", totalAmount);
                intent.putExtra("RECEIVED_AMOUNT", finalReceived);
                intent.putExtra("CHANGE_DUE", finalChange);
                intent.putExtra("DISCOUNT_AMOUNT", discountAmount);
                intent.putExtra("PEDIDO_ID", pedidoId);
                intent.putExtra("ORDER_NUMBER", orderNumber);
                intent.putExtra("TIPO_ENTREGA", tipoEntrega);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(PagoActivity.this, "Error al registrar pago: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}