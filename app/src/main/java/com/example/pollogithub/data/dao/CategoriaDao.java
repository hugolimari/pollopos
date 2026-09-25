package com.example.pollogithub.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.pollogithub.data.entity.CategoriaEntity;

import java.util.List;

/**
 * Objeto de Acceso a Datos: CategoriaDao
 * 
 * Capa de Persistencia / Patrón DAO (Data Access Object)
 * Interfaz compilada por Room ORM para SQLite.
 * 
 * Centraliza las operaciones de lectura y escritura en la tabla 'categorias'.
 * Proporciona métodos síncronos (para ejecución en background workers) y
 * observables reactivos (mediante LiveData) para mantener sincronizada la interfaz
 * ante mutaciones del catálogo.
 * 
 * Principios de Ingeniería de Software:
 * - Desacoplamiento (SoC): Aísla las sentencias SQL nativas de la capa de presentación.
 * - Patrón Observer: Retorno de LiveData para actualización automática de vistas.
 * - Idempotencia en Inserción: Estrategia OnConflictStrategy.REPLACE para evitar tuplas duplicadas.
 */
@Dao
public interface CategoriaDao {

    /**
     * Inserta por lote una colección de categorías en la base de datos.
     * Utiliza política de reemplazo en colisiones de clave primaria.
     * 
     * @param categorias Lista de entidades de categorías a persistir.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CategoriaEntity> categorias);

    /**
     * Recupera todas las categorías ordenadas por prelación visual de forma reactiva.
     * 
     * @return Contenedor LiveData que notifica a la UI cada vez que la tabla 'categorias' se modifica.
     */
    @Query("SELECT * FROM categorias ORDER BY orden ASC")
    LiveData<List<CategoriaEntity>> getAllLiveData();

    /**
     * Consulta síncrona de todas las categorías disponibles.
     * Debe ser ejecutada exclusivamente fuera del hilo principal (UI Thread).
     * 
     * @return Lista en memoria de las categorías ordenadas secuencialmente.
     */
    @Query("SELECT * FROM categorias ORDER BY orden ASC")
    List<CategoriaEntity> getAll();

    /**
     * Función de agregación que contabiliza el total de tuplas registradas en 'categorias'.
     * Utilizada principalmente durante la siembra de datos iniciales (Data Seeding).
     * 
     * @return Cantidad entera de categorías existentes.
     */
    @Query("SELECT COUNT(*) FROM categorias")
    int count();
}
