package com.example.propuestasonidosonic.model;

import java.util.Objects;
import java.util.UUID;

public class Usuario {
    public static final String ROL_SOCIO = "SOCIO";
    public static final String ROL_CLIENTE = "CLIENTE";
    public static final String ROL_ASISTENTE = "ASISTENTE";

    private String id;
    private String nombre;
    private String correo;
    private String telefono;
    private String rol;
    private String contrasena;
    private boolean cuentaActiva;
    private long fechaAlta;

    public Usuario() {
        this.id = UUID.randomUUID().toString();
        this.cuentaActiva = true;
        this.fechaAlta = System.currentTimeMillis();
    }

    public Usuario(String id, String nombre, String correo, String telefono, String rol, String contrasena, boolean cuentaActiva, long fechaAlta) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
        this.rol = rol;
        this.contrasena = contrasena;
        this.cuentaActiva = cuentaActiva;
        this.fechaAlta = fechaAlta;
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

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public boolean isCuentaActiva() {
        return cuentaActiva;
    }

    public void setCuentaActiva(boolean cuentaActiva) {
        this.cuentaActiva = cuentaActiva;
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
        Usuario usuario = (Usuario) o;
        return Objects.equals(id, usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
