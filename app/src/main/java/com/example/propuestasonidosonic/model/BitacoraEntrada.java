package com.example.propuestasonidosonic.model;

import java.util.Objects;
import java.util.UUID;

public class BitacoraEntrada {
    public static final String TIPO_FALLA = "FALLA";
    public static final String TIPO_MANTENIMIENTO = "MANTENIMIENTO";

    private String id;
    private String equipoId;
    private long fecha;
    private String tipo;
    private String descripcion;
    private double costo;

    public BitacoraEntrada() {
        this.id = UUID.randomUUID().toString();
        this.fecha = System.currentTimeMillis();
        this.tipo = TIPO_MANTENIMIENTO;
    }

    public BitacoraEntrada(String id, String equipoId, long fecha, String tipo, String descripcion, double costo) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.equipoId = equipoId;
        this.fecha = fecha;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.costo = costo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEquipoId() {
        return equipoId;
    }

    public void setEquipoId(String equipoId) {
        this.equipoId = equipoId;
    }

    public long getFecha() {
        return fecha;
    }

    public void setFecha(long fecha) {
        this.fecha = fecha;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getCosto() {
        return costo;
    }

    public void setCosto(double costo) {
        this.costo = costo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BitacoraEntrada that = (BitacoraEntrada) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
