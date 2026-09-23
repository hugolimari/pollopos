package com.example.pollogithub;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pollogithub.util.ImageUtils;

import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnProductClickListener {
        void onAddToCart(Product product);
    }

    private Context context;
    private List<Product> productList;
    private OnProductClickListener listener;

    public ProductAdapter(Context context, List<Product> productList, OnProductClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    public void updateList(List<Product> newList) {
        this.productList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_card, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.tvName.setText(product.getName());
        holder.tvDesc.setText(product.getDescription());
        holder.tvPrice.setText(String.format(Locale.getDefault(), "Bs. %.2f", product.getPrice()));
        
        if (product.getEmoji() != null && !product.getEmoji().isEmpty()) {
            holder.tvEmoji.setText(product.getEmoji());
        } else {
            holder.tvEmoji.setText("");
        }

        if (holder.ivProductImage != null) {
            ImageUtils.displayProductImage(holder.ivProductImage, product.getImagenLocalPath(), holder.tvEmoji);
        }

        holder.frameThumb.setBackgroundResource(product.getThumbDrawableRes());

        if (product.isAgotado()) {
            holder.tvAgotadoTag.setVisibility(View.VISIBLE);
            holder.btnAdd.setEnabled(false);
            holder.itemView.setAlpha(0.55f);
        } else {
            holder.tvAgotadoTag.setVisibility(View.GONE);
            holder.btnAdd.setEnabled(true);
            holder.itemView.setAlpha(1.0f);
        }

        if (product.getQuantityInCart() > 0) {
            holder.tvBadgeQty.setVisibility(View.VISIBLE);
            holder.tvBadgeQty.setText(String.valueOf(product.getQuantityInCart()));
        } else {
            holder.tvBadgeQty.setVisibility(View.GONE);
        }

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

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        FrameLayout frameThumb;
        ImageView ivProductImage;
        TextView tvEmoji, tvAgotadoTag, tvBadgeQty, tvName, tvDesc, tvPrice;
        View btnAdd;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            frameThumb = itemView.findViewById(R.id.frameThumb);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
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