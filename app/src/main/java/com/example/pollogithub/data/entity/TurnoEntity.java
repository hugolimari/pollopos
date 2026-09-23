package com.example.pollogithub.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "turnos")
public class TurnoEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int sucursalId;
    private int usuarioId;
    private double fondoInicial;
    private long abiertoEn;
    private Long cerradoEn;
    private Double efectivoEsperado;
    private Double efectivoContado;
    private Double diferencia;
    private String estado; // "abierto", "cerrado"

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
