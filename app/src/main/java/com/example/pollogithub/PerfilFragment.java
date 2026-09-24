package com.example.pollogithub;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.pollogithub.data.entity.InsumoEntity;
import com.example.pollogithub.data.entity.MovimientoCajaEntity;
import com.example.pollogithub.data.entity.TurnoEntity;
import com.example.pollogithub.data.repository.PosRepository;
import com.example.pollogithub.util.ThermalPrinterManager;

import java.util.List;
import java.util.Locale;

/**
 * Controlador de Vista (Fragmento): PerfilFragment (Perfil de Operador y Configuración)
 * 
 * Capa de Presentación / Módulo de Perfil, Auditoría y Mantenimiento
 * Hereda de: Fragment
 * 
 * Gestiona la visualización del estado del cajero activo, métricas del turno,
 * accesos a inventario de insumos crudos, movimientos de caja chica, configuración
 * de impresora térmica ESC/POS, historial de turnos y cierre formal de caja.
 */
public class PerfilFragment extends Fragment {

    private static final String ARG_USER_NAME = "ARG_USER_NAME";
    private String userName = "";
    private PosRepository repository;
    private ThermalPrinterManager printerManager;

    private TextView tvProfileVentasHoy;
    private TextView tvProfilePedidosCobrados;

    public static PerfilFragment newInstance(String userName) {
        PerfilFragment fragment = new PerfilFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_NAME, userName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userName = getArguments().getString(ARG_USER_NAME, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        repository = PosRepository.getInstance(requireContext());
        printerManager = new ThermalPrinterManager(requireContext());

        if (userName == null || userName.isEmpty()) {
            userName = repository.getSessionManager().getUserName();
        }

        TextView tvProfileName = view.findViewById(R.id.tvProfileName);
        TextView tvProfileAvatar = view.findViewById(R.id.tvProfileAvatar);
        tvProfileVentasHoy = view.findViewById(R.id.tvProfileVentasHoy);
        tvProfilePedidosCobrados = view.findViewById(R.id.tvProfilePedidosCobrados);

        if (userName != null && !userName.isEmpty()) {
            tvProfileName.setText(userName);
            String initial = userName.substring(0, 1).toUpperCase(Locale.getDefault());
            tvProfileAvatar.setText(initial);
        }

        // 1. Acceso a Gestión de Catálogo y Menú
        View btnGestionMenu = view.findViewById(R.id.btnGestionMenu);
        if (btnGestionMenu != null) {
            btnGestionMenu.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), GestionProductosActivity.class);
                startActivity(intent);
            });
        }

        // 2. Control de Inventario de Insumos Crudos
        View btnInventarioInsumos = view.findViewById(R.id.btnInventarioInsumos);
        if (btnInventarioInsumos != null) {
            btnInventarioInsumos.setOnClickListener(v -> showInventarioDialog());
        }

        // 3. Movimientos de Caja Chica (Gastos / Ingresos)
        View btnMovimientoCaja = view.findViewById(R.id.btnMovimientoCaja);
        if (btnMovimientoCaja != null) {
            btnMovimientoCaja.setOnClickListener(v -> showMovimientoCajaDialog());
        }

        // 4. Historial de Turnos y Cierres
        View btnHistorialTurnos = view.findViewById(R.id.btnHistorialTurnos);
        if (btnHistorialTurnos != null) {
            btnHistorialTurnos.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), HistorialTurnosActivity.class);
                startActivity(intent);
            });
        }

        // 5. Detalle modal del turno activo en curso (Arqueo X)
        View btnShiftDetails = view.findViewById(R.id.btnShiftDetails);
        if (btnShiftDetails != null) {
            btnShiftDetails.setOnClickListener(v -> showTurnoDetailsDialog());
        }

        // 6. Configuración de Impresora Térmica
        View btnPrinterStatus = view.findViewById(R.id.btnPrinterStatus);
        if (btnPrinterStatus != null) {
            btnPrinterStatus.setOnClickListener(v -> {
                printerManager.showPrinterSelectionDialog(requireActivity(), () -> {
                    Toast.makeText(requireContext(), "Impresora vinculada correctamente", Toast.LENGTH_SHORT).show();
                });
            });
        }

        // 7. Cierre formal de turno y arqueo de caja
        View btnCloseShift = view.findViewById(R.id.btnCloseShift);
        if (btnCloseShift != null) {
            btnCloseShift.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), CierreCajaActivity.class);
                startActivity(intent);
            });
        }

        // 8. Cierre de sesión y desautenticación
        View btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Cerrar sesión")
                        .setMessage("¿Estás seguro de que deseas salir del sistema?")
                        .setPositiveButton("Salir", (dialog, which) -> {
                            repository.getSessionManager().clear();
                            Intent intent = new Intent(requireContext(), MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            if (getActivity() != null) {
                                getActivity().finish();
                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
        }

        loadProfileStats();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileStats();
    }

    private void loadProfileStats() {
        int turnoId = repository.getSessionManager().getTurnoId();
        repository.getResumenTurno(turnoId, new PosRepository.Callback<PosRepository.ResumenTurno>() {
            @Override
            public void onSuccess(PosRepository.ResumenTurno resumen) {
                if (isAdded() && resumen != null) {
                    if (tvProfileVentasHoy != null) {
                        tvProfileVentasHoy.setText(String.format(Locale.getDefault(), "Bs. %.2f", resumen.totalVentas));
                    }
                    if (tvProfilePedidosCobrados != null) {
                        tvProfilePedidosCobrados.setText(String.format(Locale.getDefault(), "%d pedidos", resumen.totalPedidos));
                    }
                }
            }

            @Override
            public void onError(String error) {}
        });
    }

    /**
     * Muestra el desglose contable del turno activo en curso con cálculo de caja esperado.
     */
    private void showTurnoDetailsDialog() {
        int turnoId = repository.getSessionManager().getTurnoId();
        repository.getResumenTurno(turnoId, new PosRepository.Callback<PosRepository.ResumenTurno>() {
            @Override
            public void onSuccess(PosRepository.ResumenTurno resumen) {
                if (!isAdded() || resumen == null) return;

                String mensaje = String.format(Locale.getDefault(),
                        "• Fondo Inicial de Caja: Bs. %.2f\n\n" +
                        "• (+) Ventas en Efectivo: Bs. %.2f\n" +
                        "• (+) Ingresos Extra en Caja: Bs. %.2f\n" +
                        "• (-) Salidas / Gastos Menores: Bs. %.2f\n\n" +
                        "• (=) EFECTIVO ESPERADO EN CAJA: Bs. %.2f\n\n" +
                        "--------------------------------\n" +
                        "• Ventas Tarjeta: Bs. %.2f\n" +
                        "• Ventas QR: Bs. %.2f\n" +
                        "• TOTAL RECAUDADO: Bs. %.2f (%d pedidos)",
                        resumen.fondoInicial,
                        resumen.totalEfectivo,
                        resumen.totalIngresosExtra,
                        resumen.totalEgresosGastos,
                        resumen.esperado,
                        resumen.totalTarjeta,
                        resumen.totalQr,
                        resumen.totalVentas,
                        resumen.totalPedidos
                );

                new AlertDialog.Builder(requireContext())
                        .setTitle("Detalle del Turno Activo (Turno #" + turnoId + ")")
                        .setMessage(mensaje)
                        .setPositiveButton("Aceptar", null)
                        .show();
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "No se pudo obtener el detalle del turno", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Muestra el diálogo modal para registrar una salida (gasto menor) o ingreso en caja chica.
     */
    private void showMovimientoCajaDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_movimiento_caja, null);

        TextView btnTipoEgreso = dialogView.findViewById(R.id.btnTipoEgreso);
        TextView btnTipoIngreso = dialogView.findViewById(R.id.btnTipoIngreso);
        EditText etMonto = dialogView.findViewById(R.id.etMontoMovimiento);
        EditText etConcepto = dialogView.findViewById(R.id.etConceptoMovimiento);
        View btnClose = dialogView.findViewById(R.id.btnCloseMovimiento);
        View btnCancelar = dialogView.findViewById(R.id.btnCancelarMovimiento);
        View btnGuardar = dialogView.findViewById(R.id.btnGuardarMovimiento);

        final String[] tipoSeleccionado = {"EGRESO"};

        btnTipoEgreso.setOnClickListener(v -> {
            tipoSeleccionado[0] = "EGRESO";
            btnTipoEgreso.setBackgroundResource(R.drawable.bg_order_mode_active);
            btnTipoEgreso.setTextColor(ContextCompat.getColor(requireContext(), R.color.ember_600));
            btnTipoIngreso.setBackgroundResource(android.R.color.transparent);
            btnTipoIngreso.setTextColor(ContextCompat.getColor(requireContext(), R.color.char_400));
        });

        btnTipoIngreso.setOnClickListener(v -> {
            tipoSeleccionado[0] = "INGRESO";
            btnTipoIngreso.setBackgroundResource(R.drawable.bg_order_mode_active);
            btnTipoIngreso.setTextColor(ContextCompat.getColor(requireContext(), R.color.ok_600));
            btnTipoEgreso.setBackgroundResource(android.R.color.transparent);
            btnTipoEgreso.setTextColor(ContextCompat.getColor(requireContext(), R.color.char_400));
        });

        // Chips rápidos de motivos
        TextView chipCarbon = dialogView.findViewById(R.id.chipMotivoCarbon);
        TextView chipHielo = dialogView.findViewById(R.id.chipMotivoHielo);
        TextView chipVerduras = dialogView.findViewById(R.id.chipMotivoVerduras);

        chipCarbon.setOnClickListener(v -> etConcepto.setText("Compra de carbón vegetal"));
        chipHielo.setOnClickListener(v -> etConcepto.setText("Compra de hielo para refrescos"));
        chipVerduras.setOnClickListener(v -> etConcepto.setText("Compra de verduras para ensalada"));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            String montoStr = etMonto.getText().toString().trim();
            if (montoStr.isEmpty()) {
                etMonto.setError("Ingresa el monto");
                return;
            }
            double monto = 0.0;
            try {
                monto = Double.parseDouble(montoStr);
            } catch (NumberFormatException e) {
                etMonto.setError("Monto inválido");
                return;
            }
            if (monto <= 0) {
                etMonto.setError("El monto debe ser mayor a cero");
                return;
            }

            String concepto = etConcepto.getText().toString().trim();
            if (concepto.isEmpty()) {
                concepto = tipoSeleccionado[0].equals("EGRESO") ? "Gasto menor de caja" : "Ingreso extra a caja";
            }

            final double finalMonto = monto;
            final String finalConcepto = concepto;

            repository.registrarMovimientoCaja(tipoSeleccionado[0], finalMonto, finalConcepto, new PosRepository.Callback<MovimientoCajaEntity>() {
                @Override
                public void onSuccess(MovimientoCajaEntity result) {
                    Toast.makeText(requireContext(), "Movimiento registrado con éxito (Bs. " + String.format(Locale.getDefault(), "%.2f", finalMonto) + ")", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadProfileStats();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(requireContext(), "Error al registrar movimiento: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    /**
     * Muestra el diálogo modal con el stock de insumos crudos y opción de recarga rápida de stock.
     */
    private void showInventarioDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_inventario_insumos, null);
        LinearLayout layoutInsumos = dialogView.findViewById(R.id.layoutInsumosContainer);
        View btnClose = dialogView.findViewById(R.id.btnCloseInventario);
        View btnListo = dialogView.findViewById(R.id.btnCerrarInventario);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnListo.setOnClickListener(v -> dialog.dismiss());

        repository.getInsumos(new PosRepository.Callback<List<InsumoEntity>>() {
            @Override
            public void onSuccess(List<InsumoEntity> insumos) {
                layoutInsumos.removeAllViews();
                if (insumos == null || insumos.isEmpty()) {
                    TextView tvVacio = new TextView(requireContext());
                    tvVacio.setText("No hay insumos registrados en el sistema.");
                    tvVacio.setTextColor(ContextCompat.getColor(requireContext(), R.color.char_400));
                    layoutInsumos.addView(tvVacio);
                    return;
                }

                for (InsumoEntity ins : insumos) {
                    LinearLayout row = new LinearLayout(requireContext());
                    row.setOrientation(LinearLayout.VERTICAL);
                    row.setPadding(0, 8, 0, 8);

                    RelativeLayout itemRow = new RelativeLayout(requireContext());

                    LinearLayout leftCol = new LinearLayout(requireContext());
                    leftCol.setOrientation(LinearLayout.VERTICAL);

                    TextView tvNombre = new TextView(requireContext());
                    tvNombre.setText(ins.getNombre());
                    tvNombre.setTextColor(ContextCompat.getColor(requireContext(), R.color.char_900));
                    tvNombre.setTextSize(13.5f);
                    tvNombre.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));

                    TextView tvStock = new TextView(requireContext());
                    boolean stockBajo = ins.getStockActual() <= ins.getStockMinimo();
                    tvStock.setText(String.format(Locale.getDefault(), "Stock: %.1f %s (Mín: %.1f)", 
                            ins.getStockActual(), ins.getUnidadMedida(), ins.getStockMinimo()));
                    tvStock.setTextColor(ContextCompat.getColor(requireContext(), stockBajo ? R.color.ember_600 : R.color.char_700));
                    tvStock.setTextSize(12f);

                    leftCol.addView(tvNombre);
                    leftCol.addView(tvStock);

                    RelativeLayout.LayoutParams lpLeft = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                    lpLeft.addRule(RelativeLayout.ALIGN_PARENT_START);
                    itemRow.addView(leftCol, lpLeft);

                    // Botón para sumar stock
                    TextView btnAdd = new TextView(requireContext());
                    btnAdd.setText("+ Añadir");
                    btnAdd.setBackgroundResource(R.drawable.bg_chip_selected);
                    btnAdd.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                    btnAdd.setTextSize(11.5f);
                    btnAdd.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
                    btnAdd.setPadding(20, 10, 20, 10);

                    RelativeLayout.LayoutParams lpBtn = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                    lpBtn.addRule(RelativeLayout.ALIGN_PARENT_END);
                    lpBtn.addRule(RelativeLayout.CENTER_VERTICAL);
                    itemRow.addView(btnAdd, lpBtn);

                    btnAdd.setOnClickListener(v -> {
                        showAgregarStockDialog(ins, dialog);
                    });

                    row.addView(itemRow);

                    View sep = new View(requireContext());
                    sep.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
                    sep.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.line_color));
                    row.addView(sep);

                    layoutInsumos.addView(row);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "Error al cargar inventario: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showAgregarStockDialog(InsumoEntity insumo, AlertDialog parentDialog) {
        EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Cantidad a ingresar");
        input.setPadding(40, 30, 40, 30);

        new AlertDialog.Builder(requireContext())
                .setTitle("Ingreso de Insumo: " + insumo.getNombre())
                .setMessage("Ingresa la cantidad en " + insumo.getUnidadMedida() + " que ingresa al local:")
                .setView(input)
                .setPositiveButton("Agregar", (d, w) -> {
                    String str = input.getText().toString().trim();
                    if (!str.isEmpty()) {
                        try {
                            double cant = Double.parseDouble(str);
                            if (cant > 0) {
                                repository.agregarStockInsumo(insumo.getId(), cant, new PosRepository.Callback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        Toast.makeText(requireContext(), "Stock actualizado", Toast.LENGTH_SHORT).show();
                                        if (parentDialog.isShowing()) {
                                            parentDialog.dismiss();
                                            showInventarioDialog();
                                        }
                                    }

                                    @Override
                                    public void onError(String error) {}
                                });
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}