package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad de Persistencia: PagoEntity
 * 
 * Capa de Datos / Modelo Financiero y Transaccional (Room ORM)
 * Tabla: "pagos"
 * 
 * Modela el registro de transacciones monetarias efectuadas para liquidar pedidos.
 * Constituye una pieza angular en el subsistema de auditoría financiera y cuadre de caja,
 * vinculando los ingresos monetarios con un pedido específico y un turno de cajero activo.
 * 
 * Conceptos de Ingeniería aplicados:
 * - Integridad Transaccional: Registro inmutable de montos liquidados y cambio devuelto.
 * - Trazabilidad y Auditoría: Atributos temporales en milisegundos Epoch ('creadoEn')
 *   y código/referencia para conciliación de medios electrónicos (tarjeta/QR).
 * - Soporte Multimoneda y Multimodal: Canalización de flujos de efectivo, pasarelas bancarias y billeteras digitales.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
@Entity(tableName = "pagos")
public class PagoEntity {

    /**
     * Identificador unívoco del comprobante de pago (Clave Primaria).
     * Autogenerado por el motor de persistencia.
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    /**
     * Clave foránea que referencia al pedido liquidado (tabla 'pedidos').
     */
    private int pedidoId;

    /**
     * Clave foránea que asocia la recaudación al turno de caja abierto (tabla 'turnos').
     * Vital para la consolidación del arqueo y cálculo del monto esperado al cierre.
     */
    private int turnoId;

    /**
     * Modalidad de pago utilizada: "efectivo", "tarjeta", "qr", "mixto".
     */
    private String metodoPago;

    /**
     * Monto neto exigido y cobrado de la orden de venta (expresado en moneda local).
     */
    private double monto;

    /**
     * Monto nominal entregado por el cliente (aplica principalmente para efectivo).
     */
    private double montoRecibido;

    /**
     * Diferencia devuelta al cliente: vuelto = montoRecibido - monto.
     */
    private double vuelto;

    /**
     * Identificador o comprobante de transacción digital (e.g. código de autorización POS o ID QR).
     */
    private String referencia;

    /**
     * Marca de tiempo UTC (milisegundos desde Unix Epoch) para análisis forense y reportería.
     */
    private long creadoEn;

    /**
     * Constructor completo para persistencia de la transacción de cobro.
     * 
     * @param pedidoId       Identificador del pedido asociado.
     * @param turnoId        Identificador del turno operativo de caja.
     * @param metodoPago     Instrumento monetario de liquidación.
     * @param monto          Importe total a pagar.
     * @param montoRecibido  Efectivo/valor entregado por el consumidor.
     * @param vuelto         Monto diferencial a devolver.
     * @param referencia     Código de autorización o nota de control.
     * @param creadoEn       Timestamp de ejecución del pago.
     */
    public PagoEntity(int pedidoId, int turnoId, String metodoPago, double monto, double montoRecibido, double vuelto, String referencia, long creadoEn) {
        this.pedidoId = pedidoId;
        this.turnoId = turnoId;
        this.metodoPago = metodoPago;
        this.monto = monto;
        this.montoRecibido = montoRecibido;
        this.vuelto = vuelto;
        this.referencia = referencia;
        this.creadoEn = creadoEn;
    }

    // ==========================================
    // MÉTODOS DE ACCESO (GETTERS Y SETTERS)
    // ==========================================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPedidoId() { return pedidoId; }
    public void setPedidoId(int pedidoId) { this.pedidoId = pedidoId; }

    public int getTurnoId() { return turnoId; }
    public void setTurnoId(int turnoId) { this.turnoId = turnoId; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public double getMontoRecibido() { return montoRecibido; }
    public void setMontoRecibido(double montoRecibido) { this.montoRecibido = montoRecibido; }

    public double getVuelto() { return vuelto; }
    public void setVuelto(double vuelto) { this.vuelto = vuelto; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public long getCreadoEn() { return creadoEn; }
    public void setCreadoEn(long creadoEn) { this.creadoEn = creadoEn; }
}
