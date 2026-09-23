package com.example.pollogithub.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.pollogithub.data.entity.PedidoDetalleEntity;

import java.util.List;

@Dao
public interface PedidoDetalleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PedidoDetalleEntity> detalles);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PedidoDetalleEntity detalle);

    @Query("SELECT * FROM pedido_detalles WHERE pedidoId = :pedidoId")
    List<PedidoDetalleEntity> getByPedidoId(int pedidoId);

    @Query("SELECT * FROM pedido_detalles WHERE pedidoId = :pedidoId")
    LiveData<List<PedidoDetalleEntity>> getByPedidoIdLiveData(int pedidoId);
}
