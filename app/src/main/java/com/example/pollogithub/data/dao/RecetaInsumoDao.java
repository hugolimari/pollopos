package com.example.pollogithub.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.pollogithub.data.entity.RecetaInsumoEntity;

import java.util.List;

@Dao
public interface RecetaInsumoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(RecetaInsumoEntity recetaInsumo);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RecetaInsumoEntity> recetaInsumos);

    @Query("SELECT * FROM receta_insumos WHERE productoId = :productoId")
    List<RecetaInsumoEntity> getByProductoId(int productoId);

    @Query("SELECT COUNT(*) FROM receta_insumos")
    int count();
}
