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

/**
 * Controlador de Vista (Fragmento): ReportesFragment (Métricas de Negocio y BI)
 * 
 * Capa de Presentación / Módulo de Inteligencia de Negocios (Business Intelligence) y Analítica
 * Hereda de: Fragment
 * 
 * Centraliza los Indicadores Clave de Desempeño (KPIs) del restaurante:
 * - Volumen bruto facturado.
 * - Conteo total de transacciones completadas.
 * - Ticket promedio por comensal (ticketPromedio = totalVentas / totalPedidos).
 * - Segmentación operativa (órdenes en mesa vs órdenes para llevar).
 * - Detección de intervalos de alta afluencia (hora pico).
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Computación Asíncrona de Métricas: Consulta vectorizada de pagos y pedidos en Worker Thread.
 * - Sincronización en el Ciclo de Vida: Invocación en 'onResume' para reflejar nuevas ventas
 *   inmediatamente después de cerrar una transacción en la pestaña de ventas.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
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

        // 1. Enlace de tarjetas métricas (KPI Cards)
        tvHeroSalesAmount = view.findViewById(R.id.tvHeroSalesAmount);
        tvStatPedidosHoy = view.findViewById(R.id.tvStatPedidosHoy);
        tvStatTicketPromedio = view.findViewById(R.id.tvStatTicketPromedio);
        tvStatParaMesa = view.findViewById(R.id.tvStatParaMesa);
        tvStatHoraPico = view.findViewById(R.id.tvStatHoraPico);

        // 2. Acceso al histórico de turnos pasados
        View btnPeriod = view.findViewById(R.id.btnPeriod);
        if (btnPeriod != null) {
            btnPeriod.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), HistorialTurnosActivity.class);
                startActivity(intent);
            });
        }

        // 3. Acceso directo al cierre de caja
        View btnCerrarCaja = view.findViewById(R.id.btnCerrarCaja);
        if (btnCerrarCaja != null) {
            btnCerrarCaja.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), CierreCajaActivity.class);
                startActivity(intent);
            });
        }

        // 4. Carga inicial de datos estadísticos
        cargarDatosReporte();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Garantiza que los indicadores se actualicen al cambiar de pestaña
        cargarDatosReporte();
    }

    /**
     * Solicita al Repositorio el cómputo asíncrono de las estadísticas consolidadas.
     */
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