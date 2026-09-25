package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad de Persistencia: RolEntity
 * 
 * Capa de Datos / Control de Acceso y Seguridad (Room ORM)
 * Tabla: "roles"
 * 
 * Modela los roles y niveles de autorización dentro del sistema conforme al modelo
 * de Control de Acceso Basado en Roles (RBAC - Role-Based Access Control).
 * Permite segregar privilegios operativos entre perfiles administrativos, personal
 * de cobranza (cajeros) y personal de preparación (cocina).
 * 
 * Conceptos de Ingeniería aplicados:
 * - Seguridad y Autorización: Principio de Menor Privilegio (Principle of Least Privilege).
 * - Normalización de Base de Datos: Tercera Forma Normal (3FN), desacoplando los roles de la tabla 'usuarios'.
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
