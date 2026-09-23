package com.example.pollogithub;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Pruebas Instrumentadas en Dispositivo: ExampleInstrumentedTest
 * 
 * Capa de Aseguramiento de Calidad (QA) / Pruebas de Integración y Entorno
 * Ejecución: Dispositivo Físico / Emulador Android (Android Runtime - ART)
 * 
 * Valida la correcta integración de la aplicación con las APIs del sistema operativo Android
 * y el contexto de ejecución del paquete.
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Pruebas Instrumentadas (AndroidX Test Runner): Acceso al Context real de la aplicación
 *   mediante InstrumentationRegistry para validar configuraciones de empaquetado y manifiesto.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    /**
     * Valida que el identificador de paquete (ApplicationId) resuelto en tiempo de ejecución
     * coincida con el namespace canónico del proyecto ("com.example.pollogithub").
     */
    @Test
    public void useAppContext() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.pollogithub", appContext.getPackageName());
    }
}