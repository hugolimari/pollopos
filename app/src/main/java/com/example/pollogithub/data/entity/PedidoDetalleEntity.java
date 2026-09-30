package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para la tabla 'pedido_detalles'.
 */
@Entity(tableName = "pedido_detalles")
public class PedidoDetalleEntity {

    /**
     * Identificador autoincremental de la línea de detalle (Clave Primaria).
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    /**
     * Clave foránea que referencia a la orden maestra (tabla 'pedidos').
     */
    private int pedidoId;

    /**
     * Clave foránea que referencia al artículo del catálogo (tabla 'productos').
     */
    private int productoId;

    /**
     * Instantánea textual (snapshot) del nombre del producto al momento de emitirse el pedido.
     */
    private String nombreProducto;

    /**
     * Cantidad discreta de unidades solicitadas (debe ser un entero positivo).
     */
    private int cantidad;

    /**
     * Tarifa monetaria por unidad aplicada al instante de registrarse la venta.
     */
    private double precioUnitario;

    /**
     * Producto del precio unitario por la cantidad: subtotal = precioUnitario * cantidad.
     */
    private double subtotal;

    /**
     * Modificadores, especificaciones o preferencias culinarias para el área de cocina (ej. "sin cebolla").
     */
    private String notas;

    /**
     * Constructor para la creación de una línea de pedido con sus valores calculados.
     * 
     * @param pedidoId       Clave foránea de la orden cabecera.
     * @param productoId     Clave foránea del producto.
     * @param nombreProducto Nombre descriptivo congelado.
     * @param cantidad       Número de unidades.
     * @param precioUnitario Precio de venta por unidad.
     * @param subtotal       Importe agregado por este renglón.
     * @param notas          Instrucciones para comanda/cocina.
     */
    public PedidoDetalleEntity(int pedidoId, int productoId, String nombreProducto, int cantidad, double precioUnitario, double subtotal, String notas) {
        this.pedidoId = pedidoId;
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = subtotal;
        this.notas = notas;
    }

    // ==========================================
    // MÉTODOS DE ACCESO (GETTERS Y SETTERS)
    // ==========================================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPedidoId() { return pedidoId; }
    public void setPedidoId(int pedidoId) { this.pedidoId = pedidoId; }

    public int getProductoId() { return productoId; }
    public void setProductoId(int productoId) { this.productoId = productoId; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}
