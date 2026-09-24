package com.example.pollogithub.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.pollogithub.data.entity.InsumoEntity;

import java.util.List;

@Dao
public interface InsumoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(InsumoEntity insumo);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<InsumoEntity> insumos);

    @Update
    void update(InsumoEntity insumo);

    @Query("SELECT * FROM insumos ORDER BY id ASC")
    List<InsumoEntity> getAll();

    @Query("SELECT * FROM insumos ORDER BY id ASC")
    LiveData<List<InsumoEntity>> getAllLiveData();

    @Query("SELECT * FROM insumos WHERE id = :id LIMIT 1")
    InsumoEntity getById(int id);

    @Query("UPDATE insumos SET stockActual = stockActual - :cantidad WHERE id = :insumoId")
    void descontarStock(int insumoId, double cantidad);

    @Query("UPDATE insumos SET stockActual = stockActual + :cantidad WHERE id = :insumoId")
    void agregarStock(int insumoId, double cantidad);

    @Query("SELECT COUNT(*) FROM insumos")
    int count();
}
