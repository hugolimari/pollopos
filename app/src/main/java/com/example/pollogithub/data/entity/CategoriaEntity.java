package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categorias")
public class CategoriaEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String nombre;
    private int orden;

    public CategoriaEntity(String nombre, int orden) {
        this.nombre = nombre;
        this.orden = orden;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}
