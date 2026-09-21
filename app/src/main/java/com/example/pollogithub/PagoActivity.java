package com.example.pollogithub;

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

import java.util.Locale;

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainPago), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        totalAmount = getIntent().getDoubleExtra("TOTAL_AMOUNT", 41.40);

        TextView tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvTotalAmount.setText(String.format(Locale.getDefault(), "%.2f", totalAmount));

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

        setupMethodSelection();
        setupCashCalculator();

        findViewById(R.id.btnConfirmPayment).setOnClickListener(v -> {
            String methodStr = selectedMethodIndex == 0 ? "Efectivo" :
                               selectedMethodIndex == 1 ? "Tarjeta" :
                               selectedMethodIndex == 2 ? "QR" : "Mixto";
            Toast.makeText(this, "¡Pago con " + methodStr + " confirmado!", Toast.LENGTH_LONG).show();
            finish();
        });
    }

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

        if (index == 0) {
            cashPanel.setVisibility(View.VISIBLE);
        } else {
            cashPanel.setVisibility(View.GONE);
        }
    }

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

        findViewById(R.id.btnQuick45).setOnClickListener(v -> {
            etAmountReceived.setText("45.00");
        });

        findViewById(R.id.btnQuick50).setOnClickListener(v -> {
            etAmountReceived.setText("50.00");
        });

        findViewById(R.id.btnQuick100).setOnClickListener(v -> {
            etAmountReceived.setText("100.00");
        });

        calculateChange();
    }

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

        tvChangeDue.setText(String.format(Locale.getDefault(), "S/ %.2f", change));
    }
}