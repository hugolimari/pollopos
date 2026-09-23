package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sucursales")
public class SucursalEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String nombre;
    private String direccion;
    private String telefono;
    private boolean activa;

    public SucursalEntity(String nombre, String direccion, String telefono, boolean activa) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.activa = activa;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}
