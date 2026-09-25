package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad de Persistencia: SucursalEntity
 * 
 * Capa de Datos / Modelo Organizacional y Multi-Sucursal (Room ORM)
 * Tabla: "sucursales"
 * 
 * Modela las unidades de negocio físicas o puntos de venta de la franquicia.
 * Centraliza la información de localización geográfica, contacto operativo
 * y estado administrativo de cada establecimiento para aislar turnos, ventas y usuarios.
 * 
 * Conceptos de Ingeniería aplicados:
 * - Soporte Multi-Tenant / Multi-Sucursal: Permite segmentar las operaciones de caja y catálogo por nodo geográfico.
 * - Desactivación Lógica: El atributo 'activa' habilita o inhabilita la operativa de una sucursal sin comprometer registros históricos.
 */
@Entity(tableName = "sucursales")
public class SucursalEntity {

    /**
     * Identificador unívoco de la sucursal (Clave Primaria).
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    /**
     * Nombre comercial o identificador de la sucursal (ej. "Sucursal Central", "Sucursal Norte").
     */
    private String nombre;

    /**
     * Dirección física o domicilio del establecimiento comercial.
     */
    private String direccion;

    /**
     * Línea telefónica de contacto o soporte del punto de venta.
     */
    private String telefono;

    /**
     * Bandera de estado operativo (true = habilitada para operaciones, false = inactiva).
     */
    private boolean activa;

    /**
     * Constructor para instanciar una nueva sede física en el sistema.
     */
    public SucursalEntity(String nombre, String direccion, String telefono, boolean activa) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.activa = activa;
    }

    // ==========================================
    // MÉTODOS DE ACCESO (GETTERS Y SETTERS)
    // ==========================================

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
