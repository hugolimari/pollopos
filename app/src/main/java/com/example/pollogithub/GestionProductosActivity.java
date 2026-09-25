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

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.pollogithub.util.ImageUtils;

import java.io.File;

/**
 * Controlador de Vista: GestionProductosActivity (Administración de Catálogo)
 * 
 * Capa de Presentación / Módulo Administrativo y de Inventario
 * Hereda de: AppCompatActivity
 * 
 * Permite a los administradores del restaurante realizar operaciones de mantenimiento
 * sobre el catálogo de artículos: alta de nuevos platos, edición de nombres, categorías,
 * precios e íconos, así como la activación o desactivación inmediata de disponibilidad (stock).
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Algoritmo de Búsqueda y Filtrado en Memoria: Evaluación combinatoria de predicados booleanos
 *   (categoría seleccionada AND coincidencia de subcadena insensible a mayúsculas/minúsculas).
 * - Componentes Modales (AlertDialog Personalizado): Formulario emergente para captura de datos
 *   con validación estricta de precondiciones numéricas (precio > 0).
 * - Arquitectura Reactiva: Suscripción a 'getProductosLiveData()' del Repositorio para reflejar
 *   instantáneamente las mutaciones en SQLite sin requerir recarga manual de la pantalla.
 */
public class GestionProductosActivity extends AppCompatActivity {

    private PosRepository repository;
    private final List<ProductoEntity> allProductos = new ArrayList<>();
    private final List<ProductoEntity> filteredProductos = new ArrayList<>();
    private GestionProductosAdapter adapter;

    private String selectedCategory = "Todos";
    private String searchQuery = "";
    private TextView tvTotalCountSubtitle;

    private final String[] categoryNames = {"Pollo frito", "A la brasa", "Combos", "Bebidas", "Acompañamientos"};

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;
    private Uri cameraTempUri = null;

    private String currentSelectedPhotoPath = null;
    private ImageView ivCurrentDialogPhoto = null;
    private View tvCurrentDialogPlaceholder = null;
    private View btnCurrentDialogRemove = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gestion_productos);

        // Ajuste de insets de ventana para barras del sistema operativo
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainGestionProductos), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Selector de Galería -> Enrutado directo a la pantalla de recorte
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                launchCropActivity(uri);
            }
        });

        // 2. Captura con Cámara -> Enrutado directo a la pantalla de recorte
        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (Boolean.TRUE.equals(success) && cameraTempUri != null) {
                launchCropActivity(cameraTempUri);
            }
        });

        // 3. Solicitud de Permiso en Tiempo de Ejecución para Cámara
        cameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (Boolean.TRUE.equals(isGranted)) {
                startCameraCapture();
            } else {
                Toast.makeText(GestionProductosActivity.this, "Permiso de cámara no concedido. No se puede capturar la foto.", Toast.LENGTH_SHORT).show();
            }
        });

        // 4. Recepción del resultado de la pantalla de recorte (CropImageActivity)
        cropLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                String savedPath = result.getData().getStringExtra(CropImageActivity.EXTRA_CROPPED_PATH);
                if (savedPath != null) {
                    currentSelectedPhotoPath = savedPath;
                    if (ivCurrentDialogPhoto != null) {
                        Bitmap bmp = ImageUtils.loadBitmapFromPath(savedPath);
                        if (bmp != null) {
                            ivCurrentDialogPhoto.setImageBitmap(bmp);
                            ivCurrentDialogPhoto.setVisibility(View.VISIBLE);
                            if (tvCurrentDialogPlaceholder != null) tvCurrentDialogPlaceholder.setVisibility(View.GONE);
                            if (btnCurrentDialogRemove != null) btnCurrentDialogRemove.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });

        repository = PosRepository.getInstance(this);

        tvTotalCountSubtitle = findViewById(R.id.tvTotalCountSubtitle);
        findViewById(R.id.btnBackGestion).setOnClickListener(v -> finish());

        // 1. Configuración del RecyclerView para la lista de productos
        RecyclerView rv = findViewById(R.id.rvProductosGestion);
        rv.setLayoutManager(new LinearLayoutManager(this));

        // 2. Inicialización del adaptador con callbacks de interacción
        adapter = new GestionProductosAdapter(this, filteredProductos, new GestionProductosAdapter.OnProductoActionListener() {
            @Override
            public void onToggleDisponible(ProductoEntity producto, boolean disponible) {
                // Actualización asíncrona de disponibilidad en SQLite
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

        // 3. Inicialización de componentes de búsqueda y chips de filtro
        setupCategoryChips();
        setupSearch();

        // 4. Botón flotante para registrar un nuevo producto (Formulario en blanco)
        findViewById(R.id.fabAddProduct).setOnClickListener(v -> showProductoFormDialog(null));

        // 5. Suscripción reactiva al catálogo persistido en Room
        repository.getProductosLiveData().observe(this, productos -> {
            if (productos != null) {
                allProductos.clear();
                allProductos.addAll(productos);
                filterList();
            }
        });
    }

    /**
     * Configura el listener de texto para filtrado reactivo a medida que el usuario escribe.
     */
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

    /**
     * Asocia los eventos de clic a los chips visuales de categorías de comida.
     */
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

    /**
     * Algoritmo de filtrado combinatorio:
     * Aplica simultáneamente el criterio de categoría seleccionada y la subcadena de búsqueda.
     */
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

    /**
     * Despliega el diálogo modal de formulario para creación o edición de producto.
     * 
     * @param productoToEdit Entidad a modificar o null si es un nuevo registro.
     */
    private void showProductoFormDialog(ProductoEntity productoToEdit) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_producto_form, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvFormTitle);
        TextView tvSubtitle = dialogView.findViewById(R.id.tvFormSubtitle);
        EditText etNombre = dialogView.findViewById(R.id.etNombre);
        Spinner spCategoria = dialogView.findViewById(R.id.spCategoria);
        EditText etPrecio = dialogView.findViewById(R.id.etPrecio);
        EditText etDescripcion = dialogView.findViewById(R.id.etDescripcion);
        SwitchCompat switchDisponible = dialogView.findViewById(R.id.switchDisponibleForm);

        // Foto del plato (Almacenamiento Local)
        ImageView ivFormProductPhoto = dialogView.findViewById(R.id.ivFormProductPhoto);
        View ivFormPhotoPlaceholder = dialogView.findViewById(R.id.ivFormPhotoPlaceholder);
        View cardPhotoContainer = dialogView.findViewById(R.id.cardPhotoContainer);
        View btnSelectPhoto = dialogView.findViewById(R.id.btnSelectPhoto);
        View btnRemovePhoto = dialogView.findViewById(R.id.btnRemovePhoto);

        ivCurrentDialogPhoto = ivFormProductPhoto;
        tvCurrentDialogPlaceholder = ivFormPhotoPlaceholder;
        btnCurrentDialogRemove = btnRemovePhoto;
        currentSelectedPhotoPath = productoToEdit != null ? productoToEdit.getImagenLocalPath() : null;

        if (currentSelectedPhotoPath != null) {
            Bitmap bmp = ImageUtils.loadBitmapFromPath(currentSelectedPhotoPath);
            if (bmp != null) {
                ivFormProductPhoto.setImageBitmap(bmp);
                ivFormProductPhoto.setVisibility(View.VISIBLE);
                if (ivFormPhotoPlaceholder != null) ivFormPhotoPlaceholder.setVisibility(View.GONE);
                btnRemovePhoto.setVisibility(View.VISIBLE);
            }
        }

        View.OnClickListener pickPhotoListener = v -> showPhotoSourceDialog();
        btnSelectPhoto.setOnClickListener(pickPhotoListener);
        if (cardPhotoContainer != null) {
            cardPhotoContainer.setOnClickListener(pickPhotoListener);
        }

        btnRemovePhoto.setOnClickListener(v -> {
            currentSelectedPhotoPath = null;
            ivFormProductPhoto.setVisibility(View.GONE);
            if (ivFormPhotoPlaceholder != null) ivFormPhotoPlaceholder.setVisibility(View.VISIBLE);
            btnRemovePhoto.setVisibility(View.GONE);
        });

        // Selectores visuales de emoji representativo (Salvavidas Opcional y Cancelable)
        TextView optNoEmoji = dialogView.findViewById(R.id.optNoEmoji);
        TextView optChicken = dialogView.findViewById(R.id.optEmojiChicken);
        TextView optFire = dialogView.findViewById(R.id.optEmojiFire);
        TextView optDrink = dialogView.findViewById(R.id.optEmojiDrink);
        TextView optFries = dialogView.findViewById(R.id.optEmojiFries);
        TextView optSalad = dialogView.findViewById(R.id.optEmojiSalad);
        TextView optBurger = dialogView.findViewById(R.id.optEmojiBurger);

        final TextView[] emojiViews = new TextView[]{optChicken, optFire, optDrink, optFries, optSalad, optBurger};
        final String[] selectedEmoji = {productoToEdit != null ? productoToEdit.getEmoji() : null};

        Runnable updateEmojiSelection = () -> {
            boolean hasSelected = selectedEmoji[0] != null && !selectedEmoji[0].isEmpty();
            if (!hasSelected) {
                optNoEmoji.setBackgroundResource(R.drawable.bg_chip_selected);
                optNoEmoji.setTextColor(getColor(R.color.white));
            } else {
                optNoEmoji.setBackgroundResource(R.drawable.bg_chip_unselected);
                optNoEmoji.setTextColor(getColor(R.color.char_700));
            }

            for (TextView ev : emojiViews) {
                if (hasSelected && ev.getText().toString().equals(selectedEmoji[0])) {
                    ev.setBackgroundResource(R.drawable.bg_chip_selected);
                } else {
                    ev.setBackgroundResource(R.drawable.bg_chip_unselected);
                }
            }
        };

        updateEmojiSelection.run();

        optNoEmoji.setOnClickListener(v -> {
            selectedEmoji[0] = null;
            updateEmojiSelection.run();
        });

        for (TextView ev : emojiViews) {
            ev.setOnClickListener(v -> {
                String clicked = ev.getText().toString();
                if (clicked.equals(selectedEmoji[0])) {
                    // Si ya estaba seleccionado, se cancela/deselecciona
                    selectedEmoji[0] = null;
                } else {
                    selectedEmoji[0] = clicked;
                }
                updateEmojiSelection.run();
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

        // Procesamiento del formulario y validación de reglas de negocio
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
                // Caso de Modificación: actualización de entidad existente
                productoToEdit.setNombre(nombre);
                productoToEdit.setCategoriaId(catId);
                productoToEdit.setPrecio(precio);
                productoToEdit.setDescripcion(descripcion);
                productoToEdit.setEmoji(selectedEmoji[0]);
                productoToEdit.setImagenLocalPath(currentSelectedPhotoPath);
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
                // Caso de Alta: creación de nueva tupla en SQLite
                ProductoEntity nuevo = new ProductoEntity(
                        sucursalId, catId, nombre, descripcion, precio, disponible, selectedEmoji[0], thumbRes, currentSelectedPhotoPath
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

    /**
     * Lanza la pantalla de recorte de imagen (CropImageActivity) con la Uri suministrada.
     */
    private void launchCropActivity(Uri uri) {
        Intent intent = new Intent(this, CropImageActivity.class);
        intent.putExtra(CropImageActivity.EXTRA_IMAGE_URI, uri);
        cropLauncher.launch(intent);
    }

    /**
     * Inicia la captura de imagen con la cámara del dispositivo utilizando un FileProvider seguro.
     */
    private void startCameraCapture() {
        try {
            File dir = new File(getCacheDir(), "camera");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File tempFile = new File(dir, "cam_" + System.currentTimeMillis() + ".jpg");
            cameraTempUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tempFile);
            takePictureLauncher.launch(cameraTempUri);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al preparar la cámara: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Despliega el modal de selección de origen de imagen (Cámara o Galería).
     */
    private void showPhotoSourceDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_photo_source, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnOptionCamera).setOnClickListener(v -> {
            dialog.dismiss();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCameraCapture();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        dialogView.findViewById(R.id.btnOptionGallery).setOnClickListener(v -> {
            dialog.dismiss();
            galleryLauncher.launch("image/*");
        });

        dialogView.findViewById(R.id.btnCancelSource).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
