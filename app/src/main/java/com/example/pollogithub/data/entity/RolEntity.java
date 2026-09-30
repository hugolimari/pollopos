package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para la tabla 'roles'.
 */
@Entity(tableName = "roles")
public class RolEntity {

    /**
     * Identificador unívoco del rol de usuario (Clave Primaria).
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    /**
     * Denominación estándar del rol: "admin", "cajero", "cocina".
     */
    private String nombre;

    /**
     * Constructor para inicializar una categoría de autorización o rol.
     * 
     * @param nombre Etiqueta identificadora del rol.
     */
    public RolEntity(String nombre) {
        this.nombre = nombre;
    }

    // ==========================================
    // MÉTODOS DE ACCESO (GETTERS Y SETTERS)
    // ==========================================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
