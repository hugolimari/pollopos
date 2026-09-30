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
 * Pasarela de pago y cobranza (efectivo con cálculo de vuelto, tarjeta, QR y mixto).
 */
public class PagoActivity extends AppCompatActivity {

    private double originalTotal = 0.0;
    private double totalAmount = 0.0;
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
        pedidoId = getIntent().getIntExtra("PEDIDO_ID", 0);
        orderNumber = getIntent().getIntExtra("ORDER_NUMBER", 1);
        originalTotal = getIntent().getDoubleExtra("TOTAL_AMOUNT", 0.0);
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

    // Variable para evitar bucles recursivos en el TextWatcher de Pago Mixto
    private boolean isUpdatingMixto = false;

    private double parseDoubleSafe(String text) {
        if (text == null) return 0.0;
        String clean = text.trim().replace(',', '.');
        if (clean.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void sumarBillete(double billete) {
        double actual = parseDoubleSafe(etAmountReceived.getText().toString());
        if (actual < totalAmount) {
            etAmountReceived.setText(String.format(Locale.US, "%.2f", billete));
        } else {
            etAmountReceived.setText(String.format(Locale.US, "%.2f", actual + billete));
        }
    }

    private void calculateChange() {
        double received = parseDoubleSafe(etAmountReceived.getText().toString());
        double change = received - totalAmount;
        if (change < -0.01) {
            tvChangeDue.setText(String.format(Locale.getDefault(), "Faltan Bs. %.2f", Math.abs(change)));
            tvChangeDue.setTextColor(ContextCompat.getColor(this, R.color.ember_600));
        } else {
            tvChangeDue.setText(String.format(Locale.getDefault(), "Bs. %.2f", Math.max(0.0, change)));
            tvChangeDue.setTextColor(ContextCompat.getColor(this, R.color.ok_600));
        }
    }

    /**
     * Calculadora y validador de Cobro Mixto.
     * Soporta auto-ajuste inteligente del monto restante y tolerancia a comas/puntos decimales.
     */
    private void setupMixtoCalculator() {
        etMixtoEfectivo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUpdatingMixto) return;
                if (etMixtoEfectivo.hasFocus()) {
                    double ef = parseDoubleSafe(s.toString());
                    if (ef >= 0 && ef <= totalAmount) {
                        isUpdatingMixto = true;
                        etMixtoDigital.setText(String.format(Locale.US, "%.2f", Math.max(0.0, totalAmount - ef)));
                        isUpdatingMixto = false;
                    }
                }
                validarMixto();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etMixtoDigital.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUpdatingMixto) return;
                if (etMixtoDigital.hasFocus()) {
                    double dig = parseDoubleSafe(s.toString());
                    if (dig >= 0 && dig <= totalAmount) {
                        isUpdatingMixto = true;
                        etMixtoEfectivo.setText(String.format(Locale.US, "%.2f", Math.max(0.0, totalAmount - dig)));
                        isUpdatingMixto = false;
                    }
                }
                validarMixto();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void recalcularMixtoPorDefecto() {
        isUpdatingMixto = true;
        if (totalAmount <= 0) {
            etMixtoEfectivo.setText("0.00");
            etMixtoDigital.setText("0.00");
            isUpdatingMixto = false;
            validarMixto();
            return;
        }

        double mitadEfectivo = Math.floor(totalAmount / 2.0);
        double restoDigital = totalAmount - mitadEfectivo;

        etMixtoEfectivo.setText(String.format(Locale.US, "%.2f", mitadEfectivo));
        etMixtoDigital.setText(String.format(Locale.US, "%.2f", restoDigital));
        isUpdatingMixto = false;
        validarMixto();
    }

    private boolean validarMixto() {
        double ef = parseDoubleSafe(etMixtoEfectivo.getText().toString());
        double dig = parseDoubleSafe(etMixtoDigital.getText().toString());

        double suma = ef + dig;
        double diff = suma - totalAmount;

        if (Math.abs(diff) < 0.01) {
            tvMixtoStatus.setText(getString(R.string.estado_mixto_cubierto_exito, suma));
            tvMixtoStatus.setTextColor(ContextCompat.getColor(this, R.color.ok_600));
            return true;
        } else if (diff < -0.01) {
            tvMixtoStatus.setText(getString(R.string.estado_mixto_faltante, Math.abs(diff)));
            tvMixtoStatus.setTextColor(ContextCompat.getColor(this, R.color.ember_600));
            return false;
        } else {
            tvMixtoStatus.setText(getString(R.string.estado_mixto_vuelto, diff));
            tvMixtoStatus.setTextColor(ContextCompat.getColor(this, R.color.ok_600));
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
        double efMixto = 0.0;
        double digMixto = 0.0;

        if (selectedMethodIndex == 0) { // Efectivo
            String inputStr = etAmountReceived.getText().toString().trim().replace(',', '.');
            if (inputStr.isEmpty()) {
                etAmountReceived.setError(getString(R.string.error_ingresar_monto_recibido));
                return;
            }
            try {
                received = Double.parseDouble(inputStr);
            } catch (NumberFormatException e) {
                etAmountReceived.setError(getString(R.string.error_monto_invalido));
                return;
            }

            if (received < totalAmount) {
                etAmountReceived.setError(getString(R.string.error_monto_minimo_formato, totalAmount));
                return;
            }
            change = Math.max(0.0, received - totalAmount);

        } else if (selectedMethodIndex == 3) { // Mixto
            if (!validarMixto()) {
                Toast.makeText(this, R.string.toast_pago_mixto_insuficiente, Toast.LENGTH_SHORT).show();
                return;
            }
            efMixto = parseDoubleSafe(etMixtoEfectivo.getText().toString());
            digMixto = parseDoubleSafe(etMixtoDigital.getText().toString());

            received = efMixto + digMixto;
            change = Math.max(0.0, received - totalAmount);
            referencia = String.format(Locale.US, "MIXTO|EF:%.2f|DIG:%.2f (Efectivo: Bs. %.2f, Digital: Bs. %.2f)", efMixto, digMixto, efMixto, digMixto);

        } else { // Tarjeta o QR
            received = totalAmount;
            change = 0.0;
        }

        final double finalReceived = received;
        final double finalChange = change;
        final String finalReferencia = referencia;
        final double finalEfMixto = efMixto;
        final double finalDigMixto = digMixto;

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
                intent.putExtra("PAYMENT_REFERENCE", finalReferencia);
                intent.putExtra("MIXTO_EFECTIVO", finalEfMixto);
                intent.putExtra("MIXTO_DIGITAL", finalDigMixto);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(PagoActivity.this, getString(R.string.toast_error_registrar_pago, error), Toast.LENGTH_SHORT).show();
            }
        });
    }
}