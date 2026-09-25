package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad de Persistencia: PedidoEntity
 * 
 * Capa de Datos / Modelo de Cabecera Transaccional (Room ORM)
 * Tabla: "pedidos"
 * 
 * Entidad central del subsistema transaccional del POS. Representa la orden de venta
 * o comanda, administrando su ciclo de vida integral mediante máquinas de estado finito
 * tanto para la logística de preparación culinaria como para el estado de cobro financiero.
 * 
 * Conceptos de Ingeniería aplicados:
 * - Patrón Maestro-Detalle: Actúa como nodo raíz de los renglones persistidos en 'pedido_detalles'.
 * - Máquina de Estados (State Pattern / FSM):
 *     * Flujo Operativo: "cocina" -> "listo" -> "entregado" (o bifurcación a "cancelado").
 *     * Flujo Financiero: "pendiente" -> "pagado" (o "cancelado").
 * - Integridad y Auditoría: Traza el cajero emisor (usuarioId), el turno de caja (turnoId)
 *   y la sucursal física (sucursalId).
 * - Algoritmo Contable: total = subtotal - montoDescuento.
 */
@Entity(tableName = "pedidos")
public class PedidoEntity {

    /**
     * Identificador primario de la transacción en la base de datos local.
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    /**
     * Clave foránea que asocia la orden a la sucursal de emisión.
     */
    private int sucursalId;

    /**
     * Clave foránea que vincula la venta al turno de caja activo en el momento de creación.
     */
    private int turnoId;

    /**
     * Clave foránea del operador del sistema (cajero o mesero) que originó el pedido.
     */
    private int usuarioId;

    /**
     * Identificador de la mesa asignada. Puede ser nulo en caso de pedidos para llevar.
     */
    private Integer mesaId;

    /**
     * Número correlativo o secuencial diario visible para control operativo del cliente.
     */
    private int numeroOrden;

    /**
     * Modalidad de consumo o despacho: "mesa", "para_llevar".
     */
    private String tipoEntrega;

    /**
     * Estado logístico de la orden: "cocina", "listo", "entregado", "cancelado".
     */
    private String estado;

    /**
     * Estado financiero de liquidación: "pendiente", "pagado", "cancelado".
     */
    private String estadoPago;

    /**
     * Sumatoria aritmética de los subtotales de cada detalle antes de aplicar deducciones.
     */
    private double subtotal;

    /**
     * Referencia opcional a una política de descuento o cupón promocional aplicado.
     */
    private Integer descuentoId;

    /**
     * Importe monetario deducido del subtotal en virtud de promociones o cortesías.
     */
    private double montoDescuento;

    /**
     * Monto neto exigible a pagar: total = subtotal - montoDescuento.
     */
    private double total;

    /**
     * Justificación obligatoria para efectos de auditoría en caso de anulación del pedido.
     */
    private String motivoCancelacion;

    /**
     * Estampa de tiempo (milisegundos Unix) que registra la creación exacta del pedido.
     */
    private long creadoEn;

    /**
     * Constructor completo para persistir la cabecera de la transacción de venta.
     */
    public PedidoEntity(int sucursalId, int turnoId, int usuarioId, Integer mesaId, int numeroOrden,
                        String tipoEntrega, String estado, String estadoPago, double subtotal,
                        Integer descuentoId, double montoDescuento, double total,
                        String motivoCancelacion, long creadoEn) {
        this.sucursalId = sucursalId;
        this.turnoId = turnoId;
        this.usuarioId = usuarioId;
        this.mesaId = mesaId;
        this.numeroOrden = numeroOrden;
        this.tipoEntrega = tipoEntrega;
        this.estado = estado;
        this.estadoPago = estadoPago;
        this.subtotal = subtotal;
        this.descuentoId = descuentoId;
        this.montoDescuento = montoDescuento;
        this.total = total;
        this.motivoCancelacion = motivoCancelacion;
        this.creadoEn = creadoEn;
    }

    // ==========================================
    // MÉTODOS DE ACCESO (GETTERS Y SETTERS)
    // ==========================================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSucursalId() { return sucursalId; }
    public void setSucursalId(int sucursalId) { this.sucursalId = sucursalId; }

    public int getTurnoId() { return turnoId; }
    public void setTurnoId(int turnoId) { this.turnoId = turnoId; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public Integer getMesaId() { return mesaId; }
    public void setMesaId(Integer mesaId) { this.mesaId = mesaId; }

    public int getNumeroOrden() { return numeroOrden; }
    public void setNumeroOrden(int numeroOrden) { this.numeroOrden = numeroOrden; }

    public String getTipoEntrega() { return tipoEntrega; }
    public void setTipoEntrega(String tipoEntrega) { this.tipoEntrega = tipoEntrega; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public Integer getDescuentoId() { return descuentoId; }
    public void setDescuentoId(Integer descuentoId) { this.descuentoId = descuentoId; }

    public double getMontoDescuento() { return montoDescuento; }
    public void setMontoDescuento(double montoDescuento) { this.montoDescuento = montoDescuento; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getMotivoCancelacion() { return motivoCancelacion; }
    public void setMotivoCancelacion(String motivoCancelacion) { this.motivoCancelacion = motivoCancelacion; }

    public long getCreadoEn() { return creadoEn; }
    public void setCreadoEn(long creadoEn) { this.creadoEn = creadoEn; }
}
