package com.example.pollogithub;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

/**
 * Controlador de Vista Principal / Dashboard: HomeActivity
 * 
 * Capa de Presentación / Contenedor Central de Navegación (Single-Activity Architecture Parcial)
 * Hereda de: AppCompatActivity
 * 
 * Actúa como panel principal del sistema POS tras la autenticación del usuario.
 * Aloja y administra una barra de navegación inferior personalizada (Bottom Navigation Bar)
 * que conmuta dinámicamente entre los cuatro módulos neurálgicos del sistema mediante transacciones
 * de fragmentos:
 * 1. Terminal de Ventas (VentaFragment)
 * 2. Comandas y Cocina / KDS (PedidosFragment)
 * 3. Analítica y Reportes (ReportesFragment)
 * 4. Perfil y Cierre de Turno (PerfilFragment)
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Modularización con Fragmentos: Desacopla la lógica y UI de cada área funcional en fragmentos reutilizables.
 * - Transacciones Atómicas de Fragmentos: Uso de 'FragmentManager.beginTransaction().replace().commit()'
 *   para garantizar transiciones de vista limpias y sin fugas de memoria.
 * - Gestión de Estado Visual: Mutación programática de ColorStateList para destacar el elemento de navegación seleccionado.
 */
public class HomeActivity extends AppCompatActivity {

    private String userName = "";

    // Componentes gráficos de la barra de navegación inferior
    private ImageView imgNavVenta, imgNavPedidos, imgNavReportes, imgNavPerfil;
    private TextView txtNavVenta, txtNavPedidos, txtNavReportes, txtNavPerfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // Compensación de insets de ventana para barras de sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainHome), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Recuperación de la identidad del operador desde los extras del Intent
        userName = getIntent().getStringExtra("USER_NAME");

        // Enlace de vistas de la barra de navegación
        imgNavVenta = findViewById(R.id.imgNavVenta);
        imgNavPedidos = findViewById(R.id.imgNavPedidos);
        imgNavReportes = findViewById(R.id.imgNavReportes);
        imgNavPerfil = findViewById(R.id.imgNavPerfil);

        txtNavVenta = findViewById(R.id.txtNavVenta);
        txtNavPedidos = findViewById(R.id.txtNavPedidos);
        txtNavReportes = findViewById(R.id.txtNavReportes);
        txtNavPerfil = findViewById(R.id.txtNavPerfil);

        // Asignación de listeners para cada pestaña operativa
        findViewById(R.id.navItemVenta).setOnClickListener(v -> selectTab(0));
        findViewById(R.id.navItemPedidos).setOnClickListener(v -> selectTab(1));
        findViewById(R.id.navItemReportes).setOnClickListener(v -> selectTab(2));
        findViewById(R.id.navItemPerfil).setOnClickListener(v -> selectTab(3));

        // Gestión de botón atrás para minimizar la aplicación en lugar de destruir la sesión
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });

        // Selección inicial de pestaña (Terminal de Ventas) en el primer arranque
        if (savedInstanceState == null) {
            selectTab(0);
        }
    }

    /**
     * Conmuta la pestaña activa actual, actualiza la apariencia visual de la barra
     * e intercambia el fragmento visible dentro del contenedor principal ('fragmentContainer').
     * 
     * @param index Índice ordinal de la pestaña (0: Venta, 1: Pedidos, 2: Reportes, 3: Perfil).
     */
    public void selectTab(int index) {
        int emberColor = ContextCompat.getColor(this, R.color.ember_600);
        int charColor = ContextCompat.getColor(this, R.color.char_400);

        // Actualización cromática de íconos y etiquetas de la barra inferior
        imgNavVenta.setImageTintList(ColorStateList.valueOf(index == 0 ? emberColor : charColor));
        txtNavVenta.setTextColor(index == 0 ? emberColor : charColor);

        imgNavPedidos.setImageTintList(ColorStateList.valueOf(index == 1 ? emberColor : charColor));
        txtNavPedidos.setTextColor(index == 1 ? emberColor : charColor);

        imgNavReportes.setImageTintList(ColorStateList.valueOf(index == 2 ? emberColor : charColor));
        txtNavReportes.setTextColor(index == 2 ? emberColor : charColor);

        imgNavPerfil.setImageTintList(ColorStateList.valueOf(index == 3 ? emberColor : charColor));
        txtNavPerfil.setTextColor(index == 3 ? emberColor : charColor);

        // Instanciación del fragmento de destino según el índice
        Fragment selectedFragment = null;
        if (index == 0) {
            selectedFragment = VentaFragment.newInstance(userName);
        } else if (index == 1) {
            selectedFragment = new PedidosFragment();
        } else if (index == 2) {
            selectedFragment = new ReportesFragment();
        } else if (index == 3) {
            selectedFragment = PerfilFragment.newInstance(userName);
        }

        // Ejecución de la transacción de reemplazo de fragmento
        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, selectedFragment)
                    .commit();
        }
    }
}