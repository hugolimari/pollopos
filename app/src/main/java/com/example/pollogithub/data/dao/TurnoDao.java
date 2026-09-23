package com.example.pollogithub.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.pollogithub.data.entity.TurnoEntity;

import java.util.List;

/**
 * Objeto de Acceso a Datos: TurnoDao
 * 
 * Capa de Persistencia / Patrón DAO (Data Access Object)
 * Interfaz compilada por Room ORM para la tabla 'turnos'.
 * 
 * Controla la persistencia de las jornadas operativas de caja.
 * Permite identificar si existe una sesión de caja actualmente activa (estado = 'abierto'),
 * registrar aperturas, liquidar cierres de turno y consultar el historial cronológico
 * de arqueos para auditorías contables.
 * 
 * Conceptos de Ingeniería aplicados:
 * - Control de Concurrencia de Negocio: Invariante de negocio donde solo un turno puede estar en estado 'abierto' por terminal.
 * - Reactive State Streams: LiveData de turno activo para reaccionar ante bloqueos o cierres de sesión desde cualquier pantalla.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
@Dao
public interface TurnoDao {

    /**
     * Persiste la apertura de un nuevo turno de caja.
     * 
     * @param turno Entidad con el fondo inicial, marca temporal y usuario responsable.
     * @return Identificador primario autogenerado para el turno.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TurnoEntity turno);

    /**
     * Actualiza el registro de un turno existente (utilizado para asentar el cierre, arqueo y diferencias).
     * 
     * @param turno Entidad del turno con los valores finales de cierre y montos contados.
     */
    @Update
    void update(TurnoEntity turno);

    /**
     * Consulta síncrona para obtener el turno que permanece en estado 'abierto'.
     * Vital para validar si el cajero debe ingresar fondo de caja antes de registrar ventas.
     * 
     * @return Instancia de TurnoEntity activo o null si la caja se encuentra cerrada.
     */
    @Query("SELECT * FROM turnos WHERE estado = 'abierto' ORDER BY id DESC LIMIT 1")
    TurnoEntity getTurnoActivo();

    /**
     * Observador reactivo del turno de caja actualmente activo.
     * 
     * @return LiveData que emite el estado del turno para adaptar dinámicamente la UI del POS.
     */
    @Query("SELECT * FROM turnos WHERE estado = 'abierto' ORDER BY id DESC LIMIT 1")
    LiveData<TurnoEntity> getTurnoActivoLiveData();

    /**
     * Localiza un turno específico mediante su identificador primario.
     * 
     * @param id Clave primaria del turno.
     * @return TurnoEntity correspondiente.
     */
    @Query("SELECT * FROM turnos WHERE id = :id LIMIT 1")
    TurnoEntity getById(int id);

    /**
     * Emite reactivamente el historial completo de turnos cerrados y abiertos,
     * ordenados descendentemente para la pantalla de historial y auditoría.
     * 
     * @return LiveData con la colección histórica de turnos.
     */
    @Query("SELECT * FROM turnos ORDER BY id DESC")
    LiveData<List<TurnoEntity>> getAllLiveData();

    /**
     * Contabiliza el número total de turnos registrados en la base de datos local.
     * 
     * @return Conteo de tuplas.
     */
    @Query("SELECT COUNT(*) FROM turnos")
    int count();
}
