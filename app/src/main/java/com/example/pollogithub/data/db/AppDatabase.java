package com.example.pollogithub.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.pollogithub.R;
import com.example.pollogithub.data.dao.CategoriaDao;
import com.example.pollogithub.data.dao.PagoDao;
import com.example.pollogithub.data.dao.PedidoDao;
import com.example.pollogithub.data.dao.PedidoDetalleDao;
import com.example.pollogithub.data.dao.ProductoDao;
import com.example.pollogithub.data.dao.TurnoDao;
import com.example.pollogithub.data.dao.UsuarioDao;
import com.example.pollogithub.data.entity.CategoriaEntity;
import com.example.pollogithub.data.entity.PagoEntity;
import com.example.pollogithub.data.entity.PedidoDetalleEntity;
import com.example.pollogithub.data.entity.PedidoEntity;
import com.example.pollogithub.data.entity.ProductoEntity;
import com.example.pollogithub.data.entity.RolEntity;
import com.example.pollogithub.data.entity.SucursalEntity;
import com.example.pollogithub.data.entity.TurnoEntity;
import com.example.pollogithub.data.entity.UsuarioEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {
        SucursalEntity.class,
        RolEntity.class,
        UsuarioEntity.class,
        CategoriaEntity.class,
        ProductoEntity.class,
        TurnoEntity.class,
        PedidoEntity.class,
        PedidoDetalleEntity.class,
        PagoEntity.class
}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;
    private static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(4);

    public abstract com.example.pollogithub.data.dao.SucursalDao sucursalDao();
    public abstract UsuarioDao usuarioDao();
    public abstract CategoriaDao categoriaDao();
    public abstract ProductoDao productoDao();
    public abstract TurnoDao turnoDao();
    public abstract PedidoDao pedidoDao();
    public abstract PedidoDetalleDao pedidoDetalleDao();
    public abstract PagoDao pagoDao();

    public static ExecutorService getDatabaseWriteExecutor() {
        return databaseWriteExecutor;
    }

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "brasa_pos_database")
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    databaseWriteExecutor.execute(() -> prepopulateData(INSTANCE));
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static void prepopulateData(AppDatabase db) {
        db.runInTransaction(() -> {
            // Sucursal inicial
            SucursalEntity sucursal = new SucursalEntity("Sucursal Centro", "Av. Principal #123", "77788990", true);
            long sucursalId = db.sucursalDao().insert(sucursal);

            // Usuarios precargados: admin / 1234
            UsuarioEntity admin = new UsuarioEntity((int) sucursalId, 1, "Administrador", "admin", "1234", "1234", true);
            UsuarioEntity cajero = new UsuarioEntity((int) sucursalId, 2, "Carlos Méndez", "carlos", "1234", "1234", true);
            db.usuarioDao().insert(admin);
            db.usuarioDao().insert(cajero);

            // Categorias
            List<CategoriaEntity> categorias = new ArrayList<>();
            categorias.add(new CategoriaEntity("Pollo frito", 1));
            categorias.add(new CategoriaEntity("A la brasa", 2));
            categorias.add(new CategoriaEntity("Combos", 3));
            categorias.add(new CategoriaEntity("Bebidas", 4));
            categorias.add(new CategoriaEntity("Acompañamientos", 5));
            db.categoriaDao().insertAll(categorias);

            // Productos
            List<ProductoEntity> productos = new ArrayList<>();
            productos.add(new ProductoEntity((int) sucursalId, 1, "Presa individual", "Pierna o pechuga", 8.50, true, "🍗", R.drawable.bg_thumb_fried));
            productos.add(new ProductoEntity((int) sucursalId, 1, "1/4 de pollo frito", "Con papas incluidas", 14.00, true, "🍗", R.drawable.bg_thumb_fried));
            productos.add(new ProductoEntity((int) sucursalId, 2, "1/2 pollo a la brasa", "Con papas y ensalada", 24.00, true, "🔥", R.drawable.bg_thumb_asado));
            productos.add(new ProductoEntity((int) sucursalId, 3, "Combo Familiar", "Pollo entero + 2 gaseosas", 52.00, true, "🥤", R.drawable.bg_thumb_combo));
            productos.add(new ProductoEntity((int) sucursalId, 4, "Gaseosa 500ml", "Varios sabores", 4.00, true, "🥤", R.drawable.bg_thumb_bebida));
            productos.add(new ProductoEntity((int) sucursalId, 5, "Papas fritas", "Porción regular", 6.00, true, "🍟", R.drawable.bg_thumb_fried));
            db.productoDao().insertAll(productos);

            // Turno inicial abierto
            TurnoEntity turno = new TurnoEntity((int) sucursalId, 1, 100.00, System.currentTimeMillis() - 3600000, null, null, null, null, "abierto");
            long turnoId = db.turnoDao().insert(turno);

            // Pedidos iniciales de ejemplo (sin delivery: solo mesa y para_llevar)
            PedidoEntity p1 = new PedidoEntity((int) sucursalId, (int) turnoId, 1, 4, 231, "mesa", "cocina", "pendiente", 41.40, null, 0.0, 41.40, null, System.currentTimeMillis() - 180000);
            long p1Id = db.pedidoDao().insert(p1);
            db.pedidoDetalleDao().insert(new PedidoDetalleEntity((int) p1Id, 2, "1/4 de pollo frito", 1, 14.00, 14.00, "bien dorado"));
            db.pedidoDetalleDao().insert(new PedidoDetalleEntity((int) p1Id, 3, "1/2 pollo a la brasa", 1, 24.00, 24.00, ""));
            db.pedidoDetalleDao().insert(new PedidoDetalleEntity((int) p1Id, 5, "Gaseosa 500ml", 2, 4.00, 8.00, ""));

            PedidoEntity p2 = new PedidoEntity((int) sucursalId, (int) turnoId, 1, null, 230, "para_llevar", "cocina", "pendiente", 58.00, null, 0.0, 58.00, null, System.currentTimeMillis() - 360000);
            long p2Id = db.pedidoDao().insert(p2);
            db.pedidoDetalleDao().insert(new PedidoDetalleEntity((int) p2Id, 4, "Combo Familiar", 1, 52.00, 52.00, ""));
            db.pedidoDetalleDao().insert(new PedidoDetalleEntity((int) p2Id, 6, "Papas fritas", 1, 6.00, 6.00, "sin sal"));

            PedidoEntity p3 = new PedidoEntity((int) sucursalId, (int) turnoId, 1, 1, 226, "mesa", "listo", "pagado", 52.00, null, 0.0, 52.00, null, System.currentTimeMillis() - 1080000);
            long p3Id = db.pedidoDao().insert(p3);
            db.pedidoDetalleDao().insert(new PedidoDetalleEntity((int) p3Id, 4, "Combo Familiar", 1, 52.00, 52.00, ""));
            db.pagoDao().insert(new PagoEntity((int) p3Id, (int) turnoId, "efectivo", 52.00, 60.00, 8.00, "", System.currentTimeMillis() - 1080000));
        });
    }
}
