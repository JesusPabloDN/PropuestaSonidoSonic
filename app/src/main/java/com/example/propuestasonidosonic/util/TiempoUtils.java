package com.example.propuestasonidosonic.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class TiempoUtils {
    private TiempoUtils() {}

    private static Calendar getCalendar() {
        return Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
    }

    public static long construir(int anio, int mes0, int dia, int minutosDelDia) {
        Calendar cal = getCalendar();
        cal.clear();
        int hora = minutosDelDia / 60;
        int minuto = minutosDelDia % 60;
        cal.set(anio, mes0, dia, hora, minuto, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long inicioDelDia(long millis) {
        Calendar cal = getCalendar();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static int minutosDelDia(long millis) {
        Calendar cal = getCalendar();
        cal.setTimeInMillis(millis);
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    }

    public static String claveDia(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        sdf.setTimeZone(TimeZone.getTimeZone(Config.ZONA_HORARIA));
        return sdf.format(new Date(millis));
    }

    public static long sumarMinutos(long millis, int minutos) {
        return millis + ((long) minutos) * 60_000L;
    }

    public static boolean solapan(long aIni, long aFin, long bIni, long bFin) {
        return aIni < bFin && bIni < aFin;
    }
}
