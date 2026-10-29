package com.example.propuestasonidosonic.model;

import com.example.propuestasonidosonic.util.Config;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Paquete {
    private String id;
    private String nombre;
    private String descripcion;
    private List<ComponentePaquete> componentes;
    private double precio;
    private int duracionBaseHoras;
    private double precioHoraExtra;
    private int operadoresRequeridos;
    private int bufferMontajeMin;
    private int bufferDesmontajeMin;
    private boolean activo;
    private String fotoUri;
    private long fechaCreacion;

    public Paquete() {
        this.id = UUID.randomUUID().toString();
        this.descripcion = "";
        this.componentes = new ArrayList<>();
        this.duracionBaseHoras = Config.DURACION_BASE_HORAS_DEFAULT;
        this.precioHoraExtra = 0.0;
        this.operadoresRequeridos = 1;
        this.bufferMontajeMin = Config.BUFFER_MONTAJE_MIN_DEFAULT;
        this.bufferDesmontajeMin = Config.BUFFER_DESMONTAJE_MIN_DEFAULT;
        this.activo = true;
        this.fechaCreacion = System.currentTimeMillis();
    }

    public Paquete(Paquete otro) {
        if (otro != null) {
            this.id = otro.id;
            this.nombre = otro.nombre;
            this.descripcion = otro.descripcion;
            this.precio = otro.precio;
            this.duracionBaseHoras = otro.duracionBaseHoras;
            this.precioHoraExtra = otro.precioHoraExtra;
            this.operadoresRequeridos = otro.operadoresRequeridos;
            this.bufferMontajeMin = otro.bufferMontajeMin;
            this.bufferDesmontajeMin = otro.bufferDesmontajeMin;
            this.activo = otro.activo;
            this.fotoUri = otro.fotoUri;
            this.fechaCreacion = otro.fechaCreacion;
            this.componentes = new ArrayList<>();
            if (otro.componentes != null) {
                for (ComponentePaquete c : otro.componentes) {
                    this.componentes.add(new ComponentePaquete(c));
                }
            }
        }
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
        this.descripcion = descripcion != null ? descripcion : "";
    }

    public List<ComponentePaquete> getComponentes() {
        return componentes;
    }

    public void setComponentes(List<ComponentePaquete> componentes) {
        this.componentes = componentes != null ? componentes : new ArrayList<>();
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    @Exclude
    public double getPrecioBase() {
        return precio;
    }

    @Exclude
    public void setPrecioBase(double precioBase) {
        this.precio = precioBase;
    }

    public int getDuracionBaseHoras() {
        return duracionBaseHoras;
    }

    public void setDuracionBaseHoras(int duracionBaseHoras) {
        this.duracionBaseHoras = duracionBaseHoras;
    }

    public double getPrecioHoraExtra() {
        return precioHoraExtra;
    }

    public void setPrecioHoraExtra(double precioHoraExtra) {
        this.precioHoraExtra = precioHoraExtra;
    }

    public int getOperadoresRequeridos() {
        return operadoresRequeridos;
    }

    public void setOperadoresRequeridos(int operadoresRequeridos) {
        this.operadoresRequeridos = operadoresRequeridos;
    }

    public int getBufferMontajeMin() {
        return bufferMontajeMin;
    }

    public void setBufferMontajeMin(int bufferMontajeMin) {
        this.bufferMontajeMin = bufferMontajeMin;
    }

    public int getBufferDesmontajeMin() {
        return bufferDesmontajeMin;
    }

    public void setBufferDesmontajeMin(int bufferDesmontajeMin) {
        this.bufferDesmontajeMin = bufferDesmontajeMin;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getFotoUri() {
        return fotoUri;
    }

    public void setFotoUri(String fotoUri) {
        this.fotoUri = fotoUri;
    }

    public long getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(long fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
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
