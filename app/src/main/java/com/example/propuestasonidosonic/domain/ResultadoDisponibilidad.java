package com.example.propuestasonidosonic.domain;

import java.util.ArrayList;
import java.util.List;

public class ResultadoDisponibilidad {
    public static final String PAQUETE_INACTIVO = "PAQUETE_INACTIVO";
    public static final String PAQUETE_INCOMPLETO = "PAQUETE_INCOMPLETO";
    public static final String DURACION_INVALIDA = "DURACION_INVALIDA";
    public static final String EN_EL_PASADO = "EN_EL_PASADO";
    public static final String FUERA_DE_HORARIO = "FUERA_DE_HORARIO";
    public static final String SIN_STOCK = "SIN_STOCK";
    public static final String MAXIMO_SIMULTANEOS = "MAXIMO_SIMULTANEOS";
    public static final String SIN_OPERADORES = "SIN_OPERADORES";

    private boolean viable;
    private String motivoNoViable;
    private String detalle;
    private List<String> operadorIds;
    private boolean requiereRevision;
    private List<String> motivosRevision;
    private long bloqueoInicio;
    private long bloqueoFin;

    public ResultadoDisponibilidad() {
        this.operadorIds = new ArrayList<>();
        this.motivosRevision = new ArrayList<>();
    }

    public static ResultadoDisponibilidad noViable(String motivo, String detalle) {
        ResultadoDisponibilidad r = new ResultadoDisponibilidad();
        r.setViable(false);
        r.setMotivoNoViable(motivo);
        r.setDetalle(detalle);
        return r;
    }

    public static ResultadoDisponibilidad viable(List<String> operadorIds, boolean requiereRevision,
                                                 List<String> motivosRevision, long bloqueoInicio, long bloqueoFin) {
        ResultadoDisponibilidad r = new ResultadoDisponibilidad();
        r.setViable(true);
        r.setOperadorIds(operadorIds);
        r.setRequiereRevision(requiereRevision);
        r.setMotivosRevision(motivosRevision);
        r.setBloqueoInicio(bloqueoInicio);
        r.setBloqueoFin(bloqueoFin);
        return r;
    }

    public boolean isViable() {
        return viable;
    }

    public void setViable(boolean viable) {
        this.viable = viable;
    }

    public String getMotivoNoViable() {
        return motivoNoViable;
    }

    public void setMotivoNoViable(String motivoNoViable) {
        this.motivoNoViable = motivoNoViable;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public List<String> getOperadorIds() {
        return operadorIds;
    }

    public void setOperadorIds(List<String> operadorIds) {
        this.operadorIds = operadorIds != null ? operadorIds : new ArrayList<>();
    }

    public boolean isRequiereRevision() {
        return requiereRevision;
    }

    public void setRequiereRevision(boolean requiereRevision) {
        this.requiereRevision = requiereRevision;
    }

    public List<String> getMotivosRevision() {
        return motivosRevision;
    }

    public void setMotivosRevision(List<String> motivosRevision) {
        this.motivosRevision = motivosRevision != null ? motivosRevision : new ArrayList<>();
    }

    public long getBloqueoInicio() {
        return bloqueoInicio;
    }

    public void setBloqueoInicio(long bloqueoInicio) {
        this.bloqueoInicio = bloqueoInicio;
    }

    public long getBloqueoFin() {
        return bloqueoFin;
    }

    public void setBloqueoFin(long bloqueoFin) {
        this.bloqueoFin = bloqueoFin;
    }
}
