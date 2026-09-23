package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad de Persistencia: CategoriaEntity
 * 
 * Capa de Datos / Modelo Relacional (Room ORM)
 * Tabla: "categorias"
 * 
 * Representa la taxonomía jerárquica para la clasificación de productos dentro
 * del sistema POS (Punto de Venta). Permite segmentar el catálogo de artículos
 * (combos, bebidas, porciones) optimizando las consultas y la presentación visual
 * en el módulo de ventas.
 * 
 * Conceptos de Ingeniería aplicados:
 * - Mapeo Objeto-Relacional (ORM) mediante Room.
 * - Atributo 'orden' para garantizar consistencia en la ordenación determinista en UI.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
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
