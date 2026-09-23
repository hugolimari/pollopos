package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pagos")
public class PagoEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int pedidoId;
    private int turnoId;
    private String metodoPago; // "efectivo", "tarjeta", "qr", "mixto"
    private double monto;
    private double montoRecibido;
    private double vuelto;
    private String referencia;
    private long creadoEn;

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
