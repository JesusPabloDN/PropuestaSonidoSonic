package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.util.Config;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Contenido del ticket de pago, en el orden de un ticket estándar de impresora térmica:
 * encabezado del negocio, datos de la operación, concepto, totales con IVA, forma de pago y pie.
 * No depende de Android para poder probarse; el dibujo lo hace util.TicketPdf.
 */
public final class TicketPago {

    public static final int TIPO_TITULO = 0;      // centrado, negritas, más grande
    public static final int TIPO_CENTRADO = 1;    // centrado
    public static final int TIPO_TEXTO = 2;       // alineado a la izquierda, con salto de línea
    public static final int TIPO_COLUMNAS = 3;    // texto a la izquierda, importe a la derecha
    public static final int TIPO_TOTAL = 4;       // columnas en negritas y más grande
    public static final int TIPO_SEPARADOR = 5;   // línea punteada
    public static final int TIPO_NOTA = 6;        // centrado, letra pequeña

    private static final Locale LOCALE_MX = new Locale("es", "MX");

    public static final class Linea {
        public final int tipo;
        public final String izquierda;
        public final String derecha;
        public final boolean negritas;

        Linea(int tipo, String izquierda, String derecha, boolean negritas) {
            this.tipo = tipo;
            this.izquierda = izquierda != null ? izquierda : "";
            this.derecha = derecha != null ? derecha : "";
            this.negritas = negritas;
        }
    }

    private TicketPago() {}

    /** Folio de 6 dígitos a partir del número consecutivo del pedido. */
    public static String folio(Pedido p) {
        return String.format(Locale.ROOT, "%06d", p != null ? p.getNumero() : 0L);
    }

    public static String nombreArchivo(Pedido p) {
        return "ticket_" + folio(p) + ".pdf";
    }

    /** Importe sin IVA. Con IVA de 0 % coincide con el total. */
    public static double subtotal(double total) {
        return MoneyUtils.redondear2(total / (1.0 + Config.IVA_PORCENTAJE));
    }

    public static double iva(double total) {
        return MoneyUtils.redondear2(total - subtotal(total));
    }

    public static List<Linea> construir(Pedido p) {
        List<Linea> lineas = new ArrayList<>();
        if (p == null) return lineas;

        double total = MoneyUtils.redondear2(p.getTotal());
        long fechaPago = p.getFechaPagoMillis() > 0 ? p.getFechaPagoMillis() : p.getFechaActualizacion();
        int ivaPct = (int) Math.round(Config.IVA_PORCENTAJE * 100);

        // Encabezado (el logo lo dibuja el renderizador antes de estas líneas)
        lineas.add(new Linea(TIPO_TITULO, Config.NEGOCIO_NOMBRE.toUpperCase(LOCALE_MX), null, true));
        lineas.add(new Linea(TIPO_CENTRADO, Config.NEGOCIO_GIRO, null, false));
        lineas.add(new Linea(TIPO_CENTRADO, Config.NEGOCIO_DIRECCION_LINEA_1, null, false));
        lineas.add(new Linea(TIPO_CENTRADO, Config.NEGOCIO_DIRECCION_LINEA_2, null, false));
        lineas.add(new Linea(TIPO_CENTRADO, "Tel. " + Config.NEGOCIO_TELEFONO_VISIBLE, null, false));
        lineas.add(separador());

        // Datos de la operación: fecha y hora en que se confirmó el pago en la app
        lineas.add(new Linea(TIPO_CENTRADO, "TICKET DE PAGO", null, true));
        lineas.add(new Linea(TIPO_COLUMNAS, "Folio:", folio(p), false));
        lineas.add(new Linea(TIPO_COLUMNAS, "Fecha: " + formatear("dd/MM/yyyy", fechaPago),
                "Hora: " + formatear("HH:mm", fechaPago), false));
        lineas.add(separador());

        lineas.add(new Linea(TIPO_TEXTO, "Cliente: " + texto(p.getClienteNombre()), null, false));
        lineas.add(new Linea(TIPO_TEXTO, "Evento: " + texto(p.getNombreEvento()), null, false));
        lineas.add(new Linea(TIPO_TEXTO, "Lugar: " + texto(p.getDireccion()), null, false));
        if (p.getInicioMillis() > 0) {
            lineas.add(new Linea(TIPO_COLUMNAS, "Fecha del evento:",
                    formatear("dd/MM/yyyy", p.getInicioMillis()), false));
            lineas.add(new Linea(TIPO_COLUMNAS, "Hora de inicio:",
                    formatear("HH:mm", p.getInicioMillis()), false));
        }
        lineas.add(separador());

        // Concepto
        lineas.add(new Linea(TIPO_COLUMNAS, "CANT DESCRIPCIÓN", "IMPORTE", true));
        lineas.add(new Linea(TIPO_TEXTO, "1  " + texto(p.getPaqueteNombre()), null, false));
        lineas.add(new Linea(TIPO_COLUMNAS, "   " + duracion(p), moneda(subtotal(total)), false));
        lineas.add(separador());

        // Totales
        lineas.add(new Linea(TIPO_COLUMNAS, "SUBTOTAL", moneda(subtotal(total)), false));
        lineas.add(new Linea(TIPO_COLUMNAS, "IVA (" + ivaPct + "%)", moneda(iva(total)), false));
        lineas.add(new Linea(TIPO_TOTAL, "TOTAL", moneda(total), true));
        lineas.add(separador());

        // Forma de pago y quién lo recibió
        lineas.add(new Linea(TIPO_COLUMNAS, "Forma de pago:", EstadosPedido.etiquetaMetodo(p.getMetodoPago()), false));
        lineas.add(new Linea(TIPO_COLUMNAS, "Pagado:", moneda(total), false));
        if (p.getPagoConfirmadoPorNombre() != null && !p.getPagoConfirmadoPorNombre().trim().isEmpty()) {
            lineas.add(new Linea(TIPO_TEXTO, "Recibió: " + p.getPagoConfirmadoPorNombre().trim(), null, false));
        }
        lineas.add(separador());

        // Pie
        lineas.add(new Linea(TIPO_CENTRADO, "¡Gracias por su preferencia!", null, true));
        lineas.add(new Linea(TIPO_NOTA, "Este ticket no es un comprobante fiscal.", null, false));
        return lineas;
    }

    private static Linea separador() {
        return new Linea(TIPO_SEPARADOR, null, null, false);
    }

    private static String texto(String valor) {
        return valor != null && !valor.trim().isEmpty() ? valor.trim() : "-";
    }

    private static String duracion(Pedido p) {
        long min = Math.max(0L, (p.getFinMillis() - p.getInicioMillis()) / 60_000L);
        if (min <= 0) return "Servicio";
        long h = min / 60;
        long resto = min % 60;
        return resto > 0 ? h + " h " + resto + " min" : h + " h de servicio";
    }

    public static String moneda(double monto) {
        return NumberFormat.getCurrencyInstance(LOCALE_MX).format(monto);
    }

    private static String formatear(String patron, long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat(patron, LOCALE_MX);
        sdf.setTimeZone(TimeZone.getTimeZone(Config.ZONA_HORARIA));
        return sdf.format(new Date(millis));
    }
}
