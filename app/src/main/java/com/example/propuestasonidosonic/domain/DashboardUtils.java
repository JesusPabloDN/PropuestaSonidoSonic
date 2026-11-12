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

    /** Índice del día actual dentro de la semana, con el lunes como 0 y el domingo como 6. */
    public static int indiceDiaSemana(long ahora) {
        Calendar cal = getCalendar();
        cal.setTimeInMillis(ahora);
        return (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
    }

    /**
     * Cuenta los eventos programados para cada día de la semana actual (lunes a domingo).
     * Solo cuenta pedidos que aparecen en la agenda, por lo que excluye cancelados,
     * rechazados y apartados vencidos.
     */
    public static int[] eventosPorDiaSemana(List<Pedido> pedidos, long ahora) {
        int[] conteo = new int[7];
        if (pedidos == null) return conteo;

        Calendar cal = getCalendar();
        cal.setTimeInMillis(TiempoUtils.inicioDelDia(ahora));
        cal.add(Calendar.DAY_OF_MONTH, -indiceDiaSemana(ahora));
        long[] limites = new long[8];
        for (int i = 0; i < 8; i++) {
            limites[i] = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        for (Pedido p : pedidos) {
            if (p == null || !AgendaUtils.apareceEnAgenda(p, ahora)) continue;
            long ini = p.getInicioMillis();
            for (int i = 0; i < 7; i++) {
                if (ini >= limites[i] && ini < limites[i + 1]) {
                    conteo[i]++;
                    break;
                }
            }
        }
        return conteo;
    }

    /** Paquete y número de veces que se ha rentado. */
    public static class ConteoPaquete {
        public final String nombre;
        public final int cantidad;

        ConteoPaquete(String nombre, int cantidad) {
            this.nombre = nombre;
            this.cantidad = cantidad;
        }
    }

    /**
     * Paquetes con más pedidos cuyo evento ocurre a partir de {@code desdeMillis}.
     * Excluye cancelados, rechazados y apartados vencidos.
     */
    public static List<ConteoPaquete> paquetesMasRentados(List<Pedido> pedidos, long desdeMillis, long ahora, int limite) {
        java.util.Map<String, Integer> conteo = new java.util.LinkedHashMap<>();
        if (pedidos != null) {
            for (Pedido p : pedidos) {
                if (p == null || !AgendaUtils.apareceEnAgenda(p, ahora)) continue;
                if (p.getInicioMillis() < desdeMillis) continue;
                String nombre = p.getPaqueteNombre();
                if (nombre == null || nombre.trim().isEmpty()) nombre = "Sin paquete";
                Integer actual = conteo.get(nombre);
                conteo.put(nombre, actual == null ? 1 : actual + 1);
            }
        }
        List<ConteoPaquete> lista = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Integer> e : conteo.entrySet()) {
            lista.add(new ConteoPaquete(e.getKey(), e.getValue()));
        }
        java.util.Collections.sort(lista, (a, b) -> Integer.compare(b.cantidad, a.cantidad));
        return lista.size() > limite ? new java.util.ArrayList<>(lista.subList(0, limite)) : lista;
    }

    /** Apartados vigentes ordenados por su vencimiento más cercano. */
    public static List<Pedido> apartadosVigentes(List<Pedido> pedidos, long ahora) {
        List<Pedido> lista = new java.util.ArrayList<>();
        if (pedidos == null) return lista;
        for (Pedido p : pedidos) {
            if (p != null && Pedido.ESTADO_APARTADO.equals(p.getEstado()) && p.getVencimientoApartadoMillis() > ahora) {
                lista.add(p);
            }
        }
        java.util.Collections.sort(lista, (a, b) -> Long.compare(a.getVencimientoApartadoMillis(), b.getVencimientoApartadoMillis()));
        return lista;
    }
}
