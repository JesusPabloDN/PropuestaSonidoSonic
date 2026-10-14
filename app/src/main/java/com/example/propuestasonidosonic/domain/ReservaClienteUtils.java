package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.model.Paquete;
import com.example.propuestasonidosonic.util.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ReservaClienteUtils {

    private ReservaClienteUtils() {}

    public static boolean esUrgente(long inicioMillis, long ahora) {
        long anticipacionMillis = ((long) Config.ANTICIPACION_REVISION_HORAS) * 3600_000L;
        return inicioMillis > ahora && (inicioMillis - ahora) < anticipacionMillis;
    }

    public static long segundosRestantes(long vencimientoMillis, long ahora) {
        if (vencimientoMillis <= ahora) {
            return 0L;
        }
        return (vencimientoMillis - ahora) / 1000L;
    }

    public static String formatoCuentaRegresiva(long segundos) {
        if (segundos < 0) {
            segundos = 0;
        }
        long minutos = segundos / 60;
        long segs = segundos % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutos, segs);
    }

    public static String mensajeAmigable(String motivoONoViable) {
        if (motivoONoViable == null) {
            return "Ese horario ya no está disponible. Elige otro, por favor.";
        }

        // Si es un error de validación de campos del formulario o anti-abuso, conservarlo tal cual
        if (motivoONoViable.contains("nombre")
                || motivoONoViable.contains("teléfono")
                || motivoONoViable.contains("dirección")
                || motivoONoViable.contains("pendiente de pago")
                || motivoONoViable.contains("obligatorio")
                || motivoONoViable.contains("caracteres")
                || motivoONoViable.contains("dígitos")) {
            return motivoONoViable;
        }

        // Mapeo amigable para los motivos reales del motor
        if (ResultadoDisponibilidad.PAQUETE_INACTIVO.equals(motivoONoViable)
                || ResultadoDisponibilidad.PAQUETE_INCOMPLETO.equals(motivoONoViable)) {
            return "Este paquete no está disponible temporalmente. Elige otro, por favor.";
        }
        if (ResultadoDisponibilidad.DURACION_INVALIDA.equals(motivoONoViable)) {
            return "La duración seleccionada no es válida.";
        }
        if (ResultadoDisponibilidad.EN_EL_PASADO.equals(motivoONoViable)) {
            return "La fecha u horario seleccionado ya pasó. Elige una fecha futura, por favor.";
        }
        if (ResultadoDisponibilidad.FUERA_DE_HORARIO.equals(motivoONoViable)) {
            return "Ese horario ya no está disponible. Elige otro, por favor.";
        }
        if (ResultadoDisponibilidad.SIN_STOCK.equals(motivoONoViable)
                || ResultadoDisponibilidad.MAXIMO_SIMULTANEOS.equals(motivoONoViable)
                || ResultadoDisponibilidad.SIN_OPERADORES.equals(motivoONoViable)) {
            return "Ese horario ya no está disponible. Elige otro, por favor.";
        }

        // Cualquier otro texto desconocido / detalle interno
        return "Ese horario ya no está disponible. Elige otro, por favor.";
    }

    public static List<Paquete> paquetesVisibles(List<Paquete> paquetes) {
        List<Paquete> lista = new ArrayList<>();
        if (paquetes == null) return lista;

        for (Paquete p : paquetes) {
            if (p != null && p.isActivo()) {
                lista.add(p);
            }
        }

        Collections.sort(lista, new Comparator<Paquete>() {
            @Override
            public int compare(Paquete p1, Paquete p2) {
                int cPrecio = Double.compare(p1.getPrecio(), p2.getPrecio());
                if (cPrecio != 0) return cPrecio;
                String n1 = p1.getNombre() != null ? p1.getNombre() : "";
                String n2 = p2.getNombre() != null ? p2.getNombre() : "";
                return n1.compareToIgnoreCase(n2);
            }
        });

        return lista;
    }
}
