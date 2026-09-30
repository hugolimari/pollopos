package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para la tabla 'usuarios'.
 */
@Entity(tableName = "usuarios")
public class UsuarioEntity {

    /**
     * Identificador unívoco del operador en la base de datos (Clave Primaria).
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    /**
     * Clave foránea que asigna al usuario a una sede física (tabla 'sucursales').
     */
    private int sucursalId;

    /**
     * Clave foránea que determina el perfil y privilegios asignados (tabla 'roles').
     */
    private int rolId;

    /**
     * Nombre y apellidos del colaborador para visualización en comprobantes y UI.
     */
    private String nombreCompleto;

    /**
     * Identificador alfanumérico único para inicio de sesión formal.
     */
    private String nombreUsuario;

    /**
     * Credencial secreta de autenticación alfanumérica.
     */
    private String password;

    /**
     * Código numérico simplificado (PIN) para desbloqueo expedito de terminales de punto de venta.
     */
    private String pinRapido;

    /**
     * Indicador booleano de vigencia del usuario (true = habilitado, false = suspendido/baja).
     */
    private boolean activo;

    /**
     * Constructor con todos los atributos requeridos para la inicialización y persistencia del operador.
     */
    public UsuarioEntity(int sucursalId, int rolId, String nombreCompleto, String nombreUsuario, String password, String pinRapido, boolean activo) {
        this.sucursalId = sucursalId;
        this.rolId = rolId;
        this.nombreCompleto = nombreCompleto;
        this.nombreUsuario = nombreUsuario;
        this.password = password;
        this.pinRapido = pinRapido;
        this.activo = activo;
    }

    // ==========================================
    // MÉTODOS DE ACCESO (GETTERS Y SETTERS)
    // ==========================================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSucursalId() { return sucursalId; }
    public void setSucursalId(int sucursalId) { this.sucursalId = sucursalId; }

    public int getRolId() { return rolId; }
    public void setRolId(int rolId) { this.rolId = rolId; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPinRapido() { return pinRapido; }
    public void setPinRapido(String pinRapido) { this.pinRapido = pinRapido; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
