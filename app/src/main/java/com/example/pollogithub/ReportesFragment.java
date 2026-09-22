package com.example.pollogithub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ReportesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reportes, container, false);

        View btnPeriod = view.findViewById(R.id.btnPeriod);
        if (btnPeriod != null) {
            btnPeriod.setOnClickListener(v ->
                android.widget.Toast.makeText(requireContext(), "Filtro de periodo: Hoy", android.widget.Toast.LENGTH_SHORT).show()
            );
        }

        View btnCerrarCaja = view.findViewById(R.id.btnCerrarCaja);
        if (btnCerrarCaja != null) {
            btnCerrarCaja.setOnClickListener(v -> {
                android.widget.Toast.makeText(requireContext(), "Cuadre y cierre de caja procesado", android.widget.Toast.LENGTH_LONG).show();
                android.content.Intent intent = new android.content.Intent(requireContext(), MainActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                requireActivity().finish();
            });
        }

        return view;
    }
}