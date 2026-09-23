package com.example.pollogithub;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

/**
 * Adaptador de Catálogo de Productos: ProductAdapter
 * 
 * Capa de Presentación / Patrón Adapter & ViewHolder
 * Hereda de: RecyclerView.Adapter<ProductAdapter.ProductViewHolder>
 * 
 * Vincula el conjunto de datos de productos comerciales con las vistas individuales
 * en la cuadrícula o lista del punto de venta (item_product_card.xml).
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Patrón ViewHolder: Reutiliza instancias de vistas evitando llamadas reiteradas
 *   y costosas a 'findViewById()', optimizando los ciclos de CPU y consumo de memoria (60 FPS scrolling).
 * - Patrón Observer / Listener: Desacopla las acciones de pulsación (añadir al carrito)
 *   hacia el componente contenedor (Fragment/Activity) mediante la interfaz 'OnProductClickListener'.
 * - Gestión de Estados de Renderizado: Alterna opacidad (alpha) y deshabilitación de componentes
 *   según el estado de disponibilidad/agotamiento del producto.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    /**
     * Interfaz de comunicación desacoplada para eventos de selección de producto.
     */
    public interface OnProductClickListener {
        /**
         * Notifica la intención de agregar una unidad del producto al carrito.
         * 
         * @param product Instancia del producto seleccionado.
         */
        void onAddToCart(Product product);
    }

    private Context context;
    private List<Product> productList;
    private OnProductClickListener listener;

    /**
     * Constructor del adaptador.
     * 
     * @param context     Contexto para inflado de recursos de layout.
     * @param productList Colección inicial de productos a renderizar.
     * @param listener    Receptor de eventos de interacción.
     */
    public ProductAdapter(Context context, List<Product> productList, OnProductClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    /**
     * Actualiza la colección de datos subyacente y refresca la vista.
     * 
     * @param newList Nueva lista de productos proveniente del ViewModel.
     */
    public void updateList(List<Product> newList) {
        this.productList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflado del layout XML para la celda de producto
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_card, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        // Mapeo de datos descriptivos y precio
        holder.tvName.setText(product.getName());
        holder.tvDesc.setText(product.getDescription());
        holder.tvPrice.setText(String.format(Locale.getDefault(), "Bs. %.2f", product.getPrice()));
        holder.tvEmoji.setText(product.getEmoji());
        holder.frameThumb.setBackgroundResource(product.getThumbDrawableRes());

        // Control visual de disponibilidad (Agotado vs Disponible)
        if (product.isAgotado()) {
            holder.tvAgotadoTag.setVisibility(View.VISIBLE);
            holder.btnAdd.setEnabled(false);
            holder.itemView.setAlpha(0.55f); // Reducción de opacidad para retroalimentación visual
        } else {
            holder.tvAgotadoTag.setVisibility(View.GONE);
            holder.btnAdd.setEnabled(true);
            holder.itemView.setAlpha(1.0f);
        }

        // Renderizado del indicador de cantidad en carrito (Badge)
        if (product.getQuantityInCart() > 0) {
            holder.tvBadgeQty.setVisibility(View.VISIBLE);
            holder.tvBadgeQty.setText(String.valueOf(product.getQuantityInCart()));
        } else {
            holder.tvBadgeQty.setVisibility(View.GONE);
        }

        // Delegación de eventos de pulsación al listener externo
        holder.btnAdd.setOnClickListener(v -> {
            if (!product.isAgotado() && listener != null) {
                listener.onAddToCart(product);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (!product.isAgotado() && listener != null) {
                listener.onAddToCart(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    /**
     * Contenedor de referencias en caché a los componentes visuales de la celda (Patrón ViewHolder).
     */
    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        FrameLayout frameThumb;
        TextView tvEmoji, tvAgotadoTag, tvBadgeQty, tvName, tvDesc, tvPrice;
        View btnAdd;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            frameThumb = itemView.findViewById(R.id.frameThumb);
            tvEmoji = itemView.findViewById(R.id.tvEmoji);
            tvAgotadoTag = itemView.findViewById(R.id.tvAgotadoTag);
            tvBadgeQty = itemView.findViewById(R.id.tvBadgeQty);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvDesc = itemView.findViewById(R.id.tvProductDesc);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            btnAdd = itemView.findViewById(R.id.btnAdd);
        }
    }
}