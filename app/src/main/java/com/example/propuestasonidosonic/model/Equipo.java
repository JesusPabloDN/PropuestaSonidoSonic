package com.example.propuestasonidosonic.model;

import java.util.Objects;
import java.util.UUID;

public class Equipo {
    public static final String ESTADO_DISPONIBLE = "DISPONIBLE";
    public static final String ESTADO_EN_REPARACION = "EN_REPARACION";
    public static final String ESTADO_DANADO = "DANADO";
    // Estado derivado para la visualización
    public static final String ESTADO_RENTADO = "RENTADO";

    private String id;
    private String nombre;
    private String categoria;
    private int numeroUnidad;
    private String estadoFisico;
    private double precioRenta;
    private String fotoUri;
    private long fechaAlta;

    public Equipo() {
        this.id = UUID.randomUUID().toString();
        this.estadoFisico = ESTADO_DISPONIBLE;
        this.fechaAlta = System.currentTimeMillis();
    }

    public Equipo(Equipo otro) {
        if (otro != null) {
            this.id = otro.id;
            this.nombre = otro.nombre;
            this.categoria = otro.categoria;
            this.numeroUnidad = otro.numeroUnidad;
            this.estadoFisico = otro.estadoFisico;
            this.precioRenta = otro.precioRenta;
            this.fotoUri = otro.fotoUri;
            this.fechaAlta = otro.fechaAlta;
        }
    }

    public Equipo(String id, String nombre, String categoria, int numeroUnidad, String estadoFisico, double precioRenta) {
        this(id, nombre, categoria, numeroUnidad, estadoFisico, precioRenta, null);
    }

    public Equipo(String id, String nombre, String categoria, int numeroUnidad, String estadoFisico, double precioRenta, String fotoUri) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.nombre = nombre;
        this.categoria = categoria;
        this.numeroUnidad = numeroUnidad;
        this.estadoFisico = estadoFisico != null ? estadoFisico : ESTADO_DISPONIBLE;
        this.precioRenta = precioRenta;
        this.fotoUri = fotoUri;
        this.fechaAlta = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public int getNumeroUnidad() {
        return numeroUnidad;
    }

    public void setNumeroUnidad(int numeroUnidad) {
        this.numeroUnidad = numeroUnidad;
    }

    public String getEstadoFisico() {
        return estadoFisico;
    }

    public void setEstadoFisico(String estadoFisico) {
        this.estadoFisico = estadoFisico;
    }

    public double getPrecioRenta() {
        return precioRenta;
    }

    public void setPrecioRenta(double precioRenta) {
        this.precioRenta = precioRenta;
    }

    public String getFotoUri() {
        return fotoUri;
    }

    public void setFotoUri(String fotoUri) {
        this.fotoUri = fotoUri;
    }

    public long getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(long fechaAlta) {
        this.fechaAlta = fechaAlta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Equipo equipo = (Equipo) o;
        return Objects.equals(id, equipo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
