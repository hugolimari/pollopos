package com.example.pollogithub;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

/**
 * Adaptador de Comandas y Pedidos: OrderAdapter
 * 
 * Capa de Presentación / Patrón Adapter & ViewHolder
 * Hereda de: RecyclerView.Adapter<OrderAdapter.OrderViewHolder>
 * 
 * Gestiona el enlace y renderizado de la lista de pedidos activos en la interfaz KDS.
 * Adapta dinámicamente los estilos visuales, colores de insignias de estado (badges)
 * y botones de acción conforme a la máquina de estados del pedido ("cocina", "listo", "camino").
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Patrón ViewHolder: Reutilización de nodos visuales en memoria para listas de alto rendimiento.
 * - Desacoplamiento de Eventos con Interfaces Funcionales: 'OnOrderActionListener' maneja las pulsaciones
 *   de detalle y avance de estado de forma segregada.
 * - Enrutamiento Polimórfico de Recursos Gráficos: Selección contextual de íconos según modalidad
 *   (mesa vs llevar vs delivery) y paleta de colores semántica (amarillo espera, verde éxito, azul info).
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    /**
     * Interfaz funcional para interceptar acciones sobre una orden del listado.
     */
    @FunctionalInterface
    public interface OnOrderActionListener {
        /**
         * Disparado al presionar la acción principal (ej. "Marcar listo" o "Entregado").
         * 
         * @param order    Instancia de la orden procesada.
         * @param position Índice posicional en el adaptador.
         */
        void onPrimaryAction(Order order, int position);

        /**
         * Disparado al presionar sobre la tarjeta para ver el desglose completo.
         * 
         * @param order    Instancia de la orden.
         * @param position Índice posicional en el adaptador.
         */
        default void onViewDetail(Order order, int position) {}
    }

    private Context context;
    private List<Order> orderList;
    private OnOrderActionListener listener;

    /**
     * Constructor del adaptador de órdenes.
     * 
     * @param context   Contexto para inflado de layout y resolución de recursos.
     * @param orderList Colección de órdenes activas.
     * @param listener  Manejador de eventos de interacción.
     */
    public OrderAdapter(Context context, List<Order> orderList, OnOrderActionListener listener) {
        this.context = context;
        this.orderList = orderList;
        this.listener = listener;
    }

    /**
     * Reemplaza el conjunto de datos y refresca la lista.
     * 
     * @param newList Nueva colección de órdenes.
     */
    public void updateList(List<Order> newList) {
        this.orderList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_card, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);

        // Mapeo de campos textuales y contables
        holder.tvOrderId.setText(order.getId());
        holder.tvOrderTime.setText(order.getTime());
        holder.tvOrderItems.setText(order.getItems());
        holder.tvOrderType.setText(order.getType());
        holder.tvOrderTotal.setText(String.format(Locale.getDefault(), "Bs. %.2f", order.getTotal()));
        holder.btnPrimaryAction.setText(order.getPrimaryActionText());

        // Asignación de ícono contextual según la modalidad de despacho
        String typeLower = order.getType().toLowerCase(Locale.getDefault());
        if (typeLower.contains("local") || typeLower.contains("mesa")) {
            holder.imgTypeIcon.setImageResource(R.drawable.ic_dining_local);
        } else if (typeLower.contains("llevar")) {
            holder.imgTypeIcon.setImageResource(R.drawable.ic_takeaway_bag);
        } else {
            holder.imgTypeIcon.setImageResource(R.drawable.ic_type_delivery);
        }

        // Estilización condicional de la insignia según la máquina de estados
        switch (order.getStatus()) {
            case "cocina":
                holder.tvStatusBadge.setText(R.string.status_cocina);
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_cocina);
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.wait_600));
                break;
            case "listo":
                holder.tvStatusBadge.setText(R.string.status_listo);
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_listo);
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.ok_600));
                break;
            case "camino":
                holder.tvStatusBadge.setText(R.string.status_camino);
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_camino);
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.info_600));
                break;
        }

        // Enlace de eventos de interacción
        holder.btnViewDetail.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetail(order, holder.getBindingAdapterPosition());
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetail(order, holder.getBindingAdapterPosition());
            }
        });

        holder.btnPrimaryAction.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPrimaryAction(order, holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    /**
     * ViewHolder para retención de componentes gráficos de la tarjeta de comanda.
     */
    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderTime, tvStatusBadge, tvOrderItems, tvOrderType, tvOrderTotal;
        ImageView imgTypeIcon;
        Button btnViewDetail, btnPrimaryAction;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderTime = itemView.findViewById(R.id.tvOrderTime);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvOrderItems = itemView.findViewById(R.id.tvOrderItems);
            tvOrderType = itemView.findViewById(R.id.tvOrderType);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
            imgTypeIcon = itemView.findViewById(R.id.imgTypeIcon);
            btnViewDetail = itemView.findViewById(R.id.btnViewDetail);
            btnPrimaryAction = itemView.findViewById(R.id.btnPrimaryAction);
        }
    }
}