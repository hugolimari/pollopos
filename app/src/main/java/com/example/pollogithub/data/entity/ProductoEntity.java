package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad de Persistencia: ProductoEntity
 * 
 * Capa de Datos / Catálogo de Productos e Inventario (Room ORM)
 * Tabla: "productos"
 * 
 * Modela un producto vendible dentro del catálogo comercial del restaurante.
 * Define la estructura de precios, asignación de categoría, disponibilidad operativa
 * y recursos multimedia (íconos y drawables) utilizados por la interfaz de usuario.
 * 
 * Conceptos de Ingeniería aplicados:
 * - Integridad Referencial: Claves foráneas hacia 'sucursales' y 'categorias'.
 * - Soft Delete / Flag de Disponibilidad: El campo 'disponible' implementa una técnica de borrado lógico
 *   o deshabilitación temporal, preservando la integridad referencial en transacciones pasadas.
 * - Desacoplamiento de Recursos Visuales: Mapea identificadores de recursos Android (@DrawableRes) y caracteres Unicode (emojis)
 *   para optimizar la renderización sin requerir sobrecarga de descarga en red.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
@Entity(tableName = "productos")
public class ProductoEntity {

    /**
     * Identificador unívoco del producto en el catálogo (Clave Primaria).
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    /**
     * Clave foránea que asocia el producto a una sucursal específica.
     */
    private int sucursalId;

    /**
     * Clave foránea que clasifica el artículo dentro de una categoría del menú.
     */
    private int categoriaId;

    /**
     * Nombre comercial o denominación del plato / ítem.
     */
    private String nombre;

    /**
     * Descripción detallada de los ingredientes o especificaciones del producto.
     */
    private String descripcion;

    /**
     * Precio base de venta al público en moneda local.
     */
    private double precio;

    /**
     * Bandera booleana que controla la disponibilidad actual del producto (control de stock/agotado).
     */
    private boolean disponible;

    /**
     * Representación gráfica textual mediante emoji Unicode para identificación rápida en UI.
     */
    private String emoji;

    /**
     * Identificador entero de recurso drawable local en Android (R.drawable.*).
     */
    private int thumbDrawableRes;
    private String imagenLocalPath;

    /**
     * Constructor principal para instanciar un producto con sus atributos de negocio, imagen local y visualización.
     */
    public ProductoEntity(int sucursalId, int categoriaId, String nombre, String descripcion, double precio, boolean disponible, String emoji, int thumbDrawableRes, String imagenLocalPath) {
        this.sucursalId = sucursalId;
        this.categoriaId = categoriaId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.disponible = disponible;
        this.emoji = emoji;
        this.thumbDrawableRes = thumbDrawableRes;
        this.imagenLocalPath = imagenLocalPath;
    }

    @androidx.room.Ignore
    public ProductoEntity(int sucursalId, int categoriaId, String nombre, String descripcion, double precio, boolean disponible, String emoji, int thumbDrawableRes) {
        this(sucursalId, categoriaId, nombre, descripcion, precio, disponible, emoji, thumbDrawableRes, null);
    }

    // ==========================================
    // MÉTODOS DE ACCESO (GETTERS Y SETTERS)
    // ==========================================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSucursalId() { return sucursalId; }
    public void setSucursalId(int sucursalId) { this.sucursalId = sucursalId; }

    public int getCategoriaId() { return categoriaId; }
    public void setCategoriaId(int categoriaId) { this.categoriaId = categoriaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public int getThumbDrawableRes() { return thumbDrawableRes; }
    public void setThumbDrawableRes(int thumbDrawableRes) { this.thumbDrawableRes = thumbDrawableRes; }
    public String getImagenLocalPath() { return imagenLocalPath; }
    public void setImagenLocalPath(String imagenLocalPath) { this.imagenLocalPath = imagenLocalPath; }
}
