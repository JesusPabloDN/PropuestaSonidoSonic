package com.example.propuestasonidosonic.model;

import java.util.Objects;
import java.util.UUID;

public class Pago {
    public static final String TIPO_ANTICIPO = "ANTICIPO";
    public static final String TIPO_ABONO = "ABONO";
    public static final String TIPO_TOTAL = "TOTAL";
    public static final String TIPO_REEMBOLSO = "REEMBOLSO";

    public static final String METODO_EFECTIVO = "EFECTIVO";
    public static final String METODO_TRANSFERENCIA = "TRANSFERENCIA";
    public static final String METODO_TARJETA = "TARJETA";

    private String id;
    private String pedidoId;
    private String clienteId;
    private double monto;
    private String tipo;
    private String metodo;
    private long fecha;
    private String folio;

    public Pago() {
        this.id = UUID.randomUUID().toString();
        this.tipo = TIPO_ANTICIPO;
        this.metodo = METODO_EFECTIVO;
        this.fecha = System.currentTimeMillis();
    }

    public Pago(String id, String pedidoId, String clienteId, double monto, String tipo, String metodo, long fecha) {
        this(id, pedidoId, clienteId, monto, tipo, metodo, fecha, null);
    }

    public Pago(String id, String pedidoId, String clienteId, double monto, String tipo, String metodo, long fecha, String folio) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.pedidoId = pedidoId;
        this.clienteId = clienteId;
        this.monto = monto;
        this.tipo = tipo != null ? tipo : TIPO_ANTICIPO;
        this.metodo = metodo != null ? metodo : METODO_EFECTIVO;
        this.fecha = fecha;
        this.folio = folio;
    }

    public Pago(Pago otro) {
        if (otro != null) {
            this.id = otro.id;
            this.pedidoId = otro.pedidoId;
            this.clienteId = otro.clienteId;
            this.monto = otro.monto;
            this.tipo = otro.tipo;
            this.metodo = otro.metodo;
            this.fecha = otro.fecha;
            this.folio = otro.folio;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(String pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMetodo() {
        return metodo;
    }

    public void setMetodo(String metodo) {
        this.metodo = metodo;
    }

    public long getFecha() {
        return fecha;
    }

    public void setFecha(long fecha) {
        this.fecha = fecha;
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pago pago = (Pago) o;
        return Objects.equals(id, pago.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
