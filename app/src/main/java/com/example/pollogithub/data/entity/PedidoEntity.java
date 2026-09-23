package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pedidos")
public class PedidoEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int sucursalId;
    private int turnoId;
    private int usuarioId;
    private Integer mesaId;
    private int numeroOrden;
    private String tipoEntrega; // "mesa", "para_llevar"
    private String estado;      // "cocina", "listo", "entregado", "cancelado"
    private String estadoPago;  // "pendiente", "pagado", "cancelado"
    private double subtotal;
    private Integer descuentoId;
    private double montoDescuento;
    private double total;
    private String motivoCancelacion;
    private long creadoEn;

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
