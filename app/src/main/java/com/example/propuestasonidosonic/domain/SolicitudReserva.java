package com.example.propuestasonidosonic.domain;

public class SolicitudReserva {
    private String paqueteId;
    private long inicioMillis;
    private int duracionMin;
    private String origen;
    private String nombreEvento;
    private String clienteId;
    private String clienteNombre;
    private String clienteTelefono;
    private String direccion;
    private String notas;
    private Integer horasApartado;

    public SolicitudReserva() {}

    public SolicitudReserva(String paqueteId, long inicioMillis, int duracionMin, String origen,
                            String nombreEvento, String clienteId, String clienteNombre,
                            String clienteTelefono, String direccion, String notas, Integer horasApartado) {
        this.paqueteId = paqueteId;
        this.inicioMillis = inicioMillis;
        this.duracionMin = duracionMin;
        this.origen = origen;
        this.nombreEvento = nombreEvento;
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.clienteTelefono = clienteTelefono;
        this.direccion = direccion;
        this.notas = notas;
        this.horasApartado = horasApartado;
    }

    public String getPaqueteId() {
        return paqueteId;
    }

    public void setPaqueteId(String paqueteId) {
        this.paqueteId = paqueteId;
    }

    public long getInicioMillis() {
        return inicioMillis;
    }

    public void setInicioMillis(long inicioMillis) {
        this.inicioMillis = inicioMillis;
    }

    public int getDuracionMin() {
        return duracionMin;
    }

    public void setDuracionMin(int duracionMin) {
        this.duracionMin = duracionMin;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getNombreEvento() {
        return nombreEvento;
    }

    public void setNombreEvento(String nombreEvento) {
        this.nombreEvento = nombreEvento;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getClienteTelefono() {
        return clienteTelefono;
    }

    public void setClienteTelefono(String clienteTelefono) {
        this.clienteTelefono = clienteTelefono;
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

    public Integer getHorasApartado() {
        return horasApartado;
    }

    public void setHorasApartado(Integer horasApartado) {
        this.horasApartado = horasApartado;
    }
}
