package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.model.DiaOperativo;
import com.example.propuestasonidosonic.model.ModeloInventario;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContextoDisponibilidad {
    private long ahora;
    private List<ModeloInventario> modelos;
    private List<Pedido> reservas;
    private List<Usuario> socios;
    private List<Usuario> asistentes;
    private Map<String, DiaOperativo> dias;

    public ContextoDisponibilidad() {
        this.modelos = new ArrayList<>();
        this.reservas = new ArrayList<>();
        this.socios = new ArrayList<>();
        this.asistentes = new ArrayList<>();
        this.dias = new HashMap<>();
    }

    public ContextoDisponibilidad(long ahora, List<ModeloInventario> modelos, List<Pedido> reservas,
                                  List<Usuario> socios, List<Usuario> asistentes, Map<String, DiaOperativo> dias) {
        this.ahora = ahora;
        this.modelos = modelos != null ? modelos : new ArrayList<>();
        this.reservas = reservas != null ? reservas : new ArrayList<>();
        this.socios = socios != null ? socios : new ArrayList<>();
        this.asistentes = asistentes != null ? asistentes : new ArrayList<>();
        this.dias = dias != null ? dias : new HashMap<>();
    }

    public long getAhora() {
        return ahora;
    }

    public void setAhora(long ahora) {
        this.ahora = ahora;
    }

    public List<ModeloInventario> getModelos() {
        return modelos;
    }

    public void setModelos(List<ModeloInventario> modelos) {
        this.modelos = modelos != null ? modelos : new ArrayList<>();
    }

    public List<Pedido> getReservas() {
        return reservas;
    }

    public void setReservas(List<Pedido> reservas) {
        this.reservas = reservas != null ? reservas : new ArrayList<>();
    }

    public List<Usuario> getSocios() {
        return socios;
    }

    public void setSocios(List<Usuario> socios) {
        this.socios = socios != null ? socios : new ArrayList<>();
    }

    public List<Usuario> getAsistentes() {
        return asistentes;
    }

    public void setAsistentes(List<Usuario> asistentes) {
        this.asistentes = asistentes != null ? asistentes : new ArrayList<>();
    }

    public Map<String, DiaOperativo> getDias() {
        return dias;
    }

    public void setDias(Map<String, DiaOperativo> dias) {
        this.dias = dias != null ? dias : new HashMap<>();
    }
}
