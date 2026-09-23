package com.example.pollogithub;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pollogithub.data.entity.ProductoEntity;
import com.example.pollogithub.data.repository.PosRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GestionProductosActivity extends AppCompatActivity {

    private PosRepository repository;
    private final List<ProductoEntity> allProductos = new ArrayList<>();
    private final List<ProductoEntity> filteredProductos = new ArrayList<>();
    private GestionProductosAdapter adapter;

    private String selectedCategory = "Todos";
    private String searchQuery = "";
    private TextView tvTotalCountSubtitle;

    private final String[] categoryNames = {"Pollo frito", "A la brasa", "Combos", "Bebidas", "Acompañamientos"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gestion_productos);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainGestionProductos), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = PosRepository.getInstance(this);

        tvTotalCountSubtitle = findViewById(R.id.tvTotalCountSubtitle);
        findViewById(R.id.btnBackGestion).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvProductosGestion);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new GestionProductosAdapter(this, filteredProductos, new GestionProductosAdapter.OnProductoActionListener() {
            @Override
            public void onToggleDisponible(ProductoEntity producto, boolean disponible) {
                repository.setProductoDisponible(producto.getId(), disponible, new PosRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        String msg = disponible ? producto.getNombre() + " habilitado para la venta" : producto.getNombre() + " marcado como agotado";
                        Toast.makeText(GestionProductosActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(GestionProductosActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onEditProducto(ProductoEntity producto) {
                showProductoFormDialog(producto);
            }
        });
        rv.setAdapter(adapter);

        setupCategoryChips();
        setupSearch();

        findViewById(R.id.fabAddProduct).setOnClickListener(v -> showProductoFormDialog(null));

        repository.getProductosLiveData().observe(this, productos -> {
            if (productos != null) {
                allProductos.clear();
                allProductos.addAll(productos);
                filterList();
            }
        });
    }

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearchGestion);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.getDefault());
                filterList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategoryChips() {
        TextView chipTodos = findViewById(R.id.chipTodos);
        TextView chipPolloFrito = findViewById(R.id.chipPolloFrito);
        TextView chipBrasa = findViewById(R.id.chipBrasa);
        TextView chipCombos = findViewById(R.id.chipCombos);
        TextView chipAcompanamientos = findViewById(R.id.chipAcompanamientos);
        TextView chipBebidas = findViewById(R.id.chipBebidas);

        TextView[] chips = new TextView[]{chipTodos, chipPolloFrito, chipBrasa, chipCombos, chipAcompanamientos, chipBebidas};

        for (TextView chip : chips) {
            chip.setOnClickListener(v -> {
                selectedCategory = chip.getText().toString();
                for (TextView c : chips) {
                    if (c == chip) {
                        c.setBackgroundResource(R.drawable.bg_chip_selected);
                        c.setTextColor(getColor(R.color.white));
                    } else {
                        c.setBackgroundResource(R.drawable.bg_chip_unselected);
                        c.setTextColor(getColor(R.color.char_700));
                    }
                }
                filterList();
            });
        }
    }

    private void filterList() {
        filteredProductos.clear();
        int activeCount = 0;

        for (ProductoEntity p : allProductos) {
            if (p.isDisponible()) activeCount++;

            boolean matchesCategory = selectedCategory.equals("Todos") || getCategoryName(p.getCategoriaId()).equalsIgnoreCase(selectedCategory);
            boolean matchesSearch = searchQuery.isEmpty()
                    || p.getNombre().toLowerCase(Locale.getDefault()).contains(searchQuery)
                    || (p.getDescripcion() != null && p.getDescripcion().toLowerCase(Locale.getDefault()).contains(searchQuery));

            if (matchesCategory && matchesSearch) {
                filteredProductos.add(p);
            }
        }

        if (tvTotalCountSubtitle != null) {
            tvTotalCountSubtitle.setText(String.format(Locale.getDefault(), "%d productos en menú (%d disponibles)", allProductos.size(), activeCount));
        }

        if (adapter != null) {
            adapter.updateList(filteredProductos);
        }
    }

    private String getCategoryName(int catId) {
        switch (catId) {
            case 1: return "Pollo frito";
            case 2: return "A la brasa";
            case 3: return "Combos";
            case 4: return "Bebidas";
            case 5: return "Acompañamientos";
            default: return "Todos";
        }
    }

    private int getCategoryId(String name) {
        switch (name) {
            case "Pollo frito": return 1;
            case "A la brasa": return 2;
            case "Combos": return 3;
            case "Bebidas": return 4;
            case "Acompañamientos": return 5;
            default: return 1;
        }
    }

    private void showProductoFormDialog(ProductoEntity productoToEdit) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_producto_form, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvFormTitle);
        TextView tvSubtitle = dialogView.findViewById(R.id.tvFormSubtitle);
        EditText etNombre = dialogView.findViewById(R.id.etNombre);
        Spinner spCategoria = dialogView.findViewById(R.id.spCategoria);
        EditText etPrecio = dialogView.findViewById(R.id.etPrecio);
        EditText etDescripcion = dialogView.findViewById(R.id.etDescripcion);
        SwitchCompat switchDisponible = dialogView.findViewById(R.id.switchDisponibleForm);

        // Emoji selectors
        TextView optChicken = dialogView.findViewById(R.id.optEmojiChicken);
        TextView optFire = dialogView.findViewById(R.id.optEmojiFire);
        TextView optDrink = dialogView.findViewById(R.id.optEmojiDrink);
        TextView optFries = dialogView.findViewById(R.id.optEmojiFries);
        TextView optSalad = dialogView.findViewById(R.id.optEmojiSalad);
        TextView optBurger = dialogView.findViewById(R.id.optEmojiBurger);

        final TextView[] emojiViews = new TextView[]{optChicken, optFire, optDrink, optFries, optSalad, optBurger};
        final String[] selectedEmoji = {productoToEdit != null ? productoToEdit.getEmoji() : "🍗"};

        for (TextView ev : emojiViews) {
            if (ev.getText().toString().equals(selectedEmoji[0])) {
                ev.setBackgroundResource(R.drawable.bg_chip_selected);
            } else {
                ev.setBackgroundResource(R.drawable.bg_chip_unselected);
            }

            ev.setOnClickListener(v -> {
                selectedEmoji[0] = ev.getText().toString();
                for (TextView other : emojiViews) {
                    other.setBackgroundResource(other == ev ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
                }
            });
        }

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryNames);
        spCategoria.setAdapter(catAdapter);

        boolean isEditing = productoToEdit != null;
        if (isEditing) {
            tvTitle.setText("Modificar comida");
            tvSubtitle.setText("Actualiza los detalles del plato en el menú");
            etNombre.setText(productoToEdit.getNombre());
            etPrecio.setText(String.format(Locale.US, "%.2f", productoToEdit.getPrecio()));
            if (productoToEdit.getDescripcion() != null) {
                etDescripcion.setText(productoToEdit.getDescripcion());
            }
            switchDisponible.setChecked(productoToEdit.isDisponible());

            String catName = getCategoryName(productoToEdit.getCategoriaId());
            for (int i = 0; i < categoryNames.length; i++) {
                if (categoryNames[i].equalsIgnoreCase(catName)) {
                    spCategoria.setSelection(i);
                    break;
                }
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialogView.findViewById(R.id.btnCancelarForm).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnGuardarForm).setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            String precioStr = etPrecio.getText().toString().trim();
            String descripcion = etDescripcion.getText().toString().trim();
            String selectedCategoryName = (String) spCategoria.getSelectedItem();
            int catId = getCategoryId(selectedCategoryName);
            boolean disponible = switchDisponible.isChecked();

            if (nombre.isEmpty()) {
                etNombre.setError("Ingresa el nombre del plato");
                return;
            }
            if (precioStr.isEmpty()) {
                etPrecio.setError("Ingresa el precio");
                return;
            }

            double precio;
            try {
                precio = Double.parseDouble(precioStr);
                if (precio <= 0) {
                    etPrecio.setError("El precio debe ser mayor a 0");
                    return;
                }
            } catch (NumberFormatException e) {
                etPrecio.setError("Precio inválido");
                return;
            }

            int sucursalId = repository.getSessionManager().getSucursalId();
            int thumbRes = getThumbDrawableForCat(catId);

            if (isEditing) {
                productoToEdit.setNombre(nombre);
                productoToEdit.setCategoriaId(catId);
                productoToEdit.setPrecio(precio);
                productoToEdit.setDescripcion(descripcion);
                productoToEdit.setEmoji(selectedEmoji[0]);
                productoToEdit.setDisponible(disponible);

                repository.updateProducto(productoToEdit, new PosRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(GestionProductosActivity.this, "Plato actualizado con éxito", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(GestionProductosActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                ProductoEntity nuevo = new ProductoEntity(
                        sucursalId, catId, nombre, descripcion, precio, disponible, selectedEmoji[0], thumbRes
                );

                repository.insertProducto(nuevo, new PosRepository.Callback<Long>() {
                    @Override
                    public void onSuccess(Long id) {
                        Toast.makeText(GestionProductosActivity.this, "Plato añadido al catálogo", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(GestionProductosActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }

    private int getThumbDrawableForCat(int catId) {
        switch (catId) {
            case 1: return R.drawable.bg_thumb_fried;
            case 2: return R.drawable.bg_thumb_asado;
            case 3: return R.drawable.bg_thumb_combo;
            case 4: return R.drawable.bg_thumb_bebida;
            default: return R.drawable.bg_thumb_fried;
        }
    }
}
