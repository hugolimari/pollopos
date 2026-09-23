package com.example.pollogithub;

public class Product {
    private int id;
    private String name;
    private String description;
    private double price;
    private String emoji;
    private String category;
    private int thumbDrawableRes;
    private boolean isAgotado;
    private int quantityInCart;
    private String imagenLocalPath;

    public Product(int id, String name, String description, double price, String emoji, String category, int thumbDrawableRes, boolean isAgotado, int quantityInCart, String imagenLocalPath) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.emoji = emoji;
        this.category = category;
        this.thumbDrawableRes = thumbDrawableRes;
        this.isAgotado = isAgotado;
        this.quantityInCart = quantityInCart;
        this.imagenLocalPath = imagenLocalPath;
    }

    public Product(int id, String name, String description, double price, String emoji, String category, int thumbDrawableRes, boolean isAgotado, int quantityInCart) {
        this(id, name, description, price, emoji, category, thumbDrawableRes, isAgotado, quantityInCart, null);
    }

    public Product(String name, String description, double price, String emoji, String category, int thumbDrawableRes, boolean isAgotado, int quantityInCart) {
        this(0, name, description, price, emoji, category, thumbDrawableRes, isAgotado, quantityInCart, null);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getEmoji() { return emoji; }
    public String getCategory() { return category; }
    public int getThumbDrawableRes() { return thumbDrawableRes; }
    public boolean isAgotado() { return isAgotado; }
    public int getQuantityInCart() { return quantityInCart; }
    public void setQuantityInCart(int quantityInCart) { this.quantityInCart = quantityInCart; }
    public String getImagenLocalPath() { return imagenLocalPath; }
    public void setImagenLocalPath(String imagenLocalPath) { this.imagenLocalPath = imagenLocalPath; }
}