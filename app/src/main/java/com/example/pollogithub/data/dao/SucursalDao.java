package com.example.pollogithub.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.pollogithub.data.entity.SucursalEntity;

/**
 * Objeto de Acceso a Datos: SucursalDao
 * 
 * Capa de Persistencia / Patrón DAO (Data Access Object)
 * Interfaz compilada por Room ORM para la tabla 'sucursales'.
 * 
 * Gestiona el acceso y almacenamiento de las entidades de sucursal.
 * Permite registrar la sede física operativa del establecimiento y obtener
 * la sucursal predeterminada para inicializar el contexto de la aplicación.
 * 
 * Conceptos de Ingeniería aplicados:
 * - Aislamiento Contextual: Provisión de la sucursal activa para vincular ventas y arqueos.
 * - Inicialización del Sistema: Consulta 'getFirst' para configuración predeterminada en entornos de tienda única.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
@Dao
public interface SucursalDao {

    /**
     * Persiste o actualiza los datos de una sucursal en la base de datos local.
     * 
     * @param sucursal Entidad con la información del punto de venta físico.
     * @return Identificador primario generado.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SucursalEntity sucursal);

    /**
     * Recupera la primera sucursal registrada en el sistema.
     * Utilizada como valor de contingencia o configuración por defecto en la inicialización de sesiones.
     * 
     * @return Primera entidad SucursalEntity registrada o null si no se ha sembrado la base de datos.
     */
    @Query("SELECT * FROM sucursales LIMIT 1")
    SucursalEntity getFirst();
}
