package com.example.propuestasonidosonic.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Paquete {
    private String id;
    private String nombre;
    private String descripcion;
    private double precio;
    private List<Partida> partidas;

    public Paquete() {
        this.id = UUID.randomUUID().toString();
        this.partidas = new ArrayList<>();
    }

    public Paquete(String id, String nombre, String descripcion, double precio, List<Partida> partidas) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.partidas = partidas != null ? partidas : new ArrayList<>();
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public List<Partida> getPartidas() {
        return partidas;
    }

    public void setPartidas(List<Partida> partidas) {
        this.partidas = partidas != null ? partidas : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Paquete paquete = (Paquete) o;
        return Objects.equals(id, paquete.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
