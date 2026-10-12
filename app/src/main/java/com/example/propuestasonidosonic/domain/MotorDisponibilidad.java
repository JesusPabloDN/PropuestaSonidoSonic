package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.DiaOperativo;
import com.example.propuestasonidosonic.model.ModeloInventario;
import com.example.propuestasonidosonic.model.Paquete;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.TiempoUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class MotorDisponibilidad {
    private MotorDisponibilidad() {}

    private static class EventoBarrido implements Comparable<EventoBarrido> {
        final long tiempo;
        final int delta;

        EventoBarrido(long tiempo, int delta) {
            this.tiempo = tiempo;
            this.delta = delta;
        }

        @Override
        public int compareTo(EventoBarrido o) {
            if (this.tiempo != o.tiempo) {
                return Long.compare(this.tiempo, o.tiempo);
            }
            // A igualdad de tiempo, procesar primero los negativos (salidas/liberaciones)
            return Integer.compare(this.delta, o.delta);
        }
    }

    public static boolean bloquea(Pedido p, long ahora) {
        if (p == null || p.getEstado() == null) return false;
        String estado = p.getEstado();
        if (Pedido.ESTADO_APARTADO.equals(estado)) {
            return p.getVencimientoApartadoMillis() > ahora;
        }
        return Pedido.ESTADO_PAGADO.equals(estado)
                || Pedido.ESTADO_CONFIRMADO.equals(estado)
                || Pedido.ESTADO_EN_REVISION.equals(estado);
    }

    public static double calcularTotal(Paquete paquete, int duracionMin) {
        if (paquete == null) return 0.0;
        int baseMin = paquete.getDuracionBaseHoras() * 60;
        int excesoMin = duracionMin - baseMin;
        int horasExtra = 0;
        if (excesoMin > 0) {
            horasExtra = (int) Math.ceil(excesoMin / 60.0);
        }
        return paquete.getPrecio() + (horasExtra * paquete.getPrecioHoraExtra());
    }

    public static double calcularAnticipo(double total) {
        return Math.round(total * Config.ANTICIPO_PORCENTAJE * 100.0) / 100.0;
    }

    public static ResultadoDisponibilidad evaluar(ContextoDisponibilidad ctx, Paquete paquete,
                                                  long inicioMillis, int duracionMin, String ignorarPedidoId) {
        if (ctx == null) {
            return ResultadoDisponibilidad.noViable(ResultadoDisponibilidad.PAQUETE_INACTIVO, "Contexto nulo");
        }

        // Regla 1: Paquete no nulo, activo y con componentes
        if (paquete == null || !paquete.isActivo()
                || paquete.getComponentes() == null || paquete.getComponentes().isEmpty()) {
            return ResultadoDisponibilidad.noViable(ResultadoDisponibilidad.PAQUETE_INACTIVO,
                    "El paquete está inactivo o no tiene componentes");
        }

        // Regla 2: Duración válida
        if (duracionMin < Config.DURACION_MIN_MIN
                || duracionMin > Config.DURACION_MAX_MIN
                || (duracionMin % Config.PASO_DURACION_MIN != 0)) {
            return ResultadoDisponibilidad.noViable(ResultadoDisponibilidad.DURACION_INVALIDA,
                    "Duración inválida. Debe ser entre " + Config.DURACION_MIN_MIN + " y "
                            + Config.DURACION_MAX_MIN + " min en múltiplos de " + Config.PASO_DURACION_MIN);
        }

        // Regla 3: Inicio posterior a ahora
        if (inicioMillis <= ctx.getAhora()) {
            return ResultadoDisponibilidad.noViable(ResultadoDisponibilidad.EN_EL_PASADO,
                    "La fecha y hora de inicio debe ser posterior al momento actual");
        }

        // Regla 4: Horario permitido del día
        int minutoDia = TiempoUtils.minutosDelDia(inicioMillis);
        if (minutoDia < Config.PRIMER_INICIO_MIN
                || minutoDia > Config.ULTIMO_INICIO_MIN
                || (minutoDia % Config.PASO_HORARIO_MIN != 0)) {
            return ResultadoDisponibilidad.noViable(ResultadoDisponibilidad.FUERA_DE_HORARIO,
                    "El horario de inicio debe estar entre 09:00 y 22:00 en intervalos de 30 min");
        }

        // Regla 5: Ventana de bloqueo
        long finMillis = TiempoUtils.sumarMinutos(inicioMillis, duracionMin);
        long bufferMontajeMs = ((long) paquete.getBufferMontajeMin()) * 60_000L;
        long bufferDesmontajeMs = ((long) paquete.getBufferDesmontajeMin()) * 60_000L;
        long ventanaInicio = inicioMillis - bufferMontajeMs;
        long ventanaFin = finMillis + bufferDesmontajeMs;

        // Regla 6: Reservas bloqueantes
        List<Pedido> bloqueantes = new ArrayList<>();
        if (ctx.getReservas() != null) {
            for (Pedido r : ctx.getReservas()) {
                if (r == null) continue;
                if (ignorarPedidoId != null && ignorarPedidoId.equals(r.getId())) continue;
                if (!bloquea(r, ctx.getAhora())) continue;

                long rBloqIni = r.getBloqueoInicioMillis();
                long rBloqFin = r.getBloqueoFinMillis();
                if (rBloqIni == 0 && rBloqFin == 0) {
                    rBloqIni = r.getInicioMillis();
                    rBloqFin = r.getFinMillis();
                }
                if (TiempoUtils.solapan(rBloqIni, rBloqFin, ventanaInicio, ventanaFin)) {
                    bloqueantes.add(r);
                }
            }
        }

        // Regla 7: Existencias por componente
        Map<String, ModeloInventario> modelosMap = new HashMap<>();
        if (ctx.getModelos() != null) {
            for (ModeloInventario m : ctx.getModelos()) {
                if (m != null && m.getClave() != null) {
                    modelosMap.put(m.getClave(), m);
                }
            }
        }

        for (ComponentePaquete comp : paquete.getComponentes()) {
            ModeloInventario mod = modelosMap.get(comp.getModeloClave());
            if (mod == null) {
                return ResultadoDisponibilidad.noViable(ResultadoDisponibilidad.PAQUETE_INCOMPLETO,
                        "El modelo no existe en inventario: " + comp.getNombreModelo());
            }

            int capacidad = mod.getUnidadesDisponibles();
            List<EventoBarrido> eventosModelo = new ArrayList<>();
            for (Pedido r : bloqueantes) {
                int cantEnReserva = 0;
                if (r.getComponentes() != null) {
                    for (ComponentePaquete cr : r.getComponentes()) {
                        if (comp.getModeloClave().equals(cr.getModeloClave())) {
                            cantEnReserva += cr.getCantidad();
                        }
                    }
                }
                if (cantEnReserva > 0) {
                    long rBloqIni = r.getBloqueoInicioMillis();
                    long rBloqFin = r.getBloqueoFinMillis();
                    if (rBloqIni == 0 && rBloqFin == 0) {
                        rBloqIni = r.getInicioMillis();
                        rBloqFin = r.getFinMillis();
                    }
                    long start = Math.max(rBloqIni, ventanaInicio);
                    long end = Math.min(rBloqFin, ventanaFin);
                    if (start < end) {
                        eventosModelo.add(new EventoBarrido(start, cantEnReserva));
                        eventosModelo.add(new EventoBarrido(end, -cantEnReserva));
                    }
                }
            }

            Collections.sort(eventosModelo);
            int picoModelo = 0;
            int acumuladoModelo = 0;
            for (EventoBarrido ev : eventosModelo) {
                acumuladoModelo += ev.delta;
                if (acumuladoModelo > picoModelo) {
                    picoModelo = acumuladoModelo;
                }
            }

            if (picoModelo + comp.getCantidad() > capacidad) {
                return ResultadoDisponibilidad.noViable(ResultadoDisponibilidad.SIN_STOCK,
                        "No alcanzan las unidades de " + mod.getNombreModelo());
            }
        }

        // Regla 8: Límite de eventos simultáneos
        List<EventoBarrido> eventosConcurrencia = new ArrayList<>();
        for (Pedido r : bloqueantes) {
            long rBloqIni = r.getBloqueoInicioMillis();
            long rBloqFin = r.getBloqueoFinMillis();
            if (rBloqIni == 0 && rBloqFin == 0) {
                rBloqIni = r.getInicioMillis();
                rBloqFin = r.getFinMillis();
            }
            long start = Math.max(rBloqIni, ventanaInicio);
            long end = Math.min(rBloqFin, ventanaFin);
            if (start < end) {
                eventosConcurrencia.add(new EventoBarrido(start, 1));
                eventosConcurrencia.add(new EventoBarrido(end, -1));
            }
        }
        Collections.sort(eventosConcurrencia);
        int picoConcurrencia = 0;
        int acumuladoConcurrencia = 0;
        for (EventoBarrido ev : eventosConcurrencia) {
            acumuladoConcurrencia += ev.delta;
            if (acumuladoConcurrencia > picoConcurrencia) {
                picoConcurrencia = acumuladoConcurrencia;
            }
        }

        if (picoConcurrencia + 1 > Config.MAX_EVENTOS_SIMULTANEOS) {
            return ResultadoDisponibilidad.noViable(ResultadoDisponibilidad.MAXIMO_SIMULTANEOS,
                    "Se superaría el límite de " + Config.MAX_EVENTOS_SIMULTANEOS + " eventos simultáneos");
        }

        // Regla 9: Asignación de operadores
        String fechaClave = TiempoUtils.claveDia(inicioMillis);
        DiaOperativo diaOp = ctx.getDias() != null ? ctx.getDias().get(fechaClave) : null;

        Set<String> sociosNoDisp = (diaOp != null && diaOp.getSociosNoDisponiblesIds() != null)
                ? new HashSet<>(diaOp.getSociosNoDisponiblesIds())
                : Collections.emptySet();
        Set<String> asistentesDisp = (diaOp != null && diaOp.getAsistentesDisponiblesIds() != null)
                ? new HashSet<>(diaOp.getAsistentesDisponiblesIds())
                : Collections.emptySet();

        Set<String> operadoresOcupados = new HashSet<>();
        for (Pedido r : bloqueantes) {
            if (r.getOperadorIds() != null) {
                operadoresOcupados.addAll(r.getOperadorIds());
            }
        }

        List<Usuario> sociosLibres = new ArrayList<>();
        if (ctx.getSocios() != null) {
            for (Usuario u : ctx.getSocios()) {
                if (u != null && u.isCuentaActiva() && !sociosNoDisp.contains(u.getId())
                        && !operadoresOcupados.contains(u.getId())) {
                    sociosLibres.add(u);
                }
            }
        }

        List<Usuario> asistentesLibres = new ArrayList<>();
        if (ctx.getAsistentes() != null) {
            for (Usuario u : ctx.getAsistentes()) {
                if (u != null && u.isCuentaActiva() && asistentesDisp.contains(u.getId())
                        && !operadoresOcupados.contains(u.getId())) {
                    asistentesLibres.add(u);
                }
            }
        }

        Comparator<Usuario> compId = new Comparator<Usuario>() {
            @Override
            public int compare(Usuario o1, Usuario o2) {
                String id1 = o1.getId() != null ? o1.getId() : "";
                String id2 = o2.getId() != null ? o2.getId() : "";
                return id1.compareTo(id2);
            }
        };

        Collections.sort(sociosLibres, compId);
        Collections.sort(asistentesLibres, compId);

        List<Usuario> ordenados = new ArrayList<>();
        if (Config.PREFERIR_ASISTENTES) {
            ordenados.addAll(asistentesLibres);
            ordenados.addAll(sociosLibres);
        } else {
            ordenados.addAll(sociosLibres);
            ordenados.addAll(asistentesLibres);
        }

        int requeridos = paquete.getOperadoresRequeridos();
        if (requeridos <= 0) {
            requeridos = 1;
        }

        if (ordenados.size() < requeridos) {
            return ResultadoDisponibilidad.noViable(ResultadoDisponibilidad.SIN_OPERADORES,
                    "No hay suficientes operadores disponibles");
        }

        List<String> operadorIdsAsignados = new ArrayList<>();
        for (int i = 0; i < requeridos; i++) {
            operadorIdsAsignados.add(ordenados.get(i).getId());
        }

        // Regla 10: Revisión por excepción (anticipación urgente)
        long margenUrgenteMs = ((long) Config.ANTICIPACION_REVISION_HORAS) * 3600_000L;
        boolean requiereRevision = (inicioMillis - ctx.getAhora()) < margenUrgenteMs;
        List<String> motivosRevision = new ArrayList<>();
        if (requiereRevision) {
            motivosRevision.add(Pedido.MOTIVO_URGENTE);
        }

        // Regla 11: Resultado viable
        return ResultadoDisponibilidad.viable(operadorIdsAsignados, requiereRevision,
                motivosRevision, ventanaInicio, ventanaFin);
    }

    public static List<Long> horariosDisponibles(ContextoDisponibilidad ctx, Paquete paquete,
                                                 long unMomentoDelDia, int duracionMin, String ignorarPedidoId) {
        List<Long> result = new ArrayList<>();
        if (ctx == null || paquete == null) return result;

        long diaInicio = TiempoUtils.inicioDelDia(unMomentoDelDia);
        for (int min = Config.PRIMER_INICIO_MIN; min <= Config.ULTIMO_INICIO_MIN; min += Config.PASO_HORARIO_MIN) {
            long inicioMillis = TiempoUtils.sumarMinutos(diaInicio, min);
            ResultadoDisponibilidad res = evaluar(ctx, paquete, inicioMillis, duracionMin, ignorarPedidoId);
            if (res.isViable()) {
                result.add(inicioMillis);
            }
        }
        return result;
    }

    public static List<Long> horariosDisponibles(ContextoDisponibilidad ctx, Paquete paquete,
                                                 long unMomentoDelDia, int duracionMin) {
        return horariosDisponibles(ctx, paquete, unMomentoDelDia, duracionMin, null);
    }

    public static Set<Integer> diasDisponibles(ContextoDisponibilidad ctx, Paquete paquete,
                                               int anio, int mes0, int duracionMin) {
        Set<Integer> result = new TreeSet<>();
        if (ctx == null || paquete == null) return result;

        // Calcular días del mes usando Calendar
        java.util.Calendar cal = java.util.Calendar.getInstance(
                java.util.TimeZone.getTimeZone(Config.ZONA_HORARIA), java.util.Locale.ROOT);
        cal.clear();
        cal.set(anio, mes0, 1, 12, 0, 0);
        int maxDias = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);

        for (int dia = 1; dia <= maxDias; dia++) {
            long momento = TiempoUtils.construir(anio, mes0, dia, Config.PRIMER_INICIO_MIN);
            List<Long> horarios = horariosDisponibles(ctx, paquete, momento, duracionMin);
            if (!horarios.isEmpty()) {
                result.add(dia);
            }
        }
        return result;
    }

    public static List<Long> proximasAlternativas(ContextoDisponibilidad ctx, Paquete paquete,
                                                  long desdeMillis, int duracionMin, int max) {
        List<Long> result = new ArrayList<>();
        if (ctx == null || paquete == null || max <= 0) return result;

        long diaActual = TiempoUtils.inicioDelDia(desdeMillis);
        for (int d = 0; d < Config.DIAS_BUSQUEDA_MAX; d++) {
            long diaMomento = TiempoUtils.sumarMinutos(diaActual, d * 24 * 60);
            List<Long> horarios = horariosDisponibles(ctx, paquete, diaMomento, duracionMin);
            for (Long h : horarios) {
                if (h >= desdeMillis) {
                    result.add(h);
                    if (result.size() >= max) {
                        return result;
                    }
                }
            }
        }
        return result;
    }
}
