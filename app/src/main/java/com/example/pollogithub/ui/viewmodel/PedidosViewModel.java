package com.example.pollogithub.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.pollogithub.Order;
import com.example.pollogithub.data.entity.PedidoDetalleEntity;
import com.example.pollogithub.data.entity.PedidoEntity;
import com.example.pollogithub.data.repository.PosRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Modelo de Vista para Gestión de Pedidos y KDS: PedidosViewModel
 * 
 * Capa de Presentación / Arquitectura MVVM (Model-View-ViewModel)
 * Hereda de: AndroidViewModel
 * 
 * Coordina la visualización en tiempo real del flujo de comandas y pedidos en cocina/despacho.
 * Transforma y enriquece las entidades de base de datos (PedidoEntity y PedidoDetalleEntity)
 * en modelos de presentación listos para ser renderizados por el adaptador (Order).
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Patrón MediatorLiveData: Centraliza y combina fuentes de datos heterogéneas, reaccionando a mutaciones
 *   de las entidades de persistencia y disparando la transformación al modelo de vista (DTO).
 * - Separación de Modelos (Entity vs DTO / Presentation Model): Mapeo explícito que aísla la estructura
 *   de la base de datos de los requerimientos de formateo de texto, fechas y estados visuales.
 * - Máquina de Estados Operativa: Método 'avanzarEstadoPedido' que gestiona la transición progresiva
 *   ("cocina" -> "listo" -> "entregado") conforme a la dinámica culinaria y de despacho.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
public class PedidosViewModel extends AndroidViewModel {

    /**
     * Capa de abstracción de datos para consultas de pedidos y detalles.
     */
    private final PosRepository repository;

    /**
     * Fuente de datos reactiva mediadora que expone la lista de modelos de presentación 'Order'.
     */
    private final MediatorLiveData<List<Order>> ordersLiveData = new MediatorLiveData<>();

    /**
     * Constructor que vincula el observador reactivo a la base de datos Room mediante MediatorLiveData.
     * 
     * @param application Contexto de aplicación para inyección en el Repositorio.
     */
    public PedidosViewModel(@NonNull Application application) {
        super(application);
        repository = PosRepository.getInstance(application);

        // Suscripción reactiva al flujo de pedidos en SQLite
        LiveData<List<PedidoEntity>> dbOrders = repository.getPedidosLiveData();
        ordersLiveData.addSource(dbOrders, entities -> {
            if (entities != null) {
                mapEntitiesToOrders(entities);
            }
        });
    }

    /**
     * Expone el flujo reactivo de órdenes activas para observación desde Fragmentos y Actividades.
     * 
     * @return LiveData de colección de órdenes formateadas para UI.
     */
    public LiveData<List<Order>> getOrdersLiveData() {
        return ordersLiveData;
    }

    /**
     * Transforma las entidades relacionales de pedidos en objetos de presentación 'Order'.
     * Realiza un filtrado de exclusión de órdenes inactivas (entregadas/canceladas)
     * e invoca de manera asíncrona la carga de detalles de productos para componer la descripción de la comanda.
     * 
     * @param entities Colección de entidades crudas emitidas por Room.
     */
    private void mapEntitiesToOrders(List<PedidoEntity> entities) {
        List<Order> orders = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());

        for (PedidoEntity e : entities) {
            // Filtrado de estados terminales: la pantalla KDS solo visualiza pedidos pendientes
            if ("entregado".equalsIgnoreCase(e.getEstado()) || "cancelado".equalsIgnoreCase(e.getEstado())) {
                continue;
            }

            String hora = sdf.format(new Date(e.getCreadoEn()));
            boolean isLocal = "mesa".equalsIgnoreCase(e.getTipoEntrega()) || "local".equalsIgnoreCase(e.getTipoEntrega());
            String subtitle = hora + " · " + (isLocal ? "En el local" : "Para llevar");
            String actionText = "cocina".equalsIgnoreCase(e.getEstado()) ? "Marcar listo" : "Entregado";
            String tipo = isLocal ? "En el local" : "Para llevar";

            Order order = new Order(
                    e.getId(),
                    "Pedido #" + String.format(Locale.getDefault(), "%04d", e.getNumeroOrden()),
                    subtitle,
                    e.getEstado(),
                    "Cargando detalle...",
                    tipo,
                    e.getTotal(),
                    actionText
            );

            // Carga asíncrona desacoplada de renglones de comanda (Maestro-Detalle)
            repository.getPedidoDetalles(e.getId(), new PosRepository.Callback<List<PedidoDetalleEntity>>() {
                @Override
                public void onSuccess(List<PedidoDetalleEntity> result) {
                    if (result != null && !result.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < result.size(); i++) {
                            PedidoDetalleEntity d = result.get(i);
                            if (i > 0) sb.append("\n");
                            sb.append(d.getCantidad()).append("× ").append(d.getNombreProducto());
                            if (d.getNotas() != null && !d.getNotas().trim().isEmpty()) {
                                sb.append(" [").append(d.getNotas().trim()).append("]");
                            }
                        }
                        order.setItems(sb.toString());
                        // Notificación de mutación de detalle al observador
                        ordersLiveData.setValue(orders);
                    }
                }

                @Override
                public void onError(String error) {}
            });

            orders.add(order);
        }
        ordersLiveData.setValue(orders);
    }

    /**
     * Avanza el estado operativo de la orden en el flujo de trabajo:
     * - Si está en "cocina", transiciona a "listo".
     * - Si está en "listo", transiciona a "entregado".
     * 
     * @param order    Modelo de presentación de la orden a actualizar.
     * @param callback Callback opcional para confirmar el cambio de estado en la UI.
     */
    public void avanzarEstadoPedido(Order order, PosRepository.Callback<Void> callback) {
        String nuevoEstado;
        if ("cocina".equalsIgnoreCase(order.getStatus())) {
            nuevoEstado = "listo";
        } else {
            nuevoEstado = "entregado";
        }

        repository.updatePedidoEstado(order.getPedidoId(), nuevoEstado, new PosRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }
}
