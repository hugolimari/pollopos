package com.example.pollogithub;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pollogithub.ui.widget.CustomCropView;
import com.example.pollogithub.util.ImageUtils;

/**
 * Controlador de Vista: CropImageActivity
 * 
 * Permite recortar fotos tomadas con la cámara o seleccionadas de la galería
 * con proporciones predeterminadas optimizadas para los platos y tarjetas del menú (4:3 o 1:1).
 * Implementa rotación rápida a 90° y compresión de alta fidelidad en WebP local.
 */
public class CropImageActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_CROPPED_PATH = "extra_cropped_path";

    private CustomCropView customCropView;
    private TextView chipRatio43;
    private TextView chipRatio11;
    private TextView tvCropSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_crop_image);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainCropLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        customCropView = findViewById(R.id.customCropView);
        chipRatio43 = findViewById(R.id.chipRatio43);
        chipRatio11 = findViewById(R.id.chipRatio11);
        tvCropSubtitle = findViewById(R.id.tvCropSubtitle);

        findViewById(R.id.btnCancelCrop).setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        Uri imageUri = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            imageUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI, Uri.class);
        } else {
            imageUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);
        }
        if (imageUri == null && getIntent().getData() != null) {
            imageUri = getIntent().getData();
        }

        if (imageUri == null) {
            Toast.makeText(this, "No se recibió ninguna imagen para recortar", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Carga eficiente con decodificación de muestra y corrección EXIF
        Bitmap bitmap = ImageUtils.loadBitmapFromUriWithExif(this, imageUri, 1800);
        if (bitmap == null) {
            Toast.makeText(this, "Error al cargar la imagen para recorte", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        customCropView.setBitmap(bitmap);

        // Control de proporción 4:3 (Preseleccionada - Recomendada para el menú)
        chipRatio43.setOnClickListener(v -> {
            customCropView.setAspectRatio(4.0f / 3.0f);
            chipRatio43.setBackgroundResource(R.drawable.bg_chip_selected);
            chipRatio43.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));

            chipRatio11.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipRatio11.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.char_700));

            tvCropSubtitle.setText("Proporción recomendada: 4:3");
        });

        // Control de proporción 1:1 (Cuadrada)
        chipRatio11.setOnClickListener(v -> {
            customCropView.setAspectRatio(1.0f);
            chipRatio11.setBackgroundResource(R.drawable.bg_chip_selected);
            chipRatio11.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));

            chipRatio43.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipRatio43.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.char_700));

            tvCropSubtitle.setText("Proporción cuadrada: 1:1");
        });

        // Rotación a 90 grados en el sentido del reloj
        findViewById(R.id.btnRotateCrop).setOnClickListener(v -> customCropView.rotate90Clockwise());

        // Confirmar recorte y procesar almacenamiento en WebP local
        findViewById(R.id.btnDoneCrop).setOnClickListener(v -> {
            Bitmap cropped = customCropView.getCroppedBitmap();
            if (cropped == null) {
                Toast.makeText(this, "No se pudo realizar el recorte", Toast.LENGTH_SHORT).show();
                return;
            }

            // Guardar optimizado a máximo 600px en formato WebP local
            String savedPath = ImageUtils.saveBitmapAsWebp(this, cropped, 600);
            cropped.recycle();

            if (savedPath != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_CROPPED_PATH, savedPath);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Error al guardar la imagen recortada", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
