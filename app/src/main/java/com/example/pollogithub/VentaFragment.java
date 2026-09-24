package com.example.pollogithub;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.res.ColorStateList;
import android.widget.EditText;
import android.widget.ImageView;
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

    private String currentTipoEntrega = "mesa"; // "mesa" representa "En el local" en la base de datos
    private View btnHeaderModeLocal, btnHeaderModeLlevar;
    private ImageView ivHeaderIconLocal, ivHeaderIconLlevar;
    private TextView tvHeaderTextLocal, tvHeaderTextLlevar;

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

        // Control segmentado de modalidad de entrega
        btnHeaderModeLocal = view.findViewById(R.id.btnHeaderModeLocal);
        btnHeaderModeLlevar = view.findViewById(R.id.btnHeaderModeLlevar);
        ivHeaderIconLocal = view.findViewById(R.id.ivHeaderIconLocal);
        ivHeaderIconLlevar = view.findViewById(R.id.ivHeaderIconLlevar);
        tvHeaderTextLocal = view.findViewById(R.id.tvHeaderTextLocal);
        tvHeaderTextLlevar = view.findViewById(R.id.tvHeaderTextLlevar);

        btnHeaderModeLocal.setOnClickListener(v -> setOrderDeliveryMode("mesa"));
        btnHeaderModeLlevar.setOnClickListener(v -> setOrderDeliveryMode("para_llevar"));

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

        adapter = new ProductAdapter(requireContext(), displayedProducts, new ProductAdapter.OnProductClickListener() {
            @Override
            public void onAddToCart(Product product) {
                ventaViewModel.addProductToCart(product);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onRemoveFromCart(Product product) {
                ventaViewModel.removeProductFromCart(product);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCustomizeProduct(Product product) {
                showProductModifiersDialog(product);
            }
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

        return view;
    }

    /**
     * Conmuta la modalidad de despacho activa y actualiza el control segmentado de la cabecera.
     * 
     * @param mode "mesa" (Consumo en el local) o "para_llevar".
     */
    private void setOrderDeliveryMode(String mode) {
        currentTipoEntrega = mode;
        boolean isLocal = "mesa".equalsIgnoreCase(mode);

        if (isLocal) {
            btnHeaderModeLocal.setBackgroundResource(R.drawable.bg_order_mode_active);
            ivHeaderIconLocal.setImageTintList(ColorStateList.valueOf(requireContext().getColor(R.color.ember_600)));
            tvHeaderTextLocal.setTextColor(requireContext().getColor(R.color.ember_600));
            tvHeaderTextLocal.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            btnHeaderModeLlevar.setBackgroundResource(android.R.color.transparent);
            ivHeaderIconLlevar.setImageTintList(ColorStateList.valueOf(requireContext().getColor(R.color.char_400)));
            tvHeaderTextLlevar.setTextColor(requireContext().getColor(R.color.char_400));
            tvHeaderTextLlevar.setTypeface(android.graphics.Typeface.DEFAULT);
        } else {
            btnHeaderModeLlevar.setBackgroundResource(R.drawable.bg_order_mode_active);
            ivHeaderIconLlevar.setImageTintList(ColorStateList.valueOf(requireContext().getColor(R.color.ember_600)));
            tvHeaderTextLlevar.setTextColor(requireContext().getColor(R.color.ember_600));
            tvHeaderTextLlevar.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            btnHeaderModeLocal.setBackgroundResource(android.R.color.transparent);
            ivHeaderIconLocal.setImageTintList(ColorStateList.valueOf(requireContext().getColor(R.color.char_400)));
            tvHeaderTextLocal.setTextColor(requireContext().getColor(R.color.char_400));
            tvHeaderTextLocal.setTypeface(android.graphics.Typeface.DEFAULT);
        }
    }

    /**
     * Despliega el diálogo modal estilizado para elegir si la orden se consumirá en el local o para llevar.
     */
    private void showOrderConfirmationDialog() {
        Integer cartCount = ventaViewModel.getCartCount().getValue();
        if (cartCount == null || cartCount <= 0) {
            Toast.makeText(requireContext(), "El pedido está vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        Double totalPrice = ventaViewModel.getCartTotal().getValue();
        if (totalPrice == null) totalPrice = 0.0;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_tipo_pedido, null);

        View cardOptionLocal = dialogView.findViewById(R.id.cardOptionLocal);
        View cardOptionLlevar = dialogView.findViewById(R.id.cardOptionLlevar);
        View iconContainerLocal = dialogView.findViewById(R.id.iconContainerLocal);
        View iconContainerLlevar = dialogView.findViewById(R.id.iconContainerLlevar);
        ImageView ivModalIconLocal = dialogView.findViewById(R.id.ivModalIconLocal);
        ImageView ivModalIconLlevar = dialogView.findViewById(R.id.ivModalIconLlevar);
        ImageView radioLocalIndicator = dialogView.findViewById(R.id.radioLocalIndicator);
        ImageView radioLlevarIndicator = dialogView.findViewById(R.id.radioLlevarIndicator);
        TextView tvModalCartSummary = dialogView.findViewById(R.id.tvModalCartSummary);
        TextView tvModalTotalAmount = dialogView.findViewById(R.id.tvModalTotalAmount);
        androidx.appcompat.widget.AppCompatButton btnConfirmOrderMode = dialogView.findViewById(R.id.btnConfirmOrderMode);
        View btnDismissModal = dialogView.findViewById(R.id.btnDismissModal);

        tvModalCartSummary.setText(String.format(Locale.getDefault(), "%d productos en el pedido", cartCount));
        tvModalTotalAmount.setText(String.format(Locale.getDefault(), "Bs. %.2f", totalPrice));
        btnConfirmOrderMode.setText(String.format(Locale.getDefault(), "Continuar a Cobrar (Bs. %.2f) →", totalPrice));

        final String[] selectedMode = {currentTipoEntrega};

        Runnable updateDialogCardsUI = () -> {
            boolean isLocal = "mesa".equalsIgnoreCase(selectedMode[0]);
            if (isLocal) {
                cardOptionLocal.setBackgroundResource(R.drawable.bg_card_mode_selected);
                iconContainerLocal.setBackgroundResource(R.drawable.bg_avatar_circle);
                ivModalIconLocal.setImageTintList(ColorStateList.valueOf(requireContext().getColor(R.color.white)));
                radioLocalIndicator.setImageResource(R.drawable.ic_check_circle_ember);

                cardOptionLlevar.setBackgroundResource(R.drawable.bg_card_mode_unselected);
                iconContainerLlevar.setBackgroundResource(R.drawable.bg_icon_btn);
                ivModalIconLlevar.setImageTintList(ColorStateList.valueOf(requireContext().getColor(R.color.char_700)));
                radioLlevarIndicator.setImageResource(R.drawable.ic_circle_outline);
            } else {
                cardOptionLlevar.setBackgroundResource(R.drawable.bg_card_mode_selected);
                iconContainerLlevar.setBackgroundResource(R.drawable.bg_avatar_circle);
                ivModalIconLlevar.setImageTintList(ColorStateList.valueOf(requireContext().getColor(R.color.white)));
                radioLlevarIndicator.setImageResource(R.drawable.ic_check_circle_ember);

                cardOptionLocal.setBackgroundResource(R.drawable.bg_card_mode_unselected);
                iconContainerLocal.setBackgroundResource(R.drawable.bg_icon_btn);
                ivModalIconLocal.setImageTintList(ColorStateList.valueOf(requireContext().getColor(R.color.char_700)));
                radioLocalIndicator.setImageResource(R.drawable.ic_circle_outline);
            }
        };

        updateDialogCardsUI.run();

        cardOptionLocal.setOnClickListener(v -> {
            selectedMode[0] = "mesa";
            updateDialogCardsUI.run();
        });

        cardOptionLlevar.setOnClickListener(v -> {
            selectedMode[0] = "para_llevar";
            updateDialogCardsUI.run();
        });

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnDismissModal.setOnClickListener(v -> dialog.dismiss());

        btnConfirmOrderMode.setOnClickListener(v -> {
            setOrderDeliveryMode(selectedMode[0]);
            dialog.dismiss();
            procederAlPago(selectedMode[0], null);
        });

        dialog.show();
    }

    /**
     * Confirma la orden en el ViewModel y navega a la pasarela de cobranza (PagoActivity).
     * 
     * @param tipoEntrega "mesa" (Consumo en el local) o "para_llevar".
     * @param mesaId      Número de mesa (nulo por diseño de negocio).
     */
    private void procederAlPago(String tipoEntrega, Integer mesaId) {
        ventaViewModel.confirmarPedido(tipoEntrega, null, new PosRepository.Callback<PedidoEntity>() {
            @Override
            public void onSuccess(PedidoEntity pedido) {
                Intent intent = new Intent(requireContext(), PagoActivity.class);
                intent.putExtra("PEDIDO_ID", pedido.getId());
                intent.putExtra("ORDER_NUMBER", pedido.getNumeroOrden());
                intent.putExtra("TOTAL_AMOUNT", pedido.getTotal());
                intent.putExtra("TIPO_ENTREGA", tipoEntrega);
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

    /**
     * Muestra el diálogo modal para personalizar el plato con modificadores de cocina rápidos
     * (Pierna, Pechuga, Bien dorado, Sin ensalada, Sin ají, Salsa aparte) o notas de comanda.
     */
    private void showProductModifiersDialog(Product product) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modificador_producto, null);

        TextView tvModalProdName = dialogView.findViewById(R.id.tvModalProdName);
        TextView tvModalProdPrice = dialogView.findViewById(R.id.tvModalProdPrice);
        EditText etCustomNotes = dialogView.findViewById(R.id.etCustomNotes);
        View btnCloseModifier = dialogView.findViewById(R.id.btnCloseModifier);
        View btnCancelModifier = dialogView.findViewById(R.id.btnCancelModifier);
        View btnConfirmModifier = dialogView.findViewById(R.id.btnConfirmModifier);

        tvModalProdName.setText(product.getName());
        tvModalProdPrice.setText(String.format(Locale.getDefault(), "Bs. %.2f", product.getPrice()));

        TextView chipPierna = dialogView.findViewById(R.id.chipPierna);
        TextView chipPechuga = dialogView.findViewById(R.id.chipPechuga);
        TextView chipDorado = dialogView.findViewById(R.id.chipDorado);
        TextView chipSinEnsalada = dialogView.findViewById(R.id.chipSinEnsalada);
        TextView chipSinAji = dialogView.findViewById(R.id.chipSinAji);
        TextView chipSalsaAparte = dialogView.findViewById(R.id.chipSalsaAparte);

        TextView[] chips = new TextView[]{chipPierna, chipPechuga, chipDorado, chipSinEnsalada, chipSinAji, chipSalsaAparte};
        boolean[] chipStates = new boolean[chips.length];

        for (int i = 0; i < chips.length; i++) {
            final int idx = i;
            chips[idx].setOnClickListener(v -> {
                chipStates[idx] = !chipStates[idx];
                if (chipStates[idx]) {
                    chips[idx].setBackgroundResource(R.drawable.bg_chip_selected);
                    chips[idx].setTextColor(requireContext().getColor(R.color.white));
                } else {
                    chips[idx].setBackgroundResource(R.drawable.bg_chip_unselected);
                    chips[idx].setTextColor(requireContext().getColor(R.color.char_700));
                }
            });
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCloseModifier.setOnClickListener(v -> dialog.dismiss());
        btnCancelModifier.setOnClickListener(v -> dialog.dismiss());

        btnConfirmModifier.setOnClickListener(v -> {
            List<String> activeModifiers = new ArrayList<>();
            for (int i = 0; i < chips.length; i++) {
                if (chipStates[i]) {
                    activeModifiers.add(chips[i].getText().toString());
                }
            }

            String customText = etCustomNotes.getText().toString().trim();
            if (!customText.isEmpty()) {
                activeModifiers.add(customText);
            }

            String finalNotes = String.join(" · ", activeModifiers);
            product.setNotes(finalNotes);

            ventaViewModel.addProductToCart(product);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

            dialog.dismiss();
            Toast.makeText(requireContext(), product.getName() + (finalNotes.isEmpty() ? " agregado" : " (" + finalNotes + ")"), Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
}