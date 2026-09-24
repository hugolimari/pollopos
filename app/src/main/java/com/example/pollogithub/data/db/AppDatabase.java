package com.example.pollogithub.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.pollogithub.R;
import com.example.pollogithub.data.dao.CategoriaDao;
import com.example.pollogithub.data.dao.InsumoDao;
import com.example.pollogithub.data.dao.MovimientoCajaDao;
import com.example.pollogithub.data.dao.PagoDao;
import com.example.pollogithub.data.dao.PedidoDao;
import com.example.pollogithub.data.dao.PedidoDetalleDao;
import com.example.pollogithub.data.dao.ProductoDao;
import com.example.pollogithub.data.dao.RecetaInsumoDao;
import com.example.pollogithub.data.dao.SucursalDao;
import com.example.pollogithub.data.dao.TurnoDao;
import com.example.pollogithub.data.dao.UsuarioDao;
import com.example.pollogithub.data.entity.CategoriaEntity;
import com.example.pollogithub.data.entity.InsumoEntity;
import com.example.pollogithub.data.entity.MovimientoCajaEntity;
import com.example.pollogithub.data.entity.PagoEntity;
import com.example.pollogithub.data.entity.PedidoDetalleEntity;
import com.example.pollogithub.data.entity.PedidoEntity;
import com.example.pollogithub.data.entity.ProductoEntity;
import com.example.pollogithub.data.entity.RecetaInsumoEntity;
import com.example.pollogithub.data.entity.RolEntity;
import com.example.pollogithub.data.entity.SucursalEntity;
import com.example.pollogithub.data.entity.TurnoEntity;
import com.example.pollogithub.data.entity.UsuarioEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Base de Datos Principal de la Aplicación: AppDatabase
 * 
 * Capa de Persistencia / Motor SQLite gestionado mediante Room ORM
 * 
 * Centraliza la definición del esquema relacional local, el registro de entidades
 * y la exposición de los Objetos de Acceso a Datos (DAOs). Implementa el patrón
 * Singleton con inicialización perezosa (Lazy Initialization) y bloqueo de doble comprobación
 * (Double-Checked Locking) para garantizar una única instancia de la base de datos
 * compartida en toda la aplicación.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 2.0
 */
@Database(entities = {
        SucursalEntity.class,
        RolEntity.class,
        UsuarioEntity.class,
        CategoriaEntity.class,
        ProductoEntity.class,
        TurnoEntity.class,
        PedidoEntity.class,
        PedidoDetalleEntity.class,
        PagoEntity.class,
        MovimientoCajaEntity.class,
        InsumoEntity.class,
        RecetaInsumoEntity.class
}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    /**
     * Referencia volátil de la instancia única según el patrón Singleton.
     * La palabra clave 'volatile' asegura visibilidad inmediata entre diferentes hilos.
     */
    private static volatile AppDatabase INSTANCE;

    /**
     * Pool de subprocesos dedicado a operaciones asíncronas de base de datos.
     * Configurado con 4 hilos de ejecución concurrentes.
     */
    private static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(4);

    // ==========================================
    // MÉTODOS DE FÁBRICA ABSTRACTOS PARA DAOs
    // Implementados automáticamente por Room
    // ==========================================

    public abstract SucursalDao sucursalDao();
    public abstract UsuarioDao usuarioDao();
    public abstract CategoriaDao categoriaDao();
    public abstract ProductoDao productoDao();
    public abstract TurnoDao turnoDao();
    public abstract PedidoDao pedidoDao();
    public abstract PedidoDetalleDao pedidoDetalleDao();
    public abstract PagoDao pagoDao();
    public abstract MovimientoCajaDao movimientoCajaDao();
    public abstract InsumoDao insumoDao();
    public abstract RecetaInsumoDao recetaInsumoDao();

    /**
     * Proporciona acceso global al ejecutor de subprocesos de base de datos.
     * 
     * @return ExecutorService configurado para tareas de escritura y lectura pesadas.
     */
    public static ExecutorService getDatabaseWriteExecutor() {
        return databaseWriteExecutor;
    }

    /**
     * Retorna la instancia Singleton de AppDatabase, instanciándola si aún no existe.
     * 
     * @param context Contexto de la aplicación Android.
     * @return Instancia única de AppDatabase.
     */
    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "brasa_pos_database")
                            .fallbackToDestructiveMigration()
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    databaseWriteExecutor.execute(() -> checkAndPrepopulate(INSTANCE));
                                }

                                @Override
                                public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
                                    super.onDestructiveMigration(db);
                                    databaseWriteExecutor.execute(() -> checkAndPrepopulate(INSTANCE));
                                }

                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    databaseWriteExecutor.execute(() -> checkAndPrepopulate(INSTANCE));
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Verifica si la base de datos carece de usuarios o catálogos y ejecuta la siembra inicial si es necesario.
     * 
     * @param db Referencia a la instancia de AppDatabase.
     */
    public static void checkAndPrepopulate(AppDatabase db) {
        if (db == null) return;
        try {
            if (db.usuarioDao().count() == 0) {
                prepopulateData(db);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Ejecuta la inserción masiva inicial de datos (Data Seeding) dentro de una transacción atómica.
     * Garantiza que la aplicación disponga de sucursal, operadores, categorías y catálogo listos para operar.
     * 
     * @param db Referencia a la instancia de la base de datos construida.
     */
    private static void prepopulateData(AppDatabase db) {
        if (db == null) return;
        db.runInTransaction(() -> {
            if (db.usuarioDao().count() > 0) {
                return;
            }

            // 1. Inserción o reutilización de la Sede Operativa Principal
            SucursalEntity sucursalExistente = db.sucursalDao().getFirst();
            long sucursalId;
            if (sucursalExistente != null) {
                sucursalId = sucursalExistente.getId();
            } else {
                SucursalEntity sucursal = new SucursalEntity("Sucursal Centro", "Av. Principal #123", "77788990", true);
                sucursalId = db.sucursalDao().insert(sucursal);
            }

            // 2. Siembra de Usuarios y Roles predeterminados (Admin y Cajero)
            UsuarioEntity admin = new UsuarioEntity((int) sucursalId, 1, "Administrador", "admin", "1234", "1234", true);
            UsuarioEntity cajero = new UsuarioEntity((int) sucursalId, 2, "Carlos Méndez", "carlos", "1234", "1234", true);
            db.usuarioDao().insert(admin);
            db.usuarioDao().insert(cajero);

            // 3. Taxonomía de Categorías de Productos
            if (db.categoriaDao().count() == 0) {
                List<CategoriaEntity> categorias = new ArrayList<>();
                categorias.add(new CategoriaEntity("Pollo frito", 1));
                categorias.add(new CategoriaEntity("A la brasa", 2));
                categorias.add(new CategoriaEntity("Combos", 3));
                categorias.add(new CategoriaEntity("Bebidas", 4));
                categorias.add(new CategoriaEntity("Acompañamientos", 5));
                db.categoriaDao().insertAll(categorias);
            }

            // 4. Catálogo de Artículos de Venta con referencias a recursos drawables locales
            if (db.productoDao().count() == 0) {
                List<ProductoEntity> productos = new ArrayList<>();
                productos.add(new ProductoEntity((int) sucursalId, 1, "Presa individual", "Pierna o pechuga", 8.50, true, "", R.drawable.bg_thumb_fried));
                productos.add(new ProductoEntity((int) sucursalId, 1, "1/4 de pollo frito", "Con papas incluidas", 14.00, true, "", R.drawable.bg_thumb_fried));
                productos.add(new ProductoEntity((int) sucursalId, 2, "1/2 pollo a la brasa", "Con papas y ensalada", 24.00, true, "", R.drawable.bg_thumb_asado));
                productos.add(new ProductoEntity((int) sucursalId, 3, "Combo Familiar", "Pollo entero + 2 gaseosas", 52.00, true, "", R.drawable.bg_thumb_combo));
                productos.add(new ProductoEntity((int) sucursalId, 4, "Gaseosa 500ml", "Varios sabores", 4.00, true, "", R.drawable.bg_thumb_bebida));
                productos.add(new ProductoEntity((int) sucursalId, 5, "Papas fritas", "Porción regular", 6.00, true, "", R.drawable.bg_thumb_fried));
                db.productoDao().insertAll(productos);
            }

            // 5. Insumos Crudos (Inventario de Materia Prima)
            if (db.insumoDao().count() == 0) {
                List<InsumoEntity> insumos = new ArrayList<>();
                insumos.add(new InsumoEntity((int) sucursalId, "Pollo entero crudo", "unidades", 40.0, 5.0, 22.00));
                insumos.add(new InsumoEntity((int) sucursalId, "Papas para freír", "kg", 30.0, 8.0, 6.50));
                insumos.add(new InsumoEntity((int) sucursalId, "Gaseosa 500ml", "unidades", 50.0, 10.0, 2.80));
                insumos.add(new InsumoEntity((int) sucursalId, "Aceite freidora", "litros", 20.0, 5.0, 12.00));
                db.insumoDao().insertAll(insumos);
            }

            // 6. Recetas de Conversión de Insumos por Producto
            if (db.recetaInsumoDao().count() == 0) {
                List<RecetaInsumoEntity> recetas = new ArrayList<>();
                // Presa individual -> 0.125 pollo (1/8)
                recetas.add(new RecetaInsumoEntity(1, 1, 0.125));
                // 1/4 de pollo frito -> 0.25 pollo + 0.25 kg papas
                recetas.add(new RecetaInsumoEntity(2, 1, 0.25));
                recetas.add(new RecetaInsumoEntity(2, 2, 0.25));
                // 1/2 pollo a la brasa -> 0.50 pollo + 0.35 kg papas
                recetas.add(new RecetaInsumoEntity(3, 1, 0.50));
                recetas.add(new RecetaInsumoEntity(3, 2, 0.35));
                // Combo Familiar -> 1.0 pollo + 0.60 kg papas + 2 gaseosas
                recetas.add(new RecetaInsumoEntity(4, 1, 1.00));
                recetas.add(new RecetaInsumoEntity(4, 2, 0.60));
                recetas.add(new RecetaInsumoEntity(4, 3, 2.00));
                // Gaseosa 500ml -> 1.0 gaseosa
                recetas.add(new RecetaInsumoEntity(5, 3, 1.00));
                // Papas fritas -> 0.35 kg papas
                recetas.add(new RecetaInsumoEntity(6, 2, 0.35));
                db.recetaInsumoDao().insertAll(recetas);
            }

            // 7. Turno Inicial de Operación en Caja (si no existe turno)
            if (db.turnoDao().getTurnoActivo() == null) {
                TurnoEntity turno = new TurnoEntity((int) sucursalId, 1, 100.00, System.currentTimeMillis() - 3600000, null, null, null, null, "abierto");
                long turnoId = db.turnoDao().insert(turno);

                // Movimiento inicial de prueba en caja chica (Gasto de carbón)
                db.movimientoCajaDao().insert(new MovimientoCajaEntity((int) turnoId, "EGRESO", 25.00, "Compra de carbón vegetal", System.currentTimeMillis() - 3000000));

                // Transacciones Demo: Pedido en cocina (Local)
                PedidoEntity p1 = new PedidoEntity((int) sucursalId, (int) turnoId, 1, null, 231, "mesa", "cocina", "pendiente", 41.40, null, 0.0, 41.40, null, System.currentTimeMillis() - 180000);
                long p1Id = db.pedidoDao().insert(p1);
                db.pedidoDetalleDao().insert(new PedidoDetalleEntity((int) p1Id, 2, "1/4 de pollo frito", 1, 14.00, 14.00, "Pierna · bien dorado"));
                db.pedidoDetalleDao().insert(new PedidoDetalleEntity((int) p1Id, 3, "1/2 pollo a la brasa", 1, 24.00, 24.00, "Sin ensalada"));
                db.pedidoDetalleDao().insert(new PedidoDetalleEntity((int) p1Id, 5, "Gaseosa 500ml", 2, 4.00, 8.00, ""));

                // Transacciones Demo: Pedido para llevar
                PedidoEntity p2 = new PedidoEntity((int) sucursalId, (int) turnoId, 1, null, 230, "para_llevar", "cocina", "pendiente", 58.00, null, 0.0, 58.00, null, System.currentTimeMillis() - 360000);
                long p2Id = db.pedidoDao().insert(p2);
                db.pedidoDetalleDao().insert(new PedidoDetalleEntity((int) p2Id, 4, "Combo Familiar", 1, 52.00, 52.00, "Salsa aparte"));
                db.pedidoDetalleDao().insert(new PedidoDetalleEntity((int) p2Id, 6, "Papas fritas", 1, 6.00, 6.00, "sin sal"));

                // Transacciones Demo: Pedido completado y liquidado en efectivo
                PedidoEntity p3 = new PedidoEntity((int) sucursalId, (int) turnoId, 1, null, 226, "mesa", "listo", "pagado", 52.00, null, 0.0, 52.00, null, System.currentTimeMillis() - 1080000);
                long p3Id = db.pedidoDao().insert(p3);
                db.pedidoDetalleDao().insert(new PedidoDetalleEntity((int) p3Id, 4, "Combo Familiar", 1, 52.00, 52.00, ""));
                db.pagoDao().insert(new PagoEntity((int) p3Id, (int) turnoId, "efectivo", 52.00, 60.00, 8.00, "", System.currentTimeMillis() - 1080000));
            }
        });
    }
}
