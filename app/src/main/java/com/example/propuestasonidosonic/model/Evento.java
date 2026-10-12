package com.example.propuestasonidosonic.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Evento {
    public static final String ESTADO_PROGRAMADO = "PROGRAMADO";
    public static final String ESTADO_CANCELADO = "CANCELADO";
    public static final String ESTADO_FINALIZADO = "FINALIZADO";

    private String id;
    private String nombre;
    private long inicioMillis;
    private long finMillis;
    private String direccion;
    private String socioResponsableId;
    private String tecnicoId; // nullable
    private List<String> equipoIds;
    private String pedidoId; // nullable
    private String estado;
    private String notas;
    private List<ChecklistItem> checklist;

    public Evento() {
        this.id = UUID.randomUUID().toString();
        this.estado = ESTADO_PROGRAMADO;
        this.equipoIds = new ArrayList<>();
        this.checklist = new ArrayList<>();
        this.notas = "";
    }

    public Evento(String id, String nombre, long inicioMillis, long finMillis, String direccion,
                  String socioResponsableId, String tecnicoId, List<String> equipoIds,
                  String pedidoId, String estado, String notas, List<ChecklistItem> checklist) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.nombre = nombre;
        this.inicioMillis = inicioMillis;
        this.finMillis = finMillis;
        this.direccion = direccion;
        this.socioResponsableId = socioResponsableId;
        this.tecnicoId = tecnicoId;
        this.equipoIds = equipoIds != null ? equipoIds : new ArrayList<>();
        this.pedidoId = pedidoId;
        this.estado = estado != null ? estado : ESTADO_PROGRAMADO;
        this.notas = notas != null ? notas : "";
        this.checklist = checklist != null ? checklist : new ArrayList<>();
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

    public String getSocioResponsableId() {
        return socioResponsableId;
    }

    public void setSocioResponsableId(String socioResponsableId) {
        this.socioResponsableId = socioResponsableId;
    }

    public String getTecnicoId() {
        return tecnicoId;
    }

    public void setTecnicoId(String tecnicoId) {
        this.tecnicoId = tecnicoId;
    }

    public List<String> getEquipoIds() {
        return equipoIds;
    }

    public void setEquipoIds(List<String> equipoIds) {
        this.equipoIds = equipoIds != null ? equipoIds : new ArrayList<>();
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(String pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public List<ChecklistItem> getChecklist() {
        return checklist;
    }

    public void setChecklist(List<ChecklistItem> checklist) {
        this.checklist = checklist != null ? checklist : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Evento evento = (Evento) o;
        return Objects.equals(id, evento.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
