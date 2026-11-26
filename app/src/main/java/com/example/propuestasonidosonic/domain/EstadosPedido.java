package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.model.Pedido;

/**
 * Reglas del ciclo de vida de un pedido.
 *
 * Solicitud desde la app → POR_APROBAR → (socio aprueba) CONFIRMADO o (socio rechaza) RECHAZADO.
 * Si nadie la aprueba antes de que empiece el evento → VENCIDO.
 * Pedido registrado por un socio → CONFIRMADO directamente.
 * Al terminar el evento: con pago confirmado → FINALIZADO; sin pago → POR_COBRAR,
 * y pasa a FINALIZADO cuando un socio confirma que recibió el pago.
 */
public final class EstadosPedido {

    private EstadosPedido() {}

    public static boolean esPorAprobar(String estado) {
        return Pedido.ESTADO_POR_APROBAR.equals(estado);
    }

    /** El pedido sigue vigente y ocupa agenda, equipo y personal (por aprobar o confirmado). */
    public static boolean ocupaRecursos(String estado) {
        return Pedido.ESTADO_POR_APROBAR.equals(estado)
                || Pedido.ESTADO_CONFIRMADO.equals(estado);
    }

    /** El evento ya se realizó (por cobrar o finalizado). */
    public static boolean esRealizado(String estado) {
        return Pedido.ESTADO_POR_COBRAR.equals(estado)
                || Pedido.ESTADO_FINALIZADO.equals(estado);
    }

    /** Pedido aceptado por el negocio: confirmado, por cobrar o finalizado. */
    public static boolean esAceptado(String estado) {
        return Pedido.ESTADO_CONFIRMADO.equals(estado) || esRealizado(estado);
    }

    /** Pedido cerrado sin realizarse: rechazado, cancelado o vencido. */
    public static boolean esDescartado(String estado) {
        return Pedido.ESTADO_RECHAZADO.equals(estado)
                || Pedido.ESTADO_CANCELADO.equals(estado)
                || Pedido.ESTADO_VENCIDO.equals(estado);
    }

    /** Un socio puede confirmar el pago antes o después del evento, una sola vez. */
    public static boolean puedeConfirmarPago(Pedido p) {
        if (p == null || p.isPagoConfirmado()) return false;
        return Pedido.ESTADO_CONFIRMADO.equals(p.getEstado())
                || Pedido.ESTADO_POR_COBRAR.equals(p.getEstado());
    }

    /** Monto que el cliente todavía debe (0 si ya pagó o si el pedido no fue aceptado). */
    public static double montoPendiente(Pedido p) {
        if (p == null || p.isPagoConfirmado() || !esAceptado(p.getEstado())) return 0.0;
        return MoneyUtils.redondear2(p.getTotal());
    }

    public static boolean puedeReprogramarse(Pedido p, long ahora) {
        return p != null && ocupaRecursos(p.getEstado()) && p.getInicioMillis() > ahora;
    }

    public static boolean puedeCancelarse(Pedido p, long ahora) {
        return p != null && ocupaRecursos(p.getEstado()) && p.getInicioMillis() > ahora;
    }

    public static long finBloqueo(Pedido p) {
        if (p == null) return 0L;
        return p.getBloqueoFinMillis() > 0 ? p.getBloqueoFinMillis() : p.getFinMillis();
    }

    public static boolean metodoValido(String metodo) {
        return Pedido.METODO_EFECTIVO.equals(metodo) || Pedido.METODO_TRANSFERENCIA.equals(metodo);
    }

    public static String etiquetaMetodo(String metodo) {
        if (Pedido.METODO_TRANSFERENCIA.equals(metodo)) return "Transferencia";
        if (Pedido.METODO_EFECTIVO.equals(metodo)) return "Efectivo";
        return metodo != null ? metodo : "";
    }

    /** Etiqueta corta del estado para insignias y listas. */
    public static String etiqueta(String estado) {
        if (estado == null) return "";
        switch (estado) {
            case Pedido.ESTADO_POR_APROBAR: return "Por aprobar";
            case Pedido.ESTADO_CONFIRMADO: return "Confirmado";
            case Pedido.ESTADO_POR_COBRAR: return "Por cobrar";
            case Pedido.ESTADO_FINALIZADO: return "Finalizado";
            case Pedido.ESTADO_RECHAZADO: return "Rechazado";
            case Pedido.ESTADO_CANCELADO: return "Cancelado";
            case Pedido.ESTADO_VENCIDO: return "Vencido";
            default: return estado;
        }
    }
}
