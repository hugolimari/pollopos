package com.example.pollogithub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReciboActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recibo);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRecibo), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String paymentMethod = getIntent().getStringExtra("PAYMENT_METHOD");
        if (paymentMethod == null) paymentMethod = "Efectivo";

        double totalAmount = getIntent().getDoubleExtra("TOTAL_AMOUNT", 41.40);
        double receivedAmount = getIntent().getDoubleExtra("RECEIVED_AMOUNT", 50.00);
        double changeDue = getIntent().getDoubleExtra("CHANGE_DUE", 8.60);

        TextView tvSubtitleReceipt = findViewById(R.id.tvSubtitleReceipt);
        tvSubtitleReceipt.setText(String.format("Pedido #0231 cobrado en %s", paymentMethod.toLowerCase(Locale.getDefault())));

        TextView tvReceiptMeta = findViewById(R.id.tvReceiptMeta);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault());
        String currentDateTime = sdf.format(new Date());
        tvReceiptMeta.setText(String.format("Sucursal Centro · %s", currentDateTime));

        TextView tvReceiptSubtotal = findViewById(R.id.tvReceiptSubtotal);
        TextView tvReceiptDiscount = findViewById(R.id.tvReceiptDiscount);
        TextView tvReceiptGrandTotal = findViewById(R.id.tvReceiptGrandTotal);
        TextView tvReceiptAmountReceived = findViewById(R.id.tvReceiptAmountReceived);
        TextView tvReceiptChange = findViewById(R.id.tvReceiptChange);

        double subtotal = totalAmount + 4.60;
        tvReceiptSubtotal.setText(String.format(Locale.getDefault(), "Bs. %.2f", subtotal));
        tvReceiptDiscount.setText("− Bs. 4.60");
        tvReceiptGrandTotal.setText(String.format(Locale.getDefault(), "Bs. %.2f", totalAmount));
        tvReceiptAmountReceived.setText(String.format(Locale.getDefault(), "Bs. %.2f", receivedAmount));
        tvReceiptChange.setText(String.format(Locale.getDefault(), "Bs. %.2f", changeDue));

        findViewById(R.id.btnPrintReceipt).setOnClickListener(v ->
            Toast.makeText(this, "Enviando ticket a la impresora...", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.btnWhatsappReceipt).setOnClickListener(v ->
            Toast.makeText(this, "Enviando comprobante por WhatsApp...", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.btnNewSale).setOnClickListener(v -> {
            Intent intent = new Intent(ReciboActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}