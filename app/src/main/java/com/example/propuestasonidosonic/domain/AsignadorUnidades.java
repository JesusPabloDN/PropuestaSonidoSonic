package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.util.TiempoUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AsignadorUnidades {

    private AsignadorUnidades() {}

    public static class Demanda {
        public final String pedidoId;
        public final String modeloClave;
        public final int cantidad;
        public final long desde;
        public final long hasta;
        public final List<String> unidadesPrevias;
        public final boolean fija;

        public Demanda(String pedidoId, String modeloClave, int cantidad, long desde, long hasta,
                       List<String> unidadesPrevias, boolean fija) {
            this.pedidoId = pedidoId != null ? pedidoId : "";
            this.modeloClave = modeloClave != null ? modeloClave : "";
            this.cantidad = Math.max(0, cantidad);
            this.desde = desde;
            this.hasta = hasta;
            this.unidadesPrevias = unidadesPrevias != null ? new ArrayList<>(unidadesPrevias) : Collections.<String>emptyList();
            this.fija = fija;
        }
    }

    public static class UnidadDisponible {
        public final String id;
        public final String modeloClave;
        public final int numeroUnidad;

        public UnidadDisponible(String id, String modeloClave, int numeroUnidad) {
            this.id = id != null ? id : "";
            this.modeloClave = modeloClave != null ? modeloClave : "";
            this.numeroUnidad = numeroUnidad;
        }
    }

    public static class Resultado {
        public final Map<String, List<String>> unidadesPorPedido;
        public final Set<String> pedidosIncompletos;

        public Resultado(Map<String, List<String>> unidadesPorPedido, Set<String> pedidosIncompletos) {
            this.unidadesPorPedido = unidadesPorPedido != null ? unidadesPorPedido : Collections.<String, List<String>>emptyMap();
            this.pedidosIncompletos = pedidosIncompletos != null ? pedidosIncompletos : Collections.<String>emptySet();
        }
    }

    private static class Intervalo {
        final long desde;
        final long hasta;

        Intervalo(long desde, long hasta) {
            this.desde = desde;
            this.hasta = hasta;
        }

        boolean solapaCon(long d, long h) {
            return TiempoUtils.solapan(this.desde, this.hasta, d, h);
        }
    }

    public static Resultado asignar(List<Demanda> demandas, List<UnidadDisponible> unidades) {
        Map<String, List<String>> resultadoPorPedido = new LinkedHashMap<>();
        Set<String> pedidosIncompletos = new LinkedHashSet<>();

        if (demandas == null || demandas.isEmpty()) {
            return new Resultado(resultadoPorPedido, pedidosIncompletos);
        }

        // Inicializar pedidos en el mapa de resultados
        for (Demanda d : demandas) {
            if (!resultadoPorPedido.containsKey(d.pedidoId)) {
                resultadoPorPedido.put(d.pedidoId, new ArrayList<String>());
            }
        }

        // Agrupar unidades y demandas por modeloClave
        Map<String, List<UnidadDisponible>> unidadesPorModelo = new HashMap<>();
        if (unidades != null) {
            for (UnidadDisponible u : unidades) {
                List<UnidadDisponible> lista = unidadesPorModelo.get(u.modeloClave);
                if (lista == null) {
                    lista = new ArrayList<>();
                    unidadesPorModelo.put(u.modeloClave, lista);
                }
                lista.add(u);
            }
        }

        Map<String, List<Demanda>> demandasPorModelo = new HashMap<>();
        for (Demanda d : demandas) {
            List<Demanda> lista = demandasPorModelo.get(d.modeloClave);
            if (lista == null) {
                lista = new ArrayList<>();
                demandasPorModelo.put(d.modeloClave, lista);
            }
            lista.add(d);
        }

        // Procesar cada modelo por separado
        for (Map.Entry<String, List<Demanda>> entry : demandasPorModelo.entrySet()) {
            String modeloClave = entry.getKey();
            List<Demanda> demandasModelo = new ArrayList<>(entry.getValue());
            List<UnidadDisponible> unidadesModelo = unidadesPorModelo.get(modeloClave);
            if (unidadesModelo == null) {
                unidadesModelo = Collections.emptyList();
            }

            // Mapeo id -> UnidadDisponible para búsqueda rápida
            Map<String, UnidadDisponible> mapUnidades = new HashMap<>();
            for (UnidadDisponible u : unidadesModelo) {
                mapUnidades.put(u.id, u);
            }

            // Intervalos ocupados por cada unidad
            Map<String, List<Intervalo>> intervalosPorUnidad = new HashMap<>();

            // Ordenar demandas: desde asc, fija primero, pedidoId asc
            Collections.sort(demandasModelo, new Comparator<Demanda>() {
                @Override
                public int compare(Demanda d1, Demanda d2) {
                    int cDesde = Long.compare(d1.desde, d2.desde);
                    if (cDesde != 0) return cDesde;

                    if (d1.fija != d2.fija) {
                        return d1.fija ? -1 : 1;
                    }
                    return d1.pedidoId.compareTo(d2.pedidoId);
                }
            });

            // Asignar unidades a cada demanda del modelo
            for (Demanda d : demandasModelo) {
                List<String> asignadas = new ArrayList<>();

                if (d.fija && !d.unidadesPrevias.isEmpty()) {
                    // Demanda fija: conserva exactamente sus previas
                    for (String uid : d.unidadesPrevias) {
                        asignadas.add(uid);
                        registrarIntervalo(intervalosPorUnidad, uid, d.desde, d.hasta);
                    }
                    if (asignadas.size() < d.cantidad) {
                        pedidosIncompletos.add(d.pedidoId);
                    }
                } else {
                    // Demanda no fija:
                    // 1) Reutilizar unidades previas que sigan en el catálogo y estén libres
                    Set<String> asignadasSet = new HashSet<>();
                    for (String uid : d.unidadesPrevias) {
                        if (asignadas.size() >= d.cantidad) break;
                        if (mapUnidades.containsKey(uid) && !asignadasSet.contains(uid)) {
                            if (estaLibre(intervalosPorUnidad, uid, d.desde, d.hasta)) {
                                asignadas.add(uid);
                                asignadasSet.add(uid);
                                registrarIntervalo(intervalosPorUnidad, uid, d.desde, d.hasta);
                            }
                        }
                    }

                    // 2) Para las faltantes, elegir la unidad libre de mejor ajuste
                    while (asignadas.size() < d.cantidad) {
                        UnidadDisponible mejor = null;
                        long mejorUltimoFin = -2L;

                        for (UnidadDisponible u : unidadesModelo) {
                            if (asignadasSet.contains(u.id)) continue;
                            if (!estaLibre(intervalosPorUnidad, u.id, d.desde, d.hasta)) continue;

                            long ultimoFin = calcularUltimoFinAntesDe(intervalosPorUnidad, u.id, d.desde);

                            if (mejor == null) {
                                mejor = u;
                                mejorUltimoFin = ultimoFin;
                            } else {
                                if (ultimoFin > mejorUltimoFin) {
                                    mejor = u;
                                    mejorUltimoFin = ultimoFin;
                                } else if (ultimoFin == mejorUltimoFin) {
                                    if (u.numeroUnidad < mejor.numeroUnidad) {
                                        mejor = u;
                                        mejorUltimoFin = ultimoFin;
                                    } else if (u.numeroUnidad == mejor.numeroUnidad && u.id.compareTo(mejor.id) < 0) {
                                        mejor = u;
                                        mejorUltimoFin = ultimoFin;
                                    }
                                }
                            }
                        }

                        if (mejor != null) {
                            asignadas.add(mejor.id);
                            asignadasSet.add(mejor.id);
                            registrarIntervalo(intervalosPorUnidad, mejor.id, d.desde, d.hasta);
                        } else {
                            // No hay más unidades libres
                            break;
                        }
                    }

                    if (asignadas.size() < d.cantidad) {
                        pedidosIncompletos.add(d.pedidoId);
                    }
                }

                // Agregar asignadas al acumulador del pedido
                List<String> listTotal = resultadoPorPedido.get(d.pedidoId);
                if (listTotal != null) {
                    listTotal.addAll(asignadas);
                }
            }
        }

        return new Resultado(resultadoPorPedido, pedidosIncompletos);
    }

    private static boolean estaLibre(Map<String, List<Intervalo>> intervalosPorUnidad, String unidadId, long desde, long hasta) {
        List<Intervalo> lista = intervalosPorUnidad.get(unidadId);
        if (lista == null || lista.isEmpty()) return true;
        for (Intervalo inv : lista) {
            if (inv.solapaCon(desde, hasta)) {
                return false;
            }
        }
        return true;
    }

    private static void registrarIntervalo(Map<String, List<Intervalo>> intervalosPorUnidad, String unidadId, long desde, long hasta) {
        List<Intervalo> lista = intervalosPorUnidad.get(unidadId);
        if (lista == null) {
            lista = new ArrayList<>();
            intervalosPorUnidad.put(unidadId, lista);
        }
        lista.add(new Intervalo(desde, hasta));
    }

    private static long calcularUltimoFinAntesDe(Map<String, List<Intervalo>> intervalosPorUnidad, String unidadId, long desde) {
        List<Intervalo> lista = intervalosPorUnidad.get(unidadId);
        if (lista == null || lista.isEmpty()) return -1L;
        long maxFin = -1L;
        for (Intervalo inv : lista) {
            if (inv.hasta <= desde) {
                if (inv.hasta > maxFin) {
                    maxFin = inv.hasta;
                }
            }
        }
        return maxFin;
    }
}
