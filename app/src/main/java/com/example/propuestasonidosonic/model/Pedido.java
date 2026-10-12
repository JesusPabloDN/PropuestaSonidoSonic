package com.example.propuestasonidosonic.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Pedido {
    public static final String CANAL_LLAMADA = "LLAMADA";
    public static final String CANAL_WHATSAPP = "WHATSAPP";
    public static final String CANAL_APP = "APP";

    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_CONFIRMADO = "CONFIRMADO";
    public static final String ESTADO_PAGADO = "PAGADO";
    public static final String ESTADO_ENTREGADO = "ENTREGADO";
    public static final String ESTADO_CANCELADO = "CANCELADO";

    private String id;
    private long numero;
    private String clienteId;
    private String clienteNombre;
    private String clienteTelefono;
    private String canalOrigen;
    private List<Partida> partidas;
    private long inicioMillis;
    private long finMillis;
    private String direccion;
    private String estado;
    private double total;
    private String eventoId; // nullable
    private long fechaCreacion;
    private long fechaActualizacion;

    public Pedido() {
        this.id = UUID.randomUUID().toString();
        this.canalOrigen = CANAL_WHATSAPP;
        this.partidas = new ArrayList<>();
        this.estado = ESTADO_PENDIENTE;
        this.fechaCreacion = System.currentTimeMillis();
        this.fechaActualizacion = System.currentTimeMillis();
    }

    public Pedido(String id, long numero, String clienteId, String clienteNombre, String clienteTelefono,
                  String canalOrigen, List<Partida> partidas, long inicioMillis, long finMillis,
                  String direccion, String estado, double total, String eventoId,
                  long fechaCreacion, long fechaActualizacion) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.numero = numero;
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.clienteTelefono = clienteTelefono;
        this.canalOrigen = canalOrigen != null ? canalOrigen : CANAL_WHATSAPP;
        this.partidas = partidas != null ? partidas : new ArrayList<>();
        this.inicioMillis = inicioMillis;
        this.finMillis = finMillis;
        this.direccion = direccion;
        this.estado = estado != null ? estado : ESTADO_PENDIENTE;
        this.total = total;
        this.eventoId = eventoId;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getNumero() {
        return numero;
    }

    public void setNumero(long numero) {
        this.numero = numero;
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

    public String getCanalOrigen() {
        return canalOrigen;
    }

    public void setCanalOrigen(String canalOrigen) {
        this.canalOrigen = canalOrigen;
    }

    public List<Partida> getPartidas() {
        return partidas;
    }

    public void setPartidas(List<Partida> partidas) {
        this.partidas = partidas != null ? partidas : new ArrayList<>();
    }

    public long getInicioMillis() {
        return inicioMillis;
    }

    public void setInicioMillis(long inicioMillis) {
        this.inicioMillis = inicioMillis;
    }

    public long getFinMillis() {
        return finMillis;
    }

    public void setFinMillis(long finMillis) {
        this.finMillis = finMillis;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getEventoId() {
        return eventoId;
    }

    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
    }

    public long getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(long fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public long getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(long fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pedido pedido = (Pedido) o;
        return Objects.equals(id, pedido.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
