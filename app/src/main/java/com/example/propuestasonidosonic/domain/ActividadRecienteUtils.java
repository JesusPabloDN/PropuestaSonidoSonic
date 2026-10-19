package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Pago;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.util.TiempoUtils;

import java.util.List;

public final class ActividadRecienteUtils {
    private ActividadRecienteUtils() {}

    public static class TiempoRelativo {
        public static final String AHORA = "AHORA";
        public static final String MINUTOS = "MINUTOS";
        public static final String HORAS = "HORAS";
        public static final String AYER = "AYER";
        public static final String DIAS = "DIAS";
        public static final String FECHA = "FECHA";

        private final String tipo;
        private final long valor;

        public TiempoRelativo(String tipo, long valor) {
            this.tipo = tipo;
            this.valor = valor;
        }

        public String getTipo() {
            return tipo;
        }

        public long getValor() {
            return valor;
        }
    }

    /**
     * El pedido creado más recientemente (desempate por número consecutivo mayor).
     * Incluye cualquier estado.
     */
    public static Pedido ultimoPedido(List<Pedido> pedidos) {
        if (pedidos == null || pedidos.isEmpty()) {
            return null;
        }
        Pedido masReciente = null;
        for (Pedido p : pedidos) {
            if (p == null) continue;
            if (masReciente == null) {
                masReciente = p;
            } else {
                if (p.getFechaCreacion() > masReciente.getFechaCreacion()) {
                    masReciente = p;
                } else if (p.getFechaCreacion() == masReciente.getFechaCreacion()) {
                    if (p.getNumero() > masReciente.getNumero()) {
                        masReciente = p;
                    }
                }
            }
        }
        return masReciente;
    }

    /**
     * De los pedidos CONFIRMADO, PAGADO o EN_REVISION cuyo finMillis sea mayor a ahora
     * (incluye el que está en curso), el de inicioMillis más cercano.
     * Excluye apartados, cancelados, rechazados, vencidos y completados.
     */
    public static Pedido proximoEvento(List<Pedido> pedidos, long ahora) {
        if (pedidos == null || pedidos.isEmpty()) {
            return null;
        }
        Pedido proximo = null;
        for (Pedido p : pedidos) {
            if (p == null) continue;
            String est = p.getEstado();
            boolean estadoValido = Pedido.ESTADO_CONFIRMADO.equals(est)
                    || Pedido.ESTADO_PAGADO.equals(est)
                    || Pedido.ESTADO_EN_REVISION.equals(est);
            if (!estadoValido) continue;
            if (p.getFinMillis() <= ahora) continue;

            if (proximo == null || p.getInicioMillis() < proximo.getInicioMillis()) {
                proximo = p;
            }
        }
        return proximo;
    }

    /**
     * El pago de fecha más reciente (incluye anticipos, abonos, pagos totales y reembolsos).
     */
    public static Pago ultimoPago(List<Pago> pagos) {
        if (pagos == null || pagos.isEmpty()) {
            return null;
        }
        Pago masReciente = null;
        for (Pago p : pagos) {
            if (p == null) continue;
            if (masReciente == null || p.getFecha() > masReciente.getFecha()) {
                masReciente = p;
            }
        }
        return masReciente;
    }

    /**
     * El equipo de fechaAlta más reciente (desempate por numeroUnidad mayor).
     */
    public static Equipo ultimoEquipoAgregado(List<Equipo> equipos) {
        if (equipos == null || equipos.isEmpty()) {
            return null;
        }
        Equipo masReciente = null;
        for (Equipo e : equipos) {
            if (e == null) continue;
            if (masReciente == null) {
                masReciente = e;
            } else {
                if (e.getFechaAlta() > masReciente.getFechaAlta()) {
                    masReciente = e;
                } else if (e.getFechaAlta() == masReciente.getFechaAlta()) {
                    if (e.getNumeroUnidad() > masReciente.getNumeroUnidad()) {
                        masReciente = e;
                    }
                }
            }
        }
        return masReciente;
    }

    /**
     * Calcula la representación de tiempo relativo entre 'ahora' y un 'momento' pasado.
     */
    public static TiempoRelativo tiempoRelativo(long ahora, long momento) {
        if (momento > ahora) {
            return new TiempoRelativo(TiempoRelativo.AHORA, 0);
        }
        long inicioAhora = TiempoUtils.inicioDelDia(ahora);
        long inicioMomento = TiempoUtils.inicioDelDia(momento);
        long diffDias = (inicioAhora - inicioMomento) / 86400000L;

        if (diffDias <= 0) {
            long diffMs = ahora - momento;
            if (diffMs < 60_000L) {
                return new TiempoRelativo(TiempoRelativo.AHORA, 0);
            }
            long minutos = diffMs / 60_000L;
            if (minutos < 60) {
                return new TiempoRelativo(TiempoRelativo.MINUTOS, minutos);
            }
            long horas = diffMs / 3600_000L;
            return new TiempoRelativo(TiempoRelativo.HORAS, horas);
        } else if (diffDias == 1) {
            return new TiempoRelativo(TiempoRelativo.AYER, 1);
        } else if (diffDias <= 7) {
            return new TiempoRelativo(TiempoRelativo.DIAS, diffDias);
        } else {
            return new TiempoRelativo(TiempoRelativo.FECHA, 0);
        }
    }
}
