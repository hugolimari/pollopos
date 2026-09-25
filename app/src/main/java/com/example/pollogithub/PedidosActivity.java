package com.example.pollogithub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de Vista: PedidosActivity (Monitor KDS y Despacho)
 * 
 * Capa de Presentación / Sistema de Visualización de Cocina (Kitchen Display System - KDS)
 * Hereda de: AppCompatActivity
 * 
 * Gestiona el ciclo operativo de preparación culinaria y entrega de comandas.
 * Clasifica los pedidos en tres fases logísticas ("cocina", "listo", "camino"),
 * permitiendo al personal avanzar dinámicamente los estados mediante pulsadores de acción rápida.
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Máquina de Estados Finitos (FSM): Transiciones deterministas:
 *     * "cocina" -> "listo" -> "entregado" (para mesas y consumo en local)
 *     * "cocina" -> "listo" -> "camino" -> "entregado" (para despachos con repartidor a domicilio)
 * - Filtrado en Memoria y Conteo Segmentado: Recálculo en tiempo real de los contadores numéricos
 *   asociados a cada pestaña de estado operativo.
 * - Desacoplamiento de Eventos: Uso de listeners en el adaptador para mutar el modelo y refrescar la vista.
 */
public class PedidosActivity extends AppCompatActivity {

    private final List<Order> allOrders = new ArrayList<>();
    private final List<Order> displayedOrders = new ArrayList<>();
    private OrderAdapter adapter;

    private String currentTab = "cocina"; // "cocina", "listo", "camino"

    private LinearLayout tabCocina, tabListos, tabCamino;
    private TextView tvTabCocinaText, tvTabListosText, tvTabCaminoText;
    private TextView tvTabCocinaCount, tvTabListosCount, tvTabCaminoCount;
    private View lineTabCocina, lineTabListos, lineTabCamino;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pedidos);

        // Compensación de insets de ventana para barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainPedidos), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Enlace de vistas para pestañas de filtrado logístico
        tabCocina = findViewById(R.id.tabCocina);
        tabListos = findViewById(R.id.tabListos);
        tabCamino = findViewById(R.id.tabCamino);

        tvTabCocinaText = findViewById(R.id.tvTabCocinaText);
        tvTabListosText = findViewById(R.id.tvTabListosText);
        tvTabCaminoText = findViewById(R.id.tvTabCaminoText);

        tvTabCocinaCount = findViewById(R.id.tvTabCocinaCount);
        tvTabListosCount = findViewById(R.id.tvTabListosCount);
        tvTabCaminoCount = findViewById(R.id.tvTabCaminoCount);

        lineTabCocina = findViewById(R.id.lineTabCocina);
        lineTabListos = findViewById(R.id.lineTabListos);
        lineTabCamino = findViewById(R.id.lineTabCamino);

        // 2. Carga inicial de datos de demostración
        initOrdersList();

        // 3. Configuración del RecyclerView y enlace con el adaptador
        RecyclerView rvOrders = findViewById(R.id.rvOrders);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));

        adapter = new OrderAdapter(this, displayedOrders, (order, position) -> {
            // Transición de estados de la orden según la lógica de preparación
            if (order.getStatus().equals("cocina")) {
                order.setStatus("listo");
                order.setPrimaryActionText(order.getType().contains("Delivery") ? "Enviar repartidor" : "Entregado");
                Toast.makeText(this, order.getId() + " marcado como listo", Toast.LENGTH_SHORT).show();
            } else if (order.getStatus().equals("listo")) {
                order.setStatus("camino");
                order.setPrimaryActionText("Entregado");
                Toast.makeText(this, order.getId() + " enviado con repartidor", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, order.getId() + " entregado al cliente", Toast.LENGTH_SHORT).show();
            }
            updateTabCounts();
            filterOrders();
        });
        rvOrders.setAdapter(adapter);

        setupTabs();
        setupBottomNav();
        updateTabCounts();
    }

    /**
     * Inicializa pedidos de prueba para verificación de la interfaz KDS.
     */
    private void initOrdersList() {
        allOrders.add(new Order("Pedido #0231", "Hace 3 min · Mesa 4", "cocina", "1/4 pollo frito, 1/2 pollo a la brasa, 2× gaseosa", "Para mesa", 41.40, "Marcar listo"));
        allOrders.add(new Order("Pedido #0230", "Hace 6 min · Para llevar", "cocina", "1× combo familiar, 1× papas fritas", "Para llevar", 58.00, "Marcar listo"));
        allOrders.add(new Order("Pedido #0228", "Hace 12 min · Mesa 2", "cocina", "1/2 pollo a la brasa, 1× gaseosa 1.5L", "Para mesa", 30.00, "Marcar listo"));
        allOrders.add(new Order("Pedido #0227", "Hace 15 min · Para llevar", "cocina", "2× presa individual", "Para llevar", 17.00, "Marcar listo"));

        allOrders.add(new Order("Pedido #0229", "Hace 9 min · Delivery", "listo", "2× presa individual, 1× gaseosa 1.5L", "Delivery", 33.00, "Enviar repartidor"));
        allOrders.add(new Order("Pedido #0226", "Hace 18 min · Mesa 1", "listo", "1× combo familiar", "Para mesa", 52.00, "Entregado"));

        allOrders.add(new Order("Pedido #0225", "Hace 22 min · Delivery", "camino", "1/2 pollo a la brasa, 1× papas fritas", "Delivery", 30.00, "Entregado"));

        filterOrders();
    }

    /**
     * Recalcula los contadores cuantitativos para cada pestaña logística.
     */
    private void updateTabCounts() {
        int cocinaCount = 0;
        int listosCount = 0;
        int caminoCount = 0;

        for (Order o : allOrders) {
            if (o.getStatus().equalsIgnoreCase("cocina")) cocinaCount++;
            else if (o.getStatus().equalsIgnoreCase("listo")) listosCount++;
            else if (o.getStatus().equalsIgnoreCase("camino")) caminoCount++;
        }

        tvTabCocinaCount.setText(String.valueOf(cocinaCount));
        tvTabListosCount.setText(String.valueOf(listosCount));
        tvTabCaminoCount.setText(String.valueOf(caminoCount));
    }

    /**
     * Asocia los eventos de cambio de pestaña para filtrar los pedidos.
     */
    private void setupTabs() {
        tabCocina.setOnClickListener(v -> selectTab("cocina"));
        tabListos.setOnClickListener(v -> selectTab("listo"));
        tabCamino.setOnClickListener(v -> selectTab("camino"));
    }

    /**
     * Aplica los estilos cromáticos a la pestaña seleccionada y actualiza el filtro.
     * 
     * @param tab Nombre del estado logístico ("cocina", "listo", "camino").
     */
    private void selectTab(String tab) {
        currentTab = tab;

        int activeColor = getColor(R.color.ember_600);
        int inactiveColor = getColor(R.color.char_400);

        tvTabCocinaText.setTextColor(tab.equals("cocina") ? activeColor : inactiveColor);
        lineTabCocina.setBackgroundColor(tab.equals("cocina") ? activeColor : getColor(android.R.color.transparent));

        tvTabListosText.setTextColor(tab.equals("listo") ? activeColor : inactiveColor);
        lineTabListos.setBackgroundColor(tab.equals("listo") ? activeColor : getColor(android.R.color.transparent));

        tvTabCaminoText.setTextColor(tab.equals("camino") ? activeColor : inactiveColor);
        lineTabCamino.setBackgroundColor(tab.equals("camino") ? activeColor : getColor(android.R.color.transparent));

        filterOrders();
    }

    /**
     * Filtra la colección de órdenes en memoria según la pestaña logística activa.
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
     * Configuración de la barra de navegación inferior para conmutar entre módulos.
     */
    private void setupBottomNav() {
        findViewById(R.id.navItemVenta).setOnClickListener(v -> {
            Intent intent = new Intent(PedidosActivity.this, VentaActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navItemReportes).setOnClickListener(v ->
            Toast.makeText(this, "Sección Reportes", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.navItemPerfil).setOnClickListener(v -> {
            Intent intent = new Intent(PedidosActivity.this, PerfilActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
    }
}