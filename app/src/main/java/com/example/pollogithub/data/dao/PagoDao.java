package com.example.pollogithub.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.pollogithub.data.entity.PagoEntity;

import java.util.List;

@Dao
public interface PagoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PagoEntity pago);

    @Query("SELECT * FROM pagos WHERE pedidoId = :pedidoId LIMIT 1")
    PagoEntity getByPedidoId(int pedidoId);

    @Query("SELECT * FROM pagos WHERE turnoId = :turnoId")
    List<PagoEntity> getByTurnoId(int turnoId);

    @Query("SELECT SUM(monto) FROM pagos WHERE turnoId = :turnoId")
    Double getTotalVentasByTurno(int turnoId);

    @Query("SELECT SUM(monto) FROM pagos WHERE turnoId = :turnoId AND metodoPago = 'efectivo'")
    Double getTotalEfectivoByTurno(int turnoId);

    @Query("SELECT * FROM pagos ORDER BY id DESC")
    LiveData<List<PagoEntity>> getAllLiveData();
}
