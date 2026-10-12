package com.example.propuestasonidosonic.util;

public final class Config {
    private Config() {}

    // RN-04: Recordatorio en horas para pedidos pendientes sin respuesta
    public static final int RECORDATORIO_HORAS = 24;

    // Cuenta fija de Socio para pruebas en memoria (sin Firebase)
    public static final String CUENTA_SOCIO_CORREO = "socio@sonidosonic.com";
    public static final String CUENTA_SOCIO_CONTRASENA = "Socio#2026";
    public static final String CUENTA_SOCIO_NOMBRE = "Socio Principal";
    public static final String CUENTA_SOCIO_TELEFONO = "5551234567";
}
