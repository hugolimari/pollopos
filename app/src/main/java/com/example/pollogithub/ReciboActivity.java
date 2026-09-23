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

/**
 * Controlador de Vista: ReciboActivity (Comprobante Fiscal / Recibo Digital)
 * 
 * Capa de Presentación / Módulo de Facturación y Emisión de Comprobantes
 * Hereda de: AppCompatActivity
 * 
 * Despliega la confirmación visual de la venta concretada y cobrada.
 * Muestra el desglose del total, importe entregado, vuelto, fecha/hora y número
 * correlativo de orden, facilitando además la impresión en ticketera térmica
 * o el envío digital mediante aplicaciones de mensajería (WhatsApp).
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Despliegue de Comprobante Contable: Integración de metadata de la transacción
 *   (número de orden, medio de pago, fecha formateada, desglose monetario).
 * - Ciclo de Navegación 'New Sale': Redirección al panel principal (HomeActivity)
 *   empleando 'FLAG_ACTIVITY_CLEAR_TOP' para iniciar una nueva venta con el carrito limpio.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
public class ReciboActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recibo);

        // Compensación de insets de ventana respecto a las barras de sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRecibo), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Extracción de parámetros contables transportados mediante Intent
        String paymentMethod = getIntent().getStringExtra("PAYMENT_METHOD");
        if (paymentMethod == null) paymentMethod = "Efectivo";

        double totalAmount = getIntent().getDoubleExtra("TOTAL_AMOUNT", 41.40);
        double receivedAmount = getIntent().getDoubleExtra("RECEIVED_AMOUNT", 50.00);
        double changeDue = getIntent().getDoubleExtra("CHANGE_DUE", 8.60);

        int orderNumber = getIntent().getIntExtra("ORDER_NUMBER", 231);

        // 2. Renderizado de cabecera y metadata del comprobante
        TextView tvSubtitleReceipt = findViewById(R.id.tvSubtitleReceipt);
        tvSubtitleReceipt.setText(String.format(Locale.getDefault(), "Pedido #%04d cobrado en %s", orderNumber, paymentMethod.toLowerCase(Locale.getDefault())));

        TextView tvReceiptMeta = findViewById(R.id.tvReceiptMeta);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault());
        String currentDateTime = sdf.format(new Date());
        tvReceiptMeta.setText(String.format("Sucursal Centro · %s", currentDateTime));

        // 3. Renderizado de renglones contables (Subtotal, Descuento, Total, Recibido, Cambio)
        TextView tvReceiptSubtotal = findViewById(R.id.tvReceiptSubtotal);
        TextView tvReceiptDiscount = findViewById(R.id.tvReceiptDiscount);
        TextView tvReceiptGrandTotal = findViewById(R.id.tvReceiptGrandTotal);
        TextView tvReceiptAmountReceived = findViewById(R.id.tvReceiptAmountReceived);
        TextView tvReceiptChange = findViewById(R.id.tvReceiptChange);

        tvReceiptSubtotal.setText(String.format(Locale.getDefault(), "Bs. %.2f", totalAmount));
        tvReceiptDiscount.setText("− Bs. 0.00");
        tvReceiptGrandTotal.setText(String.format(Locale.getDefault(), "Bs. %.2f", totalAmount));
        tvReceiptAmountReceived.setText(String.format(Locale.getDefault(), "Bs. %.2f", receivedAmount));
        tvReceiptChange.setText(String.format(Locale.getDefault(), "Bs. %.2f", changeDue));

        // 4. Integraciones de salida de comprobante (Impresora física y canal digital)
        findViewById(R.id.btnPrintReceipt).setOnClickListener(v ->
            Toast.makeText(this, "Enviando ticket a la impresora...", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.btnWhatsappReceipt).setOnClickListener(v ->
            Toast.makeText(this, "Enviando comprobante por WhatsApp...", Toast.LENGTH_SHORT).show()
        );

        // 5. Finalización de flujo y retorno al panel de venta
        findViewById(R.id.btnNewSale).setOnClickListener(v -> {
            Intent intent = new Intent(ReciboActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}