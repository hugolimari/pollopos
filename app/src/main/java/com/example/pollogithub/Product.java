package com.example.pollogithub;

/**
 * Modelo de Dominio y Presentación: Product
 * 
 * Capa de Presentación / Objeto de Transferencia de Datos (DTO)
 * 
 * Representa un artículo comercial en la capa de vista del punto de venta (POS).
 * A diferencia de la entidad persistida (ProductoEntity), este modelo incorpora
 * el estado volátil del carrito de compras ('quantityInCart') y la bandera de
 * agotamiento ('isAgotado') para facilitar el enlace de datos (Data Binding) directo
 * con los componentes de la interfaz de usuario.
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Separación de Modelos (Domain/Presentation vs Entity): Desacopla las restricciones
 *   de base de datos del estado mutable de la interfaz gráfica.
 * - Inmutabilidad y Encapsulamiento: Atributos privados con métodos accesores y mutadores selectivos.
 */
public class Product {

    /**
     * Identificador primario del producto (corresponde a la clave en SQLite).
     */
    private int id;

    /**
     * Nombre comercial del ítem (ej. "1/4 de pollo frito").
     */
    private String name;

    /**
     * Descripción complementaria para el cliente.
     */
    private String description;

    /**
     * Precio unitario de comercialización en moneda local.
     */
    private double price;

    /**
     * Emoji representativo para renderizado visual ágil.
     */
    private String emoji;

    /**
     * Nombre descriptivo de la categoría a la que pertenece.
     */
    private String category;

    /**
     * Identificador del recurso visual en res/drawable.
     */
    private int thumbDrawableRes;

    /**
     * Indicador de agotamiento de inventario (true = bloqueado para selección).
     */
    private boolean isAgotado;

    /**
     * Cantidad transitoria de unidades seleccionadas en la sesión actual del carrito.
     */
    private int quantityInCart;
    private String imagenLocalPath;
    private String notes = "";

    /**
     * Constructor parametrizado completo.
     * 
     * @param id               Identificador único del producto.
     * @param name             Nombre descriptivo.
     * @param description      Detalle de acompañamientos.
     * @param price            Importe monetario.
     * @param emoji            Ícono representativo.
     * @param category         Categoría de pertenencia.
     * @param thumbDrawableRes Recurso de fondo.
     * @param isAgotado        Flag de indisponibilidad.
     * @param quantityInCart   Unidades añadidas al carrito.
     * @param imagenLocalPath  Ruta de imagen en almacenamiento interno local.
     */
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

    /**
     * Sobrecarga de constructor para instancias sin clave primaria explícita.
     */
    public Product(String name, String description, double price, String emoji, String category, int thumbDrawableRes, boolean isAgotado, int quantityInCart) {
        this(0, name, description, price, emoji, category, thumbDrawableRes, isAgotado, quantityInCart, null);
    }

    // ==========================================
    // MÉTODOS ACCESORES (GETTERS Y SETTERS)
    // ==========================================

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

    public String getNotes() { return notes != null ? notes : ""; }
    public void setNotes(String notes) { this.notes = notes != null ? notes : ""; }
}