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

        tabCocina = view.findViewById(R.id.tabCocina);
        tabListos = view.findViewById(R.id.tabListos);

        tvTabCocinaText = view.findViewById(R.id.tvTabCocinaText);
        tvTabListosText = view.findViewById(R.id.tvTabListosText);

        tvTabCocinaCount = view.findViewById(R.id.tvTabCocinaCount);
        tvTabListosCount = view.findViewById(R.id.tvTabListosCount);

        lineTabCocina = view.findViewById(R.id.lineTabCocina);
        lineTabListos = view.findViewById(R.id.lineTabListos);

        pedidosViewModel = new ViewModelProvider(requireActivity()).get(PedidosViewModel.class);

        RecyclerView rvOrders = view.findViewById(R.id.rvOrders);
        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new OrderAdapter(requireContext(), displayedOrders, (order, position) -> {
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
        });
        rvOrders.setAdapter(adapter);

        setupTabs();

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

    private void setupTabs() {
        tabCocina.setOnClickListener(v -> selectTab("cocina"));
        tabListos.setOnClickListener(v -> selectTab("listo"));
    }

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