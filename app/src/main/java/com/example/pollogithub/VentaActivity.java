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

/**
 * Controlador de Vista: VentaActivity (Terminal de Ventas Autónomo)
 * 
 * Capa de Presentación / Módulo Principal de Toma de Pedidos (Point of Sale)
 * Hereda de: AppCompatActivity
 * 
 * Provee la interfaz completa de venta en cuadrícula (Grid) para selección ágil de artículos
 * en el mostrador del restaurante. Administra el filtrado por categorías, motor de búsqueda,
 * acumulación en memoria del pedido en curso y transición a la pasarela de cobranza (PagoActivity).
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Diseño de Cuadrícula Adaptativa: Empleo de 'GridLayoutManager(this, 2)' para optimización del espacio en pantallas táctiles de POS.
 * - Algoritmo de Acumulación y Cálculos Contables:
 *     * totalCount = sum(quantityInCart)
 *     * totalPrice = sum(quantityInCart * price)
 * - Filtrado en Memoria (Filtering Pipeline): Combinación de criterios de selección de chips y subcadenas textuales.
 */
public class VentaActivity extends AppCompatActivity {

    private final List<Product> allProducts = new ArrayList<>();
    private final List<Product> displayedProducts = new ArrayList<>();
    private ProductAdapter adapter;

    private TextView tvCartCount;
    private TextView tvCartTotal;
    private String selectedCategory = "Todos";
    private String searchQuery = "";
    private String cashierName = "Carlos Méndez";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_venta);

        // Compensación de insets de ventana respecto a barras de sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainVenta), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvCartCount = findViewById(R.id.tvCartCount);
        tvCartTotal = findViewById(R.id.tvCartTotal);
        TextView tvCashierName = findViewById(R.id.tvCashierName);
        TextView tvAvatarHeader = findViewById(R.id.tvAvatarHeader);

        // Identificación del operador
        String userExtra = getIntent().getStringExtra("USER_NAME");
        if (userExtra != null && !userExtra.isEmpty()) {
            cashierName = userExtra;
            tvCashierName.setText(userExtra);
            String initial = userExtra.substring(0, 1).toUpperCase(Locale.getDefault());
            tvAvatarHeader.setText(initial);
        }

        // 1. Carga inicial del menú demostrativo
        initProductList();

        // 2. Configuración de la cuadrícula de 2 columnas en RecyclerView
        RecyclerView rvProducts = findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));

        // 3. Inicialización del adaptador con callbacks de adición y remoción
        adapter = new ProductAdapter(this, displayedProducts, new ProductAdapter.OnProductClickListener() {
            @Override
            public void onAddToCart(Product product) {
                product.setQuantityInCart(product.getQuantityInCart() + 1);
                adapter.notifyDataSetChanged();
                updateCartSummary();
            }

            @Override
            public void onRemoveFromCart(Product product) {
                if (product.getQuantityInCart() > 0) {
                    product.setQuantityInCart(product.getQuantityInCart() - 1);
                    adapter.notifyDataSetChanged();
                    updateCartSummary();
                }
            }
        });
        rvProducts.setAdapter(adapter);

        setupCategoryChips();
        setupSearch();
        setupBottomNav();
        updateCartSummary();

        // 4. Enrutamiento hacia la pantalla de cobranza (PagoActivity)
        View.OnClickListener openPagoListener = v -> {
            double currentTotal = calculateCartTotal();
            Intent intent = new Intent(VentaActivity.this, PagoActivity.class);
            intent.putExtra("TOTAL_AMOUNT", currentTotal > 0 ? currentTotal : 41.40);
            startActivity(intent);
        };

        findViewById(R.id.btnViewOrder).setOnClickListener(openPagoListener);
        findViewById(R.id.cartBar).setOnClickListener(openPagoListener);
    }

    /**
     * Inicializa el catálogo local con los platos principales de la franquicia.
     */
    private void initProductList() {
        allProducts.add(new Product("Presa individual", "Pierna o pechuga", 8.50, "🍗", "Pollo frito", R.drawable.bg_thumb_fried, false, 2));
        allProducts.add(new Product("1/4 de pollo frito", "Con papas incluidas", 14.00, "🍗", "Pollo frito", R.drawable.bg_thumb_fried, false, 0));
        allProducts.add(new Product("1/2 pollo a la brasa", "Con papas y ensalada", 24.00, "🔥", "A la brasa", R.drawable.bg_thumb_asado, false, 0));
        allProducts.add(new Product("Combo Familiar", "Pollo entero + 2 gaseosas", 52.00, "🥤", "Combos", R.drawable.bg_thumb_combo, false, 1));
        allProducts.add(new Product("Gaseosa 500ml", "Varios sabores", 4.00, "🥤", "Bebidas", R.drawable.bg_thumb_bebida, false, 0));
        allProducts.add(new Product("Papas fritas", "Porción regular", 6.00, "🍟", "Acompañamientos", R.drawable.bg_thumb_fried, true, 0));

        filterProducts();
    }

    /**
     * Vincula los controladores de eventos a los chips de categorías.
     */
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

    /**
     * Configura el listener de texto para filtrado inmediato por palabras clave.
     */
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

    /**
     * Filtra los artículos mostrados en la cuadrícula según los criterios seleccionados.
     */
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

    /**
     * Computa el total monetario acumulado de los ítems con cantidad activa > 0.
     * 
     * @return Importe total a liquidar.
     */
    private double calculateCartTotal() {
        double total = 0.0;
        for (Product p : allProducts) {
            if (p.getQuantityInCart() > 0) {
                total += p.getQuantityInCart() * p.getPrice();
            }
        }
        return total;
    }

    /**
     * Actualiza el badge numérico y la etiqueta de precio en la barra inferior de checkout.
     */
    private void updateCartSummary() {
        int totalCount = 0;

        for (Product p : allProducts) {
            if (p.getQuantityInCart() > 0) {
                totalCount += p.getQuantityInCart();
            }
        }

        double totalPrice = calculateCartTotal();
        tvCartCount.setText(String.valueOf(totalCount));
        tvCartTotal.setText(String.format(Locale.getDefault(), "Bs. %.2f", totalPrice));
    }

    /**
     * Configura la navegación inferior entre pantallas.
     */
    private void setupBottomNav() {
        findViewById(R.id.navItemVenta).setOnClickListener(v -> {
            // Ya se encuentra en la pantalla de ventas
        });

        findViewById(R.id.navItemPedidos).setOnClickListener(v -> {
            Intent intent = new Intent(VentaActivity.this, PedidosActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });

        findViewById(R.id.navItemReportes).setOnClickListener(v ->
            Toast.makeText(this, "Sección Reportes", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.navItemPerfil).setOnClickListener(v -> {
            Intent intent = new Intent(VentaActivity.this, PerfilActivity.class);
            intent.putExtra("USER_NAME", cashierName);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
    }
}