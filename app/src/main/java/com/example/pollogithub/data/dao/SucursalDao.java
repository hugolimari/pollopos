package com.example.pollogithub.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.pollogithub.data.entity.SucursalEntity;

/**
 * DAO para gestionar operaciones sobre la tabla 'sucursales'.
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
