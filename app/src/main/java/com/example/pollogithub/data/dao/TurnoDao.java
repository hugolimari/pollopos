package com.example.pollogithub.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.pollogithub.data.entity.TurnoEntity;

import java.util.List;

@Dao
public interface TurnoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TurnoEntity turno);

    @Update
    void update(TurnoEntity turno);

    @Query("SELECT * FROM turnos WHERE estado = 'abierto' ORDER BY id DESC LIMIT 1")
    TurnoEntity getTurnoActivo();

    @Query("SELECT * FROM turnos WHERE estado = 'abierto' ORDER BY id DESC LIMIT 1")
    LiveData<TurnoEntity> getTurnoActivoLiveData();

    @Query("SELECT * FROM turnos WHERE id = :id LIMIT 1")
    TurnoEntity getById(int id);

    @Query("SELECT * FROM turnos ORDER BY id DESC")
    LiveData<List<TurnoEntity>> getAllLiveData();

    @Query("SELECT COUNT(*) FROM turnos")
    int count();
}
