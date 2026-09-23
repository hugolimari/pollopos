package com.example.pollogithub.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.pollogithub.data.entity.PedidoEntity;

import java.util.List;

/**
 * Objeto de Acceso a Datos: PedidoDao
 * 
 * Capa de Persistencia / Patrón DAO (Data Access Object)
 * Interfaz compilada por Room ORM para la tabla 'pedidos'.
 * 
 * Gestiona el ciclo de vida CRUD y las transiciones de estado de las órdenes del restaurante.
 * Administra la numeración secuencial de comandas, consultas de visualización en cocina,
 * filtrado por estados logísticos y auditoría de cancelaciones.
 * 
 * Conceptos de Ingeniería aplicados:
 * - Persistencia y Transición de Estados: Actualizaciones parametrizadas atómicas de 'estado' y 'estadoPago'.
 * - Generación de Secuencias Autoincrementales de Negocio: Obtención del valor máximo de 'numeroOrden'
 *   para garantizar correlativos únicos por jornada.
 * - Monitoreo Reactivo: Exposición de flujos LiveData para actualización inmediata en pantallas KDS (Kitchen Display System).
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
@Dao
public interface PedidoDao {

    /**
     * Persiste una orden cabecera en el almacenamiento relacional.
     * 
     * @param pedido Instancia con la cabecera y montos de la transacción.
     * @return Identificador primario autogenerado para el nuevo pedido.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PedidoEntity pedido);

    /**
     * Actualiza la totalidad de los campos de un pedido existente.
     * 
     * @param pedido Entidad modificada a sincronizar.
     */
    @Update
    void update(PedidoEntity pedido);

    /**
     * Flujo reactivo de todos los pedidos ordenados de forma descendente (más recientes primero).
     * 
     * @return LiveData emitiendo la colección completa ante cualquier mutación en la tabla.
     */
    @Query("SELECT * FROM pedidos ORDER BY id DESC")
    LiveData<List<PedidoEntity>> getAllLiveData();

    /**
     * Consulta síncrona en lote de todas las órdenes registradas.
     * 
     * @return Lista en memoria de pedidos.
     */
    @Query("SELECT * FROM pedidos ORDER BY id DESC")
    List<PedidoEntity> getAll();

    /**
     * Observador reactivo filtrado por estado logístico (ej. órdenes en "cocina" o "listo").
     * Vital para la pantalla de visualización operativa del área culinaria.
     * 
     * @param estado Estado de control logístico solicitado.
     * @return LiveData con las órdenes correspondientes al estado indicado.
     */
    @Query("SELECT * FROM pedidos WHERE estado = :estado ORDER BY id DESC")
    LiveData<List<PedidoEntity>> getByEstadoLiveData(String estado);

    /**
     * Consulta síncrona puntual de una orden por su identificador primario.
     * 
     * @param id Clave primaria del pedido.
     * @return Instancia PedidoEntity encontrada o null.
     */
    @Query("SELECT * FROM pedidos WHERE id = :id LIMIT 1")
    PedidoEntity getById(int id);

    /**
     * Observador reactivo puntual para la vista de detalle de una orden particular.
     * 
     * @param id Clave primaria del pedido.
     * @return LiveData emitiendo los cambios específicos de esta orden.
     */
    @Query("SELECT * FROM pedidos WHERE id = :id LIMIT 1")
    LiveData<PedidoEntity> getByIdLiveData(int id);

    /**
     * Mutación focalizada del estado operativo de preparación/entrega.
     * 
     * @param id          Clave primaria del pedido.
     * @param nuevoEstado Cadena que define el nuevo estado ("cocina", "listo", "entregado").
     */
    @Query("UPDATE pedidos SET estado = :nuevoEstado WHERE id = :id")
    void updateEstado(int id, String nuevoEstado);

    /**
     * Mutación focalizada del estado contable de cobro del pedido.
     * 
     * @param id              Clave primaria del pedido.
     * @param nuevoEstadoPago Cadena del estado financiero ("pendiente", "pagado").
     */
    @Query("UPDATE pedidos SET estadoPago = :nuevoEstadoPago WHERE id = :id")
    void updateEstadoPago(int id, String nuevoEstadoPago);

    /**
     * Determina el número de comanda correlativo más alto registrado.
     * Utilizado para calcular el siguiente número de orden visible (max + 1).
     * 
     * @return Valor máximo entero o null si la tabla carece de registros previos.
     */
    @Query("SELECT MAX(numeroOrden) FROM pedidos")
    Integer getMaxNumeroOrden();

    /**
     * Métrica cuantitativa de órdenes agrupadas por estado operativo.
     * 
     * @param estado Estado logístico a contabilizar.
     * @return Cantidad entera de pedidos en dicho estado.
     */
    @Query("SELECT COUNT(*) FROM pedidos WHERE estado = :estado")
    int countByEstado(String estado);

    /**
     * Procedimiento atómico de anulación de orden: cancela el pedido, marca el pago como cancelado
     * y registra de forma indeleble el motivo de justificación para auditoría posterior.
     * 
     * @param id     Clave primaria de la orden a revocar.
     * @param motivo Justificación documentada del cajero o administrador.
     */
    @Query("UPDATE pedidos SET estado = 'cancelado', estadoPago = 'cancelado', motivoCancelacion = :motivo WHERE id = :id")
    void cancelarPedido(int id, String motivo);

    /**
     * Total absoluto de pedidos gestionados por el sistema desde su puesta en marcha.
     * 
     * @return Cantidad de registros en la tabla.
     */
    @Query("SELECT COUNT(*) FROM pedidos")
    int count();
}
