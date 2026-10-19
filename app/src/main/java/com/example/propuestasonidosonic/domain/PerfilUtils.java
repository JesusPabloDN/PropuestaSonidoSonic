package com.example.propuestasonidosonic.domain;

import java.util.Locale;

public final class PerfilUtils {
    private PerfilUtils() {}

    /**
     * Calcula las iniciales del usuario (hasta 2 letras en mayúscula de las primeras dos palabras).
     * Ejemplos:
     * - "Socio Principal" -> "SP"
     * - "  juan   pablo   damián " -> "JP"
     * - "ángel" -> "Á"
     * - vacío o nulo -> "?"
     */
    public static String iniciales(String nombre) {
        if (nombre == null) {
            return "?";
        }
        String recortado = nombre.trim();
        if (recortado.isEmpty()) {
            return "?";
        }
        String[] palabras = recortado.split("\\s+");
        if (palabras.length == 0 || palabras[0].isEmpty()) {
            return "?";
        }
        if (palabras.length == 1) {
            return palabras[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }
        String inicial1 = palabras[0].substring(0, 1);
        String inicial2 = palabras[1].substring(0, 1);
        return (inicial1 + inicial2).toUpperCase(Locale.ROOT);
    }
}
