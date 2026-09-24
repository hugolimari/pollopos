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

/**
 * Repositorio Central de Datos: PosRepository
 * 
 * Capa de Abstracción de Datos / Patrón Repository
 * 
 * Actúa como mediador y Fuente Única de Verdad (Single Source of Truth - SSOT)
 * entre las fuentes de persistencia subyacentes (Room Database / SQLite y SharedPreferences)
 * y la capa de presentación (ViewModels, Actividades y Fragmentos).
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Patrón Repository: Desacopla las operaciones CRUD y transacciones de base de datos
 *   de la lógica de interfaz de usuario, promoviendo la modularidad y testabilidad.
 * - Patrón Singleton: Instanciación única y segura para subprocesos concurrentes
 *   (Double-Checked Locking con visibilidad de memoria 'volatile').
 * - Concurrencia y Despacho de Hilos:
 *     * Operaciones de I/O y persistencia delegadas al pool 'ExecutorService' (Worker Threads).
 *     * Despacho y retorno de resultados al Hilo Principal (UI Thread) mediante 'Handler(Looper.getMainLooper())'
 *       para garantizar que las vistas reciban callbacks de forma segura sin provocar bloqueos ANR (Application Not Responding).
 * - Patrón Callback: Interfaz genérica asíncrona para propagar resultados exitosos o errores controlados.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
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
                    callback.onError("Usuario o contraseña incorrectos");
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
            int nextOrden = (maxOrden == null || maxOrden < 200) ? 232 : maxOrden + 1;

            double subtotal = 0.0;
            for (Product p : cartProducts) {
                if (p.getQuantityInCart() > 0) {
                    subtotal += p.getPrice() * p.getQuantityInCart();
                }
            }

            double total = subtotal; // Sin deducciones iniciales

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
                            p.getNotes() != null ? p.getNotes() : ""
                    ));

                    // Deducción automática de insumos crudos mediante recetas de conversión
                    List<com.example.pollogithub.data.entity.RecetaInsumoEntity> recetas = db.recetaInsumoDao().getByProductoId(p.getId());
                    if (recetas != null) {
                        for (com.example.pollogithub.data.entity.RecetaInsumoEntity r : recetas) {
                            db.insumoDao().descontarStock(r.getInsumoId(), r.getCantidadRequerida() * p.getQuantityInCart());
                        }
                    }
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

            mainHandler.post(() -> callback.onSuccess(pago));
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

            Double totalVentas = db.pagoDao().getTotalVentasByTurno(turnoId);
            if (totalVentas == null) totalVentas = 0.0;

            Double totalEfectivo = db.pagoDao().getTotalEfectivoByTurno(turnoId);
            if (totalEfectivo == null) totalEfectivo = 0.0;

            Double totalTarjeta = db.pagoDao().getTotalTarjetaByTurno(turnoId);
            if (totalTarjeta == null) totalTarjeta = 0.0;

            Double totalQr = db.pagoDao().getTotalQrByTurno(turnoId);
            if (totalQr == null) totalQr = 0.0;

            Double totalEgresos = db.movimientoCajaDao().getTotalEgresosByTurno(turnoId);
            if (totalEgresos == null) totalEgresos = 0.0;

            Double totalIngresos = db.movimientoCajaDao().getTotalIngresosExtraByTurno(turnoId);
            if (totalIngresos == null) totalIngresos = 0.0;

            List<PagoEntity> pagos = db.pagoDao().getByTurnoId(turnoId);
            int countPedidos = pagos != null ? pagos.size() : 0;

            ResumenTurno resumen = new ResumenTurno();
            resumen.fondoInicial = fondo;
            resumen.totalVentas = totalVentas;
            resumen.totalEfectivo = totalEfectivo;
            resumen.totalTarjeta = totalTarjeta;
            resumen.totalQr = totalQr;
            resumen.totalEgresosGastos = totalEgresos;
            resumen.totalIngresosExtra = totalIngresos;
            resumen.totalPedidos = countPedidos;
            resumen.esperado = fondo + totalEfectivo + totalIngresos - totalEgresos;

            mainHandler.post(() -> callback.onSuccess(resumen));
        });
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
        public String horaPico = "12:00 PM - 2:00 PM";
        public double totalEfectivo;
        public double totalTarjeta;
        public double totalQr;
        public String productoMasVendido = "1/4 de pollo frito";
        public int productoMasVendidoCantidad = 0;
    }

    /**
     * Agrega y calcula indicadores clave de rendimiento (KPIs) globales:
     * - Volumen de ventas brutas.
     * - Distribución por medio de pago.
     * - Ticket promedio (totalVentas / totalPedidos).
     * - Proporción de servicio en sala vs. pedidos para llevar.
     * 
     * @param callback Callback que retorna el DTO EstadisticasReporte calculado.
     */
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
                    if ("mesa".equalsIgnoreCase(pe.getTipoEntrega()) || "local".equalsIgnoreCase(pe.getTipoEntrega())) mesa++;
                    else llevar++;
                }
            }
            stats.pedidosMesa = mesa;
            stats.pedidosLlevar = llevar;

            mainHandler.post(() -> callback.onSuccess(stats));
        });
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
    // MÓDULO: CONTROL DE INVENTARIO E INSUMOS CRUDOS
    // ==========================================

    public LiveData<List<com.example.pollogithub.data.entity.InsumoEntity>> getInsumosLiveData() {
        return db.insumoDao().getAllLiveData();
    }

    public void getInsumos(Callback<List<com.example.pollogithub.data.entity.InsumoEntity>> callback) {
        executor.execute(() -> {
            List<com.example.pollogithub.data.entity.InsumoEntity> list = db.insumoDao().getAll();
            mainHandler.post(() -> {
                if (callback != null) callback.onSuccess(list);
            });
        });
    }

    public void agregarStockInsumo(int insumoId, double cantidad, Callback<Void> callback) {
        executor.execute(() -> {
            db.insumoDao().agregarStock(insumoId, cantidad);
            mainHandler.post(() -> {
                if (callback != null) callback.onSuccess(null);
            });
        });
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
