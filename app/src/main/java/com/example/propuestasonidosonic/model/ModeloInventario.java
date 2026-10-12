package com.example.propuestasonidosonic.model;

import java.util.Objects;

public class ModeloInventario {
    private String clave;
    private String nombreModelo;
    private String categoria;
    private int totalUnidades;
    private int unidadesDisponibles;
    private double precioUnitario;

    public ModeloInventario() {}

    public ModeloInventario(String clave, String nombreModelo, String categoria,
                            int totalUnidades, int unidadesDisponibles, double precioUnitario) {
        this.clave = clave;
        this.nombreModelo = nombreModelo;
        this.categoria = categoria;
        this.totalUnidades = totalUnidades;
        this.unidadesDisponibles = unidadesDisponibles;
        this.precioUnitario = precioUnitario;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getNombreModelo() {
        return nombreModelo;
    }

    public void setNombreModelo(String nombreModelo) {
        this.nombreModelo = nombreModelo;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public int getTotalUnidades() {
        return totalUnidades;
    }

    public void setTotalUnidades(int totalUnidades) {
        this.totalUnidades = totalUnidades;
    }

    public int getUnidadesDisponibles() {
        return unidadesDisponibles;
    }

    public void setUnidadesDisponibles(int unidadesDisponibles) {
        this.unidadesDisponibles = unidadesDisponibles;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModeloInventario that = (ModeloInventario) o;
        return Objects.equals(clave, that.clave);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clave);
    }
}
