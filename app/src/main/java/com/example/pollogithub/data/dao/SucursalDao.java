package com.example.pollogithub.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.pollogithub.data.entity.SucursalEntity;

@Dao
public interface SucursalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SucursalEntity sucursal);

    @Query("SELECT * FROM sucursales LIMIT 1")
    SucursalEntity getFirst();
}
