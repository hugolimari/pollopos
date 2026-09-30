package com.example.pollogithub.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.pollogithub.data.entity.PedidoDetalleEntity;

import java.util.List;

/**
 * DAO para gestionar operaciones sobre la tabla 'pedido_detalles'.
 */
@Dao
public interface PedidoDetalleDao {

    /**
     * Inserta por lote todas las líneas de detalle pertenecientes a una orden de venta.
     * Optimiza el rendimiento de I/O frente a inserciones individuales iterativas.
     * 
     * @param detalles Colección de detalles de productos y subtotales a persistir.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PedidoDetalleEntity> detalles);

    /**
     * Inserta un detalle de pedido individual en la base de datos.
     * 
     * @param detalle Línea de pedido con producto, cantidad y notas.
     * @return Identificador 'rowid' asignado por SQLite.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PedidoDetalleEntity detalle);

    /**
     * Recupera de forma síncrona todas las líneas de producto que componen un pedido.
     * 
     * @param pedidoId Clave foránea del pedido consultado.
     * @return Lista de detalles con nombres, cantidades y precios unitarios históricos.
     */
    @Query("SELECT * FROM pedido_detalles WHERE pedidoId = :pedidoId")
    List<PedidoDetalleEntity> getByPedidoId(int pedidoId);

    /**
     * Observador reactivo para las líneas de producto de un pedido.
     * 
     * @param pedidoId Clave foránea del pedido consultado.
     * @return LiveData que emite actualizaciones si se adicionan o modifican detalles del pedido.
     */
    @Query("SELECT * FROM pedido_detalles WHERE pedidoId = :pedidoId")
    LiveData<List<PedidoDetalleEntity>> getByPedidoIdLiveData(int pedidoId);

    /**
     * Elimina físicamente todas las líneas de detalle asociadas a un pedido.
     */
    @Query("DELETE FROM pedido_detalles WHERE pedidoId = :pedidoId")
    void deleteByPedidoId(int pedidoId);
}
