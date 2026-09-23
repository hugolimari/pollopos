package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "roles")
public class RolEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String nombre; // "admin", "cajero", "cocina"

    public RolEntity(String nombre) {
        this.nombre = nombre;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
