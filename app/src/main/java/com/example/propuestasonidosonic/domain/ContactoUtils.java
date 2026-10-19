package com.example.propuestasonidosonic.domain;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public final class ContactoUtils {

    private ContactoUtils() {}

    public static String urlWhatsApp(String telefono, String mensaje) {
        String t10 = ClienteUtils.telefono10(telefono);
        if (t10.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("https://wa.me/52").append(t10);
        if (mensaje != null && !mensaje.isEmpty()) {
            try {
                String encoded = URLEncoder.encode(mensaje, "UTF-8").replace("+", "%20");
                sb.append("?text=").append(encoded);
            } catch (UnsupportedEncodingException e) {
                // No debería ocurrir con UTF-8
            }
        }
        return sb.toString();
    }

    public static String mensajeCambioFecha(long numeroPedido, String nombreEvento) {
        String evento = nombreEvento != null ? nombreEvento : "";
        return "Hola, quiero cambiar la fecha de mi pedido #" + numeroPedido + " (" + evento + ").";
    }

    public static String mensajeCambioFecha(int numeroPedido, String nombreEvento) {
        return mensajeCambioFecha((long) numeroPedido, nombreEvento);
    }

    public static String mensajeConsulta(long numeroPedido, String nombreEvento) {
        String evento = nombreEvento != null ? nombreEvento : "";
        return "Hola, tengo una duda sobre mi pedido #" + numeroPedido + " (" + evento + ").";
    }

    public static String mensajeConsulta(int numeroPedido, String nombreEvento) {
        return mensajeConsulta((long) numeroPedido, nombreEvento);
    }
}
