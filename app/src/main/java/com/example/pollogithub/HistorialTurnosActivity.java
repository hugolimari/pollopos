package com.example.pollogithub;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pollogithub.data.entity.TurnoEntity;
import com.example.pollogithub.data.repository.PosRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de Vista: HistorialTurnosActivity
 * 
 * Capa de Presentación / Módulo de Auditoría Contable y Control de Caja
 * Hereda de: AppCompatActivity
 * 
 * Despliega el registro histórico de todas las sesiones de turno operadas en la sucursal.
 * Permite a la gerencia y supervisores fiscalizar los arqueos de caja, contrastando
 * los fondos iniciales, ventas en efectivo y eventuales faltantes o sobrantes resultantes
 * de cada jornada.
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Patrón Empty State (Estado Vacío): Manejo condicional de visibilidad entre la lista de turnos (RecyclerView)
 *   y un contenedor visual informativo ('layoutEmptyTurnos') cuando no existen registros.
 * - Enlace Reactivo con LiveData: Observación continua de 'getAllTurnosLiveData()' para asegurar que
 *   los cierres de caja recientes aparezcan automáticamente en la vista sin necesidad de recargar la actividad.
 */
public class HistorialTurnosActivity extends AppCompatActivity {

    private PosRepository repository;
    private HistorialTurnosAdapter adapter;
    private final List<TurnoEntity> turnos = new ArrayList<>();
    private View layoutEmpty;
    private TextView tvTurnosSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_historial_turnos);

        // Compensación de diseño con respecto a las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainHistorialTurnos), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = PosRepository.getInstance(this);

        layoutEmpty = findViewById(R.id.layoutEmptyTurnos);
        tvTurnosSubtitle = findViewById(R.id.tvTurnosSubtitle);

        findViewById(R.id.btnBackHistorial).setOnClickListener(v -> finish());

        // 1. Configuración del RecyclerView
        RecyclerView rv = findViewById(R.id.rvTurnosHistorial);
        rv.setLayoutManager(new LinearLayoutManager(this));

        // 2. Asignación del adaptador especializado
        adapter = new HistorialTurnosAdapter(this, turnos);
        rv.setAdapter(adapter);

        // 3. Observación reactiva de la tabla de turnos en Room
        repository.getAllTurnosLiveData().observe(this, list -> {
            turnos.clear();
            if (list != null && !list.isEmpty()) {
                turnos.addAll(list);
                layoutEmpty.setVisibility(View.GONE);
                rv.setVisibility(View.VISIBLE);
                if (tvTurnosSubtitle != null) {
                    tvTurnosSubtitle.setText(String.format(Locale.getDefault(), "%d turnos registrados en la sucursal", list.size()));
                }
            } else {
                layoutEmpty.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
                if (tvTurnosSubtitle != null) {
                    tvTurnosSubtitle.setText("Sin turnos registrados");
                }
            }
            adapter.updateList(turnos);
        });
    }
}
