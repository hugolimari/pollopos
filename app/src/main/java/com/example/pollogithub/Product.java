package com.example.pollogithub;

public class Product {
    private String name;
    private String description;
    private double price;
    private String emoji;
    private String category;
    private int thumbDrawableRes;
    private boolean isAgotado;
    private int quantityInCart;

    public Product(String name, String description, double price, String emoji, String category, int thumbDrawableRes, boolean isAgotado, int quantityInCart) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.emoji = emoji;
        this.category = category;
        this.thumbDrawableRes = thumbDrawableRes;
        this.isAgotado = isAgotado;
        this.quantityInCart = quantityInCart;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getEmoji() { return emoji; }
    public String getCategory() { return category; }
    public int getThumbDrawableRes() { return thumbDrawableRes; }
    public boolean isAgotado() { return isAgotado; }
    public int getQuantityInCart() { return quantityInCart; }
    public void setQuantityInCart(int quantityInCart) { this.quantityInCart = quantityInCart; }
}