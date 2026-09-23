package com.example.pollogithub.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.pollogithub.data.entity.PedidoEntity;

import java.util.List;

@Dao
public interface PedidoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PedidoEntity pedido);

    @Update
    void update(PedidoEntity pedido);

    @Query("SELECT * FROM pedidos ORDER BY id DESC")
    LiveData<List<PedidoEntity>> getAllLiveData();

    @Query("SELECT * FROM pedidos ORDER BY id DESC")
    List<PedidoEntity> getAll();

    @Query("SELECT * FROM pedidos WHERE estado = :estado ORDER BY id DESC")
    LiveData<List<PedidoEntity>> getByEstadoLiveData(String estado);

    @Query("SELECT * FROM pedidos WHERE id = :id LIMIT 1")
    PedidoEntity getById(int id);

    @Query("SELECT * FROM pedidos WHERE id = :id LIMIT 1")
    LiveData<PedidoEntity> getByIdLiveData(int id);

    @Query("UPDATE pedidos SET estado = :nuevoEstado WHERE id = :id")
    void updateEstado(int id, String nuevoEstado);

    @Query("UPDATE pedidos SET estadoPago = :nuevoEstadoPago WHERE id = :id")
    void updateEstadoPago(int id, String nuevoEstadoPago);

    @Query("SELECT MAX(numeroOrden) FROM pedidos")
    Integer getMaxNumeroOrden();

    @Query("SELECT COUNT(*) FROM pedidos WHERE estado = :estado")
    int countByEstado(String estado);

    @Query("UPDATE pedidos SET estado = 'cancelado', estadoPago = 'cancelado', motivoCancelacion = :motivo WHERE id = :id")
    void cancelarPedido(int id, String motivo);

    @Query("SELECT COUNT(*) FROM pedidos")
    int count();
}
