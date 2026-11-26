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
    // Solicitud hecha desde la app; espera que un socio la apruebe o rechace
    public static final String ESTADO_POR_APROBAR = "POR_APROBAR";
    // Pedido aceptado (aprobado por un socio o registrado directamente por él)
    public static final String ESTADO_CONFIRMADO = "CONFIRMADO";
    // El evento ya terminó y el socio todavía no confirma que recibió el pago
    public static final String ESTADO_POR_COBRAR = "POR_COBRAR";
    // El evento terminó y el pago ya fue confirmado
    public static final String ESTADO_FINALIZADO = "FINALIZADO";
    public static final String ESTADO_RECHAZADO = "RECHAZADO";
    public static final String ESTADO_CANCELADO = "CANCELADO";
    // Solicitud que nadie aprobó antes de la fecha del evento
    public static final String ESTADO_VENCIDO = "VENCIDO";

    // Motivos de revision
    public static final String MOTIVO_URGENTE = "URGENTE";

    // Métodos con los que se recibe el pago
    public static final String METODO_EFECTIVO = "EFECTIVO";
    public static final String METODO_TRANSFERENCIA = "TRANSFERENCIA";

    // Cancelado por
    public static final String CANCELA_CLIENTE = "CLIENTE";
    public static final String CANCELA_SOCIO = "SOCIO";

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

    // Dinero: monto que el cliente debe pagar por el paquete
    private double total;

    // Confirmación del pago (la registra un socio cuando recibe el dinero)
    private boolean pagoConfirmado;
    private String metodoPago;
    private long fechaPagoMillis;
    private String pagoConfirmadoPorId;
    private String pagoConfirmadoPorNombre;

    // Cancelación, rechazo y reprogramación
    private String canceladoPor;
    private String motivoCancelacion;
    private String motivoRechazo;
    private int reprogramaciones;

    // Ciclo de vida
    private String estado;
    private List<String> motivosRevision;
    private long fechaCreacion;
    private long fechaActualizacion;

    // Campos preservados para compatibilidad
    private String eventoId;
    private String nombreEvento;
    private List<Partida> partidas;
    private List<String> equipoIds;
    private boolean asignacionIncompleta;

    public Pedido() {
        this.id = UUID.randomUUID().toString();
        this.canalOrigen = ORIGEN_WHATSAPP;
        this.componentes = new ArrayList<>();
        this.operadorIds = new ArrayList<>();
        this.motivosRevision = new ArrayList<>();
        this.partidas = new ArrayList<>();
        this.equipoIds = new ArrayList<>();
        this.estado = ESTADO_CONFIRMADO;
        this.fechaCreacion = System.currentTimeMillis();
        this.fechaActualizacion = System.currentTimeMillis();
        this.pagoConfirmado = false;
        this.asignacionIncompleta = false;
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
            this.estado = ESTADO_CONFIRMADO;
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
        this.pagoConfirmado = otro.pagoConfirmado;
        this.metodoPago = otro.metodoPago;
        this.fechaPagoMillis = otro.fechaPagoMillis;
        this.pagoConfirmadoPorId = otro.pagoConfirmadoPorId;
        this.pagoConfirmadoPorNombre = otro.pagoConfirmadoPorNombre;
        this.estado = otro.estado;
        this.motivosRevision = otro.motivosRevision != null ? new ArrayList<>(otro.motivosRevision) : new ArrayList<>();
        this.fechaCreacion = otro.fechaCreacion;
        this.fechaActualizacion = otro.fechaActualizacion;

        this.canceladoPor = otro.canceladoPor;
        this.motivoCancelacion = otro.motivoCancelacion;
        this.motivoRechazo = otro.motivoRechazo;
        this.reprogramaciones = otro.reprogramaciones;

        this.eventoId = otro.eventoId;
        this.nombreEvento = otro.nombreEvento;
        this.partidas = otro.partidas != null ? new ArrayList<>(otro.partidas) : new ArrayList<>();
        this.equipoIds = otro.equipoIds != null ? new ArrayList<>(otro.equipoIds) : new ArrayList<>();
        this.asignacionIncompleta = otro.asignacionIncompleta;
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

    public boolean isPagoConfirmado() {
        return pagoConfirmado;
    }

    public void setPagoConfirmado(boolean pagoConfirmado) {
        this.pagoConfirmado = pagoConfirmado;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public long getFechaPagoMillis() {
        return fechaPagoMillis;
    }

    public void setFechaPagoMillis(long fechaPagoMillis) {
        this.fechaPagoMillis = fechaPagoMillis;
    }

    public String getPagoConfirmadoPorId() {
        return pagoConfirmadoPorId;
    }

    public void setPagoConfirmadoPorId(String pagoConfirmadoPorId) {
        this.pagoConfirmadoPorId = pagoConfirmadoPorId;
    }

    public String getPagoConfirmadoPorNombre() {
        return pagoConfirmadoPorNombre;
    }

    public void setPagoConfirmadoPorNombre(String pagoConfirmadoPorNombre) {
        this.pagoConfirmadoPorNombre = pagoConfirmadoPorNombre;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
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

    public String getCanceladoPor() {
        return canceladoPor;
    }

    public void setCanceladoPor(String canceladoPor) {
        this.canceladoPor = canceladoPor;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public void setMotivoCancelacion(String motivoCancelacion) {
        this.motivoCancelacion = motivoCancelacion;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public int getReprogramaciones() {
        return reprogramaciones;
    }

    public void setReprogramaciones(int reprogramaciones) {
        this.reprogramaciones = reprogramaciones;
    }

    public boolean isAsignacionIncompleta() {
        return asignacionIncompleta;
    }

    public void setAsignacionIncompleta(boolean asignacionIncompleta) {
        this.asignacionIncompleta = asignacionIncompleta;
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
