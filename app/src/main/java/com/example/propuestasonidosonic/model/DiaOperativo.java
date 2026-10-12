package com.example.propuestasonidosonic.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DiaOperativo {
    private String fechaClave; // yyyy-MM-dd
    private List<String> asistentesDisponiblesIds;
    private List<String> sociosNoDisponiblesIds;

    public DiaOperativo() {
        this.asistentesDisponiblesIds = new ArrayList<>();
        this.sociosNoDisponiblesIds = new ArrayList<>();
    }

    public DiaOperativo(String fechaClave, List<String> asistentesDisponiblesIds, List<String> sociosNoDisponiblesIds) {
        this.fechaClave = fechaClave;
        this.asistentesDisponiblesIds = asistentesDisponiblesIds != null ? new ArrayList<>(asistentesDisponiblesIds) : new ArrayList<>();
        this.sociosNoDisponiblesIds = sociosNoDisponiblesIds != null ? new ArrayList<>(sociosNoDisponiblesIds) : new ArrayList<>();
    }

    public DiaOperativo(DiaOperativo otro) {
        if (otro != null) {
            this.fechaClave = otro.fechaClave;
            this.asistentesDisponiblesIds = otro.asistentesDisponiblesIds != null ? new ArrayList<>(otro.asistentesDisponiblesIds) : new ArrayList<>();
            this.sociosNoDisponiblesIds = otro.sociosNoDisponiblesIds != null ? new ArrayList<>(otro.sociosNoDisponiblesIds) : new ArrayList<>();
        } else {
            this.asistentesDisponiblesIds = new ArrayList<>();
            this.sociosNoDisponiblesIds = new ArrayList<>();
        }
    }

    public String getFechaClave() {
        return fechaClave;
    }

    public void setFechaClave(String fechaClave) {
        this.fechaClave = fechaClave;
    }

    public List<String> getAsistentesDisponiblesIds() {
        return asistentesDisponiblesIds;
    }

    public void setAsistentesDisponiblesIds(List<String> asistentesDisponiblesIds) {
        this.asistentesDisponiblesIds = asistentesDisponiblesIds != null ? asistentesDisponiblesIds : new ArrayList<>();
    }

    public List<String> getSociosNoDisponiblesIds() {
        return sociosNoDisponiblesIds;
    }

    public void setSociosNoDisponiblesIds(List<String> sociosNoDisponiblesIds) {
        this.sociosNoDisponiblesIds = sociosNoDisponiblesIds != null ? sociosNoDisponiblesIds : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiaOperativo that = (DiaOperativo) o;
        return Objects.equals(fechaClave, that.fechaClave);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fechaClave);
    }
}
