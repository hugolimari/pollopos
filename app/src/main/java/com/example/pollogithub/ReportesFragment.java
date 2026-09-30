package com.example.pollogithub;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.pollogithub.data.repository.PosRepository;

import java.util.List;
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
 * - Tasa de crecimiento comparativa versus ayer.
 * - Distribución de ventas horarias de hoy en gráfico de barras.
 * - Ranking de productos más vendidos en los últimos 7 días.
 */
public class ReportesFragment extends Fragment {

    private TextView tvHeroSalesAmount;
    private View containerGrowthTag;
    private ImageView ivTrendIcon;
    private TextView tvGrowthRate;

    private TextView tvStatPedidosHoy;
    private TextView tvStatTicketPromedio;
    private TextView tvStatParaMesa;
    private TextView tvStatHoraPico;

    // Barras de distribución horaria de hoy
    private View containerHourlyBars;
    private TextView tvNoHourlySales;
    private final View[] barCols = new View[7];
    private final TextView[] tvCountCols = new TextView[7];
    private final TextView[] tvHoraCols = new TextView[7];
    private final View[] colHourlyContainers = new View[7];

    // Ranking de productos más vendidos de la semana
    private TextView tvNoTopProducts;
    private View rankItem1;
    private TextView tvRank1Name, tvRank1Sold, tvRank1Amount;
    private View rankItem2;
    private TextView tvRank2Name, tvRank2Sold, tvRank2Amount;
    private View rankItem3;
    private TextView tvRank3Name, tvRank3Sold, tvRank3Amount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reportes, container, false);

        // 1. Enlace de tarjetas métricas (KPI Cards)
        tvHeroSalesAmount = view.findViewById(R.id.tvHeroSalesAmount);
        containerGrowthTag = view.findViewById(R.id.containerGrowthTag);
        ivTrendIcon = view.findViewById(R.id.ivTrendIcon);
        tvGrowthRate = view.findViewById(R.id.tvGrowthRate);

        tvStatPedidosHoy = view.findViewById(R.id.tvStatPedidosHoy);
        tvStatTicketPromedio = view.findViewById(R.id.tvStatTicketPromedio);
        tvStatParaMesa = view.findViewById(R.id.tvStatParaMesa);
        tvStatHoraPico = view.findViewById(R.id.tvStatHoraPico);

        // 2. Enlace de barras y etiquetas de ventas por hora
        containerHourlyBars = view.findViewById(R.id.containerHourlyBars);
        tvNoHourlySales = view.findViewById(R.id.tvNoHourlySales);

        barCols[0] = view.findViewById(R.id.barCol11a);
        barCols[1] = view.findViewById(R.id.barCol12p);
        barCols[2] = view.findViewById(R.id.barCol1p);
        barCols[3] = view.findViewById(R.id.barCol2p);
        barCols[4] = view.findViewById(R.id.barCol3p);
        barCols[5] = view.findViewById(R.id.barCol4p);
        barCols[6] = view.findViewById(R.id.barCol5p);

        tvCountCols[0] = view.findViewById(R.id.tvCountCol0);
        tvCountCols[1] = view.findViewById(R.id.tvCountCol1);
        tvCountCols[2] = view.findViewById(R.id.tvCountCol2);
        tvCountCols[3] = view.findViewById(R.id.tvCountCol3);
        tvCountCols[4] = view.findViewById(R.id.tvCountCol4);
        tvCountCols[5] = view.findViewById(R.id.tvCountCol5);
        tvCountCols[6] = view.findViewById(R.id.tvCountCol6);

        tvHoraCols[0] = view.findViewById(R.id.tvHoraCol0);
        tvHoraCols[1] = view.findViewById(R.id.tvHoraCol1);
        tvHoraCols[2] = view.findViewById(R.id.tvHoraCol2);
        tvHoraCols[3] = view.findViewById(R.id.tvHoraCol3);
        tvHoraCols[4] = view.findViewById(R.id.tvHoraCol4);
        tvHoraCols[5] = view.findViewById(R.id.tvHoraCol5);
        tvHoraCols[6] = view.findViewById(R.id.tvHoraCol6);

        colHourlyContainers[0] = view.findViewById(R.id.colHourly0);
        colHourlyContainers[1] = view.findViewById(R.id.colHourly1);
        colHourlyContainers[2] = view.findViewById(R.id.colHourly2);
        colHourlyContainers[3] = view.findViewById(R.id.colHourly3);
        colHourlyContainers[4] = view.findViewById(R.id.colHourly4);
        colHourlyContainers[5] = view.findViewById(R.id.colHourly5);
        colHourlyContainers[6] = view.findViewById(R.id.colHourly6);

        // 3. Enlace de ranking semanal de productos
        tvNoTopProducts = view.findViewById(R.id.tvNoTopProducts);
        rankItem1 = view.findViewById(R.id.rankItem1);
        tvRank1Name = view.findViewById(R.id.tvRank1Name);
        tvRank1Sold = view.findViewById(R.id.tvRank1Sold);
        tvRank1Amount = view.findViewById(R.id.tvRank1Amount);

        rankItem2 = view.findViewById(R.id.rankItem2);
        tvRank2Name = view.findViewById(R.id.tvRank2Name);
        tvRank2Sold = view.findViewById(R.id.tvRank2Sold);
        tvRank2Amount = view.findViewById(R.id.tvRank2Amount);

        rankItem3 = view.findViewById(R.id.rankItem3);
        tvRank3Name = view.findViewById(R.id.tvRank3Name);
        tvRank3Sold = view.findViewById(R.id.tvRank3Sold);
        tvRank3Amount = view.findViewById(R.id.tvRank3Amount);

        // 4. Acceso al histórico de turnos pasados
        View btnPeriod = view.findViewById(R.id.btnPeriod);
        if (btnPeriod != null) {
            btnPeriod.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), HistorialTurnosActivity.class);
                startActivity(intent);
            });
        }

        // 5. Acceso directo al cierre de caja
        View btnCerrarCaja = view.findViewById(R.id.btnCerrarCaja);
        if (btnCerrarCaja != null) {
            btnCerrarCaja.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), CierreCajaActivity.class);
                startActivity(intent);
            });
        }

        // 6. Carga inicial de datos estadísticos
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
                if (stats == null || getContext() == null) return;

                // KPI Principales
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

                // Tasa comparativa vs ayer (sólo si existieron ventas ayer para calcularla)
                if (containerGrowthTag != null) {
                    if (stats.tieneDatosAyer) {
                        containerGrowthTag.setVisibility(View.VISIBLE);
                        if (tvGrowthRate != null) {
                            tvGrowthRate.setText(String.format(Locale.getDefault(), "%+.1f%% vs ayer", stats.porcentajeCrecimiento));
                        }
                        if (ivTrendIcon != null) {
                            ivTrendIcon.setRotation(stats.porcentajeCrecimiento >= 0 ? 0f : 180f);
                        }
                    } else {
                        containerGrowthTag.setVisibility(View.GONE);
                    }
                }

                // Distribución horaria de hoy
                actualizarGraficoHoras(stats);

                // Ranking de productos más vendidos de la semana
                actualizarTopProductos(stats.topProductosSemana);
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void actualizarGraficoHoras(PosRepository.EstadisticasReporte stats) {
        if (getContext() == null || stats == null) return;

        List<PosRepository.HoraVentaItem> detalle = stats.ventasPorHoraDetalle;
        boolean hayVentas = stats.totalPedidosHoy > 0 && detalle != null && !detalle.isEmpty();

        if (!hayVentas) {
            if (tvNoHourlySales != null) tvNoHourlySales.setVisibility(View.VISIBLE);
            if (containerHourlyBars != null) containerHourlyBars.setVisibility(View.GONE);
            return;
        }

        if (tvNoHourlySales != null) tvNoHourlySales.setVisibility(View.GONE);
        if (containerHourlyBars != null) containerHourlyBars.setVisibility(View.VISIBLE);

        int max = 0;
        for (PosRepository.HoraVentaItem item : detalle) {
            if (item.cantidad > max) max = item.cantidad;
        }

        float density = getResources().getDisplayMetrics().density;
        int minHeightPx = (int) (6 * density);
        int maxHeightPx = (int) (75 * density);

        int colorPrimary = ContextCompat.getColor(requireContext(), R.color.ember_600);
        int colorNormalBar = ContextCompat.getColor(requireContext(), R.color.ember_500);
        int colorMuted = ContextCompat.getColor(requireContext(), R.color.ember_100);
        int colorChar400 = ContextCompat.getColor(requireContext(), R.color.char_400);
        int colorChar700 = ContextCompat.getColor(requireContext(), R.color.char_700);

        for (int i = 0; i < 7; i++) {
            View bar = barCols[i];
            if (bar == null) continue;

            if (i < detalle.size()) {
                PosRepository.HoraVentaItem item = detalle.get(i);

                // Etiqueta de la hora dinámica (ej: 1a, 11a, 12p, 8p)
                if (tvHoraCols[i] != null) {
                    tvHoraCols[i].setText(item.label);
                }

                // Cantidad visible sobre la barra
                if (tvCountCols[i] != null) {
                    if (item.cantidad > 0) {
                        tvCountCols[i].setText(String.valueOf(item.cantidad));
                        tvCountCols[i].setVisibility(View.VISIBLE);
                        tvCountCols[i].setTextColor(item.isPico ? colorPrimary : colorChar700);
                    } else {
                        tvCountCols[i].setVisibility(View.INVISIBLE);
                    }
                }

                // Altura proporcional
                int heightPx = minHeightPx;
                if (max > 0 && item.cantidad > 0) {
                    heightPx = minHeightPx + (int) ((float) item.cantidad / max * (maxHeightPx - minHeightPx));
                }

                ViewGroup.LayoutParams lp = bar.getLayoutParams();
                if (lp != null) {
                    lp.height = heightPx;
                    bar.setLayoutParams(lp);
                }

                // Coloreado reactivo
                if (item.isPico) {
                    bar.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                    if (tvHoraCols[i] != null) tvHoraCols[i].setTextColor(colorPrimary);
                } else if (item.cantidad > 0) {
                    bar.setBackgroundTintList(ColorStateList.valueOf(colorNormalBar));
                    if (tvHoraCols[i] != null) tvHoraCols[i].setTextColor(colorChar700);
                } else {
                    bar.setBackgroundTintList(ColorStateList.valueOf(colorMuted));
                    if (tvHoraCols[i] != null) tvHoraCols[i].setTextColor(colorChar400);
                }

                // Interacción táctil informativa al tocar la columna
                View colContainer = colHourlyContainers[i];
                if (colContainer != null) {
                    final PosRepository.HoraVentaItem finalItem = item;
                    colContainer.setOnClickListener(v -> {
                        String msg = (finalItem.cantidad > 0)
                                ? String.format(Locale.getDefault(), "%s: %d venta%s · Bs. %.2f",
                                        finalItem.label, finalItem.cantidad, finalItem.cantidad == 1 ? "" : "s", finalItem.total)
                                : String.format(Locale.getDefault(), "%s: Sin ventas", finalItem.label);
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }
    }

    private void actualizarTopProductos(List<PosRepository.ProductoRanking> top) {
        if (getContext() == null) return;

        if (top == null || top.isEmpty()) {
            if (tvNoTopProducts != null) tvNoTopProducts.setVisibility(View.VISIBLE);
            if (rankItem1 != null) rankItem1.setVisibility(View.GONE);
            if (rankItem2 != null) rankItem2.setVisibility(View.GONE);
            if (rankItem3 != null) rankItem3.setVisibility(View.GONE);
            return;
        }

        if (tvNoTopProducts != null) tvNoTopProducts.setVisibility(View.GONE);

        // Posición 1
        if (top.size() >= 1 && rankItem1 != null) {
            rankItem1.setVisibility(View.VISIBLE);
            PosRepository.ProductoRanking p1 = top.get(0);
            if (tvRank1Name != null) tvRank1Name.setText(p1.nombre);
            if (tvRank1Sold != null) tvRank1Sold.setText(getString(R.string.etiqueta_vendidos_semana, p1.cantidad));
            if (tvRank1Amount != null) tvRank1Amount.setText(String.format(Locale.getDefault(), "Bs. %.2f", p1.total));
        } else if (rankItem1 != null) {
            rankItem1.setVisibility(View.GONE);
        }

        // Posición 2
        if (top.size() >= 2 && rankItem2 != null) {
            rankItem2.setVisibility(View.VISIBLE);
            PosRepository.ProductoRanking p2 = top.get(1);
            if (tvRank2Name != null) tvRank2Name.setText(p2.nombre);
            if (tvRank2Sold != null) tvRank2Sold.setText(getString(R.string.etiqueta_vendidos_semana, p2.cantidad));
            if (tvRank2Amount != null) tvRank2Amount.setText(String.format(Locale.getDefault(), "Bs. %.2f", p2.total));
        } else if (rankItem2 != null) {
            rankItem2.setVisibility(View.GONE);
        }

        // Posición 3
        if (top.size() >= 3 && rankItem3 != null) {
            rankItem3.setVisibility(View.VISIBLE);
            PosRepository.ProductoRanking p3 = top.get(2);
            if (tvRank3Name != null) tvRank3Name.setText(p3.nombre);
            if (tvRank3Sold != null) tvRank3Sold.setText(getString(R.string.etiqueta_vendidos_semana, p3.cantidad));
            if (tvRank3Amount != null) tvRank3Amount.setText(String.format(Locale.getDefault(), "Bs. %.2f", p3.total));
        } else if (rankItem3 != null) {
            rankItem3.setVisibility(View.GONE);
        }
    }
}