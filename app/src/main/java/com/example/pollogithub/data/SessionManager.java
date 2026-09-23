package com.example.pollogithub.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "brasa_pos_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_TURNO_ID = "turno_id";
    private static final String KEY_SUCURSAL_ID = "sucursal_id";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(int userId, String userName, String userRole, int turnoId, int sucursalId) {
        prefs.edit()
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USER_NAME, userName)
                .putString(KEY_USER_ROLE, userRole)
                .putInt(KEY_TURNO_ID, turnoId)
                .putInt(KEY_SUCURSAL_ID, sucursalId)
                .apply();
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, 1);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "Administrador");
    }

    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, "admin");
    }

    public int getTurnoId() {
        return prefs.getInt(KEY_TURNO_ID, 1);
    }

    public void setTurnoId(int turnoId) {
        prefs.edit().putInt(KEY_TURNO_ID, turnoId).apply();
    }

    public int getSucursalId() {
        return prefs.getInt(KEY_SUCURSAL_ID, 1);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
