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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PedidosFragment extends Fragment {

    private final List<Order> allOrders = new ArrayList<>();
    private final List<Order> displayedOrders = new ArrayList<>();
    private OrderAdapter adapter;

    private String currentTab = "cocina";

    private LinearLayout tabCocina, tabListos, tabCamino;
    private TextView tvTabCocinaText, tvTabListosText, tvTabCaminoText;
    private TextView tvTabCocinaCount, tvTabListosCount, tvTabCaminoCount;
    private View lineTabCocina, lineTabListos, lineTabCamino;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pedidos, container, false);

        tabCocina = view.findViewById(R.id.tabCocina);
        tabListos = view.findViewById(R.id.tabListos);
        tabCamino = view.findViewById(R.id.tabCamino);

        tvTabCocinaText = view.findViewById(R.id.tvTabCocinaText);
        tvTabListosText = view.findViewById(R.id.tvTabListosText);
        tvTabCaminoText = view.findViewById(R.id.tvTabCaminoText);

        tvTabCocinaCount = view.findViewById(R.id.tvTabCocinaCount);
        tvTabListosCount = view.findViewById(R.id.tvTabListosCount);
        tvTabCaminoCount = view.findViewById(R.id.tvTabCaminoCount);

        lineTabCocina = view.findViewById(R.id.lineTabCocina);
        lineTabListos = view.findViewById(R.id.lineTabListos);
        lineTabCamino = view.findViewById(R.id.lineTabCamino);

        initOrdersList();

        RecyclerView rvOrders = view.findViewById(R.id.rvOrders);
        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new OrderAdapter(requireContext(), displayedOrders, (order, position) -> {
            if (order.getStatus().equals("cocina")) {
                order.setStatus("listo");
                order.setPrimaryActionText(order.getType().contains("Delivery") ? "Enviar repartidor" : "Entregado");
                Toast.makeText(requireContext(), order.getId() + " marcado como listo", Toast.LENGTH_SHORT).show();
            } else if (order.getStatus().equals("listo")) {
                order.setStatus("camino");
                order.setPrimaryActionText("Entregado");
                Toast.makeText(requireContext(), order.getId() + " enviado con repartidor", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), order.getId() + " entregado al cliente", Toast.LENGTH_SHORT).show();
            }
            updateTabCounts();
            filterOrders();
        });
        rvOrders.setAdapter(adapter);

        setupTabs();
        updateTabCounts();

        return view;
    }

    private void initOrdersList() {
        allOrders.clear();
        allOrders.add(new Order("Pedido #0231", "Hace 3 min · Mesa 4", "cocina", "1/4 pollo frito, 1/2 pollo a la brasa, 2× gaseosa", "Para mesa", 41.40, "Marcar listo"));
        allOrders.add(new Order("Pedido #0230", "Hace 6 min · Para llevar", "cocina", "1× combo familiar, 1× papas fritas", "Para llevar", 58.00, "Marcar listo"));
        allOrders.add(new Order("Pedido #0228", "Hace 12 min · Mesa 2", "cocina", "1/2 pollo a la brasa, 1× gaseosa 1.5L", "Para mesa", 30.00, "Marcar listo"));
        allOrders.add(new Order("Pedido #0227", "Hace 15 min · Para llevar", "cocina", "2× presa individual", "Para llevar", 17.00, "Marcar listo"));

        allOrders.add(new Order("Pedido #0229", "Hace 9 min · Delivery", "listo", "2× presa individual, 1× gaseosa 1.5L", "Delivery", 33.00, "Enviar repartidor"));
        allOrders.add(new Order("Pedido #0226", "Hace 18 min · Mesa 1", "listo", "1× combo familiar", "Para mesa", 52.00, "Entregado"));

        allOrders.add(new Order("Pedido #0225", "Hace 22 min · Delivery", "camino", "1/2 pollo a la brasa, 1× papas fritas", "Delivery", 30.00, "Entregado"));

        filterOrders();
    }

    private void updateTabCounts() {
        int cocinaCount = 0;
        int listosCount = 0;
        int caminoCount = 0;

        for (Order o : allOrders) {
            if (o.getStatus().equalsIgnoreCase("cocina")) cocinaCount++;
            else if (o.getStatus().equalsIgnoreCase("listo")) listosCount++;
            else if (o.getStatus().equalsIgnoreCase("camino")) caminoCount++;
        }

        if (tvTabCocinaCount != null) tvTabCocinaCount.setText(String.valueOf(cocinaCount));
        if (tvTabListosCount != null) tvTabListosCount.setText(String.valueOf(listosCount));
        if (tvTabCaminoCount != null) tvTabCaminoCount.setText(String.valueOf(caminoCount));
    }

    private void setupTabs() {
        tabCocina.setOnClickListener(v -> selectTab("cocina"));
        tabListos.setOnClickListener(v -> selectTab("listo"));
        tabCamino.setOnClickListener(v -> selectTab("camino"));
    }

    private void selectTab(String tab) {
        currentTab = tab;

        int activeColor = requireContext().getColor(R.color.ember_600);
        int inactiveColor = requireContext().getColor(R.color.char_400);

        tvTabCocinaText.setTextColor(tab.equals("cocina") ? activeColor : inactiveColor);
        lineTabCocina.setBackgroundColor(tab.equals("cocina") ? activeColor : requireContext().getColor(android.R.color.transparent));

        tvTabListosText.setTextColor(tab.equals("listo") ? activeColor : inactiveColor);
        lineTabListos.setBackgroundColor(tab.equals("listo") ? activeColor : requireContext().getColor(android.R.color.transparent));

        tvTabCaminoText.setTextColor(tab.equals("camino") ? activeColor : inactiveColor);
        lineTabCamino.setBackgroundColor(tab.equals("camino") ? activeColor : requireContext().getColor(android.R.color.transparent));

        filterOrders();
    }

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
}