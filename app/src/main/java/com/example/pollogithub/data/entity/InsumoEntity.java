package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad de Persistencia: InsumoEntity
 * 
 * Capa de Datos / Control de Inventario e Insumos Crudos (Room ORM)
 * Tabla: "insumos"
 * 
 * Modela los insumos base del restaurante (ej. "Pollo entero crudo", "Papas fritas (kg)",
 * "Gaseosa 500ml", "Aceite para freidora"). Permite el descuento automático de stock
 * según la preparación de platos y el control de mermas.
 */
@Entity(tableName = "insumos")
public class InsumoEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int sucursalId;

    private String nombre;

    /**
     * Unidad de medida física: "unidades", "kg", "litros", "paquetes".
     */
    private String unidadMedida;

    /**
     * Cantidad actualmente disponible en stock.
     */
    private double stockActual;

    /**
     * Nivel mínimo antes de disparar alerta de desabastecimiento.
     */
    private double stockMinimo;

    /**
     * Costo unitario de adquisición para control de rentabilidad.
     */
    private double costoUnitario;

    public InsumoEntity(int sucursalId, String nombre, String unidadMedida, double stockActual, double stockMinimo, double costoUnitario) {
        this.sucursalId = sucursalId;
        this.nombre = nombre;
        this.unidadMedida = unidadMedida;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.costoUnitario = costoUnitario;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSucursalId() {
        return sucursalId;
    }

    public void setSucursalId(int sucursalId) {
        this.sucursalId = sucursalId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public double getStockActual() {
        return stockActual;
    }

    public void setStockActual(double stockActual) {
        this.stockActual = stockActual;
    }

    public double getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(double stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public double getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(double costoUnitario) {
        this.costoUnitario = costoUnitario;
    }
}
