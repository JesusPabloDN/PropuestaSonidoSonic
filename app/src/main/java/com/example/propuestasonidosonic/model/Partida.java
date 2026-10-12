package com.example.propuestasonidosonic.model;

import java.util.Objects;

public class Partida {
    public static final String TIPO_EQUIPO = "EQUIPO";
    public static final String TIPO_PAQUETE = "PAQUETE";

    private String tipo;
    private String nombre;
    private int cantidad;
    private double precioUnitario;

    public Partida() {
        this.tipo = TIPO_EQUIPO;
        this.cantidad = 1;
    }

    public Partida(String tipo, String nombre, int cantidad, double precioUnitario) {
        this.tipo = tipo != null ? tipo : TIPO_EQUIPO;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public double getSubtotal() {
        return cantidad * precioUnitario;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Partida partida = (Partida) o;
        return cantidad == partida.cantidad &&
                Double.compare(partida.precioUnitario, precioUnitario) == 0 &&
                Objects.equals(tipo, partida.tipo) &&
                Objects.equals(nombre, partida.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipo, nombre, cantidad, precioUnitario);
    }
}
