package com.example.pollogithub.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.pollogithub.data.entity.ProductoEntity;

import java.util.List;

@Dao
public interface ProductoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ProductoEntity producto);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProductoEntity> productos);

    @Update
    void update(ProductoEntity producto);

    @Query("SELECT * FROM productos ORDER BY id ASC")
    LiveData<List<ProductoEntity>> getAllLiveData();

    @Query("SELECT * FROM productos ORDER BY id ASC")
    List<ProductoEntity> getAll();

    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    ProductoEntity getById(int id);

    @Query("UPDATE productos SET disponible = :disponible WHERE id = :id")
    void setDisponible(int id, boolean disponible);

    @Query("SELECT COUNT(*) FROM productos")
    int count();
}
