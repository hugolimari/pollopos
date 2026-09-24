package com.example.pollogithub.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gestor de Impresión Térmica: ThermalPrinterManager
 * 
 * Capa de Infraestructura / Gestión de Periféricos y Hardware POS
 * 
 * Controla la conectividad y envío de flujos binarios ESC/POS a impresoras
 * térmicas mediante Bluetooth SPP (Serial Port Profile).
 * Cuenta con contingencia (Fallback) transparente hacia el PrintManager nativo de Android
 * para impresoras de red, WiFi, USB o generación de comprobantes PDF.
 */
public class ThermalPrinterManager {

    private static final String PREFS_NAME = "printer_prefs";
    private static final String KEY_PRINTER_MAC = "selected_printer_mac";
    private static final String KEY_IS_80MM = "printer_is_80mm";

    // UUID estándar para perfil serie Bluetooth (SPP)
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final Context context;
    private final SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface PrintCallback {
        void onSuccess();
        void onError(String error);
    }

    public ThermalPrinterManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean is80mm() {
        return prefs.getBoolean(KEY_IS_80MM, false);
    }

    public void set80mm(boolean is80mm) {
        prefs.edit().putBoolean(KEY_IS_80MM, is80mm).apply();
    }

    public String getSavedPrinterMac() {
        return prefs.getString(KEY_PRINTER_MAC, null);
    }

    public void savePrinterMac(String mac) {
        prefs.edit().putString(KEY_PRINTER_MAC, mac).apply();
    }

    /**
     * Retorna la lista de dispositivos Bluetooth emparejados.
     */
    @SuppressLint("MissingPermission")
    public List<BluetoothDevice> getPairedDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null && adapter.isEnabled()) {
                Set<BluetoothDevice> bonded = adapter.getBondedDevices();
                if (bonded != null) {
                    devices.addAll(bonded);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return devices;
    }

    /**
     * Envía comandos binarios ESC/POS a la impresora Bluetooth seleccionada.
     */
    public void printEscPosBytes(byte[] data, PrintCallback callback) {
        String mac = getSavedPrinterMac();
        if (mac == null || mac.isEmpty()) {
            if (callback != null) callback.onError("No hay ninguna impresora Bluetooth configurada.");
            return;
        }

        executor.execute(() -> {
            BluetoothSocket socket = null;
            OutputStream out = null;
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter == null || !adapter.isEnabled()) {
                    postError(callback, "El Bluetooth está desactivado en el dispositivo.");
                    return;
                }

                BluetoothDevice device = adapter.getRemoteDevice(mac);
                if (device == null) {
                    postError(callback, "No se encontró la impresora vinculada.");
                    return;
                }

                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                socket.connect();

                out = socket.getOutputStream();
                out.write(data);
                out.flush();

                // Pausa de 300ms para asegurar transmisión de buffer antes de cerrar
                Thread.sleep(300);

                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess();
                });

            } catch (Exception e) {
                postError(callback, "Error al imprimir: " + e.getMessage());
            } finally {
                try {
                    if (out != null) out.close();
                    if (socket != null) socket.close();
                } catch (Exception ignored) {}
            }
        });
    }

    /**
     * Muestra un diálogo interactivo para seleccionar la impresora o cambiar el tamaño de papel (58mm/80mm).
     */
    public void showPrinterSelectionDialog(Activity activity, Runnable onPrinterConfigured) {
        List<BluetoothDevice> devices = getPairedDevices();
        if (devices.isEmpty()) {
            new AlertDialog.Builder(activity)
                    .setTitle("Impresora Térmica")
                    .setMessage("No se encontraron impresoras Bluetooth emparejadas en este dispositivo.\n\nPor favor empareja tu impresora térmica en los Ajustes de Bluetooth de Android.")
                    .setPositiveButton("Entendido", null)
                    .show();
            return;
        }

        String[] names = new String[devices.size()];
        String currentMac = getSavedPrinterMac();
        int selectedIndex = -1;

        for (int i = 0; i < devices.size(); i++) {
            @SuppressLint("MissingPermission")
            String name = devices.get(i).getName();
            String address = devices.get(i).getAddress();
            names[i] = (name != null ? name : "Dispositivo") + "\n(" + address + ")";
            if (address.equalsIgnoreCase(currentMac)) {
                selectedIndex = i;
            }
        }

        new AlertDialog.Builder(activity)
                .setTitle("Seleccionar Impresora Térmica")
                .setSingleChoiceItems(names, selectedIndex, (dialog, which) -> {
                    BluetoothDevice chosen = devices.get(which);
                    savePrinterMac(chosen.getAddress());
                    Toast.makeText(activity, "Impresora seleccionada: " + chosen.getName(), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    if (onPrinterConfigured != null) onPrinterConfigured.run();
                })
                .setNeutralButton("Tamaño: " + (is80mm() ? "80mm" : "58mm"), (dialog, which) -> {
                    set80mm(!is80mm());
                    Toast.makeText(activity, "Formato cambiado a " + (is80mm() ? "80mm" : "58mm"), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Contingencia (Fallback): Imprime el recibo usando el framework nativo de impresión de Android (PrintManager).
     * Funciona con cualquier impresora WiFi, USB o servicio de impresión instalado, e incluye exportación a PDF.
     */
    public void printViaSystemPrintManager(Activity activity, String htmlContent, String jobName) {
        activity.runOnUiThread(() -> {
            WebView webView = new WebView(activity);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    PrintManager printManager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
                    PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter(jobName);
                    PrintAttributes.Builder builder = new PrintAttributes.Builder();
                    builder.setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME);
                    builder.setMediaSize(PrintAttributes.MediaSize.ISO_A6);
                    if (printManager != null) {
                        printManager.print(jobName, adapter, builder.build());
                    }
                }
            });
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
        });
    }

    private void postError(PrintCallback callback, String error) {
        mainHandler.post(() -> {
            if (callback != null) callback.onError(error);
        });
    }
}
