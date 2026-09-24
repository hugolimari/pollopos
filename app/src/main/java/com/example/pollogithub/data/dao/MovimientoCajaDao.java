package com.example.pollogithub.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.pollogithub.data.entity.MovimientoCajaEntity;

import java.util.List;

@Dao
public interface MovimientoCajaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MovimientoCajaEntity movimiento);

    @Query("SELECT * FROM movimientos_caja WHERE turnoId = :turnoId ORDER BY creadoEn DESC")
    List<MovimientoCajaEntity> getByTurnoId(int turnoId);

    @Query("SELECT * FROM movimientos_caja WHERE turnoId = :turnoId ORDER BY creadoEn DESC")
    LiveData<List<MovimientoCajaEntity>> getByTurnoIdLiveData(int turnoId);

    @Query("SELECT COALESCE(SUM(monto), 0.0) FROM movimientos_caja WHERE turnoId = :turnoId AND tipo = 'EGRESO'")
    Double getTotalEgresosByTurno(int turnoId);

    @Query("SELECT COALESCE(SUM(monto), 0.0) FROM movimientos_caja WHERE turnoId = :turnoId AND tipo = 'INGRESO'")
    Double getTotalIngresosExtraByTurno(int turnoId);
}
