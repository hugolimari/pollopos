package com.example.pollogithub;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pollogithub.data.entity.PedidoDetalleEntity;
import com.example.pollogithub.data.repository.PosRepository;
import com.example.pollogithub.util.EscPosTicketBuilder;
import com.example.pollogithub.util.ThermalPrinterManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de Vista: ReciboActivity (Comprobante Fiscal / Recibo Digital)
 * 
 * Capa de Presentación / Módulo de Facturación e Impresión Térmica
 * Hereda de: AppCompatActivity
 * 
 * Despliega la confirmación visual de la venta concretada y cobrada con renderizado dinámico
 * de ítems reales, notas de preparación, desglose de descuentos y soporte para
 * impresión térmica ESC/POS por Bluetooth y contingencia a PrintManager del sistema.
 */
public class ReciboActivity extends AppCompatActivity {

    private PosRepository repository;
    private ThermalPrinterManager printerManager;
    private List<PedidoDetalleEntity> detallesList = new ArrayList<>();

    private int pedidoId;
    private int orderNumber;
    private String paymentMethod;
    private String tipoEntrega;
    private double totalAmount;
    private double receivedAmount;
    private double changeDue;
    private double discountAmount;
    private String cashierName = "Cajero";

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

        repository = PosRepository.getInstance(this);
        printerManager = new ThermalPrinterManager(this);

        // 1. Extracción de parámetros transportados mediante Intent
        pedidoId = getIntent().getIntExtra("PEDIDO_ID", 1);
        orderNumber = getIntent().getIntExtra("ORDER_NUMBER", 231);
        paymentMethod = getIntent().getStringExtra("PAYMENT_METHOD");
        if (paymentMethod == null) paymentMethod = "Efectivo";

        tipoEntrega = getIntent().getStringExtra("TIPO_ENTREGA");
        if (tipoEntrega == null) tipoEntrega = "mesa";

        totalAmount = getIntent().getDoubleExtra("TOTAL_AMOUNT", 41.40);
        receivedAmount = getIntent().getDoubleExtra("RECEIVED_AMOUNT", 50.00);
        changeDue = getIntent().getDoubleExtra("CHANGE_DUE", 8.60);
        discountAmount = getIntent().getDoubleExtra("DISCOUNT_AMOUNT", 0.0);

        cashierName = repository.getSessionManager().getUserName();
        if (cashierName == null || cashierName.isEmpty()) cashierName = "Carlos";

        // 2. Renderizado de cabecera y metadata
        TextView tvSubtitleReceipt = findViewById(R.id.tvSubtitleReceipt);
        tvSubtitleReceipt.setText(String.format(Locale.getDefault(), "Pedido #%04d cobrado en %s", orderNumber, paymentMethod.toLowerCase(Locale.getDefault())));

        TextView tvReceiptMeta = findViewById(R.id.tvReceiptMeta);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault());
        String currentDateTime = sdf.format(new Date());
        tvReceiptMeta.setText(String.format("Sucursal Centro · %s", currentDateTime));

        TextView tvReceiptOrderNum = findViewById(R.id.tvReceiptOrderNum);
        if (tvReceiptOrderNum != null) {
            tvReceiptOrderNum.setText(String.format(Locale.getDefault(), "#%04d", orderNumber));
        }

        TextView tvReceiptOrderType = findViewById(R.id.tvReceiptOrderType);
        if (tvReceiptOrderType != null) {
            boolean isLocal = "mesa".equalsIgnoreCase(tipoEntrega) || "local".equalsIgnoreCase(tipoEntrega);
            tvReceiptOrderType.setText(isLocal ? "En el local" : "Para llevar");
        }

        TextView tvReceiptCashier = findViewById(R.id.tvReceiptCashier);
        if (tvReceiptCashier != null) {
            tvReceiptCashier.setText(cashierName);
        }

        // 3. Renderizado de renglones contables
        TextView tvReceiptSubtotal = findViewById(R.id.tvReceiptSubtotal);
        TextView tvReceiptDiscount = findViewById(R.id.tvReceiptDiscount);
        TextView tvReceiptGrandTotal = findViewById(R.id.tvReceiptGrandTotal);
        TextView tvReceiptAmountReceived = findViewById(R.id.tvReceiptAmountReceived);
        TextView tvReceiptChange = findViewById(R.id.tvReceiptChange);

        double subtotal = totalAmount + discountAmount;
        tvReceiptSubtotal.setText(String.format(Locale.getDefault(), "Bs. %.2f", subtotal));
        tvReceiptDiscount.setText(String.format(Locale.getDefault(), "− Bs. %.2f", discountAmount));
        tvReceiptGrandTotal.setText(String.format(Locale.getDefault(), "Bs. %.2f", totalAmount));
        tvReceiptAmountReceived.setText(String.format(Locale.getDefault(), "Bs. %.2f", receivedAmount));
        tvReceiptChange.setText(String.format(Locale.getDefault(), "Bs. %.2f", changeDue));

        // 4. Carga dinámica de ítems desde Room SQLite
        cargarDetallesPedido();

        // 5. Botón de Impresión Térmica
        findViewById(R.id.btnPrintReceipt).setOnClickListener(v -> mostrarOpcionesImpresion());

        // 6. Botón de Compartir Comprobante (WhatsApp / Digital)
        findViewById(R.id.btnWhatsappReceipt).setOnClickListener(v -> compartirComprobante());

        // 7. Retorno al panel de venta
        findViewById(R.id.btnNewSale).setOnClickListener(v -> {
            Intent intent = new Intent(ReciboActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Carga asíncrona de los renglones persistidos del pedido y armado visual dinámico.
     */
    private void cargarDetallesPedido() {
        LinearLayout layoutReceiptItems = findViewById(R.id.layoutReceiptItems);
        if (layoutReceiptItems == null) return;

        repository.getPedidoDetalles(pedidoId, new PosRepository.Callback<List<PedidoDetalleEntity>>() {
            @Override
            public void onSuccess(List<PedidoDetalleEntity> detalles) {
                detallesList = detalles != null ? detalles : new ArrayList<>();
                layoutReceiptItems.removeAllViews();

                if (detallesList.isEmpty()) {
                    TextView tvVacio = new TextView(ReciboActivity.this);
                    tvVacio.setText("Sin ítems registrados");
                    tvVacio.setTextColor(ContextCompat.getColor(ReciboActivity.this, R.color.char_400));
                    layoutReceiptItems.addView(tvVacio);
                    return;
                }

                for (PedidoDetalleEntity d : detallesList) {
                    LinearLayout rowContainer = new LinearLayout(ReciboActivity.this);
                    rowContainer.setOrientation(LinearLayout.VERTICAL);
                    rowContainer.setPadding(0, 0, 0, 16);

                    RelativeLayout itemRow = new RelativeLayout(ReciboActivity.this);

                    TextView tvNombre = new TextView(ReciboActivity.this);
                    tvNombre.setText(String.format(Locale.getDefault(), "%d×  %s", d.getCantidad(), d.getNombreProducto()));
                    tvNombre.setTextColor(ContextCompat.getColor(ReciboActivity.this, R.color.char_700));
                    tvNombre.setTextSize(12.5f);
                    tvNombre.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

                    RelativeLayout.LayoutParams lpNombre = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                    lpNombre.addRule(RelativeLayout.ALIGN_PARENT_START);
                    itemRow.addView(tvNombre, lpNombre);

                    TextView tvPrecio = new TextView(ReciboActivity.this);
                    tvPrecio.setText(String.format(Locale.getDefault(), "Bs. %.2f", d.getSubtotal()));
                    tvPrecio.setTextColor(ContextCompat.getColor(ReciboActivity.this, R.color.char_900));
                    tvPrecio.setTextSize(12.5f);
                    tvPrecio.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));

                    RelativeLayout.LayoutParams lpPrecio = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                    lpPrecio.addRule(RelativeLayout.ALIGN_PARENT_END);
                    itemRow.addView(tvPrecio, lpPrecio);

                    rowContainer.addView(itemRow);

                    // Si presenta notas o modificadores culinarios, renderizarlos debajo
                    if (d.getNotas() != null && !d.getNotas().trim().isEmpty()) {
                        TextView tvNota = new TextView(ReciboActivity.this);
                        tvNota.setText(String.format(Locale.getDefault(), "   > Nota: %s", d.getNotas().trim()));
                        tvNota.setTextColor(ContextCompat.getColor(ReciboActivity.this, R.color.ember_700));
                        tvNota.setTextSize(11.5f);
                        tvNota.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
                        rowContainer.addView(tvNota);
                    }

                    layoutReceiptItems.addView(rowContainer);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ReciboActivity.this, "Error al cargar detalles: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Despliega las alternativas de impresión térmica Bluetooth o impresora del sistema.
     */
    private void mostrarOpcionesImpresion() {
        String[] opciones = new String[]{
                "Imprimir Ticket Térmico (Bluetooth ESC/POS)",
                "Imprimir Comanda de Cocina (Bluetooth)",
                "Impresora del Sistema / Guardar PDF",
                "Configurar / Cambiar Impresora Térmica"
        };

        new AlertDialog.Builder(this)
                .setTitle("Opciones de Impresión")
                .setItems(opciones, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            ejecutarImpresionBluetooth(false);
                            break;
                        case 1:
                            ejecutarImpresionBluetooth(true);
                            break;
                        case 2:
                            imprimirConSistemaAndroid();
                            break;
                        case 3:
                            printerManager.showPrinterSelectionDialog(this, () -> {
                                Toast.makeText(this, "Impresora configurada exitosamente", Toast.LENGTH_SHORT).show();
                            });
                            break;
                    }
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    /**
     * Genera los comandos ESC/POS y los transmite por Bluetooth.
     */
    private void ejecutarImpresionBluetooth(boolean esComandaCocina) {
        if (printerManager.getSavedPrinterMac() == null) {
            printerManager.showPrinterSelectionDialog(this, () -> ejecutarImpresionBluetooth(esComandaCocina));
            return;
        }

        EscPosTicketBuilder builder = new EscPosTicketBuilder(printerManager.is80mm());
        byte[] bytes;

        if (esComandaCocina) {
            bytes = builder.buildComandaCocina(orderNumber, tipoEntrega, detallesList, cashierName);
        } else {
            double subtotal = totalAmount + discountAmount;
            bytes = builder.buildTicketCliente(
                    "POLLOS EL SABOR DEL CHEF",
                    "Sucursal Centro",
                    orderNumber,
                    tipoEntrega,
                    detallesList,
                    subtotal,
                    discountAmount,
                    totalAmount,
                    paymentMethod,
                    receivedAmount,
                    changeDue,
                    cashierName
            );
        }

        Toast.makeText(this, "Transmitiendo a impresora térmica...", Toast.LENGTH_SHORT).show();

        printerManager.printEscPosBytes(bytes, new ThermalPrinterManager.PrintCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(ReciboActivity.this, "Ticket impreso correctamente", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                new AlertDialog.Builder(ReciboActivity.this)
                        .setTitle("Error de Impresión")
                        .setMessage(error + "\n\n¿Deseas imprimir usando el servicio del sistema Android o configurar otra impresora?")
                        .setPositiveButton("Imprimir con Sistema", (d, w) -> imprimirConSistemaAndroid())
                        .setNeutralButton("Configurar Impresora", (d, w) -> printerManager.showPrinterSelectionDialog(ReciboActivity.this, null))
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        });
    }

    /**
     * Contingencia nativa Android: renderiza un comprobante HTML para cualquier impresora WiFi/USB o exportación PDF.
     */
    private void imprimirConSistemaAndroid() {
        StringBuilder sbHtml = new StringBuilder();
        sbHtml.append("<html><body style='font-family:sans-serif; padding:12px;'>");
        sbHtml.append("<div style='text-align:center;'>");
        sbHtml.append("<h2>POLLOS EL SABOR DEL CHEF</h2>");
        sbHtml.append("<p>Sucursal Centro</p>");
        sbHtml.append("<h3>ORDEN #").append(String.format(Locale.getDefault(), "%04d", orderNumber)).append("</h3>");
        String mod = "mesa".equalsIgnoreCase(tipoEntrega) || "local".equalsIgnoreCase(tipoEntrega) ? "En el local" : "Para llevar";
        sbHtml.append("<p><b>").append(mod).append("</b> · Cajero: ").append(cashierName).append("</p>");
        sbHtml.append("</div><hr/>");

        sbHtml.append("<table style='width:100%; font-size:13px;'>");
        for (PedidoDetalleEntity d : detallesList) {
            sbHtml.append("<tr>");
            sbHtml.append("<td>").append(d.getCantidad()).append("x ").append(d.getNombreProducto());
            if (d.getNotas() != null && !d.getNotas().isEmpty()) {
                sbHtml.append("<br/><small style='color:#C2410C;'><i>").append(d.getNotas()).append("</i></small>");
            }
            sbHtml.append("</td>");
            sbHtml.append("<td style='text-align:right;'>Bs. ").append(String.format(Locale.getDefault(), "%.2f", d.getSubtotal())).append("</td>");
            sbHtml.append("</tr>");
        }
        sbHtml.append("</table><hr/>");

        double subtotal = totalAmount + discountAmount;
        if (discountAmount > 0) {
            sbHtml.append("<p>Subtotal: Bs. ").append(String.format(Locale.getDefault(), "%.2f", subtotal)).append("</p>");
            sbHtml.append("<p>Descuento: -Bs. ").append(String.format(Locale.getDefault(), "%.2f", discountAmount)).append("</p>");
        }
        sbHtml.append("<h3>TOTAL: Bs. ").append(String.format(Locale.getDefault(), "%.2f", totalAmount)).append("</h3>");
        sbHtml.append("<p>Pago: ").append(paymentMethod).append(" | Recibido: Bs. ").append(String.format(Locale.getDefault(), "%.2f", receivedAmount))
                .append(" | Vuelto: Bs. ").append(String.format(Locale.getDefault(), "%.2f", changeDue)).append("</p>");
        sbHtml.append("<p style='text-align:center; margin-top:20px;'><small>¡Gracias por su compra!<br/>PolloPOS v2.0</small></p>");
        sbHtml.append("</body></html>");

        printerManager.printViaSystemPrintManager(this, sbHtml.toString(), "Recibo_" + orderNumber);
    }

    /**
     * Comparte el comprobante como texto formateado hacia WhatsApp u otras aplicaciones instaladas.
     */
    private void compartirComprobante() {
        double subtotal = totalAmount + discountAmount;
        String textTicket = EscPosTicketBuilder.buildPlainTextTicket(
                "POLLOS EL SABOR DEL CHEF",
                orderNumber,
                tipoEntrega,
                detallesList,
                subtotal,
                discountAmount,
                totalAmount,
                paymentMethod,
                receivedAmount,
                changeDue
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Comprobante de Pedido #" + orderNumber);
        shareIntent.putExtra(Intent.EXTRA_TEXT, textTicket);
        startActivity(Intent.createChooser(shareIntent, "Enviar comprobante por:"));
    }
}