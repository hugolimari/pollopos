package com.example.pollogithub;

public class Order {
    private int pedidoId;
    private String id;
    private String time;
    private String status; // "cocina", "listo"
    private String items;
    private String type; // "Para mesa", "Para llevar"
    private double total;
    private String primaryActionText;

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

    public Order(String id, String time, String status, String items, String type, double total, String primaryActionText) {
        this(0, id, time, status, items, type, total, primaryActionText);
    }

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