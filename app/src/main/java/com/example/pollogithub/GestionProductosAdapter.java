package com.example.pollogithub;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pollogithub.data.entity.ProductoEntity;
import com.example.pollogithub.util.ImageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GestionProductosAdapter extends RecyclerView.Adapter<GestionProductosAdapter.ProductoViewHolder> {

    public interface OnProductoActionListener {
        void onToggleDisponible(ProductoEntity producto, boolean disponible);
        void onEditProducto(ProductoEntity producto);
    }

    private final Context context;
    private final List<ProductoEntity> productos = new ArrayList<>();
    private final OnProductoActionListener listener;

    public GestionProductosAdapter(Context context, List<ProductoEntity> productos, OnProductoActionListener listener) {
        this.context = context;
        if (productos != null) this.productos.addAll(productos);
        this.listener = listener;
    }

    public void updateList(List<ProductoEntity> newList) {
        this.productos.clear();
        if (newList != null) {
            this.productos.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_gestion_producto, parent, false);
        return new ProductoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoViewHolder holder, int position) {
        ProductoEntity p = productos.get(position);

        holder.tvProductName.setText(p.getNombre());
        holder.tvProductCategory.setText(getCategoryDescription(p));
        holder.tvProductPrice.setText(String.format(Locale.getDefault(), "Bs. %.2f", p.getPrecio()));

        if (p.getEmoji() != null && !p.getEmoji().isEmpty()) {
            holder.tvProductEmoji.setText(p.getEmoji());
        } else {
            holder.tvProductEmoji.setText("");
        }

        if (holder.ivProductImage != null) {
            ImageUtils.displayProductImage(holder.ivProductImage, p.getImagenLocalPath(), holder.tvProductEmoji);
        }
        holder.wrapThumb.setBackgroundResource(getThumbBackground(p.getCategoriaId()));

        // Status badge & switch
        holder.switchDisponible.setOnCheckedChangeListener(null);
        holder.switchDisponible.setChecked(p.isDisponible());

        if (p.isDisponible()) {
            holder.tvProductStatusBadge.setText("Disponible");
            holder.tvProductStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.ok_600));
            holder.tvProductStatusBadge.setBackgroundResource(R.drawable.bg_badge_active_shift);
        } else {
            holder.tvProductStatusBadge.setText("Agotado");
            holder.tvProductStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.char_400));
            holder.tvProductStatusBadge.setBackgroundResource(R.drawable.bg_badge_agotado);
        }

        holder.switchDisponible.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onToggleDisponible(p, isChecked);
            }
        });

        holder.btnEditProduct.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditProducto(p);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditProducto(p);
            }
        });
    }

    private String getCategoryDescription(ProductoEntity p) {
        String catName;
        switch (p.getCategoriaId()) {
            case 1: catName = "Pollo frito"; break;
            case 2: catName = "A la brasa"; break;
            case 3: catName = "Combos"; break;
            case 4: catName = "Bebidas"; break;
            case 5: catName = "Acompañamientos"; break;
            default: catName = "Especial"; break;
        }
        if (p.getDescripcion() != null && !p.getDescripcion().trim().isEmpty()) {
            return catName + " · " + p.getDescripcion();
        }
        return catName;
    }

    private int getThumbBackground(int catId) {
        switch (catId) {
            case 1: return R.drawable.bg_thumb_fried;
            case 2: return R.drawable.bg_thumb_asado;
            case 3: return R.drawable.bg_thumb_combo;
            case 4: return R.drawable.bg_thumb_bebida;
            default: return R.drawable.bg_thumb_fried;
        }
    }

    @Override
    public int getItemCount() {
        return productos.size();
    }

    static class ProductoViewHolder extends RecyclerView.ViewHolder {
        FrameLayout wrapThumb;
        ImageView ivProductImage;
        TextView tvProductEmoji, tvProductName, tvProductCategory, tvProductPrice, tvProductStatusBadge;
        SwitchCompat switchDisponible;
        ImageButton btnEditProduct;

        public ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            wrapThumb = itemView.findViewById(R.id.wrapThumb);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductEmoji = itemView.findViewById(R.id.tvProductEmoji);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductCategory = itemView.findViewById(R.id.tvProductCategory);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvProductStatusBadge = itemView.findViewById(R.id.tvProductStatusBadge);
            switchDisponible = itemView.findViewById(R.id.switchDisponible);
            btnEditProduct = itemView.findViewById(R.id.btnEditProduct);
        }
    }
}
