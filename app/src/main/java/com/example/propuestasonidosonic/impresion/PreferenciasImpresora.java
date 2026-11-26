package com.example.propuestasonidosonic.impresion;

import android.content.Context;
import android.content.SharedPreferences;

/** Recuerda en cada teléfono la impresora que eligió el socio. */
public final class PreferenciasImpresora {

    private static final String ARCHIVO = "sonido_sonic_impresora";
    private static final String KEY_DIRECCION = "direccion";
    private static final String KEY_NOMBRE = "nombre";

    private final SharedPreferences prefs;

    public PreferenciasImpresora(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    public String getDireccion() {
        return prefs.getString(KEY_DIRECCION, null);
    }

    public String getNombre() {
        return prefs.getString(KEY_NOMBRE, null);
    }

    public boolean hayImpresora() {
        return getDireccion() != null;
    }

    public void guardar(String direccion, String nombre) {
        prefs.edit().putString(KEY_DIRECCION, direccion).putString(KEY_NOMBRE, nombre).apply();
    }

    public void olvidar() {
        prefs.edit().remove(KEY_DIRECCION).remove(KEY_NOMBRE).apply();
    }
}
