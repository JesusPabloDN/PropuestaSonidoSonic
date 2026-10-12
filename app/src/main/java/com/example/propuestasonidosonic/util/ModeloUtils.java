package com.example.propuestasonidosonic.util;

import java.util.Locale;

public final class ModeloUtils {
    private ModeloUtils() {}

    public static String nombreBase(String nombre) {
        if (nombre == null) return "";
        return nombre.trim().replaceAll("\\s+", " ");
    }

    public static String clave(String categoria, String nombre) {
        String cat = categoria != null ? categoria.trim() : "";
        String base = nombreBase(nombre).toLowerCase(Locale.ROOT);
        return cat + "|" + base;
    }
}
