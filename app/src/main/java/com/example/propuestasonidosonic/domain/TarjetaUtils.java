package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.util.Config;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class TarjetaUtils {

    private TarjetaUtils() {}

    public static String soloDigitos(String s) {
        if (s == null) return "";
        return s.replaceAll("\\D", "");
    }

    public static String marca(String numero) {
        String d = soloDigitos(numero);
        if (d.startsWith("4")) {
            return "VISA";
        }
        if (d.startsWith("34") || d.startsWith("37")) {
            return "AMEX";
        }
        if (d.length() >= 2) {
            try {
                int prefix2 = Integer.parseInt(d.substring(0, 2));
                if (prefix2 >= 51 && prefix2 <= 55) {
                    return "MASTERCARD";
                }
            } catch (NumberFormatException ignored) {}
        }
        if (d.length() >= 4) {
            try {
                int prefix4 = Integer.parseInt(d.substring(0, 4));
                if (prefix4 >= 2221 && prefix4 <= 2720) {
                    return "MASTERCARD";
                }
            } catch (NumberFormatException ignored) {}
        }
        return "DESCONOCIDA";
    }

    public static String formatearNumero(String numero) {
        String d = soloDigitos(numero);
        if (d.isEmpty()) return "";
        String m = marca(d);
        StringBuilder sb = new StringBuilder();
        if ("AMEX".equals(m)) {
            // Amex: 4-6-5
            int len = Math.min(d.length(), 15);
            for (int i = 0; i < len; i++) {
                if (i == 4 || i == 10) {
                    sb.append(' ');
                }
                sb.append(d.charAt(i));
            }
        } else {
            // Grupos de 4
            int len = Math.min(d.length(), 16);
            for (int i = 0; i < len; i++) {
                if (i > 0 && i % 4 == 0) {
                    sb.append(' ');
                }
                sb.append(d.charAt(i));
            }
        }
        return sb.toString();
    }

    public static boolean luhnValido(String numero) {
        String d = soloDigitos(numero);
        if (d.isEmpty()) return false;
        int suma = 0;
        boolean alternar = false;
        for (int i = d.length() - 1; i >= 0; i--) {
            int n = d.charAt(i) - '0';
            if (alternar) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            suma += n;
            alternar = !alternar;
        }
        return (suma % 10 == 0);
    }

    public static boolean numeroValido(String numero) {
        String d = soloDigitos(numero);
        if (d.isEmpty()) return false;
        String m = marca(d);
        if ("AMEX".equals(m)) {
            if (d.length() != 15) return false;
        } else {
            if (d.length() != 16) return false;
        }
        return luhnValido(d);
    }

    public static boolean vencimientoValido(int mes, int anio2d, long ahoraMillis) {
        if (mes < 1 || mes > 12) return false;
        if (anio2d < 0 || anio2d > 99) return false;

        int anio4d = 2000 + anio2d;
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        cal.set(Calendar.YEAR, anio4d);
        cal.set(Calendar.MONTH, mes - 1);
        int ultimoDia = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.set(Calendar.DAY_OF_MONTH, ultimoDia);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);

        return cal.getTimeInMillis() >= ahoraMillis;
    }

    public static boolean cvvValido(String cvv, String marca) {
        if (cvv == null) return false;
        String d = soloDigitos(cvv);
        if (d.length() != cvv.trim().length()) return false;
        if ("AMEX".equals(marca)) {
            return d.length() == 4;
        } else {
            return d.length() == 3;
        }
    }

    public static boolean titularValido(String titular) {
        if (titular == null) return false;
        String t = titular.trim();
        if (t.length() < 3) return false;
        return t.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$");
    }
}
