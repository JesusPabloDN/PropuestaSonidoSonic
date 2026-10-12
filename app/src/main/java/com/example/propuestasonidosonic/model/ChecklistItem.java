package com.example.propuestasonidosonic.model;

import java.util.Objects;

public class ChecklistItem {
    private String equipoId;
    private String nombre;
    private boolean salio;
    private boolean regreso;
    private String observacion;

    public ChecklistItem() {
        this.observacion = "";
    }

    public ChecklistItem(String equipoId, String nombre, boolean salio, boolean regreso, String observacion) {
        this.equipoId = equipoId;
        this.nombre = nombre;
        this.salio = salio;
        this.regreso = regreso;
        this.observacion = observacion != null ? observacion : "";
    }

    public String getEquipoId() {
        return equipoId;
    }

    public void setEquipoId(String equipoId) {
        this.equipoId = equipoId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean isSalio() {
        return salio;
    }

    public void setSalio(boolean salio) {
        this.salio = salio;
    }

    public boolean isRegreso() {
        return regreso;
    }

    public void setRegreso(boolean regreso) {
        this.regreso = regreso;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChecklistItem that = (ChecklistItem) o;
        return Objects.equals(equipoId, that.equipoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(equipoId);
    }
}
