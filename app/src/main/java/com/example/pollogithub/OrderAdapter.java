package com.example.pollogithub;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface OnOrderActionListener {
        void onPrimaryAction(Order order, int position);
    }

    private Context context;
    private List<Order> orderList;
    private OnOrderActionListener listener;

    public OrderAdapter(Context context, List<Order> orderList, OnOrderActionListener listener) {
        this.context = context;
        this.orderList = orderList;
        this.listener = listener;
    }

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

        holder.tvOrderId.setText(order.getId());
        holder.tvOrderTime.setText(order.getTime());
        holder.tvOrderItems.setText(order.getItems());
        holder.tvOrderType.setText(order.getType());
        holder.tvOrderTotal.setText(String.format(Locale.getDefault(), "Bs. %.2f", order.getTotal()));
        holder.btnPrimaryAction.setText(order.getPrimaryActionText());

        // Type icon
        if (order.getType().contains("mesa")) {
            holder.imgTypeIcon.setImageResource(R.drawable.ic_type_table);
        } else if (order.getType().contains("llevar")) {
            holder.imgTypeIcon.setImageResource(R.drawable.ic_type_takeaway);
        } else {
            holder.imgTypeIcon.setImageResource(R.drawable.ic_type_delivery);
        }

        // Status badge styling
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

        holder.btnViewDetail.setOnClickListener(v ->
            Toast.makeText(context, "Detalle de " + order.getId(), Toast.LENGTH_SHORT).show()
        );

        holder.btnPrimaryAction.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPrimaryAction(order, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

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