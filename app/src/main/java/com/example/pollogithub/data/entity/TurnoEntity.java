package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad de Persistencia: TurnoEntity
 * 
 * Capa de Datos / Control de Caja y Arqueos Operativos (Room ORM)
 * Tabla: "turnos"
 * 
 * Modela el ciclo de vida del turno de caja (apertura, operación continua y cierre contable).
 * Constituye el núcleo de la conciliación financiera diaria, registrando el fondo inicial
 * de cambio, las marcas de tiempo de operación, los cálculos teóricos del sistema frente
 * al arqueo físico ciego realizado por el cajero, y las eventuales discrepancias (faltantes o sobrantes).
 * 
 * Conceptos de Ingeniería aplicados:
 * - Ciclo de Vida de Transacción Contable: Transición de estado de "abierto" a "cerrado".
 * - Control de Integridad y Arqueo Ciego: Comparación entre efectivo esperado (fondo inicial + ventas en efectivo)
 *   y el conteo físico real (efectivo contado).
 * - Fórmulas Matemáticas de Conciliación: diferencia = efectivoContado - efectivoEsperado.
 *   (diferencia < 0: faltante de caja; diferencia > 0: sobrante de caja).
 * - Trazabilidad de Auditoría: Registro de operador (usuarioId) y sede (sucursalId).
 */
@Entity(tableName = "turnos")
public class TurnoEntity {

    /**
     * Identificador primario del turno operativo (Clave Primaria).
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    /**
     * Clave foránea que referencia a la sucursal donde se opera la gaveta de dinero.
     */
    private int sucursalId;

    /**
     * Clave foránea del cajero responsable de la custodia de los fondos durante este turno.
     */
    private int usuarioId;

    /**
     * Monto inicial asignado en caja chica para dar cambio al inicio del turno.
     */
    private double fondoInicial;

    /**
     * Marca de tiempo (milisegundos Unix Epoch) de apertura formal del turno.
     */
    private long abiertoEn;

    /**
     * Marca de tiempo de culminación y arqueo definitivo de la caja (nulo si permanece abierto).
     */
    private Long cerradoEn;

    /**
     * Total teórico calculado por el sistema: fondoInicial + sumatoria(pagos en efectivo).
     */
    private Double efectivoEsperado;

    /**
     * Monto físico real contabilizado manualmente por el cajero al finalizar su jornada.
     */
    private Double efectivoContado;

    /**
     * Saldo diferencial resultante del arqueo: diferencia = efectivoContado - efectivoEsperado.
     */
    private Double diferencia;

    /**
     * Estado del ciclo de vida del turno: "abierto", "cerrado".
     */
    private String estado;

    /**
     * Constructor completo para inicialización y persistencia de estados de turno.
     */
    public TurnoEntity(int sucursalId, int usuarioId, double fondoInicial, long abiertoEn, Long cerradoEn, Double efectivoEsperado, Double efectivoContado, Double diferencia, String estado) {
        this.sucursalId = sucursalId;
        this.usuarioId = usuarioId;
        this.fondoInicial = fondoInicial;
        this.abiertoEn = abiertoEn;
        this.cerradoEn = cerradoEn;
        this.efectivoEsperado = efectivoEsperado;
        this.efectivoContado = efectivoContado;
        this.diferencia = diferencia;
        this.estado = estado;
    }

    // ==========================================
    // MÉTODOS DE ACCESO (GETTERS Y SETTERS)
    // ==========================================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSucursalId() { return sucursalId; }
    public void setSucursalId(int sucursalId) { this.sucursalId = sucursalId; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public double getFondoInicial() { return fondoInicial; }
    public void setFondoInicial(double fondoInicial) { this.fondoInicial = fondoInicial; }

    public long getAbiertoEn() { return abiertoEn; }
    public void setAbiertoEn(long abiertoEn) { this.abiertoEn = abiertoEn; }

    public Long getCerradoEn() { return cerradoEn; }
    public void setCerradoEn(Long cerradoEn) { this.cerradoEn = cerradoEn; }

    public Double getEfectivoEsperado() { return efectivoEsperado; }
    public void setEfectivoEsperado(Double efectivoEsperado) { this.efectivoEsperado = efectivoEsperado; }

    public Double getEfectivoContado() { return efectivoContado; }
    public void setEfectivoContado(Double efectivoContado) { this.efectivoContado = efectivoContado; }

    public Double getDiferencia() { return diferencia; }
    public void setDiferencia(Double diferencia) { this.diferencia = diferencia; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
