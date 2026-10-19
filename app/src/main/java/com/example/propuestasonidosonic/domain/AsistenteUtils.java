package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.model.ChecklistItem;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.TiempoUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AsistenteUtils {

    private AsistenteUtils() {}

    private static final Comparator<ChecklistItem> COMPARADOR_ITEMS = (o1, o2) -> {
        String n1 = o1 != null && o1.getNombre() != null ? o1.getNombre() : "";
        String n2 = o2 != null && o2.getNombre() != null ? o2.getNombre() : "";
        return n1.compareToIgnoreCase(n2);
    };

    public static List<Pedido> eventosDelAsistente(List<Pedido> pedidos, String asistenteId) {
        List<Pedido> resultado = new ArrayList<>();
        if (pedidos == null || asistenteId == null || asistenteId.trim().isEmpty()) {
            return resultado;
        }

        for (Pedido p : pedidos) {
            if (p == null) continue;
            String estado = p.getEstado();
            if (Pedido.ESTADO_CONFIRMADO.equals(estado)
                    || Pedido.ESTADO_PAGADO.equals(estado)
                    || Pedido.ESTADO_COMPLETADO.equals(estado)) {
                if (p.getOperadorIds() != null && p.getOperadorIds().contains(asistenteId)) {
                    resultado.add(p);
                }
            }
        }
        return resultado;
    }

    public static long llegadaMillis(Pedido p) {
        if (p == null) return 0L;
        return p.getBloqueoInicioMillis() > 0 ? p.getBloqueoInicioMillis() : p.getInicioMillis();
    }

    public static long finBloqueoMillis(Pedido p) {
        if (p == null) return 0L;
        return p.getBloqueoFinMillis() > 0 ? p.getBloqueoFinMillis() : p.getFinMillis();
    }

    public static List<Pedido> proximos(List<Pedido> pedidos, long ahora) {
        List<Pedido> resultado = new ArrayList<>();
        if (pedidos == null) return resultado;

        for (Pedido p : pedidos) {
            if (p == null) continue;
            if (finBloqueoMillis(p) > ahora && !Pedido.ESTADO_COMPLETADO.equals(p.getEstado())) {
                resultado.add(p);
            }
        }

        Collections.sort(resultado, (o1, o2) -> {
            int cmp = Long.compare(llegadaMillis(o1), llegadaMillis(o2));
            if (cmp != 0) return cmp;
            return Long.compare(o1.getInicioMillis(), o2.getInicioMillis());
        });
        return resultado;
    }

    public static List<Pedido> pasados(List<Pedido> pedidos, long ahora) {
        List<Pedido> resultado = new ArrayList<>();
        if (pedidos == null) return resultado;

        for (Pedido p : pedidos) {
            if (p == null) continue;
            if (finBloqueoMillis(p) <= ahora || Pedido.ESTADO_COMPLETADO.equals(p.getEstado())) {
                resultado.add(p);
            }
        }

        Collections.sort(resultado, (o1, o2) -> {
            int cmp = Long.compare(llegadaMillis(o2), llegadaMillis(o1));
            if (cmp != 0) return cmp;
            return Long.compare(o2.getInicioMillis(), o1.getInicioMillis());
        });
        return resultado;
    }

    public static boolean esHoy(Pedido p, long ahora) {
        if (p == null) return false;
        return TiempoUtils.claveDia(p.getInicioMillis()).equals(TiempoUtils.claveDia(ahora));
    }

    public static boolean contactoVisible(Pedido p, long ahora) {
        if (p == null) return false;
        long llegada = llegadaMillis(p);
        long fin = finBloqueoMillis(p);
        return (llegada - 24 * 3600_000L <= ahora) && (ahora <= fin);
    }

    public static boolean checklistEditable(Pedido p, long ahora) {
        if (p == null) return false;
        long llegada = llegadaMillis(p);
        long fin = finBloqueoMillis(p);
        return (llegada - ((long) Config.CHECKLIST_ANTES_HORAS) * 3600_000L <= ahora)
                && (ahora <= fin + ((long) Config.CHECKLIST_DESPUES_HORAS) * 3600_000L);
    }

    private static String nombreUnidad(String equipoId, Map<String, Equipo> equiposPorId) {
        if (equiposPorId != null && equiposPorId.containsKey(equipoId)) {
            Equipo eq = equiposPorId.get(equipoId);
            if (eq != null) {
                return eq.getNombre() + " (" + eq.getNumeroUnidad() + ")";
            }
        }
        return "Unidad eliminada";
    }

    public static List<ChecklistItem> checklistInicial(List<String> equipoIds, Map<String, Equipo> equiposPorId) {
        List<ChecklistItem> resultado = new ArrayList<>();
        if (equipoIds == null) return resultado;

        for (String id : equipoIds) {
            if (id == null) continue;
            String nombre = nombreUnidad(id, equiposPorId);
            resultado.add(new ChecklistItem(id, nombre, false, false, ""));
        }

        Collections.sort(resultado, COMPARADOR_ITEMS);
        return resultado;
    }

    public static List<ChecklistItem> sincronizarChecklist(List<ChecklistItem> actual,
                                                           List<String> equipoIds,
                                                           Map<String, Equipo> equiposPorId) {
        List<ChecklistItem> resultado = new ArrayList<>();
        Set<String> asignados = new HashSet<>();
        if (equipoIds != null) {
            asignados.addAll(equipoIds);
        }

        Set<String> yaIncluidos = new HashSet<>();
        if (actual != null) {
            for (ChecklistItem item : actual) {
                if (item == null) continue;
                String id = item.getEquipoId();
                if (asignados.contains(id)) {
                    // Sigue asignada: conservar
                    resultado.add(new ChecklistItem(id, item.getNombre(), item.isSalio(), item.isRegreso(), item.getObservacion()));
                    yaIncluidos.add(id);
                } else if (item.isSalio()) {
                    // Ya no está asignada pero salió: conservar para no perder rastro
                    resultado.add(new ChecklistItem(id, item.getNombre(), item.isSalio(), item.isRegreso(), item.getObservacion()));
                    yaIncluidos.add(id);
                }
            }
        }

        if (equipoIds != null) {
            for (String id : equipoIds) {
                if (id != null && !yaIncluidos.contains(id)) {
                    String nombre = nombreUnidad(id, equiposPorId);
                    resultado.add(new ChecklistItem(id, nombre, false, false, ""));
                    yaIncluidos.add(id);
                }
            }
        }

        Collections.sort(resultado, COMPARADOR_ITEMS);
        return resultado;
    }

    public static List<ChecklistItem> marcar(List<ChecklistItem> items, int indice, String campo, boolean valor) {
        List<ChecklistItem> copia = new ArrayList<>();
        if (items == null) return copia;

        for (ChecklistItem item : items) {
            if (item != null) {
                copia.add(new ChecklistItem(item.getEquipoId(), item.getNombre(),
                        item.isSalio(), item.isRegreso(), item.getObservacion()));
            }
        }

        if (indice < 0 || indice >= copia.size()) {
            return copia;
        }

        ChecklistItem target = copia.get(indice);
        if ("salio".equalsIgnoreCase(campo)) {
            target.setSalio(valor);
            if (!valor) {
                target.setRegreso(false);
            }
        } else if ("regreso".equalsIgnoreCase(campo)) {
            if (valor) {
                if (target.isSalio()) {
                    target.setRegreso(true);
                } else {
                    // Regreso solo puede ser verdadero si salio lo es; devolver lista sin cambios
                    return copia;
                }
            } else {
                target.setRegreso(false);
            }
        }

        return copia;
    }

    public static List<ChecklistItem> conObservacion(List<ChecklistItem> items, int indice, String texto) {
        List<ChecklistItem> copia = new ArrayList<>();
        if (items == null) return copia;

        for (ChecklistItem item : items) {
            if (item != null) {
                copia.add(new ChecklistItem(item.getEquipoId(), item.getNombre(),
                        item.isSalio(), item.isRegreso(), item.getObservacion()));
            }
        }

        if (indice < 0 || indice >= copia.size()) {
            return copia;
        }

        String obs = texto != null ? texto.trim() : "";
        if (obs.length() > 200) {
            obs = obs.substring(0, 200);
        }

        copia.get(indice).setObservacion(obs);
        return copia;
    }

    public static int[] resumenChecklist(List<ChecklistItem> items) {
        int total = 0;
        int salieron = 0;
        int regresaron = 0;
        int conObs = 0;

        if (items != null) {
            total = items.size();
            for (ChecklistItem it : items) {
                if (it == null) continue;
                if (it.isSalio()) salieron++;
                if (it.isRegreso()) regresaron++;
                if (it.getObservacion() != null && !it.getObservacion().trim().isEmpty()) {
                    conObs++;
                }
            }
        }

        return new int[]{total, salieron, regresaron, conObs};
    }

    public static String estadoChecklist(List<ChecklistItem> items) {
        if (items == null || items.isEmpty()) {
            return "Sin equipo";
        }

        int[] res = resumenChecklist(items);
        int total = res[0];
        int salieron = res[1];
        int regresaron = res[2];

        if (salieron == 0) {
            return "Pendiente";
        }
        if (regresaron == total) {
            return "Completo";
        }
        return "En evento";
    }
}
