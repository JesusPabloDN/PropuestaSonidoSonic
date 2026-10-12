package com.example.propuestasonidosonic.util;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
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

    public static String quitarAcentos(String texto) {
        if (texto == null) return "";
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return normalizado.replaceAll("\\p{M}", "").toLowerCase(LOCALE_MX).trim();
    }
}
