package com.example.pollogithub.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.example.pollogithub.Product;
import com.example.pollogithub.R;
import com.example.pollogithub.data.SessionManager;
import com.example.pollogithub.data.db.AppDatabase;
import com.example.pollogithub.data.entity.CategoriaEntity;
import com.example.pollogithub.data.entity.PagoEntity;
import com.example.pollogithub.data.entity.PedidoDetalleEntity;
import com.example.pollogithub.data.entity.PedidoEntity;
import com.example.pollogithub.data.entity.ProductoEntity;
import com.example.pollogithub.data.entity.SucursalEntity;
import com.example.pollogithub.data.entity.TurnoEntity;
import com.example.pollogithub.data.entity.UsuarioEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Repositorio central que coordina el acceso a Room Database y SessionManager.
 */
public class PosRepository {

    /**
     * Instancia única compartida según el patrón Singleton.
     */
    private static volatile PosRepository INSTANCE;

    /**
     * Referencia a la base de datos relacional de la aplicación (Room).
     */
    private final AppDatabase db;

    /**
     * Ejecutor multihilo para procesamiento en segundo plano (I/O intensivo).
     */
    private final ExecutorService executor;

    /**
     * Manejador vinculado al ciclo de mensajes del hilo principal (Main Looper).
     */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Administrador de persistencia ligera para credenciales y tokens de sesión.
     */
    private final SessionManager sessionManager;

    private final Context appContext;

    /**
     * Interfaz genérica de comunicación asíncrona (Observer / Callback Pattern).
     * 
     * @param <T> Tipo de dato esperado como resultado de la operación.
     */
    public interface Callback<T> {
        /**
         * Notificación de culminación exitosa en el hilo principal.
         * 
         * @param result Carga útil devuelta por la operación.
         */
        void onSuccess(T result);

        /**
         * Notificación de anomalía o fallo de validación en el hilo principal.
         * 
         * @param error Mensaje descriptivo del error presentado.
         */
        void onError(String error);
    }

    /**
     * Constructor privado que previene la instanciación externa directa (Principio Singleton).
     * 
     * @param context Contexto de la aplicación para inicializar la base de datos y preferencias.
     */
    private PosRepository(Context context) {
        this.appContext = context.getApplicationContext();
        db = AppDatabase.getInstance(context);
        executor = AppDatabase.getDatabaseWriteExecutor();
        sessionManager = new SessionManager(context);
        executor.execute(() -> AppDatabase.checkAndPrepopulate(db));
    }

    /**
     * Punto de acceso global thread-safe a la instancia del repositorio.
     * 
     * @param context Contexto de Android.
     * @return Instancia única de PosRepository.
     */
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

    /**
     * Obtiene el gestor de sesiones de usuario.
     * 
     * @return Instancia de SessionManager.
     */
    public SessionManager getSessionManager() {
        return sessionManager;
    }

    // ==========================================
    // MÓDULO: AUTENTICACIÓN Y SEGURIDAD
    // ==========================================

    /**
     * Autentica a un usuario según su nombre de usuario/contraseña o código PIN rápido.
     * Procesa la consulta en un Worker Thread y devuelve el resultado en el UI Thread.
     * 
     * @param userOrPin Identificador alfanumérico o PIN de acceso.
     * @param password  Contraseña de acceso (opcional si se utiliza PIN numérico).
     * @param callback  Receptor asíncrono del resultado de autenticación.
     */
    public void login(String userOrPin, String password, Callback<UsuarioEntity> callback) {
        executor.execute(() -> {
            AppDatabase.checkAndPrepopulate(db);
            UsuarioEntity usuario = null;
            if (password == null || password.isEmpty()) {
                // Estrategia de autenticación acelerada por PIN
                usuario = db.usuarioDao().loginWithPin(userOrPin);
            } else {
                // Estrategia convencional de autenticación por credenciales
                usuario = db.usuarioDao().login(userOrPin, password);
            }

            final UsuarioEntity result = usuario;
            mainHandler.post(() -> {
                if (result != null) {
                    callback.onSuccess(result);
                } else {
                    callback.onError(appContext.getString(R.string.error_credenciales_invalidas));
                }
            });
        });
    }

    // ==========================================
    // MÓDULO: GESTIÓN DE TURNOS Y ARQUEOS DE CAJA
    // ==========================================

    /**
     * Consulta asíncrona para determinar si existe un turno de caja actualmente abierto.
     * 
     * @param callback Callback que retorna el TurnoEntity activo o null si no existe.
     */
    public void getTurnoActivo(Callback<TurnoEntity> callback) {
        executor.execute(() -> {
            TurnoEntity turno = db.turnoDao().getTurnoActivo();
            mainHandler.post(() -> callback.onSuccess(turno));
        });
    }

    /**
     * Consulta asíncrona de la sucursal activa vinculada a la sesión o primera registrada.
     */
    public void getSucursalActiva(Callback<SucursalEntity> callback) {
        executor.execute(() -> {
            int sucursalId = sessionManager.getSucursalId();
            SucursalEntity s = sucursalId > 0 ? db.sucursalDao().getById(sucursalId) : null;
            if (s == null) {
                s = db.sucursalDao().getFirst();
            }
            final SucursalEntity result = s;
            mainHandler.post(() -> {
                if (callback != null) callback.onSuccess(result);
            });
        });
    }

    /**
     * Registra formalmente la apertura de un turno de caja con su fondo inicial.
     * Si ya existiese un turno en estado abierto, previene la duplicidad y retorna el vigente.
     * 
     * @param fondoInicial Monto monetario de cambio inicial.
     * @param usuarioId    Identificador del cajero responsable.
     * @param sucursalId   Identificador de la sede operativa.
     * @param callback     Callback con la entidad de turno creada o reanudada.
     */
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

    /**
     * Ejecuta el cierre formal y arqueo contable del turno de caja.
     * Consolida los ingresos en efectivo registrados en pagos y calcula la discrepancia:
     * esperado = fondoInicial + sumatoria(efectivo)
     * diferencia = efectivoContado - esperado
     * 
     * @param turnoId         Identificador del turno a liquidar.
     * @param efectivoContado Monto físico real contabilizado por el cajero.
     * @param callback        Callback con la entidad de turno actualizada y cerrada.
     */
    public void cerrarTurno(int turnoId, double efectivoContado, Callback<TurnoEntity> callback) {
        executor.execute(() -> {
            TurnoEntity turno = turnoId > 0 ? db.turnoDao().getById(turnoId) : null;
            if (turno == null) {
                turno = db.turnoDao().getTurnoActivo();
            }
            if (turno == null) {
                mainHandler.post(() -> callback.onError(appContext.getString(R.string.error_turno_no_encontrado)));
                return;
            }

            int actualTurnoId = turno.getId();
            List<PagoEntity> pagos = db.pagoDao().getByTurnoId(actualTurnoId);
            DesglosePagos desglose = calcularDesglosePagos(pagos);

            Double totalEgresos = db.movimientoCajaDao().getTotalEgresosByTurno(actualTurnoId);
            if (totalEgresos == null) totalEgresos = 0.0;

            Double totalIngresos = db.movimientoCajaDao().getTotalIngresosExtraByTurno(actualTurnoId);
            if (totalIngresos == null) totalIngresos = 0.0;

            double esperado = turno.getFondoInicial() + desglose.totalEfectivo + totalIngresos - totalEgresos;
            double diferencia = efectivoContado - esperado;

            turno.setCerradoEn(System.currentTimeMillis());
            turno.setEfectivoEsperado(esperado);
            turno.setEfectivoContado(efectivoContado);
            turno.setDiferencia(diferencia);
            turno.setEstado("cerrado");

            db.turnoDao().update(turno);
            sessionManager.setTurnoId(0);
            TurnoEntity finalTurno = turno;
            mainHandler.post(() -> callback.onSuccess(finalTurno));
        });
    }

    // ==========================================
    // MÓDULO: PRODUCTOS Y CATEGORÍAS
    // ==========================================

    /**
     * Expone el catálogo de productos como un flujo observable reactivo.
     * 
     * @return LiveData que emite la lista completa de productos.
     */
    public LiveData<List<ProductoEntity>> getProductosLiveData() {
        return db.productoDao().getAllLiveData();
    }

    /**
     * Expone las categorías comerciales como un flujo observable reactivo.
     * 
     * @return LiveData que emite las categorías ordenadas.
     */
    public LiveData<List<CategoriaEntity>> getCategoriasLiveData() {
        return db.categoriaDao().getAllLiveData();
    }

    /**
     * Inserta un nuevo producto en el catálogo mediante un hilo de trabajo en segundo plano.
     * 
     * @param producto Entidad del producto a registrar.
     * @param callback Callback que retorna el identificador autogenerado.
     */
    public void insertProducto(ProductoEntity producto, Callback<Long> callback) {
        executor.execute(() -> {
            long id = db.productoDao().insert(producto);
            producto.setId((int) id);
            mainHandler.post(() -> {
                if (callback != null) callback.onSuccess(id);
            });
        });
    }

    /**
     * Actualiza la información y precios de un producto existente.
     * 
     * @param producto Entidad modificada.
     * @param callback Callback de confirmación.
     */
    public void updateProducto(ProductoEntity producto, Callback<Void> callback) {
        executor.execute(() -> {
            db.productoDao().update(producto);
            mainHandler.post(() -> {
                if (callback != null) callback.onSuccess(null);
            });
        });
    }

    /**
     * Modifica el estado de disponibilidad operativa (control de stock / agotado) de un producto.
     * 
     * @param id         Clave primaria del producto.
     * @param disponible true si está en inventario; false si está agotado.
     * @param callback   Callback de confirmación.
     */
    public void setProductoDisponible(int id, boolean disponible, Callback<Void> callback) {
        executor.execute(() -> {
            db.productoDao().setDisponible(id, disponible);
            mainHandler.post(() -> {
                if (callback != null) callback.onSuccess(null);
            });
        });
    }

    // ==========================================
    // MÓDULO: AUDITORÍA DE TURNOS
    // ==========================================

    /**
     * Observa reactivamente el historial cronológico de turnos de caja.
     * 
     * @return LiveData con la lista de turnos registrados.
     */
    public LiveData<List<TurnoEntity>> getAllTurnosLiveData() {
        return db.turnoDao().getAllLiveData();
    }

    /**
     * Consulta asíncrona de la lista histórica de turnos para reportes contables.
     * 
     * @param callback Callback que entrega la lista de turnos en memoria.
     */
    public void getHistorialTurnos(Callback<List<TurnoEntity>> callback) {
        executor.execute(() -> {
            List<TurnoEntity> turnos = db.turnoDao().getAllLiveData().getValue();
            mainHandler.post(() -> {
                if (callback != null) callback.onSuccess(turnos);
            });
        });
    }

    // ==========================================
    // MÓDULO: PROCESAMIENTO Y COMPOSICIÓN DE PEDIDOS
    // ==========================================

    /**
     * Genera una orden de venta transaccional completa (Cabecera y Renglones de Detalle).
     * Realiza el cálculo algorítmico del subtotal acumulado según los ítems activos en el carrito
     * y genera el número de orden correlativo para control del consumidor.
     * 
     * @param tipoEntrega  Modalidad de despacho ("mesa", "para_llevar").
     * @param mesaId       Número de mesa o null para órdenes para llevar.
     * @param cartProducts Colección de artículos con cantidad seleccionada.
     * @param callback     Callback con la entidad de cabecera creada y persistida.
     */
    public void crearPedido(String tipoEntrega, Integer mesaId, List<Product> cartProducts, Callback<PedidoEntity> callback) {
        executor.execute(() -> {
            int turnoId = sessionManager.getTurnoId();
            int usuarioId = sessionManager.getUserId();
            int sucursalId = sessionManager.getSucursalId();

            Integer maxOrden = db.pedidoDao().getMaxNumeroOrden();
            int nextOrden = (maxOrden == null || maxOrden <= 0) ? 1 : maxOrden + 1;

            double subtotal = 0.0;
            for (Product p : cartProducts) {
                if (p.getQuantityInCart() > 0) {
                    subtotal += p.getPrice() * p.getQuantityInCart();
                }
            }

            double total = subtotal; // Sin deducciones iniciales

            PedidoEntity pedido = new PedidoEntity(
                    sucursalId, turnoId, usuarioId, mesaId, nextOrden,
                    tipoEntrega, "pendiente_pago", "pendiente", subtotal,
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
                            p.getNotes() != null ? p.getNotes() : ""
                    ));
                }
            }
            db.pedidoDetalleDao().insertAll(detalles);

            mainHandler.post(() -> callback.onSuccess(pedido));
        });
    }

    /**
     * Flujo reactivo de todas las órdenes en el sistema.
     * 
     * @return LiveData de pedidos ordenados por fecha descendente.
     */
    public LiveData<List<PedidoEntity>> getPedidosLiveData() {
        return db.pedidoDao().getAllLiveData();
    }

    /**
     * Flujo reactivo filtrado por estado logístico (ej. KDS en "cocina").
     * 
     * @param estado Estado de la comanda a filtrar.
     * @return LiveData de pedidos coincidentes.
     */
    public LiveData<List<PedidoEntity>> getPedidosByEstadoLiveData(String estado) {
        return db.pedidoDao().getByEstadoLiveData(estado);
    }

    /**
     * Recupera de forma asíncrona las líneas de detalle pertenecientes a un pedido específico.
     * 
     * @param pedidoId Identificador primario de la orden.
     * @param callback Callback que retorna la lista de PedidoDetalleEntity.
     */
    public void getPedidoDetalles(int pedidoId, Callback<List<PedidoDetalleEntity>> callback) {
        executor.execute(() -> {
            List<PedidoDetalleEntity> detalles = db.pedidoDetalleDao().getByPedidoId(pedidoId);
            mainHandler.post(() -> callback.onSuccess(detalles));
        });
    }

    /**
     * Actualiza el estado operativo de una orden (ej. de "cocina" a "listo" o "entregado").
     * 
     * @param pedidoId    Identificador de la orden.
     * @param nuevoEstado Nueva etiqueta de estado.
     * @param callback    Callback de finalización.
     */
    public void updatePedidoEstado(int pedidoId, String nuevoEstado, Callback<Void> callback) {
        executor.execute(() -> {
            db.pedidoDao().updateEstado(pedidoId, nuevoEstado);
            mainHandler.post(() -> {
                if (callback != null) callback.onSuccess(null);
            });
        });
    }

    /**
     * Ejecuta la cancelación formal de un pedido, registrando la justificación de auditoría
     * y revirtiendo el estado de pago.
     * 
     * @param pedidoId Identificador del pedido a rescindir.
     * @param motivo   Causal documentada de anulación.
     * @param callback Callback de confirmación.
     */
    public void cancelarPedido(int pedidoId, String motivo, Callback<Void> callback) {
        executor.execute(() -> {
            db.pedidoDao().cancelarPedido(pedidoId, motivo);
            mainHandler.post(() -> {
                if (callback != null) callback.onSuccess(null);
            });
        });
    }

    // ==========================================
    // MÓDULO: COBRANZA Y REGISTRO DE PAGOS
    // ==========================================

    /**
     * Liquida financieramente un pedido registrando el pago y transicionando el estado contable a 'pagado'.
     * 
     * @param pedidoId   Clave foránea de la orden liquidada.
     * @param metodoPago Modalidad monetaria ("efectivo", "tarjeta", "qr").
     * @param total      Importe neto cobrado.
     * @param recibido   Monto nominal entregado por el cliente.
     * @param vuelto     Diferencial de cambio devuelto.
     * @param callback   Callback con la entidad de pago registrada.
     */
    public void registrarPago(int pedidoId, String metodoPago, double total, double recibido, double vuelto, Callback<PagoEntity> callback) {
        registrarPago(pedidoId, metodoPago, total, recibido, vuelto, "", callback);
    }

    /**
     * Liquida financieramente un pedido registrando el pago, referencia de cobro mixto/digital
     * y transicionando el estado contable a 'pagado'.
     */
    public void registrarPago(int pedidoId, String metodoPago, double total, double recibido, double vuelto, String referencia, Callback<PagoEntity> callback) {
        executor.execute(() -> {
            int turnoId = sessionManager.getTurnoId();
            PagoEntity pago = new PagoEntity(pedidoId, turnoId, metodoPago.toLowerCase(), total, recibido, vuelto, referencia != null ? referencia : "", System.currentTimeMillis());
            long pagoId = db.pagoDao().insert(pago);
            pago.setId((int) pagoId);

            db.pedidoDao().updateEstadoPago(pedidoId, "pagado");
            db.pedidoDao().updateEstado(pedidoId, "cocina");

            mainHandler.post(() -> callback.onSuccess(pago));
        });
    }

    /**
     * Elimina una orden preliminar no confirmada si el cajero abandona la pasarela de cobro.
     */
    public void descartarPedidoNoPagado(int pedidoId) {
        if (pedidoId <= 0) return;
        executor.execute(() -> {
            PedidoEntity pe = db.pedidoDao().getById(pedidoId);
            if (pe != null && !"pagado".equalsIgnoreCase(pe.getEstadoPago())) {
                db.pedidoDetalleDao().deleteByPedidoId(pedidoId);
                db.pedidoDao().deleteById(pedidoId);
            }
        });
    }

    /**
     * Purga cualquier orden preliminar huérfana que haya quedado sin cobrar.
     */
    public void descartarPedidosPendientesHuerfanos() {
        executor.execute(() -> {
            List<PedidoEntity> pendientes = db.pedidoDao().getPedidosNoPagados();
            if (pendientes != null) {
                for (PedidoEntity pe : pendientes) {
                    db.pedidoDetalleDao().deleteByPedidoId(pe.getId());
                    db.pedidoDao().deleteById(pe.getId());
                }
            }
        });
    }

    // ==========================================
    // MÓDULO: ESTRUCTURAS DTO Y REPORTERÍA FINANCIERA
    // ==========================================

    /**
     * DTO (Data Transfer Object) para el informe consolidado del cierre de turno.
     * Encapsula la agregación financiera por métodos de pago, egresos/gastos y saldo esperado en efectivo.
     */
    public static class ResumenTurno {
        public double totalVentas;
        public double totalEfectivo;
        public double totalTarjeta;
        public double totalQr;
        public int totalPedidos;
        public double fondoInicial;
        public double totalEgresosGastos;
        public double totalIngresosExtra;
        public double esperado;
    }

    /**
     * Computa las métricas de recaudación contable para un turno de caja específico,
     * considerando fondo inicial, ventas en efectivo y movimientos de caja chica (egresos e ingresos).
     * 
     * @param turnoId  Identificador del turno a resumir.
     * @param callback Callback que retorna el objeto ResumenTurno consolidado.
     */
    public void getResumenTurno(int turnoId, Callback<ResumenTurno> callback) {
        executor.execute(() -> {
            TurnoEntity turno = db.turnoDao().getById(turnoId);
            double fondo = turno != null ? turno.getFondoInicial() : 0.0;

            List<PagoEntity> pagos = db.pagoDao().getByTurnoId(turnoId);
            DesglosePagos desglose = calcularDesglosePagos(pagos);

            Double totalEgresos = db.movimientoCajaDao().getTotalEgresosByTurno(turnoId);
            if (totalEgresos == null) totalEgresos = 0.0;

            Double totalIngresos = db.movimientoCajaDao().getTotalIngresosExtraByTurno(turnoId);
            if (totalIngresos == null) totalIngresos = 0.0;

            ResumenTurno resumen = new ResumenTurno();
            resumen.fondoInicial = fondo;
            resumen.totalVentas = desglose.totalVentas;
            resumen.totalEfectivo = desglose.totalEfectivo;
            resumen.totalTarjeta = desglose.totalTarjeta;
            resumen.totalQr = desglose.totalQr;
            resumen.totalEgresosGastos = totalEgresos;
            resumen.totalIngresosExtra = totalIngresos;
            resumen.totalPedidos = desglose.totalPedidos;
            resumen.esperado = fondo + desglose.totalEfectivo + totalIngresos - totalEgresos;

            mainHandler.post(() -> callback.onSuccess(resumen));
        });
    }

    /**
     * DTO para ítems de ranking de productos más vendidos.
     */
    public static class ProductoRanking {
        public String nombre;
        public int cantidad;
        public double total;

        public ProductoRanking(String nombre, int cantidad, double total) {
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.total = total;
        }
    }

    /**
     * DTO para estadísticas globales de Business Intelligence y métricas operativas del POS.
     */
    public static class EstadisticasReporte {
        public double totalVentas;
        public int totalPedidos;
        public double ticketPromedio;
        public int pedidosMesa;
        public int pedidosLlevar;
        public String horaPico = "Sin ventas";
        public double totalEfectivo;
        public double totalTarjeta;
        public double totalQr;
        public double ventasAyer = 0.0;
        public double porcentajeCrecimiento = 0.0;
        public boolean tieneDatosAyer = false;
        public int[] ventasPorHora = new int[7]; // 11, 12, 13, 14, 15, 16, 17+
        public List<ProductoRanking> topProductosSemana = new ArrayList<>();
    }

    /**
     * Agrega y calcula indicadores clave de rendimiento (KPIs) globales:
     * - Volumen de ventas brutas.
     * - Distribución por medio de pago (incluyendo desglose mixto).
     * - Ticket promedio (totalVentas / totalPedidos).
     * - Proporción de servicio en sala vs. pedidos para llevar.
     * - Hora pico real basada en ventas del día (o "Sin ventas" si no hay).
     * - Ventas por hora de hoy para el gráfico de barras.
     * - Productos más vendidos en los últimos 7 días.
     * - Comparativa de crecimiento contra el día anterior.
     * 
     * @param callback Callback que retorna el DTO EstadisticasReporte calculado.
     */
    public void getEstadisticasReporte(Callback<EstadisticasReporte> callback) {
        executor.execute(() -> {
            EstadisticasReporte stats = new EstadisticasReporte();
            List<PagoEntity> todosLosPagos = db.pagoDao().getAll();
            DesglosePagos desglose = calcularDesglosePagos(todosLosPagos);

            stats.totalPedidos = desglose.totalPedidos;
            stats.totalVentas = desglose.totalVentas;
            stats.totalEfectivo = desglose.totalEfectivo;
            stats.totalTarjeta = desglose.totalTarjeta;
            stats.totalQr = desglose.totalQr;
            stats.ticketPromedio = stats.totalPedidos > 0 ? (stats.totalVentas / stats.totalPedidos) : 0.0;

            java.util.Calendar calHoy = java.util.Calendar.getInstance();
            calHoy.set(java.util.Calendar.HOUR_OF_DAY, 0);
            calHoy.set(java.util.Calendar.MINUTE, 0);
            calHoy.set(java.util.Calendar.SECOND, 0);
            calHoy.set(java.util.Calendar.MILLISECOND, 0);
            long inicioHoy = calHoy.getTimeInMillis();
            long inicioAyer = inicioHoy - 24L * 60L * 60L * 1000L;
            long inicioSemana = inicioHoy - 6L * 24L * 60L * 60L * 1000L;

            List<PedidoEntity> todosPedidos = db.pedidoDao().getAll();
            int mesa = 0, llevar = 0;
            int[] hourlyCount = new int[24];
            java.util.Map<String, int[]> rankingMap = new java.util.HashMap<>(); // nombre -> [cantidad, totalCents]

            if (todosPedidos != null) {
                for (PedidoEntity pe : todosPedidos) {
                    if (!"pagado".equalsIgnoreCase(pe.getEstadoPago())) continue;
                    if ("cancelado".equalsIgnoreCase(pe.getEstado())) continue;

                    if ("mesa".equalsIgnoreCase(pe.getTipoEntrega()) || "local".equalsIgnoreCase(pe.getTipoEntrega())) mesa++;
                    else llevar++;

                    long creado = pe.getCreadoEn();

                    // Ventas de hoy y distribución horaria
                    if (creado >= inicioHoy) {
                        java.util.Calendar calP = java.util.Calendar.getInstance();
                        calP.setTimeInMillis(creado);
                        int h = calP.get(java.util.Calendar.HOUR_OF_DAY);
                        if (h >= 0 && h < 24) {
                            hourlyCount[h]++;
                        }

                        // Mapeo a las 7 columnas del gráfico (11am a 5pm)
                        if (h == 11) stats.ventasPorHora[0]++;
                        else if (h == 12) stats.ventasPorHora[1]++;
                        else if (h == 13) stats.ventasPorHora[2]++;
                        else if (h == 14) stats.ventasPorHora[3]++;
                        else if (h == 15) stats.ventasPorHora[4]++;
                        else if (h == 16) stats.ventasPorHora[5]++;
                        else if (h >= 17) stats.ventasPorHora[6]++;
                    } else if (creado >= inicioAyer && creado < inicioHoy) {
                        stats.ventasAyer += pe.getTotal();
                    }

                    // Productos más vendidos de la semana (últimos 7 días)
                    if (creado >= inicioSemana) {
                        List<PedidoDetalleEntity> detalles = db.pedidoDetalleDao().getByPedidoId(pe.getId());
                        if (detalles != null) {
                            for (PedidoDetalleEntity det : detalles) {
                                String nom = det.getNombreProducto();
                                if (nom == null || nom.trim().isEmpty()) continue;
                                int[] agg = rankingMap.get(nom);
                                if (agg == null) {
                                    agg = new int[]{0, 0};
                                    rankingMap.put(nom, agg);
                                }
                                agg[0] += det.getCantidad();
                                agg[1] += (int) Math.round(det.getSubtotal() * 100.0);
                            }
                        }
                    }
                }
            }
            stats.pedidosMesa = mesa;
            stats.pedidosLlevar = llevar;

            // Determinar hora pico de hoy
            int maxHour = -1;
            int maxCount = 0;
            for (int h = 0; h < 24; h++) {
                if (hourlyCount[h] > maxCount) {
                    maxCount = hourlyCount[h];
                    maxHour = h;
                }
            }
            if (maxCount > 0 && maxHour >= 0) {
                int startHour12 = maxHour % 12 == 0 ? 12 : maxHour % 12;
                String startAmPm = maxHour < 12 ? "am" : "pm";
                int endHour24 = (maxHour + 1) % 24;
                int endHour12 = endHour24 % 12 == 0 ? 12 : endHour24 % 12;
                String endAmPm = endHour24 < 12 ? "am" : "pm";
                stats.horaPico = String.format(java.util.Locale.getDefault(), "%d%s - %d%s", startHour12, startAmPm, endHour12, endAmPm);
            } else {
                stats.horaPico = "Sin ventas";
            }

            // Comparativa vs ayer
            if (stats.ventasAyer > 0) {
                stats.tieneDatosAyer = true;
                stats.porcentajeCrecimiento = ((stats.totalVentas - stats.ventasAyer) / stats.ventasAyer) * 100.0;
            } else {
                stats.tieneDatosAyer = false;
                stats.porcentajeCrecimiento = 0.0;
            }

            // Ordenar productos top de la semana
            List<ProductoRanking> rankingList = new ArrayList<>();
            for (java.util.Map.Entry<String, int[]> entry : rankingMap.entrySet()) {
                rankingList.add(new ProductoRanking(entry.getKey(), entry.getValue()[0], entry.getValue()[1] / 100.0));
            }
            Collections.sort(rankingList, (a, b) -> Integer.compare(b.cantidad, a.cantidad));
            if (rankingList.size() > 3) {
                stats.topProductosSemana = rankingList.subList(0, 3);
            } else {
                stats.topProductosSemana = rankingList;
            }

            mainHandler.post(() -> callback.onSuccess(stats));
        });
    }

    /**
     * DTO interno para el cómputo exacto de ingresos distribuidos por instrumento de cobro,
     * particionando fielmente los pedidos liquidados con método mixto (Efectivo + QR/Tarjeta).
     */
    public static class DesglosePagos {
        public double totalVentas = 0.0;
        public double totalEfectivo = 0.0;
        public double totalTarjeta = 0.0;
        public double totalQr = 0.0;
        public int totalPedidos = 0;
    }

    /**
     * Algoritmo contable que procesa la colección de pagos, asegurando que los pagos mixtos
     * sumen su cuota de papel moneda al efectivo físico de caja y su cuota electrónica a QR/bancos.
     * 
     * @param pagos Lista de pagos del turno o histórico.
     * @return Desglose consolidado sin discrepancias aritméticas.
     */
    public static DesglosePagos calcularDesglosePagos(List<PagoEntity> pagos) {
        DesglosePagos d = new DesglosePagos();
        if (pagos == null) return d;

        java.util.Set<Integer> distinctOrders = new java.util.HashSet<>();
        for (PagoEntity p : pagos) {
            d.totalVentas += p.getMonto();
            distinctOrders.add(p.getPedidoId());

            String metodo = p.getMetodoPago() != null ? p.getMetodoPago().toLowerCase().trim() : "";
            if ("efectivo".equals(metodo)) {
                d.totalEfectivo += p.getMonto();
            } else if ("tarjeta".equals(metodo)) {
                d.totalTarjeta += p.getMonto();
            } else if ("qr".equals(metodo)) {
                d.totalQr += p.getMonto();
            } else if ("mixto".equals(metodo)) {
                double[] partes = extraerPartesMixto(p.getMonto(), p.getReferencia());
                d.totalEfectivo += partes[0];
                d.totalQr += partes[1];
            } else {
                d.totalEfectivo += p.getMonto();
            }
        }
        d.totalPedidos = distinctOrders.size();
        return d;
    }

    /**
     * Extrae de forma robusta la parte en efectivo y la parte digital de una transacción mixta.
     * Soporta formato estructurado MIXTO|EF:xx|DIG:xx y formatos textuales legacy.
     */
    public static double[] extraerPartesMixto(double montoTotal, String ref) {
        double ef = 0.0;
        double dig = 0.0;
        if (ref != null && !ref.isEmpty()) {
            try {
                if (ref.contains("EF:") && ref.contains("DIG:")) {
                    String[] parts = ref.split("\\|");
                    for (String part : parts) {
                        if (part.startsWith("EF:")) {
                            String val = part.substring(3).trim();
                            int spaceIdx = val.indexOf(' ');
                            if (spaceIdx > 0) val = val.substring(0, spaceIdx);
                            ef = Double.parseDouble(val.replace(',', '.'));
                        } else if (part.startsWith("DIG:")) {
                            String val = part.substring(4).trim();
                            int spaceIdx = val.indexOf(' ');
                            if (spaceIdx > 0) val = val.substring(0, spaceIdx);
                            dig = Double.parseDouble(val.replace(',', '.'));
                        }
                    }
                } else {
                    java.util.regex.Matcher mEf = java.util.regex.Pattern.compile(
                            "(?:efectivo|ef)\\s*[:=]\\s*(?:bs\\.?\\s*)?([0-9]+(?:[.,][0-9]+)?)",
                            java.util.regex.Pattern.CASE_INSENSITIVE).matcher(ref);
                    if (mEf.find()) {
                        ef = Double.parseDouble(mEf.group(1).replace(",", "."));
                    }
                    java.util.regex.Matcher mDig = java.util.regex.Pattern.compile(
                            "(?:digital|qr|tarjeta|dig)\\s*[:=]\\s*(?:bs\\.?\\s*)?([0-9]+(?:[.,][0-9]+)?)",
                            java.util.regex.Pattern.CASE_INSENSITIVE).matcher(ref);
                    if (mDig.find()) {
                        dig = Double.parseDouble(mDig.group(1).replace(",", "."));
                    }
                }
            } catch (Exception ignored) {}
        }

        if (ef <= 0 && dig <= 0) {
            ef = Math.floor(montoTotal / 2.0);
            dig = montoTotal - ef;
        } else if (ef > 0 && dig <= 0) {
            dig = Math.max(0.0, montoTotal - ef);
        } else if (dig > 0 && ef <= 0) {
            ef = Math.max(0.0, montoTotal - dig);
        }

        // Si la suma de las partes excede el monto total de la venta (ej. cliente entregó billete mayor):
        // La cuota digital no puede exceder el total facturado, y el efectivo neto en caja es el saldo restante.
        if (dig > montoTotal) {
            dig = montoTotal;
            ef = 0.0;
        } else if (ef + dig > montoTotal) {
            ef = Math.max(0.0, montoTotal - dig);
        }

        return new double[]{ef, dig};
    }

    // ==========================================
    // MÓDULO: CONTROL DE CAJA CHICA Y EGRESOS
    // ==========================================

    public void registrarMovimientoCaja(String tipo, double monto, String concepto, Callback<com.example.pollogithub.data.entity.MovimientoCajaEntity> callback) {
        executor.execute(() -> {
            int turnoId = sessionManager.getTurnoId();
            com.example.pollogithub.data.entity.MovimientoCajaEntity mov = 
                new com.example.pollogithub.data.entity.MovimientoCajaEntity(turnoId, tipo.toUpperCase(), monto, concepto, System.currentTimeMillis());
            long id = db.movimientoCajaDao().insert(mov);
            mov.setId((int) id);
            mainHandler.post(() -> {
                if (callback != null) callback.onSuccess(mov);
            });
        });
    }

    public void getMovimientosCajaByTurno(int turnoId, Callback<List<com.example.pollogithub.data.entity.MovimientoCajaEntity>> callback) {
        executor.execute(() -> {
            List<com.example.pollogithub.data.entity.MovimientoCajaEntity> list = db.movimientoCajaDao().getByTurnoId(turnoId);
            mainHandler.post(() -> {
                if (callback != null) callback.onSuccess(list);
            });
        });
    }

    public LiveData<List<com.example.pollogithub.data.entity.MovimientoCajaEntity>> getMovimientosCajaLiveData(int turnoId) {
        return db.movimientoCajaDao().getByTurnoIdLiveData(turnoId);
    }

    // ==========================================
    // MÓDULO: DESCUENTOS Y PROMOCIONES
    // ==========================================

    public void aplicarDescuentoPedido(int pedidoId, double montoDescuento, Callback<Void> callback) {
        executor.execute(() -> {
            db.pedidoDao().updateDescuento(pedidoId, montoDescuento);
            mainHandler.post(() -> {
                if (callback != null) callback.onSuccess(null);
            });
        });
    }
}
