package com.example.pollogithub.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageUtils {

    private static final String PRODUCT_IMAGE_DIR = "productos";

    /**
     * Procesa una imagen seleccionada de la galería, la redimensiona y la comprime en formato WebP
     * en el almacenamiento privado de la app (filesDir/productos/).
     *
     * @param context Contexto de la aplicación
     * @param imageUri Uri de la imagen seleccionada de la galería
     * @param maxDimension Dimensión máxima (ancho o alto) para optimizar memoria (ej. 500px)
     * @return Ruta absoluta del archivo .webp guardado, o null si ocurrió un error
     */
    public static String saveGalleryImageAsWebp(Context context, Uri imageUri, int maxDimension) {
        if (context == null || imageUri == null) return null;

        try {
            InputStream is = context.getContentResolver().openInputStream(imageUri);
            if (is == null) return null;

            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();

            if (bitmap == null) return null;

            // Escalar manteniendo proporción
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float ratio = (float) width / (float) height;

            int targetWidth = width;
            int targetHeight = height;

            if (width > maxDimension || height > maxDimension) {
                if (ratio > 1) {
                    targetWidth = maxDimension;
                    targetHeight = Math.round(maxDimension / ratio);
                } else {
                    targetHeight = maxDimension;
                    targetWidth = Math.round(maxDimension * ratio);
                }
            }

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
            if (scaledBitmap != bitmap) {
                bitmap.recycle();
            }

            File dir = new File(context.getFilesDir(), PRODUCT_IMAGE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filename = "prod_" + System.currentTimeMillis() + ".webp";
            File targetFile = new File(dir, filename);

            FileOutputStream fos = new FileOutputStream(targetFile);
            scaledBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, fos);
            fos.flush();
            fos.close();

            scaledBitmap.recycle();

            return targetFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Guarda un Bitmap en almacenamiento local en formato WebP con dimensiones optimizadas.
     */
    public static String saveBitmapAsWebp(Context context, Bitmap bitmap, int maxDimension) {
        if (context == null || bitmap == null) return null;

        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float ratio = (float) width / (float) height;

            int targetWidth = width;
            int targetHeight = height;

            if (width > maxDimension || height > maxDimension) {
                if (ratio > 1) {
                    targetWidth = maxDimension;
                    targetHeight = Math.round(maxDimension / ratio);
                } else {
                    targetHeight = maxDimension;
                    targetWidth = Math.round(maxDimension * ratio);
                }
            }

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);

            File dir = new File(context.getFilesDir(), PRODUCT_IMAGE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filename = "prod_" + System.currentTimeMillis() + ".webp";
            File targetFile = new File(dir, filename);

            FileOutputStream fos = new FileOutputStream(targetFile);
            scaledBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 88, fos);
            fos.flush();
            fos.close();

            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle();
            }

            return targetFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decodifica un Bitmap de forma segura desde un Uri, optimizando la memoria y respetando la orientación EXIF.
     */
    public static Bitmap loadBitmapFromUriWithExif(Context context, Uri uri, int maxDimension) {
        if (context == null || uri == null) return null;

        try {
            // 1. Obtener dimensiones sin cargar a memoria
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            InputStream isBounds = context.getContentResolver().openInputStream(uri);
            if (isBounds == null) return null;
            BitmapFactory.decodeStream(isBounds, null, boundsOptions);
            isBounds.close();

            int sampleSize = 1;
            while ((boundsOptions.outWidth / sampleSize) > maxDimension || (boundsOptions.outHeight / sampleSize) > maxDimension) {
                sampleSize *= 2;
            }

            // 2. Decodificar con sampleSize adecuado
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = sampleSize;
            InputStream isDecode = context.getContentResolver().openInputStream(uri);
            if (isDecode == null) return null;
            Bitmap bitmap = BitmapFactory.decodeStream(isDecode, null, decodeOptions);
            isDecode.close();

            if (bitmap == null) return null;

            // 3. Inspeccionar metadatos EXIF para rotación
            try {
                InputStream exifStream = context.getContentResolver().openInputStream(uri);
                if (exifStream != null) {
                    ExifInterface exif = new ExifInterface(exifStream);
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    exifStream.close();

                    int rotation = 0;
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;
                    else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;
                    else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;

                    if (rotation != 0) {
                        Matrix matrix = new Matrix();
                        matrix.postRotate(rotation);
                        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        if (rotated != bitmap) {
                            bitmap.recycle();
                            bitmap = rotated;
                        }
                    }
                }
            } catch (Exception ignored) {}

            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Carga un Bitmap desde una ruta de archivo local.
     */
    public static Bitmap loadBitmapFromPath(String localPath) {
        if (localPath == null || localPath.trim().isEmpty()) return null;

        try {
            File file = new File(localPath);
            if (!file.exists()) return null;

            return BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Muestra la imagen local en un ImageView. Si no existe, muestra el emoji o ilustración de fallback.
     */
    public static void displayProductImage(ImageView imageView, String localPath, View fallbackEmojiView) {
        if (imageView == null) return;

        if (localPath != null && !localPath.trim().isEmpty()) {
            File file = new File(localPath);
            if (file.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bmp != null) {
                    imageView.setImageBitmap(bmp);
                    imageView.setVisibility(View.VISIBLE);
                    if (fallbackEmojiView != null) {
                        fallbackEmojiView.setVisibility(View.GONE);
                    }
                    return;
                }
            }
        }

        // Si no hay imagen o falló, mostrar emoji o fallback
        imageView.setVisibility(View.GONE);
        if (fallbackEmojiView != null) {
            fallbackEmojiView.setVisibility(View.VISIBLE);
        }
    }
}
