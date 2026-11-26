package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.model.Pedido;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PedidosClienteUtils {

    private PedidosClienteUtils() {}

    public static List<Pedido> delCliente(List<Pedido> pedidos, String clienteId) {
        if (pedidos == null || clienteId == null || clienteId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String idTrim = clienteId.trim();
        List<Pedido> res = new ArrayList<>();
        for (Pedido p : pedidos) {
            if (p != null && idTrim.equals(p.getClienteId())) {
                res.add(p);
            }
        }
        return res;
    }

    public static boolean esVisible(Pedido p, long ahora) {
        // Las solicitudes vencidas (o que están por vencer porque su evento ya empezó) no se muestran al cliente
        if (p == null || Pedido.ESTADO_VENCIDO.equals(p.getEstado())) return false;
        return !(Pedido.ESTADO_POR_APROBAR.equals(p.getEstado()) && p.getInicioMillis() <= ahora);
    }

    /** Solicitudes por aprobar y pedidos confirmados cuyo evento aún no termina. */
    public static boolean esActivo(Pedido p, long ahora) {
        if (p == null || !esVisible(p, ahora)) return false;
        return EstadosPedido.ocupaRecursos(p.getEstado()) && EstadosPedido.finBloqueo(p) > ahora;
    }

    public static List<Pedido> activos(List<Pedido> pedidos, long ahora) {
        if (pedidos == null) return new ArrayList<>();
        List<Pedido> res = new ArrayList<>();
        for (Pedido p : pedidos) {
            if (esVisible(p, ahora) && esActivo(p, ahora)) {
                res.add(p);
            }
        }
        res.sort(Comparator.comparingLong(Pedido::getInicioMillis));
        return res;
    }

    public static List<Pedido> historial(List<Pedido> pedidos, long ahora) {
        if (pedidos == null) return new ArrayList<>();
        List<Pedido> res = new ArrayList<>();
        for (Pedido p : pedidos) {
            if (esVisible(p, ahora) && !esActivo(p, ahora)) {
                res.add(p);
            }
        }
        res.sort(Comparator.comparingLong(Pedido::getInicioMillis).reversed());
        return res;
    }

    public static boolean puedeCancelar(Pedido p, long ahora) {
        return EstadosPedido.puedeCancelarse(p, ahora);
    }

    public static String etiquetaEstado(String estado) {
        if (Pedido.ESTADO_POR_APROBAR.equals(estado)) return "Por aprobar";
        if (Pedido.ESTADO_CONFIRMADO.equals(estado)) return "Confirmado";
        if (Pedido.ESTADO_POR_COBRAR.equals(estado)) return "Pago pendiente";
        if (Pedido.ESTADO_FINALIZADO.equals(estado)) return "Realizado";
        if (Pedido.ESTADO_CANCELADO.equals(estado)) return "Cancelado";
        if (Pedido.ESTADO_RECHAZADO.equals(estado)) return "No aprobado";
        if (Pedido.ESTADO_VENCIDO.equals(estado)) return "Vencido";
        return estado != null ? estado : "";
    }
}
