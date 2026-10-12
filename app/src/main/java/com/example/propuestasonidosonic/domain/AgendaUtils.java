package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.TiempoUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

public final class AgendaUtils {

    private AgendaUtils() {}

    private static Calendar getCalendar() {
        return Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
    }

    /**
     * Determina si un pedido debe aparecer en la agenda:
     * - CONFIRMADO, PAGADO, EN_REVISION y COMPLETADO: siempre true
     * - APARTADO: true solo si su vencimiento es estrictamente posterior a 'ahora'
     * - Cualquier otro estado: false
     */
    public static boolean apareceEnAgenda(Pedido p, long ahora) {
        if (p == null) return false;
        String st = p.getEstado();
        if (st == null) return false;

        switch (st) {
            case Pedido.ESTADO_CONFIRMADO:
            case Pedido.ESTADO_PAGADO:
            case Pedido.ESTADO_EN_REVISION:
            case Pedido.ESTADO_COMPLETADO:
                return true;
            case Pedido.ESTADO_APARTADO:
                return p.getVencimientoApartadoMillis() > ahora;
            default:
                return false;
        }
    }

    /**
     * Agrupa pedidos por día local de inicio (yyyy-MM-dd), ordenando cada día por inicio ascendente.
     * Solo incluye pedidos que cumplen apareceEnAgenda(p, ahora).
     */
    public static Map<String, List<Pedido>> agruparPorDia(List<Pedido> pedidos, long ahora) {
        Map<String, List<Pedido>> mapa = new TreeMap<>();
        if (pedidos == null) return mapa;

        for (Pedido p : pedidos) {
            if (apareceEnAgenda(p, ahora)) {
                String clave = TiempoUtils.claveDia(p.getInicioMillis());
                List<Pedido> lista = mapa.get(clave);
                if (lista == null) {
                    lista = new ArrayList<>();
                    mapa.put(clave, lista);
                }
                lista.add(p);
            }
        }

        Comparator<Pedido> comp = (o1, o2) -> Long.compare(o1.getInicioMillis(), o2.getInicioMillis());
        for (List<Pedido> lista : mapa.values()) {
            Collections.sort(lista, comp);
        }

        return mapa;
    }

    /**
     * Devuelve los pedidos que aparecen en agenda y cuyo inicio cae en el mes (anio, mes0),
     * ordenados por inicioMillis ascendente.
     */
    public static List<Pedido> delMes(List<Pedido> pedidos, int anio, int mes0, long ahora) {
        List<Pedido> resultado = new ArrayList<>();
        if (pedidos == null) return resultado;

        Calendar cal = getCalendar();
        cal.clear();
        cal.set(anio, mes0, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long inicioMes = cal.getTimeInMillis();

        cal.add(Calendar.MONTH, 1);
        long finMes = cal.getTimeInMillis();

        for (Pedido p : pedidos) {
            if (apareceEnAgenda(p, ahora)) {
                long ini = p.getInicioMillis();
                if (ini >= inicioMes && ini < finMes) {
                    resultado.add(p);
                }
            }
        }

        Collections.sort(resultado, (o1, o2) -> Long.compare(o1.getInicioMillis(), o2.getInicioMillis()));
        return resultado;
    }

    /**
     * Devuelve {eventosConfirmados, apartados} para los pedidos de un día:
     * - eventosConfirmados cuenta CONFIRMADO, PAGADO, EN_REVISION y COMPLETADO
     * - apartados cuenta APARTADO
     */
    public static int[] marcadoresDia(List<Pedido> delDia) {
        int confirmados = 0;
        int apartados = 0;
        if (delDia != null) {
            for (Pedido p : delDia) {
                if (p == null) continue;
                String st = p.getEstado();
                if (Pedido.ESTADO_APARTADO.equals(st)) {
                    apartados++;
                } else if (Pedido.ESTADO_CONFIRMADO.equals(st)
                        || Pedido.ESTADO_PAGADO.equals(st)
                        || Pedido.ESTADO_EN_REVISION.equals(st)
                        || Pedido.ESTADO_COMPLETADO.equals(st)) {
                    confirmados++;
                }
            }
        }
        return new int[]{confirmados, apartados};
    }

    /**
     * Genera siempre 42 celdas consecutivas (6 semanas × 7 días), cada una representando
     * el inicio del día en milisegundos en la zona horaria del negocio,
     * iniciando en el lunes de la semana que contiene el día 1 del mes.
     */
    public static List<Long> celdasDelMes(int anio, int mes0) {
        List<Long> celdas = new ArrayList<>(42);
        Calendar cal = getCalendar();
        cal.clear();
        cal.set(anio, mes0, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int diaSemana = cal.get(Calendar.DAY_OF_WEEK); // 1 = Sunday, 2 = Monday...
        int diasAntes = (diaSemana - Calendar.MONDAY + 7) % 7;

        cal.add(Calendar.DAY_OF_MONTH, -diasAntes);

        for (int i = 0; i < 42; i++) {
            celdas.add(TiempoUtils.inicioDelDia(cal.getTimeInMillis()));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return celdas;
    }
}
