package com.example.pollogithub.util;

import com.example.pollogithub.data.entity.PedidoDetalleEntity;
import com.example.pollogithub.data.entity.PedidoEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Generador de Comandos ESC/POS y Formateador de Tickets Térmicos: EscPosTicketBuilder
 * 
 * Capa de Infraestructura / Controladores de Hardware Térmico
 * 
 * Genera secuencias binarias directas de bytes según el estándar internacional ESC/POS
 * compatible con todas las impresoras térmicas de 58mm (32 columnas) y 80mm (48 columnas)
 * vía Bluetooth SPP, USB OTG o Red Ethernet.
 * 
 * Comandos ESC/POS nativos implementados:
 * - ESC @: Inicializar impresora.
 * - ESC a n: Alineación (0=Izq, 1=Centro, 2=Der).
 * - ESC E n: Negrita activada/desactivada.
 * - GS ! n: Tamaño doble / normal de fuente.
 * - GS V m n: Corte de papel automático.
 */
public class EscPosTicketBuilder {

    // Comandos ESC/POS en bytes
    private static final byte[] INIT = new byte[]{0x1B, 0x40};
    private static final byte[] ALIGN_LEFT = new byte[]{0x1B, 0x61, 0x00};
    private static final byte[] ALIGN_CENTER = new byte[]{0x1B, 0x61, 0x01};
    private static final byte[] ALIGN_RIGHT = new byte[]{0x1B, 0x61, 0x02};
    private static final byte[] BOLD_ON = new byte[]{0x1B, 0x45, 0x01};
    private static final byte[] BOLD_OFF = new byte[]{0x1B, 0x45, 0x00};
    private static final byte[] DOUBLE_HEIGHT_WIDTH = new byte[]{0x1D, 0x21, 0x11};
    private static final byte[] DOUBLE_HEIGHT = new byte[]{0x1D, 0x21, 0x01};
    private static final byte[] NORMAL_SIZE = new byte[]{0x1D, 0x21, 0x00};
    private static final byte[] CUT_PAPER = new byte[]{0x1D, 0x56, 0x41, 0x10};
    private static final byte[] FEED_3_LINES = new byte[]{0x1B, 0x64, 0x03};

    private final int lineWidth; // 32 para 58mm, 48 para 80mm

    public EscPosTicketBuilder(boolean is80mm) {
        this.lineWidth = is80mm ? 48 : 32;
    }

    /**
     * Construye los bytes del ticket de venta comercial para el cliente.
     */
    public byte[] buildTicketCliente(
            String nombreNegocio,
            String sucursal,
            int numeroOrden,
            String tipoEntrega,
            List<PedidoDetalleEntity> detalles,
            double subtotal,
            double descuento,
            double total,
            String metodoPago,
            double recibido,
            double vuelto,
            String cajero
    ) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(INIT);

            // 1. Cabecera Centrada
            out.write(ALIGN_CENTER);
            out.write(BOLD_ON);
            out.write(DOUBLE_HEIGHT);
            out.write((nombreNegocio + "\n").getBytes(StandardCharsets.UTF_8));
            out.write(NORMAL_SIZE);
            out.write(BOLD_OFF);
            out.write((sucursal + "\n").getBytes(StandardCharsets.UTF_8));

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            out.write((sdf.format(new Date()) + "\n").getBytes(StandardCharsets.UTF_8));

            String modalidad = "mesa".equalsIgnoreCase(tipoEntrega) || "local".equalsIgnoreCase(tipoEntrega) 
                    ? "CONSUMO EN EL LOCAL" : "PEDIDO PARA LLEVAR";
            out.write(BOLD_ON);
            out.write(("*** " + modalidad + " ***\n").getBytes(StandardCharsets.UTF_8));
            out.write(BOLD_OFF);

            out.write(DOUBLE_HEIGHT);
            out.write(String.format(Locale.getDefault(), "ORDEN #%04d\n", numeroOrden).getBytes(StandardCharsets.UTF_8));
            out.write(NORMAL_SIZE);

            if (cajero != null && !cajero.isEmpty()) {
                out.write(("Cajero: " + cajero + "\n").getBytes(StandardCharsets.UTF_8));
            }

            out.write(getSeparator().getBytes(StandardCharsets.UTF_8));

            // 2. Renglón de Columnas
            out.write(ALIGN_LEFT);
            out.write(formatColumns("CANT. DESCRIPCION", "TOTAL").getBytes(StandardCharsets.UTF_8));
            out.write(getSeparator().getBytes(StandardCharsets.UTF_8));

            // 3. Detalle de Ítems y Modificadores
            if (detalles != null) {
                for (PedidoDetalleEntity d : detalles) {
                    String cantNombre = d.getCantidad() + "x " + d.getNombreProducto();
                    String precioStr = String.format(Locale.getDefault(), "Bs.%.2f", d.getSubtotal());
                    out.write(formatColumns(cantNombre, precioStr).getBytes(StandardCharsets.UTF_8));

                    // Modificadores / notas de cocina resaltadas
                    if (d.getNotas() != null && !d.getNotas().trim().isEmpty()) {
                        out.write(("   > " + d.getNotas().trim() + "\n").getBytes(StandardCharsets.UTF_8));
                    }
                }
            }

            out.write(getSeparator().getBytes(StandardCharsets.UTF_8));

            // 4. Totales y Descuentos
            out.write(ALIGN_LEFT);
            if (descuento > 0) {
                out.write(formatColumns("Subtotal:", String.format(Locale.getDefault(), "Bs. %.2f", subtotal)).getBytes(StandardCharsets.UTF_8));
                out.write(formatColumns("Descuento:", String.format(Locale.getDefault(), "-Bs. %.2f", descuento)).getBytes(StandardCharsets.UTF_8));
            }

            out.write(BOLD_ON);
            out.write(DOUBLE_HEIGHT);
            out.write(formatColumns("TOTAL:", String.format(Locale.getDefault(), "Bs. %.2f", total)).getBytes(StandardCharsets.UTF_8));
            out.write(NORMAL_SIZE);
            out.write(BOLD_OFF);

            out.write(getSeparator().getBytes(StandardCharsets.UTF_8));

            // 5. Forma de Pago
            out.write(formatColumns("Metodo de Pago:", metodoPago).getBytes(StandardCharsets.UTF_8));
            if ("efectivo".equalsIgnoreCase(metodoPago) || "mixto".equalsIgnoreCase(metodoPago)) {
                out.write(formatColumns("Importe Recibido:", String.format(Locale.getDefault(), "Bs. %.2f", recibido)).getBytes(StandardCharsets.UTF_8));
                out.write(formatColumns("Cambio / Vuelto:", String.format(Locale.getDefault(), "Bs. %.2f", vuelto)).getBytes(StandardCharsets.UTF_8));
            }

            // 6. Pie de Página
            out.write(ALIGN_CENTER);
            out.write("\n".getBytes(StandardCharsets.UTF_8));
            out.write("Gracias por su preferencia!\n".getBytes(StandardCharsets.UTF_8));
            out.write("PolloPOS v2.0 - Sistema Gastronomico\n\n".getBytes(StandardCharsets.UTF_8));

            out.write(FEED_3_LINES);
            out.write(CUT_PAPER);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    /**
     * Construye la comanda para el cocinero (sin precios, con notas y platos grandes).
     */
    public byte[] buildComandaCocina(
            int numeroOrden,
            String tipoEntrega,
            List<PedidoDetalleEntity> detalles,
            String cajero
    ) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(INIT);
            out.write(ALIGN_CENTER);
            out.write(BOLD_ON);
            out.write(DOUBLE_HEIGHT_WIDTH);
            out.write("*** COCINA ***\n".getBytes(StandardCharsets.UTF_8));
            out.write(String.format(Locale.getDefault(), "ORDEN #%04d\n", numeroOrden).getBytes(StandardCharsets.UTF_8));
            out.write(NORMAL_SIZE);

            String modalidad = "mesa".equalsIgnoreCase(tipoEntrega) || "local".equalsIgnoreCase(tipoEntrega)
                    ? "CONSUMO EN EL LOCAL" : ">> PARA LLEVAR <<";
            out.write(DOUBLE_HEIGHT);
            out.write((modalidad + "\n").getBytes(StandardCharsets.UTF_8));
            out.write(NORMAL_SIZE);
            out.write(BOLD_OFF);

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss · dd/MM", Locale.getDefault());
            out.write((sdf.format(new Date()) + "\n").getBytes(StandardCharsets.UTF_8));

            out.write(getSeparator().getBytes(StandardCharsets.UTF_8));
            out.write(ALIGN_LEFT);

            if (detalles != null) {
                for (PedidoDetalleEntity d : detalles) {
                    out.write(BOLD_ON);
                    out.write(DOUBLE_HEIGHT);
                    out.write(String.format(Locale.getDefault(), "[ %d ] %s\n", d.getCantidad(), d.getNombreProducto()).getBytes(StandardCharsets.UTF_8));
                    out.write(NORMAL_SIZE);
                    out.write(BOLD_OFF);

                    if (d.getNotas() != null && !d.getNotas().trim().isEmpty()) {
                        out.write(BOLD_ON);
                        out.write(("   *** NOTA: " + d.getNotas().trim() + " ***\n").getBytes(StandardCharsets.UTF_8));
                        out.write(BOLD_OFF);
                    }
                    out.write("\n".getBytes(StandardCharsets.UTF_8));
                }
            }

            out.write(getSeparator().getBytes(StandardCharsets.UTF_8));
            out.write(FEED_3_LINES);
            out.write(CUT_PAPER);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    /**
     * Genera una versión en texto plano formateada para compartir por WhatsApp o visualizar.
     */
    public static String buildPlainTextTicket(
            String nombreNegocio,
            int numeroOrden,
            String tipoEntrega,
            List<PedidoDetalleEntity> detalles,
            double subtotal,
            double descuento,
            double total,
            String metodoPago,
            double recibido,
            double vuelto
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("🍗 *").append(nombreNegocio).append("*\n");
        sb.append("📋 *ORDEN #").append(String.format(Locale.getDefault(), "%04d", numeroOrden)).append("*\n");
        String mod = "mesa".equalsIgnoreCase(tipoEntrega) || "local".equalsIgnoreCase(tipoEntrega) 
                ? "En el local" : "Para llevar";
        sb.append("📍 *Modalidad:* ").append(mod).append("\n");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        sb.append("🕒 *Fecha:* ").append(sdf.format(new Date())).append("\n");
        sb.append("--------------------------------\n");

        if (detalles != null) {
            for (PedidoDetalleEntity d : detalles) {
                sb.append(d.getCantidad()).append("× ").append(d.getNombreProducto())
                  .append(" - Bs. ").append(String.format(Locale.getDefault(), "%.2f", d.getSubtotal())).append("\n");
                if (d.getNotas() != null && !d.getNotas().trim().isEmpty()) {
                    sb.append("   _Nota: ").append(d.getNotas().trim()).append("_\n");
                }
            }
        }

        sb.append("--------------------------------\n");
        if (descuento > 0) {
            sb.append("Subtotal: Bs. ").append(String.format(Locale.getDefault(), "%.2f", subtotal)).append("\n");
            sb.append("Descuento: -Bs. ").append(String.format(Locale.getDefault(), "%.2f", descuento)).append("\n");
        }
        sb.append("💰 *TOTAL: Bs. ").append(String.format(Locale.getDefault(), "%.2f", total)).append("*\n");
        sb.append("💳 *Metodo de pago:* ").append(metodoPago).append("\n");
        if ("efectivo".equalsIgnoreCase(metodoPago) || "mixto".equalsIgnoreCase(metodoPago)) {
            sb.append("💵 Recibido: Bs. ").append(String.format(Locale.getDefault(), "%.2f", recibido)).append("\n");
            sb.append("🪙 Vuelto: Bs. ").append(String.format(Locale.getDefault(), "%.2f", vuelto)).append("\n");
        }
        sb.append("\n¡Gracias por su preferencia!\n");
        return sb.toString();
    }

    private String getSeparator() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lineWidth; i++) {
            sb.append("-");
        }
        sb.append("\n");
        return sb.toString();
    }

    private String formatColumns(String left, String right) {
        int leftLen = left.length();
        int rightLen = right.length();
        if (leftLen + rightLen >= lineWidth) {
            int maxLeft = lineWidth - rightLen - 1;
            if (maxLeft > 0 && leftLen > maxLeft) {
                left = left.substring(0, maxLeft);
            }
        }
        int spaces = lineWidth - left.length() - right.length();
        if (spaces < 1) spaces = 1;
        StringBuilder sb = new StringBuilder();
        sb.append(left);
        for (int i = 0; i < spaces; i++) {
            sb.append(" ");
        }
        sb.append(right).append("\n");
        return sb.toString();
    }
}
