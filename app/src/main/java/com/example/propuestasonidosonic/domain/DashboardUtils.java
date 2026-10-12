package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.TiempoUtils;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class DashboardUtils {

    private DashboardUtils() {}

    private static Calendar getCalendar() {
        return Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
    }

    /**
     * Calcula los 4 contadores operativos del Dashboard:
     * [0] eventosHoy: pedidos que aparecen en agenda (excluyendo apartados) con inicio hoy.
     * [1] proximos7Dias: igual, con inicio entre mañana y los 7 días siguientes inclusive.
     * [2] apartadosPorPagar: apartados vigentes (no vencidos).
     * [3] asignacionIncompleta: pedidos activos con asignacionIncompleta == true.
     *
     * @return int[] { eventosHoy, proximos7Dias, apartadosPorPagar, asignacionIncompleta }
     */
    public static int[] contadores(List<Pedido> pedidos, long ahora) {
        int eventosHoy = 0;
        int proximos7Dias = 0;
        int apartadosPorPagar = 0;
        int asignacionIncompleta = 0;

        if (pedidos == null) {
            return new int[]{0, 0, 0, 0};
        }

        long hoyInicio = TiempoUtils.inicioDelDia(ahora);
        Calendar cal = getCalendar();
        cal.setTimeInMillis(hoyInicio);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        long mananaInicio = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_MONTH, 7);
        long fin7Dias = cal.getTimeInMillis();

        for (Pedido p : pedidos) {
            if (p == null) continue;

            String st = p.getEstado();

            // 1 & 2: Eventos en agenda (no apartados)
            if (AgendaUtils.apareceEnAgenda(p, ahora) && !Pedido.ESTADO_APARTADO.equals(st)) {
                long ini = p.getInicioMillis();
                if (ini >= hoyInicio && ini < mananaInicio) {
                    eventosHoy++;
                } else if (ini >= mananaInicio && ini < fin7Dias) {
                    proximos7Dias++;
                }
            }

            // 3: Apartados por pagar (vigentes)
            if (Pedido.ESTADO_APARTADO.equals(st) && p.getVencimientoApartadoMillis() > ahora) {
                apartadosPorPagar++;
            }

            // 4: Asignación incompleta en pedidos activos
            boolean activo = Pedido.ESTADO_CONFIRMADO.equals(st)
                    || Pedido.ESTADO_PAGADO.equals(st)
                    || Pedido.ESTADO_EN_REVISION.equals(st);
            if (activo && p.isAsignacionIncompleta()) {
                asignacionIncompleta++;
            }
        }

        return new int[]{eventosHoy, proximos7Dias, apartadosPorPagar, asignacionIncompleta};
    }

    public static int contarEnRevision(List<Pedido> pedidos) {
        if (pedidos == null) return 0;
        int count = 0;
        for (Pedido p : pedidos) {
            if (p != null && Pedido.ESTADO_EN_REVISION.equals(p.getEstado())) {
                count++;
            }
        }
        return count;
    }
}
