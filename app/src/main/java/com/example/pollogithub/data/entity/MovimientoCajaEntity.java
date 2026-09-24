package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad de Persistencia: MovimientoCajaEntity
 * 
 * Capa de Datos / Control de Caja Chica y Egresos Operativos (Room ORM)
 * Tabla: "movimientos_caja"
 * 
 * Modela los ingresos adicionales y las salidas/gastos de efectivo (compra de insumos,
 * carbón, hielo, verduras de urgencia, pagos de flete) realizados durante el turno de caja.
 * Es crucial para que el arqueo de caja final cuadre con exactitud.
 */
@Entity(tableName = "movimientos_caja")
public class MovimientoCajaEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int turnoId;

    /**
     * Tipo de movimiento: "EGRESO" (gasto o retiro) o "INGRESO" (inyección de efectivo).
     */
    private String tipo;

    private double monto;

    /**
     * Motivo o justificación del gasto (e.g., "Compra de carbón", "Hielo para bebidas").
     */
    private String concepto;

    private long creadoEn;

    public MovimientoCajaEntity(int turnoId, String tipo, double monto, String concepto, long creadoEn) {
        this.turnoId = turnoId;
        this.tipo = tipo;
        this.monto = monto;
        this.concepto = concepto;
        this.creadoEn = creadoEn;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTurnoId() {
        return turnoId;
    }

    public void setTurnoId(int turnoId) {
        this.turnoId = turnoId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public long getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(long creadoEn) {
        this.creadoEn = creadoEn;
    }
}
