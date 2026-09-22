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

import java.util.Locale;

public class PerfilFragment extends Fragment {

    private static final String ARG_USER_NAME = "ARG_USER_NAME";
    private String userName = "";

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

        TextView tvProfileName = view.findViewById(R.id.tvProfileName);
        TextView tvProfileAvatar = view.findViewById(R.id.tvProfileAvatar);

        if (userName != null && !userName.isEmpty()) {
            tvProfileName.setText(userName);
            String initial = userName.substring(0, 1).toUpperCase(Locale.getDefault());
            tvProfileAvatar.setText(initial);
        }

        view.findViewById(R.id.btnPrinterStatus).setOnClickListener(v ->
            Toast.makeText(requireContext(), "Impresora conectada vía Bluetooth", Toast.LENGTH_SHORT).show()
        );

        view.findViewById(R.id.btnShiftDetails).setOnClickListener(v ->
            Toast.makeText(requireContext(), "Monto de apertura: Bs. 100.00", Toast.LENGTH_SHORT).show()
        );

        view.findViewById(R.id.btnCloseShift).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CierreCajaActivity.class);
            startActivity(intent);
        });

        return view;
    }
}