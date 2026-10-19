package com.example.propuestasonidosonic.util;

import android.content.Context;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.domain.ActividadRecienteUtils;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class FormatUtils {

    private static final Locale LOCALE_MX = new Locale("es", "MX");

    private FormatUtils() {}

    public static String formatoMoneda(double monto) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(LOCALE_MX);
        return formato.format(monto);
    }

    public static String formatoFecha(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", LOCALE_MX);
        return sdf.format(new Date(millis));
    }

    public static String formatoFechaCompleta(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE d MMM yyyy", LOCALE_MX);
        return sdf.format(new Date(millis));
    }

    public static String fechaLarga(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy", LOCALE_MX);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone(Config.ZONA_HORARIA));
        return sdf.format(new Date(millis));
    }

    public static String fechaHoraCorta(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE d MMM yyyy · HH:mm", LOCALE_MX);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone(Config.ZONA_HORARIA));
        return sdf.format(new Date(millis)).replace(".", "");
    }

    public static String hora(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", LOCALE_MX);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone(Config.ZONA_HORARIA));
        return sdf.format(new Date(millis));
    }

    public static String rangoHorario(long inicioMillis, long finMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", LOCALE_MX);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone(Config.ZONA_HORARIA));
        return sdf.format(new Date(inicioMillis)) + "–" + sdf.format(new Date(finMillis));
    }

    public static String mesAnio(int anio, int mes0) {
        Calendar cal = Calendar.getInstance(java.util.TimeZone.getTimeZone(Config.ZONA_HORARIA), LOCALE_MX);
        cal.clear();
        cal.set(anio, mes0, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM 'de' yyyy", LOCALE_MX);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone(Config.ZONA_HORARIA));
        return sdf.format(cal.getTime());
    }

    public static String fechaEncabezadoMes(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE d 'de' MMMM", LOCALE_MX);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone(Config.ZONA_HORARIA));
        return sdf.format(new Date(millis));
    }

    public static String tiempoRestante(long millis) {
        if (millis <= 0) {
            return "Vencido";
        }
        long totalMin = millis / 60_000L;
        long horas = totalMin / 60;
        long minutos = totalMin % 60;
        if (horas > 0) {
            if (minutos > 0) {
                return horas + " h " + minutos + " min";
            } else {
                return horas + " h";
            }
        } else {
            return Math.max(1, minutos) + " min";
        }
    }

    public static String tiempoRestante(long vencimientoMillis, long ahoraMillis) {
        return tiempoRestante(vencimientoMillis - ahoraMillis);
    }

    public static String quitarAcentos(String texto) {
        if (texto == null) return "";
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return normalizado.replaceAll("\\p{M}", "").toLowerCase(LOCALE_MX).trim();
    }

    public static String tiempoRelativoFormateado(Context context, long ahora, long momento) {
        if (context == null) return "";
        ActividadRecienteUtils.TiempoRelativo tr = ActividadRecienteUtils.tiempoRelativo(ahora, momento);
        switch (tr.getTipo()) {
            case ActividadRecienteUtils.TiempoRelativo.AHORA:
                return context.getString(R.string.tiempo_ahora);
            case ActividadRecienteUtils.TiempoRelativo.MINUTOS:
                return context.getResources().getQuantityString(R.plurals.tiempo_minutos, (int) tr.getValor(), (int) tr.getValor());
            case ActividadRecienteUtils.TiempoRelativo.HORAS:
                return context.getResources().getQuantityString(R.plurals.tiempo_horas, (int) tr.getValor(), (int) tr.getValor());
            case ActividadRecienteUtils.TiempoRelativo.AYER:
                return context.getString(R.string.tiempo_ayer);
            case ActividadRecienteUtils.TiempoRelativo.DIAS:
                return context.getResources().getQuantityString(R.plurals.tiempo_dias, (int) tr.getValor(), (int) tr.getValor());
            case ActividadRecienteUtils.TiempoRelativo.FECHA:
            default:
                return formatoFecha(momento);
        }
    }
}

