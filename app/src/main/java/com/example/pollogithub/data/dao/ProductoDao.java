package com.example.pollogithub.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.pollogithub.data.entity.ProductoEntity;

import java.util.List;

/**
 * Objeto de Acceso a Datos: ProductoDao
 * 
 * Capa de Persistencia / Patrón DAO (Data Access Object)
 * Interfaz compilada por Room ORM para la tabla 'productos'.
 * 
 * Provee la interfaz programática para la administración del inventario y catálogo.
 * Soporta operaciones de alta, actualización de precios/descripciones, alternancia
 * del estado de disponibilidad operativa (in stock / agotado) y flujos reactivos
 * para la visualización del menú en tiempo real.
 * 
 * Conceptos de Ingeniería aplicados:
 * - Mutación Atómica de Flags: 'setDisponible' actualiza exclusivamente la columna de estado sin sobrecargar el registro.
 * - Reactive Data Streams: Emisión mediante LiveData para refresco instantáneo del catálogo en el punto de venta.
 */
@Dao
public interface ProductoDao {

    /**
     * Inserta un nuevo producto en el catálogo del sistema.
     * 
     * @param producto Entidad del producto con precios, categorías y recursos gráficos.
     * @return Identificador primario autogenerado.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ProductoEntity producto);

    /**
     * Inserta masivamente una colección de productos. Utilizado en la inicialización o sembrado de datos (Seeding).
     * 
     * @param productos Lista de productos a persistir.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProductoEntity> productos);

    /**
     * Actualiza integralmente las propiedades de un producto existente.
     * 
     * @param producto Entidad con los datos modificados.
     */
    @Update
    void update(ProductoEntity producto);

    /**
     * Observa de forma reactiva el inventario completo de productos ordenado por identificador.
     * 
     * @return LiveData que propaga cambios a los adaptadores de la vista de ventas.
     */
    @Query("SELECT * FROM productos ORDER BY id ASC")
    LiveData<List<ProductoEntity>> getAllLiveData();

    /**
     * Consulta síncrona de todo el catálogo de productos.
     * Debe invocarse dentro de un Worker Thread para no penalizar el hilo principal.
     * 
     * @return Colección en memoria de productos.
     */
    @Query("SELECT * FROM productos ORDER BY id ASC")
    List<ProductoEntity> getAll();

    /**
     * Localiza un producto por su clave primaria.
     * 
     * @param id Identificador único del producto.
     * @return ProductoEntity coincidente o null si no se localiza.
     */
    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    ProductoEntity getById(int id);

    /**
     * Modifica de manera selectiva la bandera de disponibilidad comercial del ítem.
     * 
     * @param id         Clave primaria del producto.
     * @param disponible true si el producto se encuentra disponible para venta; false si está agotado.
     */
    @Query("UPDATE productos SET disponible = :disponible WHERE id = :id")
    void setDisponible(int id, boolean disponible);

    /**
     * Contabiliza el número total de artículos en el catálogo.
     * 
     * @return Número entero de registros existentes.
     */
    @Query("SELECT COUNT(*) FROM productos")
    int count();
}
