package com.example.pollogithub;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Pruebas Unitarias Locales: ExampleUnitTest
 * 
 * Capa de Aseguramiento de Calidad (QA) / Pruebas de Software
 * Ejecución: JVM Local (Host Machine)
 * 
 * Verifica el comportamiento funcional aislado de algoritmos y componentes
 * de lógica pura sin dependencia del framework de Android.
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Ciclo de Pruebas Unitarias (JUnit 4): Validación de aserciones lógicas deterministas.
 * - Eficiencia en CI/CD: Pruebas ultrarrápidas al no requerir emulador ni dispositivo físico.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
public class ExampleUnitTest {

    /**
     * Prueba básica de aserción aritmética para verificación del entorno de testing JUnit.
     */
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }
}