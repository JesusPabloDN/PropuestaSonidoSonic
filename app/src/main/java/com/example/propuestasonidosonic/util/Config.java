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
    // Solicitudes con menos horas de anticipación se marcan como urgentes para el socio
    // valor provisional, validar con los socios
    public static final int ANTICIPACION_REVISION_HORAS = 48;
    // Solicitudes por aprobar que un cliente puede tener al mismo tiempo
    // valor provisional, validar con los socios
    public static final int SOLICITUDES_POR_APROBAR_MAX = 1;
    // valor provisional, validar con los socios
    public static final boolean PREFERIR_ASISTENTES = true;
    // valor provisional, validar con los socios
    public static final int DIAS_BUSQUEDA_MAX = 60;

    // Ticket de pago (impresora térmica de 58 mm, área imprimible de 54 mm)
    public static final String NEGOCIO_NOMBRE = "Sonido Sonic";
    public static final String NEGOCIO_GIRO = "Renta de audio para eventos";
    // Dirección en dos renglones para que no se corte a la mitad en el ticket de 54 mm
    public static final String NEGOCIO_DIRECCION_LINEA_1 = "San Rafael Norte Ixtapa";
    public static final String NEGOCIO_DIRECCION_LINEA_2 = "Mza 10 LT 6 CP 39016";
    public static final String NEGOCIO_DIRECCION = NEGOCIO_DIRECCION_LINEA_1 + " " + NEGOCIO_DIRECCION_LINEA_2;
    public static final String NEGOCIO_TELEFONO_VISIBLE = "747 109 0459";
    // Los datos fiscales (RFC, razón social y régimen) se agregarán al ticket cuando el negocio se registre en el SAT
    // El negocio no desglosa IVA; se muestra en el ticket como 0 %
    public static final double IVA_PORCENTAJE = 0.0;

    // Contacto y atención
    // valor provisional, validar con los socios
    public static final String NEGOCIO_WHATSAPP = "7471090459";

    // Checklist de equipo del asistente
    // valor provisional, validar con los socios
    public static final int CHECKLIST_ANTES_HORAS = 3;
    // valor provisional, validar con los socios
    public static final int CHECKLIST_DESPUES_HORAS = 24;
}


