package com.example.pollogithub;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

public class HomeActivity extends AppCompatActivity {

    private String userName = "";

    private ImageView imgNavVenta, imgNavPedidos, imgNavReportes, imgNavPerfil;
    private TextView txtNavVenta, txtNavPedidos, txtNavReportes, txtNavPerfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainHome), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        userName = getIntent().getStringExtra("USER_NAME");

        imgNavVenta = findViewById(R.id.imgNavVenta);
        imgNavPedidos = findViewById(R.id.imgNavPedidos);
        imgNavReportes = findViewById(R.id.imgNavReportes);
        imgNavPerfil = findViewById(R.id.imgNavPerfil);

        txtNavVenta = findViewById(R.id.txtNavVenta);
        txtNavPedidos = findViewById(R.id.txtNavPedidos);
        txtNavReportes = findViewById(R.id.txtNavReportes);
        txtNavPerfil = findViewById(R.id.txtNavPerfil);

        findViewById(R.id.navItemVenta).setOnClickListener(v -> selectTab(0));
        findViewById(R.id.navItemPedidos).setOnClickListener(v -> selectTab(1));
        findViewById(R.id.navItemReportes).setOnClickListener(v -> selectTab(2));
        findViewById(R.id.navItemPerfil).setOnClickListener(v -> selectTab(3));

        if (savedInstanceState == null) {
            selectTab(0);
        }
    }

    public void selectTab(int index) {
        int emberColor = ContextCompat.getColor(this, R.color.ember_600);
        int charColor = ContextCompat.getColor(this, R.color.char_400);

        imgNavVenta.setImageTintList(ColorStateList.valueOf(index == 0 ? emberColor : charColor));
        txtNavVenta.setTextColor(index == 0 ? emberColor : charColor);

        imgNavPedidos.setImageTintList(ColorStateList.valueOf(index == 1 ? emberColor : charColor));
        txtNavPedidos.setTextColor(index == 1 ? emberColor : charColor);

        imgNavReportes.setImageTintList(ColorStateList.valueOf(index == 2 ? emberColor : charColor));
        txtNavReportes.setTextColor(index == 2 ? emberColor : charColor);

        imgNavPerfil.setImageTintList(ColorStateList.valueOf(index == 3 ? emberColor : charColor));
        txtNavPerfil.setTextColor(index == 3 ? emberColor : charColor);

        Fragment selectedFragment = null;
        if (index == 0) {
            selectedFragment = VentaFragment.newInstance(userName);
        } else if (index == 1) {
            selectedFragment = new PedidosFragment();
        } else if (index == 2) {
            selectedFragment = new ReportesFragment();
        } else if (index == 3) {
            selectedFragment = PerfilFragment.newInstance(userName);
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, selectedFragment)
                    .commit();
        }
    }
}