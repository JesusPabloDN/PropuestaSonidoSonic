package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.TiempoUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class DisponibilidadAsistenteUtils {

    private DisponibilidadAsistenteUtils() {}

    private static Calendar getCalendar() {
        return Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
    }

    public static boolean puedeMarcar(String fechaClave, long ahora) {
        if (fechaClave == null || fechaClave.length() != 10) {
            return false;
        }

        try {
            int anio = Integer.parseInt(fechaClave.substring(0, 4));
            int mes = Integer.parseInt(fechaClave.substring(5, 7)) - 1;
            int dia = Integer.parseInt(fechaClave.substring(8, 10));

            Calendar cal = getCalendar();
            cal.clear();
            cal.set(anio, mes, dia, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long inicioDiaClave = cal.getTimeInMillis();

            long hoyInicio = TiempoUtils.inicioDelDia(ahora);
            return inicioDiaClave >= hoyInicio;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean puedeDesmarcar(String fechaClave, long ahora, List<Pedido> eventosActivosDelAsistente) {
        if (!puedeMarcar(fechaClave, ahora)) {
            return false;
        }

        if (eventosActivosDelAsistente != null) {
            for (Pedido p : eventosActivosDelAsistente) {
                if (p == null) continue;
                String st = p.getEstado();
                if (Pedido.ESTADO_CANCELADO.equals(st)
                        || Pedido.ESTADO_RECHAZADO.equals(st)
                        || Pedido.ESTADO_VENCIDO.equals(st)) {
                    continue;
                }
                // Si el evento inicia en esta fecha
                if (fechaClave.equals(TiempoUtils.claveDia(p.getInicioMillis()))) {
                    return false;
                }
            }
        }

        return true;
    }

    public static List<String> clavesFinesDeSemana(int anio, int mes0, long ahora) {
        List<String> resultado = new ArrayList<>();
        Calendar cal = getCalendar();
        cal.clear();
        cal.set(anio, mes0, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int maxDia = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int d = 1; d <= maxDia; d++) {
            cal.set(Calendar.DAY_OF_MONTH, d);
            int diaSemana = cal.get(Calendar.DAY_OF_WEEK);
            if (diaSemana == Calendar.FRIDAY || diaSemana == Calendar.SATURDAY || diaSemana == Calendar.SUNDAY) {
                String clave = String.format(Locale.ROOT, "%04d-%02d-%02d", anio, mes0 + 1, d);
                if (puedeMarcar(clave, ahora)) {
                    resultado.add(clave);
                }
            }
        }

        Collections.sort(resultado);
        return resultado;
    }

    public static List<String> clavesDelMes(int anio, int mes0) {
        List<String> resultado = new ArrayList<>();
        Calendar cal = getCalendar();
        cal.clear();
        cal.set(anio, mes0, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int maxDia = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int d = 1; d <= maxDia; d++) {
            resultado.add(String.format(Locale.ROOT, "%04d-%02d-%02d", anio, mes0 + 1, d));
        }
        return resultado;
    }
}
