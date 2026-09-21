package com.example.pollogithub;

public class Order {
    private String id;
    private String time;
    private String status; // "cocina", "listo", "camino"
    private String items;
    private String type; // "Para mesa", "Para llevar", "Delivery"
    private double total;
    private String primaryActionText;

    public Order(String id, String time, String status, String items, String type, double total, String primaryActionText) {
        this.id = id;
        this.time = time;
        this.status = status;
        this.items = items;
        this.type = type;
        this.total = total;
        this.primaryActionText = primaryActionText;
    }

    public String getId() { return id; }
    public String getTime() { return time; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getItems() { return items; }
    public String getType() { return type; }
    public double getTotal() { return total; }
    public String getPrimaryActionText() { return primaryActionText; }
    public void setPrimaryActionText(String primaryActionText) { this.primaryActionText = primaryActionText; }
}