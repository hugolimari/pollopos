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
 * Historial de turnos y arqueos de caja anteriores.
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
                    tvTurnosSubtitle.setText(getString(R.string.conteo_turnos_registrados, list.size()));
                }
            } else {
                layoutEmpty.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
                if (tvTurnosSubtitle != null) {
                    tvTurnosSubtitle.setText(R.string.sin_turnos_registrados);
                }
            }
            adapter.updateList(turnos);
        });
    }
}
