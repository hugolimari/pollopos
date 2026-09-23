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
    private TextView tvStatPedidosHoy;
    private TextView tvStatTicketPromedio;
    private TextView tvStatParaMesa;
    private TextView tvStatHoraPico;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reportes, container, false);

        tvHeroSalesAmount = view.findViewById(R.id.tvHeroSalesAmount);
        tvStatPedidosHoy = view.findViewById(R.id.tvStatPedidosHoy);
        tvStatTicketPromedio = view.findViewById(R.id.tvStatTicketPromedio);
        tvStatParaMesa = view.findViewById(R.id.tvStatParaMesa);
        tvStatHoraPico = view.findViewById(R.id.tvStatHoraPico);

        View btnPeriod = view.findViewById(R.id.btnPeriod);
        if (btnPeriod != null) {
            btnPeriod.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), HistorialTurnosActivity.class);
                startActivity(intent);
            });
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

        repo.getEstadisticasReporte(new PosRepository.Callback<PosRepository.EstadisticasReporte>() {
            @Override
            public void onSuccess(PosRepository.EstadisticasReporte stats) {
                if (stats != null) {
                    if (tvHeroSalesAmount != null) {
                        tvHeroSalesAmount.setText(String.format(Locale.getDefault(), "Bs. %.2f", stats.totalVentas));
                    }
                    if (tvStatPedidosHoy != null) {
                        tvStatPedidosHoy.setText(String.valueOf(stats.totalPedidos));
                    }
                    if (tvStatTicketPromedio != null) {
                        tvStatTicketPromedio.setText(String.format(Locale.getDefault(), "Bs. %.2f", stats.ticketPromedio));
                    }
                    if (tvStatParaMesa != null) {
                        tvStatParaMesa.setText(String.valueOf(stats.pedidosMesa));
                    }
                    if (tvStatHoraPico != null) {
                        tvStatHoraPico.setText(stats.horaPico);
                    }
                }
            }

            @Override
            public void onError(String error) {}
        });
    }
}