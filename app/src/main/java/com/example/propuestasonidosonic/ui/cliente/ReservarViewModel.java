package com.example.propuestasonidosonic.ui.cliente;

import androidx.lifecycle.ViewModel;

import com.example.propuestasonidosonic.model.Paquete;

public class ReservarViewModel extends ViewModel {
    private String paqueteId;
    private Paquete paquete;
    private String nombreEvento = "";
    private long fechaMillis = 0;
    private Long horarioInicioMillis = null;
    private int duracionMin = 240;
    private String telefono = "";
    private String direccion = "";
    private String notas = "";

    public String getPaqueteId() {
        return paqueteId;
    }

    public void setPaqueteId(String paqueteId) {
        this.paqueteId = paqueteId;
    }

    public Paquete getPaquete() {
        return paquete;
    }

    public void setPaquete(Paquete paquete) {
        this.paquete = paquete;
    }

    public String getNombreEvento() {
        return nombreEvento;
    }

    public void setNombreEvento(String nombreEvento) {
        this.nombreEvento = nombreEvento;
    }

    public long getFechaMillis() {
        return fechaMillis;
    }

    public void setFechaMillis(long fechaMillis) {
        this.fechaMillis = fechaMillis;
    }

    public Long getHorarioInicioMillis() {
        return horarioInicioMillis;
    }

    public void setHorarioInicioMillis(Long horarioInicioMillis) {
        this.horarioInicioMillis = horarioInicioMillis;
    }

    public int getDuracionMin() {
        return duracionMin;
    }

    public void setDuracionMin(int duracionMin) {
        this.duracionMin = duracionMin;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }
}
