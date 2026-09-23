package com.example.pollogithub;

/**
 * Modelo de Transferencia de Datos de Pedido: Order
 * 
 * Capa de Presentación / Data Transfer Object (DTO)
 * 
 * Representa una orden de comanda estructurada específicamente para su consumo
 * en la interfaz gráfica del módulo de cocina y pedidos (PedidosFragment / PedidosActivity).
 * Agrupa la información de cabecera con el detalle resumido de ítems y las etiquetas
 * de acción contextual requeridas por el adaptador.
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Patrón DTO (Data Transfer Object): Aísla los requerimientos de representación visual
 *   (fechas formateadas, textos de botones dinámicos) de la entidad de persistencia relacional 'PedidoEntity'.
 * - Abstracción de Interfaz de Usuario: Simplifica el enlace de datos (Data Binding) para la vista KDS.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
public class Order {

    /**
     * Identificador primario en la base de datos local (clave foránea para operaciones).
     */
    private int pedidoId;

    /**
     * Identificador formateado visible para el usuario (ej. "Pedido #0231").
     */
    private String id;

    /**
     * Marca horaria formateada para presentación (ej. "12:45 PM").
     */
    private String time;

    /**
     * Estado logístico de preparación ("cocina", "listo", "camino").
     */
    private String status;

    /**
     * Cadena textual con el resumen de productos solicitados (ej. "2× 1/4 Pollo, 1× Gaseosa").
     */
    private String items;

    /**
     * Modalidad de consumo formateada: "Para mesa", "Para llevar".
     */
    private String type;

    /**
     * Importe monetario total del pedido.
     */
    private double total;

    /**
     * Texto dinámico del botón de acción según la máquina de estados ("Marcar listo" / "Entregado").
     */
    private String primaryActionText;

    /**
     * Constructor completo del modelo de presentación de orden.
     */
    public Order(int pedidoId, String id, String time, String status, String items, String type, double total, String primaryActionText) {
        this.pedidoId = pedidoId;
        this.id = id;
        this.time = time;
        this.status = status;
        this.items = items;
        this.type = type;
        this.total = total;
        this.primaryActionText = primaryActionText;
    }

    /**
     * Constructor sobrecargado con inicialización predeterminada de identificador interno.
     */
    public Order(String id, String time, String status, String items, String type, double total, String primaryActionText) {
        this(0, id, time, status, items, type, total, primaryActionText);
    }

    // ==========================================
    // MÉTODOS ACCESORES (GETTERS Y SETTERS)
    // ==========================================

    public int getPedidoId() { return pedidoId; }
    public void setPedidoId(int pedidoId) { this.pedidoId = pedidoId; }

    public String getId() { return id; }
    public String getTime() { return time; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getItems() { return items; }
    public void setItems(String items) { this.items = items; }
    public String getType() { return type; }
    public double getTotal() { return total; }
    public String getPrimaryActionText() { return primaryActionText; }
    public void setPrimaryActionText(String primaryActionText) { this.primaryActionText = primaryActionText; }
}