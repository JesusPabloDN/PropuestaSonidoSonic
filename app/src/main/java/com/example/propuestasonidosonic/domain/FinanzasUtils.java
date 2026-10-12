package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.model.Pago;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.util.Config;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class FinanzasUtils {

    public static final int FILTRO_TODOS = 0;
    public static final int FILTRO_INGRESOS = 1;
    public static final int FILTRO_REEMBOLSOS = 2;

    private FinanzasUtils() {}

    private static Calendar getCalendar() {
        return Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
    }

    /**
     * Resumen financiero de un mes (anio, mes0 indexado en 0).
     * Límites del mes: [inicio del mes, inicio del mes siguiente) en la zona horaria del negocio.
     *
     * @return double[] { cobrado, reembolsos, neto, porCobrar }
     */
    public static double[] resumenMes(List<Pago> pagos, List<Pedido> pedidos, int anio, int mes0) {
        Calendar cal = getCalendar();
        cal.clear();
        cal.set(anio, mes0, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long inicioMes = cal.getTimeInMillis();

        cal.add(Calendar.MONTH, 1);
        long finMes = cal.getTimeInMillis();

        double cobrado = 0.0;
        double reembolsos = 0.0;

        if (pagos != null) {
            for (Pago p : pagos) {
                if (p == null) continue;
                long f = p.getFecha();
                if (f >= inicioMes && f < finMes) {
                    String tipo = p.getTipo();
                    if (Pago.TIPO_ANTICIPO.equals(tipo) || Pago.TIPO_ABONO.equals(tipo) || Pago.TIPO_TOTAL.equals(tipo)) {
                        cobrado += p.getMonto();
                    } else if (Pago.TIPO_REEMBOLSO.equals(tipo)) {
                        reembolsos += p.getMonto();
                    }
                }
            }
        }

        cobrado = MoneyUtils.redondear2(cobrado);
        reembolsos = MoneyUtils.redondear2(reembolsos);
        double neto = MoneyUtils.redondear2(cobrado - reembolsos);

        double porCobrar = 0.0;
        if (pedidos != null) {
            for (Pedido ped : pedidos) {
                if (ped == null) continue;
                String st = ped.getEstado();
                boolean valido = Pedido.ESTADO_CONFIRMADO.equals(st)
                        || Pedido.ESTADO_PAGADO.equals(st)
                        || Pedido.ESTADO_EN_REVISION.equals(st)
                        || Pedido.ESTADO_COMPLETADO.equals(st);
                if (valido) {
                    long ini = ped.getInicioMillis();
                    if (ini >= inicioMes && ini < finMes) {
                        porCobrar += ped.getSaldoPendiente();
                    }
                }
            }
        }
        porCobrar = MoneyUtils.redondear2(porCobrar);

        return new double[]{cobrado, reembolsos, neto, porCobrar};
    }

    /**
     * Lista de movimientos del mes según el filtro:
     * 0 = todos, 1 = ingresos, 2 = reembolsos.
     * Ordenados por fecha descendente (desempate por folio descendente).
     */
    public static List<Pago> movimientosDelMes(List<Pago> pagos, int anio, int mes0, int filtro) {
        List<Pago> resultado = new ArrayList<>();
        if (pagos == null) return resultado;

        Calendar cal = getCalendar();
        cal.clear();
        cal.set(anio, mes0, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long inicioMes = cal.getTimeInMillis();

        cal.add(Calendar.MONTH, 1);
        long finMes = cal.getTimeInMillis();

        for (Pago p : pagos) {
            if (p == null) continue;
            long f = p.getFecha();
            if (f >= inicioMes && f < finMes) {
                String tipo = p.getTipo();
                boolean esIngreso = Pago.TIPO_ANTICIPO.equals(tipo)
                        || Pago.TIPO_ABONO.equals(tipo)
                        || Pago.TIPO_TOTAL.equals(tipo);
                boolean esReembolso = Pago.TIPO_REEMBOLSO.equals(tipo);

                if (filtro == FILTRO_TODOS) {
                    resultado.add(p);
                } else if (filtro == FILTRO_INGRESOS && esIngreso) {
                    resultado.add(p);
                } else if (filtro == FILTRO_REEMBOLSOS && esReembolso) {
                    resultado.add(p);
                }
            }
        }

        Collections.sort(resultado, new Comparator<Pago>() {
            @Override
            public int compare(Pago o1, Pago o2) {
                int cmp = Long.compare(o2.getFecha(), o1.getFecha());
                if (cmp != 0) return cmp;
                String f1 = o1.getFolio() != null ? o1.getFolio() : "";
                String f2 = o2.getFolio() != null ? o2.getFolio() : "";
                return f2.compareTo(f1);
            }
        });

        return resultado;
    }
}
