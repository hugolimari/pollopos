package com.example.pollogithub;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pollogithub.data.entity.TurnoEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adaptador del Historial de Turnos y Auditoría de Arqueos: HistorialTurnosAdapter
 * 
 * Capa de Presentación / Patrón Adapter & ViewHolder
 * Hereda de: RecyclerView.Adapter<HistorialTurnosAdapter.TurnoViewHolder>
 * 
 * Modela el enlace de la colección de entidades TurnoEntity con la vista de auditoría
 * (item_historial_turno.xml). Proporciona visualización diferenciada para turnos
 * en curso (abiertos) versus turnos cerrados, destacando los resultados contables
 * del arqueo (Faltante en rojo, Sobrante en verde o Cuadre exacto).
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Algoritmo Visual de Conciliación Contable:
 *     * diferencia < -0.01: Desviación negativa (Faltante de caja).
 *     * diferencia > 0.01: Desviación positiva (Sobrante de caja).
 *     * abs(diferencia) <= 0.01: Conciliación perfecta (Caja cuadrada).
 * - Formateo Temporal de Auditoría: Normalización de marcas de tiempo Unix a formatos legibles
 *   mediante SimpleDateFormat.
 */
public class HistorialTurnosAdapter extends RecyclerView.Adapter<HistorialTurnosAdapter.TurnoViewHolder> {

    private final Context context;
    private final List<TurnoEntity> turnos = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault());
    private final SimpleDateFormat hourFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    /**
     * Constructor del adaptador de turnos históricos.
     * 
     * @param context Contexto de la actividad.
     * @param turnos  Colección de turnos para renderizar.
     */
    public HistorialTurnosAdapter(Context context, List<TurnoEntity> turnos) {
        this.context = context;
        if (turnos != null) this.turnos.addAll(turnos);
    }

    /**
     * Actualiza la colección de datos y redibuja la lista en pantalla.
     * 
     * @param newList Lista actualizada de entidades de turno.
     */
    public void updateList(List<TurnoEntity> newList) {
        this.turnos.clear();
        if (newList != null) this.turnos.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TurnoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_historial_turno, parent, false);
        return new TurnoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TurnoViewHolder holder, int position) {
        TurnoEntity t = turnos.get(position);

        holder.tvTurnoNumero.setText(String.format(Locale.getDefault(), "Turno #%04d", t.getId()));

        String fechaApertura = dateFormat.format(new Date(t.getAbiertoEn()));
        String fechaCierre = t.getCerradoEn() != null ? hourFormat.format(new Date(t.getCerradoEn())) : "En curso";
        holder.tvTurnoFechas.setText(String.format("%s · Hasta %s", fechaApertura, fechaCierre));

        holder.tvFondoInicialItem.setText(String.format(Locale.getDefault(), "Bs. %.2f", t.getFondoInicial()));

        boolean isAbierto = "abierto".equalsIgnoreCase(t.getEstado());
        if (isAbierto) {
            // Renderizado de estado de turno activo
            holder.tvTurnoEstadoBadge.setText("En curso");
            holder.tvTurnoEstadoBadge.setTextColor(ContextCompat.getColor(context, R.color.ok_600));
            holder.tvTurnoEstadoBadge.setBackgroundResource(R.drawable.bg_badge_active_shift);

            holder.tvEsperadoItem.setText("Calculando...");
            holder.tvContadoItem.setText("Pendiente");
            holder.tvDiferenciaLabel.setText("Estado del turno:");
            holder.tvDiferenciaMonto.setText("Turno abierto actualmente");
            holder.tvDiferenciaMonto.setTextColor(ContextCompat.getColor(context, R.color.ember_600));
        } else {
            // Renderizado de turno liquidado y arqueado
            holder.tvTurnoEstadoBadge.setText("Cerrado");
            holder.tvTurnoEstadoBadge.setTextColor(ContextCompat.getColor(context, R.color.char_700));
            holder.tvTurnoEstadoBadge.setBackgroundResource(R.drawable.bg_badge_agotado);

            double esperado = t.getEfectivoEsperado() != null ? t.getEfectivoEsperado() : 0.0;
            double contado = t.getEfectivoContado() != null ? t.getEfectivoContado() : 0.0;
            double diff = t.getDiferencia() != null ? t.getDiferencia() : (contado - esperado);

            holder.tvEsperadoItem.setText(String.format(Locale.getDefault(), "Bs. %.2f", esperado));
            holder.tvContadoItem.setText(String.format(Locale.getDefault(), "Bs. %.2f", contado));

            holder.tvDiferenciaLabel.setText("Diferencia en caja:");
            // Evaluación semántica del arqueo
            if (diff < -0.01) {
                // Alerta de faltante (Déficit de efectivo)
                holder.tvDiferenciaMonto.setText(String.format(Locale.getDefault(), "- Bs. %.2f (Faltante)", Math.abs(diff)));
                holder.tvDiferenciaMonto.setTextColor(Color.parseColor("#D32F2F"));
            } else if (diff > 0.01) {
                // Superávit (Sobrante de efectivo)
                holder.tvDiferenciaMonto.setText(String.format(Locale.getDefault(), "+ Bs. %.2f (Sobrante)", diff));
                holder.tvDiferenciaMonto.setTextColor(ContextCompat.getColor(context, R.color.ok_600));
            } else {
                // Conciliación exacta
                holder.tvDiferenciaMonto.setText("Bs. 0.00 (Cuadrado)");
                holder.tvDiferenciaMonto.setTextColor(ContextCompat.getColor(context, R.color.ok_600));
            }
        }
    }

    @Override
    public int getItemCount() {
        return turnos.size();
    }

    /**
     * ViewHolder con la vinculación directa a los elementos de la tarjeta de arqueo.
     */
    static class TurnoViewHolder extends RecyclerView.ViewHolder {
        TextView tvTurnoNumero, tvTurnoFechas, tvTurnoEstadoBadge;
        TextView tvFondoInicialItem, tvEsperadoItem, tvContadoItem;
        TextView tvDiferenciaLabel, tvDiferenciaMonto;

        public TurnoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTurnoNumero = itemView.findViewById(R.id.tvTurnoNumero);
            tvTurnoFechas = itemView.findViewById(R.id.tvTurnoFechas);
            tvTurnoEstadoBadge = itemView.findViewById(R.id.tvTurnoEstadoBadge);
            tvFondoInicialItem = itemView.findViewById(R.id.tvFondoInicialItem);
            tvEsperadoItem = itemView.findViewById(R.id.tvEsperadoItem);
            tvContadoItem = itemView.findViewById(R.id.tvContadoItem);
            tvDiferenciaLabel = itemView.findViewById(R.id.tvDiferenciaLabel);
            tvDiferenciaMonto = itemView.findViewById(R.id.tvDiferenciaMonto);
        }
    }
}
