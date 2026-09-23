package com.example.pollogithub.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gestor de Sesión y Preferencias: SessionManager
 * 
 * Capa de Datos / Infraestructura de Almacenamiento Ligero (SharedPreferences)
 * 
 * Encapsula la gestión de persistencia de estado de sesión de usuario y contexto operativo.
 * Administra tokens de identidad, identificador del cajero autenticado, rol RBAC,
 * turno de caja activo e identificador de sucursal asignada en almacenamiento local clave-valor.
 * 
 * Conceptos de Ingeniería aplicados:
 * - Patrón Facade / Wrapper: Oculta la complejidad de la API de SharedPreferences proporcionando
 *   una interfaz fuertemente tipada a la capa de presentación y repositorio.
 * - Concurrencia y Persistencia Asíncrona: Utilización de 'apply()' en lugar de 'commit()'
 *   para garantizar escrituras no bloqueantes en memoria RAM y persistencia diferida a disco.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
public class SessionManager {

    /**
     * Nombre del archivo XML de preferencias compartidas en el sandbox de la aplicación.
     */
    private static final String PREF_NAME = "brasa_pos_session";

    // Claves unívocas para el almacenamiento clave-valor
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_TURNO_ID = "turno_id";
    private static final String KEY_SUCURSAL_ID = "sucursal_id";

    private final SharedPreferences prefs;

    /**
     * Constructor que inicializa el acceso a SharedPreferences en modo privado (MODE_PRIVATE)
     * para asegurar aislamiento entre aplicaciones del sistema operativo Android.
     * 
     * @param context Contexto de la aplicación.
     */
    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Persiste de forma atómica y asíncrona la sesión completa del usuario autenticado.
     * 
     * @param userId     Identificador primario del operador.
     * @param userName   Nombre completo para despliegue visual.
     * @param userRole   Rol de autorización asignado ("admin", "cajero").
     * @param turnoId    Identificador del turno de caja activo (0 si requiere apertura).
     * @param sucursalId Identificador de la sucursal operativa.
     */
    public void saveSession(int userId, String userName, String userRole, int turnoId, int sucursalId) {
        prefs.edit()
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USER_NAME, userName)
                .putString(KEY_USER_ROLE, userRole)
                .putInt(KEY_TURNO_ID, turnoId)
                .putInt(KEY_SUCURSAL_ID, sucursalId)
                .apply();
    }

    /**
     * Recupera el identificador del usuario autenticado actualmente.
     * 
     * @return Entero con el ID del usuario o valor predeterminado (1).
     */
    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, 1);
    }

    /**
     * Recupera el nombre de pantalla del usuario activo.
     * 
     * @return Cadena con el nombre o "Administrador" por defecto.
     */
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "Administrador");
    }

    /**
     * Recupera el rol de autorización del operador para validaciones de privilegios en UI.
     * 
     * @return Cadena con el rol ("admin", "cajero").
     */
    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, "admin");
    }

    /**
     * Obtiene el identificador del turno de caja vigente.
     * 
     * @return Entero con el ID del turno activo.
     */
    public int getTurnoId() {
        return prefs.getInt(KEY_TURNO_ID, 1);
    }

    /**
     * Actualiza puntualmente el identificador del turno tras una apertura o cierre de caja.
     * 
     * @param turnoId Nuevo ID de turno asignado o 0 para indicar caja cerrada.
     */
    public void setTurnoId(int turnoId) {
        prefs.edit().putInt(KEY_TURNO_ID, turnoId).apply();
    }

    /**
     * Obtiene la sucursal a la que pertenece la sesión activa.
     * 
     * @return Entero con el ID de la sucursal.
     */
    public int getSucursalId() {
        return prefs.getInt(KEY_SUCURSAL_ID, 1);
    }

    /**
     * Revoca y elimina las credenciales persistidas, restableciendo el estado previo al login.
     */
    public void clear() {
        prefs.edit().clear().apply();
    }
}
