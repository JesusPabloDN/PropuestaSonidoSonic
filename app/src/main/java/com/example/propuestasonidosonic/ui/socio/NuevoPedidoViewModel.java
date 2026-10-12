package com.example.propuestasonidosonic.ui.socio;

import androidx.lifecycle.ViewModel;

import com.example.propuestasonidosonic.model.Paquete;
import com.example.propuestasonidosonic.model.Pedido;

public class NuevoPedidoViewModel extends ViewModel {
    private Paquete paqueteSeleccionado;
    private int duracionMin = 240;
    private long fechaMillis = 0;
    private Long horarioInicioMillis = null;

    private String nombreEvento = "";
    private String nombreCliente = "";
    private String telefonoCliente = "";
    private String direccion = "";
    private String notas = "";
    private String origen = Pedido.ORIGEN_LLAMADA;
    private int horasApartado = 24;

    public Paquete getPaqueteSeleccionado() {
        return paqueteSeleccionado;
    }

    public void setPaqueteSeleccionado(Paquete paqueteSeleccionado) {
        this.paqueteSeleccionado = paqueteSeleccionado;
    }

    public int getDuracionMin() {
        return duracionMin;
    }

    public void setDuracionMin(int duracionMin) {
        this.duracionMin = duracionMin;
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

    public String getNombreEvento() {
        return nombreEvento;
    }

    public void setNombreEvento(String nombreEvento) {
        this.nombreEvento = nombreEvento;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getTelefonoCliente() {
        return telefonoCliente;
    }

    public void setTelefonoCliente(String telefonoCliente) {
        this.telefonoCliente = telefonoCliente;
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

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public int getHorasApartado() {
        return horasApartado;
    }

    public void setHorasApartado(int horasApartado) {
        this.horasApartado = horasApartado;
    }
}
