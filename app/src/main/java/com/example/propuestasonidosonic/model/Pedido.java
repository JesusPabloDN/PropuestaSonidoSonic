package com.example.propuestasonidosonic.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Pedido {
    // Constantes de canal / origen
    public static final String ORIGEN_APP = "APP";
    public static final String ORIGEN_LLAMADA = "LLAMADA";
    public static final String ORIGEN_WHATSAPP = "WHATSAPP";
    public static final String ORIGEN_PRESENCIAL = "PRESENCIAL";

    // Constantes de estado
    public static final String ESTADO_APARTADO = "APARTADO";
    public static final String ESTADO_PAGADO = "PAGADO";
    public static final String ESTADO_CONFIRMADO = "CONFIRMADO";
    public static final String ESTADO_EN_REVISION = "EN_REVISION";
    public static final String ESTADO_RECHAZADO = "RECHAZADO";
    public static final String ESTADO_CANCELADO = "CANCELADO";
    public static final String ESTADO_VENCIDO = "VENCIDO";
    public static final String ESTADO_COMPLETADO = "COMPLETADO";

    // Motivos de revision
    public static final String MOTIVO_URGENTE = "URGENTE";

    // Identidad y folio
    private String id;
    private long numero; // folio consecutivo

    // Paquete y origen
    private String paqueteId;
    private String paqueteNombre;
    private String canalOrigen;

    // Horarios y ventanas de bloqueo
    private long inicioMillis;
    private long finMillis;
    private long bloqueoInicioMillis;
    private long bloqueoFinMillis;

    // Recursos comprometidos
    private List<ComponentePaquete> componentes;
    private int operadoresRequeridos;
    private List<String> operadorIds;

    // Cliente
    private String clienteId;
    private String clienteNombre;
    private String clienteTelefono;
    private String direccion;
    private String notas;

    // Dinero
    private double total;
    private double anticipoRequerido;
    private double anticipoPagado;

    // Ciclo de vida y revisión
    private String estado;
    private long vencimientoApartadoMillis;
    private boolean requiereRevision;
    private List<String> motivosRevision;
    private long fechaCreacion;
    private long fechaActualizacion;

    // Campos preservados para compatibilidad
    private String eventoId;
    private String nombreEvento;
    private List<Partida> partidas;
    private List<String> equipoIds;
    private boolean cobrado;
    private long fechaCobro;

    public Pedido() {
        this.id = UUID.randomUUID().toString();
        this.canalOrigen = ORIGEN_WHATSAPP;
        this.componentes = new ArrayList<>();
        this.operadorIds = new ArrayList<>();
        this.motivosRevision = new ArrayList<>();
        this.partidas = new ArrayList<>();
        this.equipoIds = new ArrayList<>();
        this.estado = ESTADO_APARTADO;
        this.fechaCreacion = System.currentTimeMillis();
        this.fechaActualizacion = System.currentTimeMillis();
        this.cobrado = false;
    }

    public Pedido(Pedido otro) {
        if (otro == null) {
            this.id = UUID.randomUUID().toString();
            this.canalOrigen = ORIGEN_WHATSAPP;
            this.componentes = new ArrayList<>();
            this.operadorIds = new ArrayList<>();
            this.motivosRevision = new ArrayList<>();
            this.partidas = new ArrayList<>();
            this.equipoIds = new ArrayList<>();
            this.estado = ESTADO_APARTADO;
            this.fechaCreacion = System.currentTimeMillis();
            this.fechaActualizacion = System.currentTimeMillis();
            return;
        }
        this.id = otro.id;
        this.numero = otro.numero;
        this.paqueteId = otro.paqueteId;
        this.paqueteNombre = otro.paqueteNombre;
        this.canalOrigen = otro.canalOrigen;
        this.inicioMillis = otro.inicioMillis;
        this.finMillis = otro.finMillis;
        this.bloqueoInicioMillis = otro.bloqueoInicioMillis;
        this.bloqueoFinMillis = otro.bloqueoFinMillis;

        this.componentes = new ArrayList<>();
        if (otro.componentes != null) {
            for (ComponentePaquete c : otro.componentes) {
                this.componentes.add(new ComponentePaquete(c));
            }
        }

        this.operadoresRequeridos = otro.operadoresRequeridos;
        this.operadorIds = otro.operadorIds != null ? new ArrayList<>(otro.operadorIds) : new ArrayList<>();
        this.clienteId = otro.clienteId;
        this.clienteNombre = otro.clienteNombre;
        this.clienteTelefono = otro.clienteTelefono;
        this.direccion = otro.direccion;
        this.notas = otro.notas;
        this.total = otro.total;
        this.anticipoRequerido = otro.anticipoRequerido;
        this.anticipoPagado = otro.anticipoPagado;
        this.estado = otro.estado;
        this.vencimientoApartadoMillis = otro.vencimientoApartadoMillis;
        this.requiereRevision = otro.requiereRevision;
        this.motivosRevision = otro.motivosRevision != null ? new ArrayList<>(otro.motivosRevision) : new ArrayList<>();
        this.fechaCreacion = otro.fechaCreacion;
        this.fechaActualizacion = otro.fechaActualizacion;

        this.eventoId = otro.eventoId;
        this.nombreEvento = otro.nombreEvento;
        this.partidas = otro.partidas != null ? new ArrayList<>(otro.partidas) : new ArrayList<>();
        this.equipoIds = otro.equipoIds != null ? new ArrayList<>(otro.equipoIds) : new ArrayList<>();
        this.cobrado = otro.cobrado;
        this.fechaCobro = otro.fechaCobro;
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

    public String getPaqueteId() {
        return paqueteId;
    }

    public void setPaqueteId(String paqueteId) {
        this.paqueteId = paqueteId;
    }

    public String getPaqueteNombre() {
        return paqueteNombre;
    }

    public void setPaqueteNombre(String paqueteNombre) {
        this.paqueteNombre = paqueteNombre;
    }

    public String getCanalOrigen() {
        return canalOrigen;
    }

    public void setCanalOrigen(String canalOrigen) {
        this.canalOrigen = canalOrigen;
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

    public long getBloqueoInicioMillis() {
        return bloqueoInicioMillis;
    }

    public void setBloqueoInicioMillis(long bloqueoInicioMillis) {
        this.bloqueoInicioMillis = bloqueoInicioMillis;
    }

    public long getBloqueoFinMillis() {
        return bloqueoFinMillis;
    }

    public void setBloqueoFinMillis(long bloqueoFinMillis) {
        this.bloqueoFinMillis = bloqueoFinMillis;
    }

    public List<ComponentePaquete> getComponentes() {
        return componentes;
    }

    public void setComponentes(List<ComponentePaquete> componentes) {
        this.componentes = componentes != null ? componentes : new ArrayList<>();
    }

    public int getOperadoresRequeridos() {
        return operadoresRequeridos;
    }

    public void setOperadoresRequeridos(int operadoresRequeridos) {
        this.operadoresRequeridos = operadoresRequeridos;
    }

    public List<String> getOperadorIds() {
        return operadorIds;
    }

    public void setOperadorIds(List<String> operadorIds) {
        this.operadorIds = operadorIds != null ? operadorIds : new ArrayList<>();
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

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getAnticipoRequerido() {
        return anticipoRequerido;
    }

    public void setAnticipoRequerido(double anticipoRequerido) {
        this.anticipoRequerido = anticipoRequerido;
    }

    public double getAnticipoPagado() {
        return anticipoPagado;
    }

    public void setAnticipoPagado(double anticipoPagado) {
        this.anticipoPagado = anticipoPagado;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public long getVencimientoApartadoMillis() {
        return vencimientoApartadoMillis;
    }

    public void setVencimientoApartadoMillis(long vencimientoApartadoMillis) {
        this.vencimientoApartadoMillis = vencimientoApartadoMillis;
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

    public String getEventoId() {
        return eventoId;
    }

    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
    }

    public String getNombreEvento() {
        return nombreEvento;
    }

    public void setNombreEvento(String nombreEvento) {
        this.nombreEvento = nombreEvento;
    }

    public List<Partida> getPartidas() {
        return partidas;
    }

    public void setPartidas(List<Partida> partidas) {
        this.partidas = partidas != null ? partidas : new ArrayList<>();
    }

    public List<String> getEquipoIds() {
        return equipoIds;
    }

    public void setEquipoIds(List<String> equipoIds) {
        this.equipoIds = equipoIds != null ? equipoIds : new ArrayList<>();
    }

    public boolean isCobrado() {
        return cobrado;
    }

    public void setCobrado(boolean cobrado) {
        this.cobrado = cobrado;
    }

    public long getFechaCobro() {
        return fechaCobro;
    }

    public void setFechaCobro(long fechaCobro) {
        this.fechaCobro = fechaCobro;
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
