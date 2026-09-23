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

/**
 * Controlador de Vista (Fragmento): VentaFragment (Terminal de Punto de Venta)
 * 
 * Capa de Presentación / Módulo de Facturación y Mostrador
 * Hereda de: Fragment
 * 
 * Administra la experiencia de venta táctil en el mostrador del restaurante:
 * - Renderizado en cuadrícula de productos disponibles clasificados por categorías.
 * - Barra flotante de carrito de compras reactiva (Cart Bar) que emerge al seleccionar ítems.
 * - Modal para selección de modalidad de consumo (Consumo en Mesa o Para Llevar).
 * - Enlace reactivo mediante 'VentaViewModel' según la arquitectura MVVM.
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Arquitectura MVVM con LiveData: Desacoplamiento total de la lógica de precios y estado del carrito.
 * - Flujo Unidireccional de Datos (UDF): Las interacciones de usuario disparan mutaciones en el ViewModel,
 *   y la UI se reconstruye automáticamente como observadora pasiva de LiveData.
 * - Control Reactivo de Visibilidad: La barra de checkout se visibiliza exclusivamente cuando cartCount > 0.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
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

    /**
     * Patrón Factory para instanciación estandarizada con paso seguro de argumentos.
     * 
     * @param userName Nombre del cajero activo.
     * @return Nueva instancia de VentaFragment.
     */
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

        // Ocultamiento preventivo de la barra de checkout hasta que existan artículos seleccionados
        cartBar.setVisibility(View.GONE);

        if (userName != null && !userName.isEmpty()) {
            tvCashierName.setText(userName);
            String initial = userName.substring(0, 1).toUpperCase(Locale.getDefault());
            tvAvatarHeader.setText(initial);
        }

        // 1. Obtención del ViewModel compartido a nivel de Activity
        ventaViewModel = new ViewModelProvider(requireActivity()).get(VentaViewModel.class);

        // 2. Configuración de cuadrícula responsiva de 2 columnas
        RecyclerView rvProducts = view.findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        adapter = new ProductAdapter(requireContext(), displayedProducts, product -> {
            ventaViewModel.addProductToCart(product);
            adapter.notifyDataSetChanged();
        });
        rvProducts.setAdapter(adapter);

        // 3. Suscripciones Reactivas a los flujos observables del ViewModel

        // Observador: Visibilidad de la barra flotante de checkout
        ventaViewModel.getIsCartVisible().observe(getViewLifecycleOwner(), visible -> {
            cartBar.setVisibility(Boolean.TRUE.equals(visible) ? View.VISIBLE : View.GONE);
        });

        // Observador: Contador total de unidades añadidas
        ventaViewModel.getCartCount().observe(getViewLifecycleOwner(), count -> {
            if (tvCartCount != null) tvCartCount.setText(String.valueOf(count));
        });

        // Observador: Importe monetario acumulado
        ventaViewModel.getCartTotal().observe(getViewLifecycleOwner(), total -> {
            if (tvCartTotal != null) {
                tvCartTotal.setText(String.format(Locale.getDefault(), "Bs. %.2f", total != null ? total : 0.0));
            }
        });

        // Observador: Catálogo de artículos emitido desde SQLite
        ventaViewModel.getProductsLiveData().observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                allProducts.clear();
                allProducts.addAll(products);
                filterProducts();
            }
        });

        setupCategoryChips(view);
        setupSearch(view);

        // 4. Disparo de confirmación y selección de modalidad de consumo
        View.OnClickListener openPagoListener = v -> showOrderConfirmationDialog();

        view.findViewById(R.id.btnViewOrder).setOnClickListener(openPagoListener);
        cartBar.setOnClickListener(openPagoListener);

        view.findViewById(R.id.btnNotification).setOnClickListener(v ->
            Toast.makeText(requireContext(), "Sin notificaciones pendientes", Toast.LENGTH_SHORT).show()
        );

        return view;
    }

    /**
     * Despliega el diálogo modal para elegir si la orden se consumirá en mesa o para llevar.
     */
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

    /**
     * Confirma la orden en el ViewModel y navega a la pasarela de cobranza (PagoActivity).
     * 
     * @param tipoEntrega "mesa" o "para_llevar".
     * @param mesaId      Número de mesa (opcional).
     */
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

    /**
     * Configuración de los chips de filtrado por categoría de comida.
     */
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

    /**
     * Configuración del TextWatcher de búsqueda por texto.
     */
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

    /**
     * Filtra los artículos de la cuadrícula evaluando el predicado de categoría y búsqueda textual.
     */
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