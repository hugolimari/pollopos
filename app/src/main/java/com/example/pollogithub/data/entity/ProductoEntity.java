package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "productos")
public class ProductoEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int sucursalId;
    private int categoriaId;
    private String nombre;
    private String descripcion;
    private double precio;
    private boolean disponible;
    private String emoji;
    private int thumbDrawableRes;
    private String imagenLocalPath;

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
