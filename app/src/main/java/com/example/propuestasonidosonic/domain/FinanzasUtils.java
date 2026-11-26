package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.util.Config;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Finanzas de los socios a partir de los pedidos: lo que el cliente debe pagar por el paquete
 * (total del pedido) y si un socio ya confirmó que recibió ese pago.
 */
public final class FinanzasUtils {

    public static final int FILTRO_TODOS = 0;
    public static final int FILTRO_COBRADOS = 1;
    public static final int FILTRO_POR_COBRAR = 2;

    private FinanzasUtils() {}

    private static long[] limitesMes(int anio, int mes0) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        cal.clear();
        cal.set(anio, mes0, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long inicio = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, 1);
        return new long[]{inicio, cal.getTimeInMillis()};
    }

    /** Pago confirmado dentro del mes (por la fecha en que el socio lo confirmó). */
    public static boolean cobradoEnMes(Pedido p, long inicioMes, long finMes) {
        return p != null && p.isPagoConfirmado()
                && p.getFechaPagoMillis() >= inicioMes && p.getFechaPagoMillis() < finMes;
    }

    /** Pedido aceptado sin pago confirmado cuyo evento cae en el mes. */
    public static boolean porCobrarEnMes(Pedido p, long inicioMes, long finMes) {
        return p != null && EstadosPedido.montoPendiente(p) > 0
                && p.getInicioMillis() >= inicioMes && p.getInicioMillis() < finMes;
    }

    /**
     * Resumen financiero de un mes (mes0 indexado en 0) en la zona horaria del negocio.
     *
     * @return double[] { cobrado, porCobrar, cobradoEfectivo, cobradoTransferencia }
     */
    public static double[] resumenMes(List<Pedido> pedidos, int anio, int mes0) {
        long[] mes = limitesMes(anio, mes0);
        double cobrado = 0.0;
        double porCobrar = 0.0;
        double efectivo = 0.0;
        double transferencia = 0.0;

        if (pedidos != null) {
            for (Pedido p : pedidos) {
                if (cobradoEnMes(p, mes[0], mes[1])) {
                    cobrado += p.getTotal();
                    if (Pedido.METODO_TRANSFERENCIA.equals(p.getMetodoPago())) {
                        transferencia += p.getTotal();
                    } else {
                        efectivo += p.getTotal();
                    }
                } else if (porCobrarEnMes(p, mes[0], mes[1])) {
                    porCobrar += EstadosPedido.montoPendiente(p);
                }
            }
        }

        return new double[]{
                MoneyUtils.redondear2(cobrado),
                MoneyUtils.redondear2(porCobrar),
                MoneyUtils.redondear2(efectivo),
                MoneyUtils.redondear2(transferencia)
        };
    }

    /** Total cobrado en el mes (para la gráfica de ingresos). */
    public static double cobradoDelMes(List<Pedido> pedidos, int anio, int mes0) {
        return resumenMes(pedidos, anio, mes0)[0];
    }

    /**
     * Pedidos del mes según el filtro: 0 = todos, 1 = cobrados, 2 = por cobrar.
     * Primero lo pendiente (evento más antiguo arriba) y después lo cobrado (más reciente arriba).
     */
    public static List<Pedido> movimientosDelMes(List<Pedido> pedidos, int anio, int mes0, int filtro) {
        List<Pedido> porCobrar = new ArrayList<>();
        List<Pedido> cobrados = new ArrayList<>();
        if (pedidos == null) return new ArrayList<>();

        long[] mes = limitesMes(anio, mes0);
        for (Pedido p : pedidos) {
            if (cobradoEnMes(p, mes[0], mes[1])) {
                cobrados.add(p);
            } else if (porCobrarEnMes(p, mes[0], mes[1])) {
                porCobrar.add(p);
            }
        }
        Collections.sort(porCobrar, (a, b) -> Long.compare(a.getInicioMillis(), b.getInicioMillis()));
        Collections.sort(cobrados, (a, b) -> Long.compare(b.getFechaPagoMillis(), a.getFechaPagoMillis()));

        List<Pedido> resultado = new ArrayList<>();
        if (filtro != FILTRO_COBRADOS) resultado.addAll(porCobrar);
        if (filtro != FILTRO_POR_COBRAR) resultado.addAll(cobrados);
        return resultado;
    }

    /** Pedidos cuyo evento ya terminó y siguen sin pago confirmado. */
    public static List<Pedido> pedidosPorCobrar(List<Pedido> pedidos) {
        List<Pedido> lista = new ArrayList<>();
        if (pedidos == null) return lista;
        for (Pedido p : pedidos) {
            if (p != null && Pedido.ESTADO_POR_COBRAR.equals(p.getEstado()) && !p.isPagoConfirmado()) {
                lista.add(p);
            }
        }
        Collections.sort(lista, (a, b) -> Long.compare(a.getInicioMillis(), b.getInicioMillis()));
        return lista;
    }

    public static double sumaTotales(List<Pedido> pedidos) {
        double total = 0.0;
        if (pedidos != null) {
            for (Pedido p : pedidos) {
                if (p != null) total += p.getTotal();
            }
        }
        return MoneyUtils.redondear2(total);
    }
}
