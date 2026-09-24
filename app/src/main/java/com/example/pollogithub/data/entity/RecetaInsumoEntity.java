package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad de Persistencia: RecetaInsumoEntity
 * 
 * Capa de Datos / Recetas y Conversión de Insumos (Room ORM)
 * Tabla: "receta_insumos"
 * 
 * Vincula un producto comercial (ej. "1/4 de pollo frito") con uno o varios
 * insumos crudos (ej. "Pollo entero crudo: 0.25", "Papas: 0.25 kg").
 * Al vender un producto, el sistema descuenta automáticamente la cantidad requerida.
 */
@Entity(tableName = "receta_insumos")
public class RecetaInsumoEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int productoId;

    private int insumoId;

    /**
     * Cantidad del insumo requerida por unidad vendida del producto.
     * Ej: 0.25 para un cuarto de pollo, 0.50 para medio pollo, 1.0 para un pollo entero.
     */
    private double cantidadRequerida;

    public RecetaInsumoEntity(int productoId, int insumoId, double cantidadRequerida) {
        this.productoId = productoId;
        this.insumoId = insumoId;
        this.cantidadRequerida = cantidadRequerida;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProductoId() {
        return productoId;
    }

    public void setProductoId(int productoId) {
        this.productoId = productoId;
    }

    public int getInsumoId() {
        return insumoId;
    }

    public void setInsumoId(int insumoId) {
        this.insumoId = insumoId;
    }

    public double getCantidadRequerida() {
        return cantidadRequerida;
    }

    public void setCantidadRequerida(double cantidadRequerida) {
        this.cantidadRequerida = cantidadRequerida;
    }
}
