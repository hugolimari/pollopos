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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pollogithub.data.entity.PedidoEntity;
import com.example.pollogithub.data.repository.PosRepository;
import com.example.pollogithub.ui.viewmodel.VentaViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VentaFragment extends Fragment {

    private static final String ARG_USER_NAME = "ARG_USER_NAME";

    private final List<Product> allProducts = new ArrayList<>();
    private final List<Product> displayedProducts = new ArrayList<>();
    private ProductAdapter adapter;

    private VentaViewModel ventaViewModel;
    private View cartBar;
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

        cartBar = view.findViewById(R.id.cartBar);
        tvCartCount = view.findViewById(R.id.tvCartCount);
        tvCartTotal = view.findViewById(R.id.tvCartTotal);
        TextView tvCashierName = view.findViewById(R.id.tvCashierName);
        TextView tvAvatarHeader = view.findViewById(R.id.tvAvatarHeader);

        // Inicialmente ocultar la barra de pedido si está vacía
        cartBar.setVisibility(View.GONE);

        if (userName != null && !userName.isEmpty()) {
            tvCashierName.setText(userName);
            String initial = userName.substring(0, 1).toUpperCase(Locale.getDefault());
            tvAvatarHeader.setText(initial);
        }

        ventaViewModel = new ViewModelProvider(requireActivity()).get(VentaViewModel.class);

        RecyclerView rvProducts = view.findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        adapter = new ProductAdapter(requireContext(), displayedProducts, product -> {
            ventaViewModel.addProductToCart(product);
            adapter.notifyDataSetChanged();
        });
        rvProducts.setAdapter(adapter);

        // Observar visibilidad de la barra de pedido (solo cuando hay ítems seleccionados)
        ventaViewModel.getIsCartVisible().observe(getViewLifecycleOwner(), visible -> {
            cartBar.setVisibility(Boolean.TRUE.equals(visible) ? View.VISIBLE : View.GONE);
        });

        ventaViewModel.getCartCount().observe(getViewLifecycleOwner(), count -> {
            if (tvCartCount != null) tvCartCount.setText(String.valueOf(count));
        });

        ventaViewModel.getCartTotal().observe(getViewLifecycleOwner(), total -> {
            if (tvCartTotal != null) {
                tvCartTotal.setText(String.format(Locale.getDefault(), "Bs. %.2f", total != null ? total : 0.0));
            }
        });

        ventaViewModel.getProductsLiveData().observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                allProducts.clear();
                allProducts.addAll(products);
                filterProducts();
            }
        });

        setupCategoryChips(view);
        setupSearch(view);

        View.OnClickListener openPagoListener = v -> showOrderConfirmationDialog();

        view.findViewById(R.id.btnViewOrder).setOnClickListener(openPagoListener);
        cartBar.setOnClickListener(openPagoListener);

        view.findViewById(R.id.btnNotification).setOnClickListener(v ->
            Toast.makeText(requireContext(), "Sin notificaciones pendientes", Toast.LENGTH_SHORT).show()
        );

        return view;
    }

    private void showOrderConfirmationDialog() {
        String[] options = {"Para mesa (Mesa 1)", "Para llevar"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Tipo de pedido")
                .setItems(options, (dialog, which) -> {
                    String tipoEntrega = which == 0 ? "mesa" : "para_llevar";
                    Integer mesaId = which == 0 ? 1 : null;
                    procederAlPago(tipoEntrega, mesaId);
                })
                .show();
    }

    private void procederAlPago(String tipoEntrega, Integer mesaId) {
        ventaViewModel.confirmarPedido(tipoEntrega, mesaId, new PosRepository.Callback<PedidoEntity>() {
            @Override
            public void onSuccess(PedidoEntity pedido) {
                Intent intent = new Intent(requireContext(), PagoActivity.class);
                intent.putExtra("PEDIDO_ID", pedido.getId());
                intent.putExtra("ORDER_NUMBER", pedido.getNumeroOrden());
                intent.putExtra("TOTAL_AMOUNT", pedido.getTotal());
                startActivity(intent);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
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
            boolean matchesSearch = searchQuery.isEmpty()
                    || p.getName().toLowerCase(Locale.getDefault()).contains(searchQuery)
                    || (p.getDescription() != null && p.getDescription().toLowerCase(Locale.getDefault()).contains(searchQuery));

            if (matchesCategory && matchesSearch) {
                displayedProducts.add(p);
            }
        }
        if (adapter != null) {
            adapter.updateList(displayedProducts);
        }
    }
}