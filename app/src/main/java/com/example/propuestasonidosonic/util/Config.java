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

    // Constantes operativas de paquetes
    // valor provisional, validar con los socios
    public static final int DURACION_BASE_HORAS_DEFAULT = 4;
    // valor provisional, validar con los socios
    public static final int BUFFER_MONTAJE_MIN_DEFAULT = 90;
    // valor provisional, validar con los socios
    public static final int BUFFER_DESMONTAJE_MIN_DEFAULT = 120;
    // valor provisional, validar con los socios
    public static final int OPERADORES_POR_PAQUETE_MAX = 2;
    // valor provisional, validar con los socios
    public static final int MAX_EVENTOS_SIMULTANEOS = 3;

    // Motor de disponibilidad y reservas
    // valor provisional, validar con los socios
    public static final String ZONA_HORARIA = "America/Mexico_City";
    // valor provisional, validar con los socios
    public static final int PRIMER_INICIO_MIN = 540;
    // valor provisional, validar con los socios
    public static final int ULTIMO_INICIO_MIN = 1320;
    // valor provisional, validar con los socios
    public static final int PASO_HORARIO_MIN = 30;
    // valor provisional, validar con los socios
    public static final int DURACION_MIN_MIN = 60;
    // valor provisional, validar con los socios
    public static final int DURACION_MAX_MIN = 720;
    // valor provisional, validar con los socios
    public static final int PASO_DURACION_MIN = 30;
    // valor provisional, validar con los socios
    public static final int APARTADO_APP_MIN = 30;
    // valor provisional, validar con los socios
    public static final int APARTADO_MANUAL_HORAS = 24;
    // valor provisional, validar con los socios
    public static final int ANTICIPACION_REVISION_HORAS = 48;
    // valor provisional, validar con los socios
    public static final double ANTICIPO_PORCENTAJE = 0.30;
    // valor provisional, validar con los socios
    public static final boolean PREFERIR_ASISTENTES = true;
    // valor provisional, validar con los socios
    public static final int DIAS_BUSQUEDA_MAX = 60;

    // Política de reembolso y apartados
    // valor provisional, validar con los socios
    public static final int REEMBOLSO_TOTAL_DIAS = 7;
    // valor provisional, validar con los socios
    public static final int REEMBOLSO_PARCIAL_HORAS = 72;
    // valor provisional, validar con los socios
    public static final double REEMBOLSO_PARCIAL_PORCENTAJE = 0.5;
    // valor provisional, validar con los socios
    public static final int[] APARTADO_MANUAL_OPCIONES_HORAS = {2, 6, 12, 24, 48};

    // Contacto y atención
    // valor provisional, validar con los socios
    public static final String NEGOCIO_WHATSAPP = CUENTA_SOCIO_TELEFONO;

    // Checklist de equipo del asistente
    // valor provisional, validar con los socios
    public static final int CHECKLIST_ANTES_HORAS = 3;
    // valor provisional, validar con los socios
    public static final int CHECKLIST_DESPUES_HORAS = 24;
}


