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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.pollogithub.data.entity.TurnoEntity;
import com.example.pollogithub.data.repository.PosRepository;

import java.util.Locale;

public class PerfilFragment extends Fragment {

    private static final String ARG_USER_NAME = "ARG_USER_NAME";
    private String userName = "";
    private PosRepository repository;

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

        // 1. Gestión de Menú y Comida
        View btnGestionMenu = view.findViewById(R.id.btnGestionMenu);
        if (btnGestionMenu != null) {
            btnGestionMenu.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), GestionProductosActivity.class);
                startActivity(intent);
            });
        }

        // 2. Historial de Turnos y Cierres
        View btnHistorialTurnos = view.findViewById(R.id.btnHistorialTurnos);
        if (btnHistorialTurnos != null) {
            btnHistorialTurnos.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), HistorialTurnosActivity.class);
                startActivity(intent);
            });
        }

        // 3. Detalle del turno activo
        View btnShiftDetails = view.findViewById(R.id.btnShiftDetails);
        if (btnShiftDetails != null) {
            btnShiftDetails.setOnClickListener(v -> showTurnoDetailsDialog());
        }

        // 4. Impresora
        View btnPrinterStatus = view.findViewById(R.id.btnPrinterStatus);
        if (btnPrinterStatus != null) {
            btnPrinterStatus.setOnClickListener(v ->
                Toast.makeText(requireContext(), "🖨️ Impresora térmica conectada (Bluetooth / 58mm)", Toast.LENGTH_SHORT).show()
            );
        }

        // 5. Cerrar Turno
        View btnCloseShift = view.findViewById(R.id.btnCloseShift);
        if (btnCloseShift != null) {
            btnCloseShift.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), CierreCajaActivity.class);
                startActivity(intent);
            });
        }

        // 6. Cerrar Sesión
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
            public void onError(String error) {
                // Keep default if error
            }
        });
    }

    private void showTurnoDetailsDialog() {
        int turnoId = repository.getSessionManager().getTurnoId();
        repository.getResumenTurno(turnoId, new PosRepository.Callback<PosRepository.ResumenTurno>() {
            @Override
            public void onSuccess(PosRepository.ResumenTurno resumen) {
                if (!isAdded() || resumen == null) return;

                String mensaje = String.format(Locale.getDefault(),
                        "• Fondo Inicial (Caja): Bs. %.2f\n\n" +
                        "• Ventas en Efectivo: Bs. %.2f\n" +
                        "• Ventas con Tarjeta: Bs. %.2f\n" +
                        "• Ventas con QR: Bs. %.2f\n\n" +
                        "• Total Vendido: Bs. %.2f (%d pedidos)\n" +
                        "• Efectivo en caja esperado: Bs. %.2f",
                        resumen.fondoInicial,
                        resumen.totalEfectivo,
                        resumen.totalTarjeta,
                        resumen.totalQr,
                        resumen.totalVentas,
                        resumen.totalPedidos,
                        resumen.esperado
                );

                new AlertDialog.Builder(requireContext())
                        .setTitle("Detalle de Turno Activo (Turno #" + turnoId + ")")
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
}