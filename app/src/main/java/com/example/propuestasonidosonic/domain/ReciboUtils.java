package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.model.Pago;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ReciboUtils {

    private ReciboUtils() {}

    private static class PagoIndexed {
        final Pago pago;
        final int index;

        PagoIndexed(Pago pago, int index) {
            this.pago = pago;
            this.index = index;
        }
    }

    private static List<PagoIndexed> ordenarPagos(List<Pago> pagos) {
        List<PagoIndexed> lista = new ArrayList<>();
        if (pagos != null) {
            for (int i = 0; i < pagos.size(); i++) {
                Pago p = pagos.get(i);
                if (p != null) {
                    lista.add(new PagoIndexed(p, i));
                }
            }
        }
        Collections.sort(lista, new Comparator<PagoIndexed>() {
            @Override
            public int compare(PagoIndexed o1, PagoIndexed o2) {
                int cmp = Long.compare(o1.pago.getFecha(), o2.pago.getFecha());
                if (cmp != 0) return cmp;
                return Integer.compare(o1.index, o2.index);
            }
        });
        return lista;
    }

    /**
     * Calcula el neto pagado (ingresos - reembolsos) hasta el pago indicado inclusive.
     */
    public static double totalPagadoHasta(List<Pago> pagosDelPedido, String pagoId) {
        List<PagoIndexed> ordenados = ordenarPagos(pagosDelPedido);
        double acumulado = 0.0;
        for (PagoIndexed pi : ordenados) {
            Pago p = pi.pago;
            String tipo = p.getTipo();
            if (Pago.TIPO_ANTICIPO.equals(tipo) || Pago.TIPO_ABONO.equals(tipo) || Pago.TIPO_TOTAL.equals(tipo)) {
                acumulado += p.getMonto();
            } else if (Pago.TIPO_REEMBOLSO.equals(tipo)) {
                acumulado -= p.getMonto();
            }
            if (pagoId != null && pagoId.equals(p.getId())) {
                break;
            }
        }
        return MoneyUtils.redondear2(acumulado);
    }

    /**
     * Calcula el saldo restante después del pago indicado inclusive: max(0, totalPedido - neto pagado).
     */
    public static double saldoDespuesDe(double totalPedido, List<Pago> pagosDelPedido, String pagoId) {
        double neto = totalPagadoHasta(pagosDelPedido, pagoId);
        double saldo = totalPedido - neto;
        if (saldo < 0) {
            saldo = 0.0;
        }
        return MoneyUtils.redondear2(saldo);
    }

    /**
     * Título en español según el tipo de pago.
     */
    public static String tituloRecibo(String tipo) {
        if (Pago.TIPO_ANTICIPO.equals(tipo)) {
            return "Recibo de anticipo";
        } else if (Pago.TIPO_ABONO.equals(tipo)) {
            return "Recibo de abono";
        } else if (Pago.TIPO_TOTAL.equals(tipo)) {
            return "Recibo de pago total";
        } else if (Pago.TIPO_REEMBOLSO.equals(tipo)) {
            return "Comprobante de reembolso";
        }
        return "Recibo de pago";
    }
}
