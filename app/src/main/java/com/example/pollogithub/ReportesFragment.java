package com.example.pollogithub;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.pollogithub.data.repository.PosRepository;

import java.util.Locale;

public class ReportesFragment extends Fragment {

    private TextView tvHeroSalesAmount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reportes, container, false);

        tvHeroSalesAmount = view.findViewById(R.id.tvHeroSalesAmount);

        View btnPeriod = view.findViewById(R.id.btnPeriod);
        if (btnPeriod != null) {
            btnPeriod.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Filtro de periodo: Hoy", Toast.LENGTH_SHORT).show()
            );
        }

        View btnCerrarCaja = view.findViewById(R.id.btnCerrarCaja);
        if (btnCerrarCaja != null) {
            btnCerrarCaja.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), CierreCajaActivity.class);
                startActivity(intent);
            });
        }

        cargarDatosReporte();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarDatosReporte();
    }

    private void cargarDatosReporte() {
        if (getContext() == null) return;
        PosRepository repo = PosRepository.getInstance(requireContext());
        int turnoId = repo.getSessionManager().getTurnoId();

        repo.getResumenTurno(turnoId, new PosRepository.Callback<PosRepository.ResumenTurno>() {
            @Override
            public void onSuccess(PosRepository.ResumenTurno result) {
                if (tvHeroSalesAmount != null && result != null) {
                    tvHeroSalesAmount.setText(String.format(Locale.getDefault(), "Bs. %.2f", result.totalVentas));
                }
            }

            @Override
            public void onError(String error) {}
        });
    }
}