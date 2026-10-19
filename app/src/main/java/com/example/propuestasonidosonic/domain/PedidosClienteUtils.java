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
        if (p == null) return false;
        String estado = p.getEstado();
        if (Pedido.ESTADO_VENCIDO.equals(estado)) {
            return false;
        }
        if (Pedido.ESTADO_APARTADO.equals(estado) && p.getVencimientoApartadoMillis() <= ahora) {
            return false;
        }
        return true;
    }

    public static boolean esActivo(Pedido p, long ahora) {
        if (p == null || !esVisible(p, ahora)) return false;
        String estado = p.getEstado();
        if (Pedido.ESTADO_APARTADO.equals(estado)) {
            return p.getVencimientoApartadoMillis() > ahora;
        }
        if (Pedido.ESTADO_CONFIRMADO.equals(estado)
                || Pedido.ESTADO_PAGADO.equals(estado)
                || Pedido.ESTADO_EN_REVISION.equals(estado)) {
            long fin = p.getBloqueoFinMillis() > 0 ? p.getBloqueoFinMillis() : p.getFinMillis();
            return fin > ahora;
        }
        return false;
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
        if (p == null) return false;
        if (p.getInicioMillis() <= ahora) return false;
        String estado = p.getEstado();
        if (Pedido.ESTADO_APARTADO.equals(estado)) {
            return p.getVencimientoApartadoMillis() > ahora;
        }
        return Pedido.ESTADO_CONFIRMADO.equals(estado)
                || Pedido.ESTADO_PAGADO.equals(estado)
                || Pedido.ESTADO_EN_REVISION.equals(estado);
    }

    public static boolean puedePagarAnticipo(Pedido p, long ahora) {
        if (p == null) return false;
        return Pedido.ESTADO_APARTADO.equals(p.getEstado()) && p.getVencimientoApartadoMillis() > ahora;
    }

    public static String etiquetaEstado(String estado) {
        if (Pedido.ESTADO_APARTADO.equals(estado)) return "Pendiente de pago";
        if (Pedido.ESTADO_CONFIRMADO.equals(estado) || Pedido.ESTADO_PAGADO.equals(estado)) return "Confirmado";
        if (Pedido.ESTADO_EN_REVISION.equals(estado)) return "En revisión";
        if (Pedido.ESTADO_COMPLETADO.equals(estado)) return "Realizado";
        if (Pedido.ESTADO_CANCELADO.equals(estado)) return "Cancelado";
        if (Pedido.ESTADO_RECHAZADO.equals(estado)) return "No aprobado";
        return estado != null ? estado : "";
    }

    public static String textoVencimiento(long vencimientoMillis, long ahora) {
        long diffMs = vencimientoMillis - ahora;
        if (diffMs <= 0) {
            return "Vencida";
        }
        long minutos = (long) Math.ceil(diffMs / 60000.0);
        if (minutos < 1) minutos = 1;
        return "Vence en " + minutos + " min";
    }
}
