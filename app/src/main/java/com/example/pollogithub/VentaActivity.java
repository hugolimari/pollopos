package com.example.pollogithub;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VentaActivity extends AppCompatActivity {

    private final List<Product> allProducts = new ArrayList<>();
    private final List<Product> displayedProducts = new ArrayList<>();
    private ProductAdapter adapter;

    private TextView tvCartCount;
    private TextView tvCartTotal;
    private String selectedCategory = "Todos";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_venta);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainVenta), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvCartCount = findViewById(R.id.tvCartCount);
        tvCartTotal = findViewById(R.id.tvCartTotal);
        TextView tvCashierName = findViewById(R.id.tvCashierName);
        TextView tvAvatarHeader = findViewById(R.id.tvAvatarHeader);

        String userExtra = getIntent().getStringExtra("USER_NAME");
        if (userExtra != null && !userExtra.isEmpty()) {
            tvCashierName.setText(userExtra);
            String initial = userExtra.substring(0, 1).toUpperCase(Locale.getDefault());
            tvAvatarHeader.setText(initial);
        }

        initProductList();

        RecyclerView rvProducts = findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new ProductAdapter(this, displayedProducts, product -> {
            product.setQuantityInCart(product.getQuantityInCart() + 1);
            adapter.notifyDataSetChanged();
            updateCartSummary();
        });
        rvProducts.setAdapter(adapter);

        setupCategoryChips();
        setupSearch();
        setupBottomNav();
        updateCartSummary();

        View.OnClickListener openPagoListener = v -> {
            double currentTotal = calculateCartTotal();
            Intent intent = new Intent(VentaActivity.this, PagoActivity.class);
            intent.putExtra("TOTAL_AMOUNT", currentTotal > 0 ? currentTotal : 41.40);
            startActivity(intent);
        };

        findViewById(R.id.btnViewOrder).setOnClickListener(openPagoListener);
        findViewById(R.id.cartBar).setOnClickListener(openPagoListener);

        findViewById(R.id.btnNotification).setOnClickListener(v ->
            Toast.makeText(this, "Sin notificaciones pendientes", Toast.LENGTH_SHORT).show()
        );
    }

    private void initProductList() {
        allProducts.add(new Product("Presa individual", "Pierna o pechuga", 8.50, "🍗", "Pollo frito", R.drawable.bg_thumb_fried, false, 2));
        allProducts.add(new Product("1/4 de pollo frito", "Con papas incluidas", 14.00, "🍗", "Pollo frito", R.drawable.bg_thumb_fried, false, 0));
        allProducts.add(new Product("1/2 pollo a la brasa", "Con papas y ensalada", 24.00, "🔥", "A la brasa", R.drawable.bg_thumb_asado, false, 0));
        allProducts.add(new Product("Combo Familiar", "Pollo entero + 2 gaseosas", 52.00, "🥤", "Combos", R.drawable.bg_thumb_combo, false, 1));
        allProducts.add(new Product("Gaseosa 500ml", "Varios sabores", 4.00, "🥤", "Bebidas", R.drawable.bg_thumb_bebida, false, 0));
        allProducts.add(new Product("Papas fritas", "Porción regular", 6.00, "🍟", "Acompañamientos", R.drawable.bg_thumb_fried, true, 0));

        filterProducts();
    }

    private void setupCategoryChips() {
        TextView chipTodos = findViewById(R.id.chipTodos);
        TextView chipPolloFrito = findViewById(R.id.chipPolloFrito);
        TextView chipBrasa = findViewById(R.id.chipBrasa);
        TextView chipCombos = findViewById(R.id.chipCombos);
        TextView chipAcompanamientos = findViewById(R.id.chipAcompanamientos);
        TextView chipBebidas = findViewById(R.id.chipBebidas);

        TextView[] chips = new TextView[]{chipTodos, chipPolloFrito, chipBrasa, chipCombos, chipAcompanamientos, chipBebidas};

        for (TextView chip : chips) {
            chip.setOnClickListener(v -> {
                selectedCategory = chip.getText().toString();
                for (TextView c : chips) {
                    if (c == chip) {
                        c.setBackgroundResource(R.drawable.bg_chip_selected);
                        c.setTextColor(getColor(R.color.white));
                    } else {
                        c.setBackgroundResource(R.drawable.bg_chip_unselected);
                        c.setTextColor(getColor(R.color.char_700));
                    }
                }
                filterProducts();
            });
        }
    }

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.getDefault());
                filterProducts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterProducts() {
        displayedProducts.clear();
        for (Product p : allProducts) {
            boolean matchesCategory = selectedCategory.equals("Todos") || p.getCategory().equalsIgnoreCase(selectedCategory);
            boolean matchesSearch = searchQuery.isEmpty() || p.getName().toLowerCase(Locale.getDefault()).contains(searchQuery)
                    || p.getDescription().toLowerCase(Locale.getDefault()).contains(searchQuery);

            if (matchesCategory && matchesSearch) {
                displayedProducts.add(p);
            }
        }
        if (adapter != null) {
            adapter.updateList(displayedProducts);
        }
    }

    private double calculateCartTotal() {
        double total = 0.0;
        for (Product p : allProducts) {
            if (p.getQuantityInCart() > 0) {
                total += p.getQuantityInCart() * p.getPrice();
            }
        }
        return total;
    }

    private void updateCartSummary() {
        int totalCount = 0;

        for (Product p : allProducts) {
            if (p.getQuantityInCart() > 0) {
                totalCount += p.getQuantityInCart();
            }
        }

        double totalPrice = calculateCartTotal();
        tvCartCount.setText(String.valueOf(totalCount));
        tvCartTotal.setText(String.format(Locale.getDefault(), "S/ %.2f", totalPrice));
    }

    private void setupBottomNav() {
        findViewById(R.id.navItemPedidos).setOnClickListener(v -> {
            Intent intent = new Intent(VentaActivity.this, PedidosActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.navItemReportes).setOnClickListener(v ->
            Toast.makeText(this, "Sección Reportes", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.navItemPerfil).setOnClickListener(v ->
            Toast.makeText(this, "Sección Perfil", Toast.LENGTH_SHORT).show()
        );
    }
}