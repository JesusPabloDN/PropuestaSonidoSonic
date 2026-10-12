package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.util.Config;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PoliticaCancelacion {

    private PoliticaCancelacion() {}

    /**
     * Calcula el monto de reembolso de acuerdo con las políticas del negocio.
     *
     * @param anticipoPagado monto pagado previamente
     * @param inicioMillis   fecha y hora de inicio del evento
     * @param ahora          marca de tiempo actual
     * @param canceladoPor   Pedido.CANCELA_SOCIO o Pedido.CANCELA_CLIENTE
     * @return monto a reembolsar redondeado a 2 decimales
     */
    public static double calcularReembolso(double anticipoPagado, long inicioMillis, long ahora, String canceladoPor) {
        if (anticipoPagado <= 0) {
            return 0.0;
        }

        if (Pedido.CANCELA_SOCIO.equals(canceladoPor)) {
            return redondear2Decimales(anticipoPagado);
        }

        if (Pedido.CANCELA_CLIENTE.equals(canceladoPor)) {
            long diferenciaMs = inicioMillis - ahora;
            if (diferenciaMs <= 0) {
                return 0.0;
            }

            double horas = (double) diferenciaMs / 3600_000.0;
            double porcentaje;

            if (horas >= Config.REEMBOLSO_TOTAL_DIAS * 24) {
                porcentaje = 1.0;
            } else if (horas >= Config.REEMBOLSO_PARCIAL_HORAS) {
                porcentaje = Config.REEMBOLSO_PARCIAL_PORCENTAJE;
            } else {
                porcentaje = 0.0;
            }

            return redondear2Decimales(anticipoPagado * porcentaje);
        }

        return 0.0;
    }

    private static double redondear2Decimales(double valor) {
        return BigDecimal.valueOf(valor)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
