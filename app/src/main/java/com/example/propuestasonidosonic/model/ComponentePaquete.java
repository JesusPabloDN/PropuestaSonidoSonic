package com.example.propuestasonidosonic.model;

import java.util.Objects;

public class ComponentePaquete {
    private String modeloClave;
    private String nombreModelo;
    private String categoria;
    private int cantidad;

    public ComponentePaquete() {}

    public ComponentePaquete(String modeloClave, String nombreModelo, String categoria, int cantidad) {
        this.modeloClave = modeloClave;
        this.nombreModelo = nombreModelo;
        this.categoria = categoria;
        this.cantidad = cantidad;
    }

    public ComponentePaquete(ComponentePaquete otro) {
        if (otro != null) {
            this.modeloClave = otro.modeloClave;
            this.nombreModelo = otro.nombreModelo;
            this.categoria = otro.categoria;
            this.cantidad = otro.cantidad;
        }
    }

    public String getModeloClave() {
        return modeloClave;
    }

    public void setModeloClave(String modeloClave) {
        this.modeloClave = modeloClave;
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

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentePaquete that = (ComponentePaquete) o;
        return cantidad == that.cantidad &&
                Objects.equals(modeloClave, that.modeloClave) &&
                Objects.equals(nombreModelo, that.nombreModelo) &&
                Objects.equals(categoria, that.categoria);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modeloClave, nombreModelo, categoria, cantidad);
    }
}
