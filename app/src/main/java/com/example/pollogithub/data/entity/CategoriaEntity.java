package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para la tabla 'categorias'.
 */
@Entity(tableName = "categorias")
public class CategoriaEntity {

    /**
     * Identificador unívoco de la entidad (Clave Primaria).
     * Autogenerado secuencialmente por SQLite.
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    /**
     * Nombre descriptivo de la categoría (ej. "Pollos", "Bebidas", "Guarniciones").
     */
    private String nombre;

    /**
     * Secuencia posicional para definir el ordenamiento en el menú de navegación de la UI.
     */
    private int orden;

    /**
     * Constructor parametrizado para la instanciación de nuevas entidades de negocio
     * previas a su persistencia en la base de datos local.
     * 
     * @param nombre Denominación textual de la categoría.
     * @param orden  Índice de prelación visual en la interfaz.
     */
    public CategoriaEntity(String nombre, int orden) {
        this.nombre = nombre;
        this.orden = orden;
    }

    // ==========================================
    // MÉTODOS DE ACCESO (GETTERS Y SETTERS)
    // Encapsulamiento del estado interno
    // ==========================================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}
