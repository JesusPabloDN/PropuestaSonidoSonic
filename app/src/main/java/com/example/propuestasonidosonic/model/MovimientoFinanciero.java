package com.example.propuestasonidosonic.model;

import java.util.Objects;
import java.util.UUID;

public class MovimientoFinanciero {
    public static final String TIPO_INGRESO = "INGRESO";
    public static final String TIPO_GASTO = "GASTO";

    public static final String CAT_RENTA = "RENTA";
    public static final String CAT_MANTENIMIENTO = "MANTENIMIENTO";
    public static final String CAT_REPARACION = "REPARACION";
    public static final String CAT_COMPRA = "COMPRA";
    public static final String CAT_OTRO = "OTRO";

    private String id;
    private String tipo;
    private double monto;
    private long fecha;
    private String concepto;
    private String categoria;
    private String pedidoId; // nullable
    private String pagoId;   // nullable
    private String creadoPor;

    public MovimientoFinanciero() {
        this.id = UUID.randomUUID().toString();
        this.tipo = TIPO_INGRESO;
        this.categoria = CAT_RENTA;
        this.fecha = System.currentTimeMillis();
    }

    public MovimientoFinanciero(String id, String tipo, double monto, long fecha, String concepto,
                               String categoria, String pedidoId, String pagoId, String creadoPor) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.tipo = tipo != null ? tipo : TIPO_INGRESO;
        this.monto = monto;
        this.fecha = fecha;
        this.concepto = concepto;
        this.categoria = categoria != null ? categoria : CAT_OTRO;
        this.pedidoId = pedidoId;
        this.pagoId = pagoId;
        this.creadoPor = creadoPor;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public long getFecha() {
        return fecha;
    }

    public void setFecha(long fecha) {
        this.fecha = fecha;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(String pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getPagoId() {
        return pagoId;
    }

    public void setPagoId(String pagoId) {
        this.pagoId = pagoId;
    }

    public String getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(String creadoPor) {
        this.creadoPor = creadoPor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovimientoFinanciero that = (MovimientoFinanciero) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
