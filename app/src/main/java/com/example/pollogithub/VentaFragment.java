package com.example.pollogithub;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VentaFragment extends Fragment {

    private static final String ARG_USER_NAME = "ARG_USER_NAME";

    private final List<Product> allProducts = new ArrayList<>();
    private final List<Product> displayedProducts = new ArrayList<>();
    private ProductAdapter adapter;

    private TextView tvCartCount;
    private TextView tvCartTotal;
    private String selectedCategory = "Todos";
    private String searchQuery = "";
    private String userName = "";

    public static VentaFragment newInstance(String userName) {
        VentaFragment fragment = new VentaFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_NAME, userName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userName = getArguments().getString(ARG_USER_NAME, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_venta, container, false);

        tvCartCount = view.findViewById(R.id.tvCartCount);
        tvCartTotal = view.findViewById(R.id.tvCartTotal);
        TextView tvCashierName = view.findViewById(R.id.tvCashierName);
        TextView tvAvatarHeader = view.findViewById(R.id.tvAvatarHeader);

        if (userName != null && !userName.isEmpty()) {
            tvCashierName.setText(userName);
            String initial = userName.substring(0, 1).toUpperCase(Locale.getDefault());
            tvAvatarHeader.setText(initial);
        }

        initProductList();

        RecyclerView rvProducts = view.findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        adapter = new ProductAdapter(requireContext(), displayedProducts, product -> {
            product.setQuantityInCart(product.getQuantityInCart() + 1);
            adapter.notifyDataSetChanged();
            updateCartSummary();
        });
        rvProducts.setAdapter(adapter);

        setupCategoryChips(view);
        setupSearch(view);
        updateCartSummary();

        View.OnClickListener openPagoListener = v -> {
            double currentTotal = calculateCartTotal();
            Intent intent = new Intent(requireContext(), PagoActivity.class);
            intent.putExtra("TOTAL_AMOUNT", currentTotal > 0 ? currentTotal : 41.40);
            startActivity(intent);
        };

        view.findViewById(R.id.btnViewOrder).setOnClickListener(openPagoListener);
        view.findViewById(R.id.cartBar).setOnClickListener(openPagoListener);

        view.findViewById(R.id.btnNotification).setOnClickListener(v ->
            Toast.makeText(requireContext(), "Sin notificaciones pendientes", Toast.LENGTH_SHORT).show()
        );

        return view;
    }

    private void initProductList() {
        allProducts.clear();
        allProducts.add(new Product("Presa individual", "Pierna o pechuga", 8.50, "🍗", "Pollo frito", R.drawable.bg_thumb_fried, false, 2));
        allProducts.add(new Product("1/4 de pollo frito", "Con papas incluidas", 14.00, "🍗", "Pollo frito", R.drawable.bg_thumb_fried, false, 0));
        allProducts.add(new Product("1/2 pollo a la brasa", "Con papas y ensalada", 24.00, "🔥", "A la brasa", R.drawable.bg_thumb_asado, false, 0));
        allProducts.add(new Product("Combo Familiar", "Pollo entero + 2 gaseosas", 52.00, "🥤", "Combos", R.drawable.bg_thumb_combo, false, 1));
        allProducts.add(new Product("Gaseosa 500ml", "Varios sabores", 4.00, "🥤", "Bebidas", R.drawable.bg_thumb_bebida, false, 0));
        allProducts.add(new Product("Papas fritas", "Porción regular", 6.00, "🍟", "Acompañamientos", R.drawable.bg_thumb_fried, true, 0));

        filterProducts();
    }

    private void setupCategoryChips(View view) {
        TextView chipTodos = view.findViewById(R.id.chipTodos);
        TextView chipPolloFrito = view.findViewById(R.id.chipPolloFrito);
        TextView chipBrasa = view.findViewById(R.id.chipBrasa);
        TextView chipCombos = view.findViewById(R.id.chipCombos);
        TextView chipAcompanamientos = view.findViewById(R.id.chipAcompanamientos);
        TextView chipBebidas = view.findViewById(R.id.chipBebidas);

        TextView[] chips = new TextView[]{chipTodos, chipPolloFrito, chipBrasa, chipCombos, chipAcompanamientos, chipBebidas};

        for (TextView chip : chips) {
            chip.setOnClickListener(v -> {
                selectedCategory = chip.getText().toString();
                for (TextView c : chips) {
                    if (c == chip) {
                        c.setBackgroundResource(R.drawable.bg_chip_selected);
                        c.setTextColor(requireContext().getColor(R.color.white));
                    } else {
                        c.setBackgroundResource(R.drawable.bg_chip_unselected);
                        c.setTextColor(requireContext().getColor(R.color.char_700));
                    }
                }
                filterProducts();
            });
        }
    }

    private void setupSearch(View view) {
        EditText etSearch = view.findViewById(R.id.etSearch);
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
        if (tvCartCount != null) tvCartCount.setText(String.valueOf(totalCount));
        if (tvCartTotal != null) tvCartTotal.setText(String.format(Locale.getDefault(), "Bs. %.2f", totalPrice));
    }
}