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

public class PedidosViewModel extends AndroidViewModel {

    private final PosRepository repository;
    private final MediatorLiveData<List<Order>> ordersLiveData = new MediatorLiveData<>();

    public PedidosViewModel(@NonNull Application application) {
        super(application);
        repository = PosRepository.getInstance(application);

        LiveData<List<PedidoEntity>> dbOrders = repository.getPedidosLiveData();
        ordersLiveData.addSource(dbOrders, entities -> {
            if (entities != null) {
                mapEntitiesToOrders(entities);
            }
        });
    }

    public LiveData<List<Order>> getOrdersLiveData() {
        return ordersLiveData;
    }

    private void mapEntitiesToOrders(List<PedidoEntity> entities) {
        List<Order> orders = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());

        for (PedidoEntity e : entities) {
            // Ignorar entregados o cancelados en la pantalla de pedidos en preparación
            if ("entregado".equalsIgnoreCase(e.getEstado()) || "cancelado".equalsIgnoreCase(e.getEstado())) {
                continue;
            }

            String hora = sdf.format(new Date(e.getCreadoEn()));
            String subtitle = hora + " · " + (e.getMesaId() != null ? "Mesa " + e.getMesaId() : "Para llevar");
            String actionText = "cocina".equalsIgnoreCase(e.getEstado()) ? "Marcar listo" : "Entregado";
            String tipo = e.getMesaId() != null ? "Para mesa" : "Para llevar";

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

            // Cargar detalle de productos
            repository.getPedidoDetalles(e.getId(), new PosRepository.Callback<List<PedidoDetalleEntity>>() {
                @Override
                public void onSuccess(List<PedidoDetalleEntity> result) {
                    if (result != null && !result.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < result.size(); i++) {
                            PedidoDetalleEntity d = result.get(i);
                            if (i > 0) sb.append(", ");
                            sb.append(d.getCantidad()).append("× ").append(d.getNombreProducto());
                        }
                        order.setItems(sb.toString());
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
