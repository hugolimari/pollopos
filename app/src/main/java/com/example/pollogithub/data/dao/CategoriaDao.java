package com.example.pollogithub.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.pollogithub.data.entity.CategoriaEntity;

import java.util.List;

@Dao
public interface CategoriaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CategoriaEntity> categorias);

    @Query("SELECT * FROM categorias ORDER BY orden ASC")
    LiveData<List<CategoriaEntity>> getAllLiveData();

    @Query("SELECT * FROM categorias ORDER BY orden ASC")
    List<CategoriaEntity> getAll();

    @Query("SELECT COUNT(*) FROM categorias")
    int count();
}
