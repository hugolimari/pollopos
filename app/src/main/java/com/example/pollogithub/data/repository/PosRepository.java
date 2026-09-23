package com.example.pollogithub.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.example.pollogithub.Product;
import com.example.pollogithub.data.SessionManager;
import com.example.pollogithub.data.db.AppDatabase;
import com.example.pollogithub.data.entity.CategoriaEntity;
import com.example.pollogithub.data.entity.PagoEntity;
import com.example.pollogithub.data.entity.PedidoDetalleEntity;
import com.example.pollogithub.data.entity.PedidoEntity;
import com.example.pollogithub.data.entity.ProductoEntity;
import com.example.pollogithub.data.entity.TurnoEntity;
import com.example.pollogithub.data.entity.UsuarioEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class PosRepository {

    private static volatile PosRepository INSTANCE;
    private final AppDatabase db;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SessionManager sessionManager;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    private PosRepository(Context context) {
        db = AppDatabase.getInstance(context);
        executor = AppDatabase.getDatabaseWriteExecutor();
        sessionManager = new SessionManager(context);
    }

    public static PosRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (PosRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PosRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    // --- AUTENTICACIÓN ---
    public void login(String userOrPin, String password, Callback<UsuarioEntity> callback) {
        executor.execute(() -> {
            UsuarioEntity usuario = null;
            if (password == null || password.isEmpty()) {
                // Intento login por PIN rápido
                usuario = db.usuarioDao().loginWithPin(userOrPin);
            } else {
                usuario = db.usuarioDao().login(userOrPin, password);
            }

            final UsuarioEntity result = usuario;
            mainHandler.post(() -> {
                if (result != null) {
                    // Guardar sesión
                    TurnoEntity turnoActivo = null;
                    callback.onSuccess(result);
                } else {
                    callback.onError("Usuario o contraseña incorrectos");
                }
            });
        });
    }

    // --- TURNOS ---
    public void getTurnoActivo(Callback<TurnoEntity> callback) {
        executor.execute(() -> {
            TurnoEntity turno = db.turnoDao().getTurnoActivo();
            mainHandler.post(() -> callback.onSuccess(turno));
        });
    }

    public void abrirTurno(double fondoInicial, int usuarioId, int sucursalId, Callback<TurnoEntity> callback) {
        executor.execute(() -> {
            TurnoEntity activo = db.turnoDao().getTurnoActivo();
            if (activo != null) {
                mainHandler.post(() -> callback.onSuccess(activo));
                return;
            }
            TurnoEntity nuevo = new TurnoEntity(sucursalId, usuarioId, fondoInicial, System.currentTimeMillis(), null, null, null, null, "abierto");
            long id = db.turnoDao().insert(nuevo);
            nuevo.setId((int) id);
            sessionManager.setTurnoId((int) id);
            mainHandler.post(() -> callback.onSuccess(nuevo));
        });
    }

    public void cerrarTurno(int turnoId, double efectivoContado, Callback<TurnoEntity> callback) {
        executor.execute(() -> {
            TurnoEntity turno = db.turnoDao().getById(turnoId);
            if (turno == null) {
                mainHandler.post(() -> callback.onError("Turno no encontrado"));
                return;
            }

            Double efectivoVentas = db.pagoDao().getTotalEfectivoByTurno(turnoId);
            if (efectivoVentas == null) efectivoVentas = 0.0;

            double esperado = turno.getFondoInicial() + efectivoVentas;
            double diferencia = efectivoContado - esperado;

            turno.setCerradoEn(System.currentTimeMillis());
            turno.setEfectivoEsperado(esperado);
            turno.setEfectivoContado(efectivoContado);
            turno.setDiferencia(diferencia);
            turno.setEstado("cerrado");

            db.turnoDao().update(turno);
            mainHandler.post(() -> callback.onSuccess(turno));
        });
    }

    // --- PRODUCTOS Y CATEGORÍAS ---
    public LiveData<List<ProductoEntity>> getProductosLiveData() {
        return db.productoDao().getAllLiveData();
    }

    public LiveData<List<CategoriaEntity>> getCategoriasLiveData() {
        return db.categoriaDao().getAllLiveData();
    }

    public void insertProducto(ProductoEntity producto, Callback<Long> callback) {
        executor.execute(() -> {
            long id = db.productoDao().insert(producto);
            producto.setId((int) id);
            mainHandler.post(() -> {
                if (callback != null) callback.onSuccess(id);
            });
        });
    }

    public void updateProducto(ProductoEntity producto, Callback<Void> callback) {
        executor.execute(() -> {
            db.productoDao().update(producto);
            mainHandler.post(() -> {
                if (callback != null) callback.onSuccess(null);
            });
        });
    }

    public void setProductoDisponible(int id, boolean disponible, Callback<Void> callback) {
        executor.execute(() -> {
            db.productoDao().setDisponible(id, disponible);
            mainHandler.post(() -> {
                if (callback != null) callback.onSuccess(null);
            });
        });
    }

    // --- TURNOS HISTÓRICOS ---
    public LiveData<List<TurnoEntity>> getAllTurnosLiveData() {
        return db.turnoDao().getAllLiveData();
    }

    public void getHistorialTurnos(Callback<List<TurnoEntity>> callback) {
        executor.execute(() -> {
            List<TurnoEntity> turnos = db.turnoDao().getAllLiveData().getValue();
            mainHandler.post(() -> {
                if (callback != null) callback.onSuccess(turnos);
            });
        });
    }

    // --- PEDIDOS ---
    public void crearPedido(String tipoEntrega, Integer mesaId, List<Product> cartProducts, Callback<PedidoEntity> callback) {
        executor.execute(() -> {
            int turnoId = sessionManager.getTurnoId();
            int usuarioId = sessionManager.getUserId();
            int sucursalId = sessionManager.getSucursalId();

            Integer maxOrden = db.pedidoDao().getMaxNumeroOrden();
            int nextOrden = (maxOrden == null || maxOrden < 200) ? 232 : maxOrden + 1;

            double subtotal = 0.0;
            for (Product p : cartProducts) {
                if (p.getQuantityInCart() > 0) {
                    subtotal += p.getPrice() * p.getQuantityInCart();
                }
            }

            double total = subtotal; // sin descuento inicial

            PedidoEntity pedido = new PedidoEntity(
                    sucursalId, turnoId, usuarioId, mesaId, nextOrden,
                    tipoEntrega, "cocina", "pendiente", subtotal,
                    null, 0.0, total, null, System.currentTimeMillis()
            );

            long pedidoId = db.pedidoDao().insert(pedido);
            pedido.setId((int) pedidoId);

            List<PedidoDetalleEntity> detalles = new ArrayList<>();
            for (Product p : cartProducts) {
                if (p.getQuantityInCart() > 0) {
                    detalles.add(new PedidoDetalleEntity(
                            (int) pedidoId,
                            p.getId(),
                            p.getName(),
                            p.getQuantityInCart(),
                            p.getPrice(),
                            p.getPrice() * p.getQuantityInCart(),
                            ""
                    ));
                }
            }
            db.pedidoDetalleDao().insertAll(detalles);

            mainHandler.post(() -> callback.onSuccess(pedido));
        });
    }

    public LiveData<List<PedidoEntity>> getPedidosLiveData() {
        return db.pedidoDao().getAllLiveData();
    }

    public LiveData<List<PedidoEntity>> getPedidosByEstadoLiveData(String estado) {
        return db.pedidoDao().getByEstadoLiveData(estado);
    }

    public void getPedidoDetalles(int pedidoId, Callback<List<PedidoDetalleEntity>> callback) {
        executor.execute(() -> {
            List<PedidoDetalleEntity> detalles = db.pedidoDetalleDao().getByPedidoId(pedidoId);
            mainHandler.post(() -> callback.onSuccess(detalles));
        });
    }

    public void updatePedidoEstado(int pedidoId, String nuevoEstado, Callback<Void> callback) {
        executor.execute(() -> {
            db.pedidoDao().updateEstado(pedidoId, nuevoEstado);
            mainHandler.post(() -> {
                if (callback != null) callback.onSuccess(null);
            });
        });
    }

    public void cancelarPedido(int pedidoId, String motivo, Callback<Void> callback) {
        executor.execute(() -> {
            db.pedidoDao().cancelarPedido(pedidoId, motivo);
            mainHandler.post(() -> {
                if (callback != null) callback.onSuccess(null);
            });
        });
    }

    // --- PAGOS ---
    public void registrarPago(int pedidoId, String metodoPago, double total, double recibido, double vuelto, Callback<PagoEntity> callback) {
        executor.execute(() -> {
            int turnoId = sessionManager.getTurnoId();
            PagoEntity pago = new PagoEntity(pedidoId, turnoId, metodoPago.toLowerCase(), total, recibido, vuelto, "", System.currentTimeMillis());
            long pagoId = db.pagoDao().insert(pago);
            pago.setId((int) pagoId);

            db.pedidoDao().updateEstadoPago(pedidoId, "pagado");

            mainHandler.post(() -> callback.onSuccess(pago));
        });
    }

    // --- REPORTES ---
    public static class ResumenTurno {
        public double totalVentas;
        public double totalEfectivo;
        public double totalTarjeta;
        public double totalQr;
        public int totalPedidos;
        public double fondoInicial;
        public double esperado;
    }

    public void getResumenTurno(int turnoId, Callback<ResumenTurno> callback) {
        executor.execute(() -> {
            TurnoEntity turno = db.turnoDao().getById(turnoId);
            double fondo = turno != null ? turno.getFondoInicial() : 0.0;

            Double totalVentas = db.pagoDao().getTotalVentasByTurno(turnoId);
            if (totalVentas == null) totalVentas = 0.0;

            Double totalEfectivo = db.pagoDao().getTotalEfectivoByTurno(turnoId);
            if (totalEfectivo == null) totalEfectivo = 0.0;

            Double totalTarjeta = db.pagoDao().getTotalTarjetaByTurno(turnoId);
            if (totalTarjeta == null) totalTarjeta = 0.0;

            Double totalQr = db.pagoDao().getTotalQrByTurno(turnoId);
            if (totalQr == null) totalQr = 0.0;

            List<PagoEntity> pagos = db.pagoDao().getByTurnoId(turnoId);
            int countPedidos = pagos != null ? pagos.size() : 0;

            ResumenTurno resumen = new ResumenTurno();
            resumen.fondoInicial = fondo;
            resumen.totalVentas = totalVentas;
            resumen.totalEfectivo = totalEfectivo;
            resumen.totalTarjeta = totalTarjeta;
            resumen.totalQr = totalQr;
            resumen.totalPedidos = countPedidos;
            resumen.esperado = fondo + totalEfectivo;

            mainHandler.post(() -> callback.onSuccess(resumen));
        });
    }

    public static class EstadisticasReporte {
        public double totalVentas;
        public int totalPedidos;
        public double ticketPromedio;
        public int pedidosMesa;
        public int pedidosLlevar;
        public String horaPico = "12:00 PM - 2:00 PM";
        public double totalEfectivo;
        public double totalTarjeta;
        public double totalQr;
        public String productoMasVendido = "1/4 de pollo frito";
        public int productoMasVendidoCantidad = 0;
    }

    public void getEstadisticasReporte(Callback<EstadisticasReporte> callback) {
        executor.execute(() -> {
            EstadisticasReporte stats = new EstadisticasReporte();
            List<PagoEntity> todosLosPagos = db.pagoDao().getAll();
            double suma = 0.0;
            double ef = 0.0, tj = 0.0, qr = 0.0;
            if (todosLosPagos != null) {
                for (PagoEntity p : todosLosPagos) {
                    suma += p.getMonto();
                    if ("efectivo".equalsIgnoreCase(p.getMetodoPago())) ef += p.getMonto();
                    else if ("tarjeta".equalsIgnoreCase(p.getMetodoPago())) tj += p.getMonto();
                    else if ("qr".equalsIgnoreCase(p.getMetodoPago())) qr += p.getMonto();
                }
                stats.totalPedidos = todosLosPagos.size();
            }
            stats.totalVentas = suma;
            stats.totalEfectivo = ef;
            stats.totalTarjeta = tj;
            stats.totalQr = qr;
            stats.ticketPromedio = stats.totalPedidos > 0 ? (stats.totalVentas / stats.totalPedidos) : 0.0;

            List<PedidoEntity> todosPedidos = db.pedidoDao().getAll();
            int mesa = 0, llevar = 0;
            if (todosPedidos != null) {
                for (PedidoEntity pe : todosPedidos) {
                    if ("mesa".equalsIgnoreCase(pe.getTipoEntrega())) mesa++;
                    else llevar++;
                }
            }
            stats.pedidosMesa = mesa;
            stats.pedidosLlevar = llevar;

            mainHandler.post(() -> callback.onSuccess(stats));
        });
    }
}
