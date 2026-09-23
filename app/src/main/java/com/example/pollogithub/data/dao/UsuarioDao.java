package com.example.pollogithub.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.pollogithub.data.entity.UsuarioEntity;

import java.util.List;

@Dao
public interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UsuarioEntity usuario);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<UsuarioEntity> usuarios);

    @Update
    void update(UsuarioEntity usuario);

    @Query("SELECT * FROM usuarios WHERE (nombreUsuario = :userOrPin OR pinRapido = :userOrPin) AND activo = 1 LIMIT 1")
    UsuarioEntity findByUsernameOrPin(String userOrPin);

    @Query("SELECT * FROM usuarios WHERE nombreUsuario = :username AND password = :password AND activo = 1 LIMIT 1")
    UsuarioEntity login(String username, String password);

    @Query("SELECT * FROM usuarios WHERE pinRapido = :pin AND activo = 1 LIMIT 1")
    UsuarioEntity loginWithPin(String pin);

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    UsuarioEntity getById(int id);

    @Query("SELECT COUNT(*) FROM usuarios")
    int count();
}
