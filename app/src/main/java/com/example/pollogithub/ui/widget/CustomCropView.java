package com.example.pollogithub.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Vista interactiva personalizada para recorte de fotos con proporción fija (Crop View).
 * Permite manipulación multitáctil (Pinch to Zoom, Arrastre/Panorámica),
 * preselección de aspecto (4:3 estándar para tarjeta de menú, 1:1, etc.),
 * guías visuales de tercios y extracción optimizada del Bitmap resultante.
 */
public class CustomCropView extends View {

    private Bitmap sourceBitmap;
    private final Matrix matrix = new Matrix();
    private final RectF cropRect = new RectF();
    private float targetAspectRatio = 4.0f / 3.0f; // 4:3 predeterminado para el menú

    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint overlayPaint = new Paint();
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private ScaleGestureDetector scaleGestureDetector;
    private float lastTouchX;
    private float lastTouchY;
    private boolean isDragging = false;

    public CustomCropView(Context context) {
        super(context);
        init(context);
    }

    public CustomCropView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CustomCropView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Fondo translúcido oscuro alrededor del marco de recorte
        overlayPaint.setColor(Color.parseColor("#B3000000"));
        overlayPaint.setStyle(Paint.Style.FILL);

        // Borde blanco del recuadro de recorte
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(1.5f));

        // Líneas tenues de regla de tercios
        gridPaint.setColor(Color.parseColor("#55FFFFFF"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dpToPx(1f));

        // Esquinas resaltadas
        cornerPaint.setColor(Color.WHITE);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(dpToPx(3.5f));

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (sourceBitmap == null) return false;
                float factor = detector.getScaleFactor();
                matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
                clampMatrixToBounds();
                invalidate();
                return true;
            }
        });
    }

    public void setBitmap(Bitmap bitmap) {
        this.sourceBitmap = bitmap;
        if (getWidth() > 0 && getHeight() > 0) {
            setupInitialCrop();
        }
        invalidate();
    }

    public void setAspectRatio(float ratio) {
        this.targetAspectRatio = ratio;
        if (getWidth() > 0 && getHeight() > 0) {
            calculateCropRect();
            clampMatrixToBounds();
            invalidate();
        }
    }

    public float getAspectRatio() {
        return targetAspectRatio;
    }

    public void rotate90Clockwise() {
        if (sourceBitmap == null) return;
        Matrix m = new Matrix();
        m.postRotate(90);
        Bitmap rotated = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), m, true);
        if (rotated != sourceBitmap) {
            sourceBitmap.recycle();
            sourceBitmap = rotated;
        }
        setupInitialCrop();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateCropRect();
        if (sourceBitmap != null) {
            setupInitialCrop();
        }
    }

    private void calculateCropRect() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float padding = dpToPx(24);
        float availWidth = width - 2 * padding;
        float availHeight = height - 2 * padding;

        float cropWidth = availWidth;
        float cropHeight = cropWidth / targetAspectRatio;

        if (cropHeight > availHeight) {
            cropHeight = availHeight;
            cropWidth = cropHeight * targetAspectRatio;
        }

        float left = (width - cropWidth) / 2f;
        float top = (height - cropHeight) / 2f;
        cropRect.set(left, top, left + cropWidth, top + cropHeight);
    }

    private void setupInitialCrop() {
        if (sourceBitmap == null || cropRect.isEmpty()) return;

        matrix.reset();

        float bmpW = sourceBitmap.getWidth();
        float bmpH = sourceBitmap.getHeight();

        // Escalar la imagen de tal modo que cubra completamente el rectángulo de recorte
        float scaleX = cropRect.width() / bmpW;
        float scaleY = cropRect.height() / bmpH;
        float scale = Math.max(scaleX, scaleY);

        matrix.postScale(scale, scale);

        // Centrar imagen respecto al cropRect
        float currentW = bmpW * scale;
        float currentH = bmpH * scale;
        float dx = cropRect.centerX() - (currentW / 2f);
        float dy = cropRect.centerY() - (currentH / 2f);

        matrix.postTranslate(dx, dy);
        clampMatrixToBounds();
    }

    private void clampMatrixToBounds() {
        if (sourceBitmap == null || cropRect.isEmpty()) return;

        RectF currentBmpRect = new RectF(0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight());
        matrix.mapRect(currentBmpRect);

        // Asegurar que el tamaño mapeado no sea inferior al área de recorte
        if (currentBmpRect.width() < cropRect.width()) {
            float s = cropRect.width() / currentBmpRect.width();
            matrix.postScale(s, s, cropRect.centerX(), cropRect.centerY());
            currentBmpRect.set(0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight());
            matrix.mapRect(currentBmpRect);
        }

        if (currentBmpRect.height() < cropRect.height()) {
            float s = cropRect.height() / currentBmpRect.height();
            matrix.postScale(s, s, cropRect.centerX(), cropRect.centerY());
            currentBmpRect.set(0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight());
            matrix.mapRect(currentBmpRect);
        }

        // Restringir desplazamiento para evitar espacios vacíos en el marco
        float deltaX = 0;
        float deltaY = 0;

        if (currentBmpRect.left > cropRect.left) {
            deltaX = cropRect.left - currentBmpRect.left;
        } else if (currentBmpRect.right < cropRect.right) {
            deltaX = cropRect.right - currentBmpRect.right;
        }

        if (currentBmpRect.top > cropRect.top) {
            deltaY = cropRect.top - currentBmpRect.top;
        } else if (currentBmpRect.bottom < cropRect.bottom) {
            deltaY = cropRect.bottom - currentBmpRect.bottom;
        }

        if (deltaX != 0 || deltaY != 0) {
            matrix.postTranslate(deltaX, deltaY);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (sourceBitmap == null) return super.onTouchEvent(event);

        scaleGestureDetector.onTouchEvent(event);

        if (scaleGestureDetector.isInProgress()) {
            isDragging = false;
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isDragging = true;
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    matrix.postTranslate(dx, dy);
                    clampMatrixToBounds();
                    invalidate();
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Dibujar imagen escalada y desplazada
        if (sourceBitmap != null && !sourceBitmap.isRecycled()) {
            canvas.drawBitmap(sourceBitmap, matrix, bitmapPaint);
        }

        int width = getWidth();
        int height = getHeight();

        // 2. Dibujar overlay oscuro exterior
        // Arriba
        canvas.drawRect(0, 0, width, cropRect.top, overlayPaint);
        // Abajo
        canvas.drawRect(0, cropRect.bottom, width, height, overlayPaint);
        // Izquierda
        canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint);
        // Derecha
        canvas.drawRect(cropRect.right, cropRect.top, width, cropRect.bottom, overlayPaint);

        // 3. Dibujar marco de recorte
        canvas.drawRect(cropRect, borderPaint);

        // 4. Guías de la regla de tercios
        float thirdW = cropRect.width() / 3f;
        float thirdH = cropRect.height() / 3f;

        canvas.drawLine(cropRect.left + thirdW, cropRect.top, cropRect.left + thirdW, cropRect.bottom, gridPaint);
        canvas.drawLine(cropRect.left + 2 * thirdW, cropRect.top, cropRect.left + 2 * thirdW, cropRect.bottom, gridPaint);
        canvas.drawLine(cropRect.left, cropRect.top + thirdH, cropRect.right, cropRect.top + thirdH, gridPaint);
        canvas.drawLine(cropRect.left, cropRect.top + 2 * thirdH, cropRect.right, cropRect.top + 2 * thirdH, gridPaint);

        // 5. Esquinas visuales decorativas (L-handles)
        float cornerLength = dpToPx(18);
        // Esquina superior izquierda
        canvas.drawLine(cropRect.left - dpToPx(1), cropRect.top, cropRect.left + cornerLength, cropRect.top, cornerPaint);
        canvas.drawLine(cropRect.left, cropRect.top - dpToPx(1), cropRect.left, cropRect.top + cornerLength, cornerPaint);
        // Esquina superior derecha
        canvas.drawLine(cropRect.right + dpToPx(1), cropRect.top, cropRect.right - cornerLength, cropRect.top, cornerPaint);
        canvas.drawLine(cropRect.right, cropRect.top - dpToPx(1), cropRect.right, cropRect.top + cornerLength, cornerPaint);
        // Esquina inferior izquierda
        canvas.drawLine(cropRect.left - dpToPx(1), cropRect.bottom, cropRect.left + cornerLength, cropRect.bottom, cornerPaint);
        canvas.drawLine(cropRect.left, cropRect.bottom + dpToPx(1), cropRect.left, cropRect.bottom - cornerLength, cornerPaint);
        // Esquina inferior derecha
        canvas.drawLine(cropRect.right + dpToPx(1), cropRect.bottom, cropRect.right - cornerLength, cropRect.bottom, cornerPaint);
        canvas.drawLine(cropRect.right, cropRect.bottom + dpToPx(1), cropRect.right, cropRect.bottom - cornerLength, cornerPaint);
    }

    /**
     * Extrae el sub-bitmap exactamente comprendido dentro del área del marco de recorte.
     */
    public Bitmap getCroppedBitmap() {
        if (sourceBitmap == null || sourceBitmap.isRecycled() || cropRect.isEmpty()) return null;

        Matrix inverse = new Matrix();
        if (!matrix.invert(inverse)) return null;

        RectF mappedCrop = new RectF();
        inverse.mapRect(mappedCrop, cropRect);

        // Acotar estrictamente dentro de los límites del bitmap original
        int left = Math.max(0, Math.round(mappedCrop.left));
        int top = Math.max(0, Math.round(mappedCrop.top));
        int right = Math.min(sourceBitmap.getWidth(), Math.round(mappedCrop.right));
        int bottom = Math.min(sourceBitmap.getHeight(), Math.round(mappedCrop.bottom));

        int width = right - left;
        int height = bottom - top;

        if (width <= 0 || height <= 0) return null;

        return Bitmap.createBitmap(sourceBitmap, left, top, width, height);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
