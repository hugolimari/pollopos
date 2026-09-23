package com.example.pollogithub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pollogithub.data.repository.PosRepository;
import com.example.pollogithub.ui.viewmodel.PedidosViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de Vista (Fragmento): PedidosFragment (Monitor de Cocina y Despacho)
 * 
 * Capa de Presentación / Módulo KDS (Kitchen Display System)
 * Hereda de: Fragment
 * 
 * Visualiza y orquesta el flujo de preparación y entrega de pedidos en tiempo real.
 * Se integra con 'PedidosViewModel' mediante la arquitectura MVVM, observando
 * los cambios en la base de datos local y proveyendo modales de auditoría para
 * ver el desglose detallado de comandas o cancelar pedidos con registro de causa justificada.
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Ciclo de Vida del Fragmento (Fragment Lifecycle): Uso de 'getViewLifecycleOwner()'
 *   para suscribirse a LiveData, evitando fugas de memoria al destruir la vista del fragmento.
 * - Patrón Factory / Inyección Compartida de ViewModel: Uso de 'requireActivity()' en ViewModelProvider
 *   para compartir el estado del ViewModel a nivel de actividad anfitriona.
 * - Inyección Dinámica de Componentes de UI: Generación programática de filas y notas de cocina en el diálogo modal.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
public class PedidosFragment extends Fragment {

    private final List<Order> allOrders = new ArrayList<>();
    private final List<Order> displayedOrders = new ArrayList<>();
    private OrderAdapter adapter;

    private String currentTab = "cocina";

    private LinearLayout tabCocina, tabListos;
    private TextView tvTabCocinaText, tvTabListosText;
    private TextView tvTabCocinaCount, tvTabListosCount;
    private View lineTabCocina, lineTabListos;

    private PedidosViewModel pedidosViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pedidos, container, false);

        // 1. Enlace de vistas para pestañas de filtrado de comandas
        tabCocina = view.findViewById(R.id.tabCocina);
        tabListos = view.findViewById(R.id.tabListos);

        tvTabCocinaText = view.findViewById(R.id.tvTabCocinaText);
        tvTabListosText = view.findViewById(R.id.tvTabListosText);

        tvTabCocinaCount = view.findViewById(R.id.tvTabCocinaCount);
        tvTabListosCount = view.findViewById(R.id.tvTabListosCount);

        lineTabCocina = view.findViewById(R.id.lineTabCocina);
        lineTabListos = view.findViewById(R.id.lineTabListos);

        // 2. Inicialización del ViewModel compartido con la Activity anfitriona
        pedidosViewModel = new ViewModelProvider(requireActivity()).get(PedidosViewModel.class);

        // 3. Configuración del RecyclerView y Adaptador de Órdenes
        RecyclerView rvOrders = view.findViewById(R.id.rvOrders);
        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new OrderAdapter(requireContext(), displayedOrders, new OrderAdapter.OnOrderActionListener() {
            @Override
            public void onPrimaryAction(Order order, int position) {
                // Avanza el estado de la comanda mediante la máquina de estados del ViewModel
                pedidosViewModel.avanzarEstadoPedido(order, new PosRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if ("cocina".equalsIgnoreCase(order.getStatus())) {
                            Toast.makeText(requireContext(), order.getId() + " marcado como listo", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), order.getId() + " entregado al cliente", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onViewDetail(Order order, int position) {
                showDetallePedidoDialog(order);
            }
        });
        rvOrders.setAdapter(adapter);

        setupTabs();

        // 4. Suscripción reactiva al LiveData de pedidos activos
        pedidosViewModel.getOrdersLiveData().observe(getViewLifecycleOwner(), orders -> {
            if (orders != null) {
                allOrders.clear();
                allOrders.addAll(orders);
                updateTabCounts();
                filterOrders();
            }
        });

        return view;
    }

    /**
     * Computa los totales por estado para actualizar los indicadores numéricos en las pestañas.
     */
    private void updateTabCounts() {
        int cocinaCount = 0;
        int listosCount = 0;

        for (Order o : allOrders) {
            if (o.getStatus().equalsIgnoreCase("cocina")) cocinaCount++;
            else if (o.getStatus().equalsIgnoreCase("listo")) listosCount++;
        }

        if (tvTabCocinaCount != null) tvTabCocinaCount.setText(String.valueOf(cocinaCount));
        if (tvTabListosCount != null) tvTabListosCount.setText(String.valueOf(listosCount));
    }

    /**
     * Asocia los manejadores de eventos a las pestañas de cocina y listos.
     */
    private void setupTabs() {
        tabCocina.setOnClickListener(v -> selectTab("cocina"));
        tabListos.setOnClickListener(v -> selectTab("listo"));
    }

    /**
     * Conmuta la pestaña activa actualizando estilos e invocando el filtrado.
     * 
     * @param tab Estado seleccionado ("cocina", "listo").
     */
    private void selectTab(String tab) {
        currentTab = tab;

        int activeColor = requireContext().getColor(R.color.ember_600);
        int inactiveColor = requireContext().getColor(R.color.char_400);

        tvTabCocinaText.setTextColor(tab.equals("cocina") ? activeColor : inactiveColor);
        lineTabCocina.setBackgroundColor(tab.equals("cocina") ? activeColor : requireContext().getColor(android.R.color.transparent));

        tvTabListosText.setTextColor(tab.equals("listo") ? activeColor : inactiveColor);
        lineTabListos.setBackgroundColor(tab.equals("listo") ? activeColor : requireContext().getColor(android.R.color.transparent));

        filterOrders();
    }

    /**
     * Aplica el filtro en memoria de acuerdo al estado operativo activo.
     */
    private void filterOrders() {
        displayedOrders.clear();
        for (Order o : allOrders) {
            if (o.getStatus().equalsIgnoreCase(currentTab)) {
                displayedOrders.add(o);
            }
        }
        if (adapter != null) {
            adapter.updateList(displayedOrders);
        }
    }

    /**
     * Genera dinámicamente un diálogo modal con el desglose de productos y notas de preparación.
     * 
     * @param order Orden seleccionada.
     */
    private void showDetallePedidoDialog(Order order) {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_detalle_pedido, null);

        TextView tvOrderId = dialogView.findViewById(R.id.tvDetalleOrderId);
        TextView tvOrderMeta = dialogView.findViewById(R.id.tvDetalleOrderMeta);
        TextView tvStatusBadge = dialogView.findViewById(R.id.tvDetalleStatusBadge);
        TextView tvTotal = dialogView.findViewById(R.id.tvDetalleTotal);
        LinearLayout container = dialogView.findViewById(R.id.layoutItemsContainer);

        tvOrderId.setText(order.getId());
        tvOrderMeta.setText(String.format("%s · %s", order.getTime(), order.getType()));
        tvTotal.setText(String.format(Locale.getDefault(), "Bs. %.2f", order.getTotal()));

        if ("cocina".equalsIgnoreCase(order.getStatus())) {
            tvStatusBadge.setText("En cocina");
            tvStatusBadge.setTextColor(requireContext().getColor(R.color.wait_600));
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_cocina);
        } else {
            tvStatusBadge.setText("Listo");
            tvStatusBadge.setTextColor(requireContext().getColor(R.color.ok_600));
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_listo);
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Carga asíncrona de las líneas de detalle asociadas a la comanda
        PosRepository.getInstance(requireContext()).getPedidoDetalles(order.getPedidoId(), new PosRepository.Callback<List<com.example.pollogithub.data.entity.PedidoDetalleEntity>>() {
            @Override
            public void onSuccess(List<com.example.pollogithub.data.entity.PedidoDetalleEntity> detalles) {
                container.removeAllViews();
                if (detalles != null && !detalles.isEmpty()) {
                    for (com.example.pollogithub.data.entity.PedidoDetalleEntity d : detalles) {
                        LinearLayout row = new LinearLayout(requireContext());
                        row.setOrientation(LinearLayout.VERTICAL);
                        row.setPadding(0, 6, 0, 6);

                        LinearLayout topRow = new LinearLayout(requireContext());
                        topRow.setOrientation(LinearLayout.HORIZONTAL);

                        TextView tvItemName = new TextView(requireContext());
                        tvItemName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
                        tvItemName.setText(String.format(Locale.getDefault(), "%d× %s", d.getCantidad(), d.getNombreProducto()));
                        tvItemName.setTextColor(requireContext().getColor(R.color.char_900));
                        tvItemName.setTextSize(13.5f);
                        tvItemName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

                        TextView tvItemSubtotal = new TextView(requireContext());
                        tvItemSubtotal.setText(String.format(Locale.getDefault(), "Bs. %.2f", d.getSubtotal()));
                        tvItemSubtotal.setTextColor(requireContext().getColor(R.color.char_700));
                        tvItemSubtotal.setTextSize(13.5f);
                        tvItemSubtotal.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

                        topRow.addView(tvItemName);
                        topRow.addView(tvItemSubtotal);
                        row.addView(topRow);

                        // Renderizado de especificaciones particulares para la cocina
                        if (d.getNotas() != null && !d.getNotas().trim().isEmpty()) {
                            TextView tvNote = new TextView(requireContext());
                            tvNote.setText(String.format("↳ Nota: \"%s\"", d.getNotas().trim()));
                            tvNote.setTextColor(requireContext().getColor(R.color.ember_600));
                            tvNote.setTextSize(11.5f);
                            tvNote.setPadding(12, 2, 0, 0);
                            row.addView(tvNote);
                        }

                        container.addView(row);
                    }
                } else {
                    TextView tvEmpty = new TextView(requireContext());
                    tvEmpty.setText("No hay ítems registrados");
                    tvEmpty.setTextColor(requireContext().getColor(R.color.char_400));
                    container.addView(tvEmpty);
                }
            }

            @Override
            public void onError(String error) {}
        });

        dialogView.findViewById(R.id.btnCerrarDetalleDialog).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnCancelarPedidoDialog).setOnClickListener(v -> {
            dialog.dismiss();
            mostrarDialogoMotivoCancelacion(order);
        });

        dialog.show();
    }

    /**
     * Despliega un menú modal para capturar la justificación requerida al revocar un pedido.
     * 
     * @param order Pedido a cancelar.
     */
    private void mostrarDialogoMotivoCancelacion(Order order) {
        String[] motivos = {
                "Cliente desistió de la compra",
                "Error en la toma del pedido",
                "Falta de insumos / producto agotado",
                "Demora excesiva en preparación",
                "Otro motivo"
        };

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Motivo de cancelación (" + order.getId() + ")")
                .setItems(motivos, (d, which) -> {
                    String motivoSeleccionado = motivos[which];
                    // Invocación al repositorio para ejecutar la cancelación con registro de auditoría
                    PosRepository.getInstance(requireContext()).cancelarPedido(order.getPedidoId(), motivoSeleccionado, new PosRepository.Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(requireContext(), order.getId() + " cancelado: " + motivoSeleccionado, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(requireContext(), "Error al cancelar: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Regresar", null)
                .show();
    }
}