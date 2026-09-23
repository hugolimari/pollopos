package com.example.pollogithub.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.pollogithub.data.entity.PagoEntity;

import java.util.List;

/**
 * Objeto de Acceso a Datos: PagoDao
 * 
 * Capa de Persistencia / Patrón DAO (Data Access Object)
 * Interfaz compilada por Room ORM para la tabla 'pagos'.
 * 
 * Encapsula la lógica de consulta y persistencia de las transacciones monetarias.
 * Implementa funciones de agregación SQL (SUM) indispensables para el arqueo de caja
 * y la consolidación de ingresos según el instrumento de pago utilizado (efectivo, tarjeta, QR).
 * 
 * Conceptos de Ingeniería aplicados:
 * - Agregación Contable SQL: Operaciones vectoriales SUM() filtradas por identificador de turno.
 * - Manejo Seguro de Nulos: Retorno de tipo envoltorio 'Double' para mitigar excepciones de NullPointerException
 *   cuando un turno no registra pagos en determinada modalidad.
 * - Integridad de Auditoría: Registro de id de inserción autogenerado para vinculación de comprobantes.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
@Dao
public interface PagoDao {

    /**
     * Persiste una nueva transacción de pago en la base de datos local.
     * 
     * @param pago Entidad con los datos de cobro, montos y marcas temporales.
     * @return Identificador 'rowid' autogenerado por SQLite tras la inserción exitosa.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PagoEntity pago);

    /**
     * Recupera el comprobante de pago asociado a una orden específica.
     * 
     * @param pedidoId Clave foránea del pedido.
     * @return Entidad PagoEntity asociada o null si el pedido aún no fue cobrado.
     */
    @Query("SELECT * FROM pagos WHERE pedidoId = :pedidoId LIMIT 1")
    PagoEntity getByPedidoId(int pedidoId);

    /**
     * Obtiene el listado completo de pagos asentados durante un turno de caja específico.
     * 
     * @param turnoId Identificador del turno a consultar.
     * @return Colección de pagos registrados en el turno.
     */
    @Query("SELECT * FROM pagos WHERE turnoId = :turnoId")
    List<PagoEntity> getByTurnoId(int turnoId);

    /**
     * Calcula la sumatoria global de ventas brutas recaudadas durante el turno.
     * 
     * @param turnoId Identificador del turno evaluado.
     * @return Monto total acumulado (Double nullable si la sumatoria carece de filas).
     */
    @Query("SELECT SUM(monto) FROM pagos WHERE turnoId = :turnoId")
    Double getTotalVentasByTurno(int turnoId);

    /**
     * Calcula la sumatoria exclusiva de ingresos liquidados en papel moneda / efectivo.
     * Dato fundamental para la fórmula de arqueo físico:
     * efectivoEsperado = fondoInicial + totalEfectivo.
     * 
     * @param turnoId Identificador del turno evaluado.
     * @return Sumatoria de cobros en efectivo.
     */
    @Query("SELECT SUM(monto) FROM pagos WHERE turnoId = :turnoId AND metodoPago = 'efectivo'")
    Double getTotalEfectivoByTurno(int turnoId);

    /**
     * Calcula la sumatoria de pagos procesados mediante terminales de tarjeta de crédito/débito.
     * 
     * @param turnoId Identificador del turno evaluado.
     * @return Sumatoria de cobros con tarjeta bancaria.
     */
    @Query("SELECT SUM(monto) FROM pagos WHERE turnoId = :turnoId AND metodoPago = 'tarjeta'")
    Double getTotalTarjetaByTurno(int turnoId);

    /**
     * Calcula la sumatoria de cobros procesados mediante pasarelas de pago móvil / código QR.
     * 
     * @param turnoId Identificador del turno evaluado.
     * @return Sumatoria de cobros QR.
     */
    @Query("SELECT SUM(monto) FROM pagos WHERE turnoId = :turnoId AND metodoPago = 'qr'")
    Double getTotalQrByTurno(int turnoId);

    /**
     * Observa de forma reactiva el historial completo de pagos ordenados cronológicamente inverso.
     * 
     * @return LiveData emitiendo la lista de pagos en tiempo real ante inserciones.
     */
    @Query("SELECT * FROM pagos ORDER BY id DESC")
    LiveData<List<PagoEntity>> getAllLiveData();

    /**
     * Consulta síncrona del histórico de pagos ordenados descendentemente.
     * 
     * @return Lista en memoria de todas las transacciones de pago registradas.
     */
    @Query("SELECT * FROM pagos ORDER BY id DESC")
    List<PagoEntity> getAll();
}
