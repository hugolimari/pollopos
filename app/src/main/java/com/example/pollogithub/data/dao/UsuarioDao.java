package com.example.pollogithub.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.pollogithub.data.entity.UsuarioEntity;

import java.util.List;

/**
 * Objeto de Acceso a Datos: UsuarioDao
 * 
 * Capa de Persistencia / Patrón DAO (Data Access Object)
 * Interfaz compilada por Room ORM para la tabla 'usuarios'.
 * 
 * Gestiona las operaciones de autenticación, validación de credenciales
 * y administración del personal operativo. Implementa métodos de búsqueda dual
 * (por nombre de usuario o código PIN rápido) garantizando que solo los usuarios
 * con estado activo (activo = 1) puedan autenticarse satisfactoriamente.
 * 
 * Conceptos de Ingeniería aplicados:
 * - Seguridad y Control de Acceso: Cláusulas SQL que validan la combinación de credenciales y vigencia del usuario.
 * - Prevención de Inyección SQL: Consultas precompiladas y parametrizadas generadas por el compilador de Room.
 * - Manejo de Identidad: Búsqueda indexada por credenciales primarias y secundarias (PIN).
 */
@Dao
public interface UsuarioDao {

    /**
     * Inserta un nuevo operador en el registro de usuarios.
     * 
     * @param usuario Entidad del usuario a persistir.
     * @return Identificador primario autogenerado.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UsuarioEntity usuario);

    /**
     * Inserta por lote una lista de usuarios. Utilizado durante el sembrado inicial (Data Seeding).
     * 
     * @param usuarios Colección de usuarios a insertar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<UsuarioEntity> usuarios);

    /**
     * Actualiza la información de perfil o credenciales de un usuario existente.
     * 
     * @param usuario Entidad con los datos modificados.
     */
    @Update
    void update(UsuarioEntity usuario);

    /**
     * Busca un usuario activo que coincida con el identificador ingresado,
     * ya sea por su nombre de usuario formal o por su código PIN rápido.
     * 
     * @param userOrPin Cadena que contiene el nombre de usuario o el PIN.
     * @return UsuarioEntity coincidente o null si no existe o está inactivo.
     */
    @Query("SELECT * FROM usuarios WHERE (nombreUsuario = :userOrPin OR pinRapido = :userOrPin) AND activo = 1 LIMIT 1")
    UsuarioEntity findByUsernameOrPin(String userOrPin);

    /**
     * Valida el inicio de sesión convencional mediante credenciales compuestas (usuario + contraseña).
     * 
     * @param username Nombre de usuario registrado.
     * @param password Contraseña de acceso.
     * @return Instancia del usuario autenticado si las credenciales son válidas y está activo; null en caso contrario.
     */
    @Query("SELECT * FROM usuarios WHERE nombreUsuario = :username AND password = :password AND activo = 1 LIMIT 1")
    UsuarioEntity login(String username, String password);

    /**
     * Valida el inicio de sesión acelerado para terminales POS mediante código PIN numérico.
     * 
     * @param pin Código PIN rápido del operador.
     * @return Instancia del usuario autenticado si el PIN es correcto y está activo; null en caso contrario.
     */
    @Query("SELECT * FROM usuarios WHERE pinRapido = :pin AND activo = 1 LIMIT 1")
    UsuarioEntity loginWithPin(String pin);

    /**
     * Consulta síncrona de un operador mediante su clave primaria.
     * 
     * @param id Clave primaria del usuario.
     * @return Entidad del usuario.
     */
    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    UsuarioEntity getById(int id);

    /**
     * Contabiliza el número total de operadores registrados en el sistema.
     * 
     * @return Cantidad entera de usuarios.
     */
    @Query("SELECT COUNT(*) FROM usuarios")
    int count();
}
