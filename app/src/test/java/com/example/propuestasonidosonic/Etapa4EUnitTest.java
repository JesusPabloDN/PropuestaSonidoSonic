package com.example.propuestasonidosonic;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.memory.InMemoryDiaOperativoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryEquipoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryEventoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPagoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPaqueteRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPedidoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryUsuarioRepository;
import com.example.propuestasonidosonic.domain.AsignadorUnidades;
import com.example.propuestasonidosonic.domain.SolicitudReserva;
import com.example.propuestasonidosonic.model.CategoriaEquipo;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.DiaOperativo;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Evento;
import com.example.propuestasonidosonic.model.Paquete;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.ModeloUtils;
import com.example.propuestasonidosonic.util.Reloj;
import com.example.propuestasonidosonic.util.TiempoUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class Etapa4EUnitTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static class RelojFalso implements Reloj {
        long tiempo;
        RelojFalso(long tiempo) { this.tiempo = tiempo; }
        @Override public long ahora() { return tiempo; }
        public void setAhora(long tiempo) { this.tiempo = tiempo; }
        public void avanzarHoras(int horas) { this.tiempo += ((long) horas) * 3600_000L; }
    }

    private static final Callback NOOP = new Callback() {
        @Override public void onSuccess(Object result) {}
        @Override public void onError(String mensaje) {}
    };

    @SuppressWarnings("unchecked")
    private static <T> Callback<T> noop() {
        return (Callback<T>) NOOP;
    }

    private RelojFalso reloj;
    private InMemoryEquipoRepository equipoRepo;
    private InMemoryPaqueteRepository paqueteRepo;
    private InMemoryUsuarioRepository usuarioRepo;
    private InMemoryDiaOperativoRepository diaRepo;
    private InMemoryPagoRepository pagoRepo;
    private InMemoryEventoRepository eventoRepo;
    private InMemoryPedidoRepository pedidoRepo;

    private Paquete paqueteSoloBocina;
    private long ahoraFijo;

    @Before
    public void setUp() {
        // Viernes 20 de noviembre de 2026, 08:00
        ahoraFijo = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 8 * 60);
        reloj = new RelojFalso(ahoraFijo);

        equipoRepo = new InMemoryEquipoRepository();
        paqueteRepo = new InMemoryPaqueteRepository(equipoRepo);
        usuarioRepo = new InMemoryUsuarioRepository(null);
        diaRepo = new InMemoryDiaOperativoRepository();
        pagoRepo = new InMemoryPagoRepository();
        eventoRepo = new InMemoryEventoRepository();

        equipoRepo.setEventoRepository(eventoRepo);

        pedidoRepo = new InMemoryPedidoRepository(
                equipoRepo,
                paqueteRepo,
                usuarioRepo,
                diaRepo,
                pagoRepo,
                eventoRepo,
                reloj
        );

        equipoRepo.setOnInventarioCambio(() -> pedidoRepo.reasignarUnidades(null));

        // 1. Agregar unidades de inventario antes de guardar paquete
        for (int i = 1; i <= 2; i++) {
            equipoRepo.agregarEquipo(new Equipo("boc-" + i, "Bocina 15", CategoriaEquipo.BOCINAS,
                    i, Equipo.ESTADO_DISPONIBLE, 500.0), noop());
        }

        // 2. Usuario socio y asistente
        Usuario socio = new Usuario("u-socio", "Carlos Socio", "carlos@sonic.com", "9611234567",
                Usuario.ROL_SOCIO, "Admin#2026", true, System.currentTimeMillis());
        usuarioRepo.crearPersonal(socio, "Admin#2026", noop());

        Usuario asistente = new Usuario("u-asistente", "Pedro Asistente", "pedro@sonic.com", "9611234568",
                Usuario.ROL_ASISTENTE, "Admin#2026", true, System.currentTimeMillis());
        usuarioRepo.crearPersonal(asistente, "Admin#2026", noop());

        // 3. Habilitar día operativo
        String claveDia = TiempoUtils.claveDia(ahoraFijo);
        diaRepo.guardarDia(new DiaOperativo(claveDia, Collections.singletonList("u-asistente"), Collections.emptyList()), noop());

        // 4. Paquete con 1 Bocina (buffer 60 min montaje, 60 min desmontaje, 1 operador)
        paqueteSoloBocina = new Paquete();
        paqueteSoloBocina.setId("paq-bocina");
        paqueteSoloBocina.setNombre("Paquete 1 Bocina");
        paqueteSoloBocina.setPrecio(1000.0);
        paqueteSoloBocina.setDuracionBaseHoras(4);
        paqueteSoloBocina.setBufferMontajeMin(60);
        paqueteSoloBocina.setBufferDesmontajeMin(60);
        paqueteSoloBocina.setOperadoresRequeridos(1);
        paqueteSoloBocina.setActivo(true);
        paqueteSoloBocina.setComponentes(Collections.singletonList(
                new ComponentePaquete(
                        ModeloUtils.clave(CategoriaEquipo.BOCINAS, "Bocina 15"),
                        "Bocina 15",
                        CategoriaEquipo.BOCINAS,
                        1
                )
        ));
        paqueteRepo.guardarPaquete(paqueteSoloBocina, noop());
    }

    // 1. AsignadorUnidades: caso A [1–10), B [12–20), C [5–15) con 2 unidades y 3 demandas nuevas
    @Test
    public void test1_asignadorCasoABC_dosUnidades_todasCompletasSinTraslapes() {
        String clave = "bocinas|bocina 15";
        List<AsignadorUnidades.UnidadDisponible> unidades = Arrays.asList(
                new AsignadorUnidades.UnidadDisponible("u1", clave, 1),
                new AsignadorUnidades.UnidadDisponible("u2", clave, 2)
        );

        List<AsignadorUnidades.Demanda> demandas = Arrays.asList(
                new AsignadorUnidades.Demanda("A", clave, 1, 1000, 10000, Collections.emptyList(), false),
                new AsignadorUnidades.Demanda("B", clave, 1, 12000, 20000, Collections.emptyList(), false),
                new AsignadorUnidades.Demanda("C", clave, 1, 5000, 15000, Collections.emptyList(), false)
        );

        AsignadorUnidades.Resultado res = AsignadorUnidades.asignar(demandas, unidades);

        assertTrue("Ningún pedido debe quedar incompleto", res.pedidosIncompletos.isEmpty());
        assertEquals(1, res.unidadesPorPedido.get("A").size());
        assertEquals(1, res.unidadesPorPedido.get("B").size());
        assertEquals(1, res.unidadesPorPedido.get("C").size());

        String unitA = res.unidadesPorPedido.get("A").get(0);
        String unitB = res.unidadesPorPedido.get("B").get(0);
        String unitC = res.unidadesPorPedido.get("C").get(0);

        // A y C solapan en [5, 10) -> no deben compartir unidad
        assertNotEquals(unitA, unitC);
        // C y B solapan en [12, 15) -> no deben compartir unidad
        assertNotEquals(unitC, unitB);
    }

    // 2. AsignadorUnidades: una demanda fija conserva sus unidades previas aunque otra posterior las prefiera
    @Test
    public void test2_asignadorDemandaFijaConservaUnidadesPrevias() {
        String clave = "bocinas|bocina 15";
        List<AsignadorUnidades.UnidadDisponible> unidades = Arrays.asList(
                new AsignadorUnidades.UnidadDisponible("u1", clave, 1),
                new AsignadorUnidades.UnidadDisponible("u2", clave, 2)
        );

        List<AsignadorUnidades.Demanda> demandas = Arrays.asList(
                new AsignadorUnidades.Demanda("Fija", clave, 1, 1000, 5000, Collections.singletonList("u2"), true),
                new AsignadorUnidades.Demanda("Nueva", clave, 1, 1000, 5000, Collections.emptyList(), false)
        );

        AsignadorUnidades.Resultado res = AsignadorUnidades.asignar(demandas, unidades);
        assertTrue(res.pedidosIncompletos.isEmpty());
        assertEquals(Collections.singletonList("u2"), res.unidadesPorPedido.get("Fija"));
        assertEquals(Collections.singletonList("u1"), res.unidadesPorPedido.get("Nueva"));
    }

    // 3. AsignadorUnidades: estabilidad; con unidades previas libres, el resultado no las cambia
    @Test
    public void test3_asignadorEstabilidadConservaUnidadesPreviasSiLibres() {
        String clave = "bocinas|bocina 15";
        List<AsignadorUnidades.UnidadDisponible> unidades = Arrays.asList(
                new AsignadorUnidades.UnidadDisponible("u1", clave, 1),
                new AsignadorUnidades.UnidadDisponible("u2", clave, 2)
        );

        // Pedido A tenía u2 (aunque u1 tiene número menor), B tenía u1
        List<AsignadorUnidades.Demanda> demandas = Arrays.asList(
                new AsignadorUnidades.Demanda("A", clave, 1, 1000, 5000, Collections.singletonList("u2"), false),
                new AsignadorUnidades.Demanda("B", clave, 1, 1000, 5000, Collections.singletonList("u1"), false)
        );

        AsignadorUnidades.Resultado res = AsignadorUnidades.asignar(demandas, unidades);
        assertTrue(res.pedidosIncompletos.isEmpty());
        assertEquals(Collections.singletonList("u2"), res.unidadesPorPedido.get("A"));
        assertEquals(Collections.singletonList("u1"), res.unidadesPorPedido.get("B"));
    }

    // 4. AsignadorUnidades: faltan unidades -> pedidosIncompletos contiene al pedido y recibe las que sí había
    @Test
    public void test4_asignadorFaltanUnidades_pedidoIncompleto() {
        String clave = "bocinas|bocina 15";
        List<AsignadorUnidades.UnidadDisponible> unidades = Collections.singletonList(
                new AsignadorUnidades.UnidadDisponible("u1", clave, 1)
        );

        List<AsignadorUnidades.Demanda> demandas = Collections.singletonList(
                new AsignadorUnidades.Demanda("P1", clave, 2, 1000, 5000, Collections.emptyList(), false)
        );

        AsignadorUnidades.Resultado res = AsignadorUnidades.asignar(demandas, unidades);
        assertTrue("P1 debe ser marcado como incompleto", res.pedidosIncompletos.contains("P1"));
        assertEquals("Debe recibir la 1 unidad que sí existía", 1, res.unidadesPorPedido.get("P1").size());
        assertEquals("u1", res.unidadesPorPedido.get("P1").get(0));
    }

    // 5. AsignadorUnidades: determinismo (dos corridas iguales dan el mismo mapa)
    @Test
    public void test5_asignadorDeterminismo() {
        String clave = "bocinas|bocina 15";
        List<AsignadorUnidades.UnidadDisponible> unidades = Arrays.asList(
                new AsignadorUnidades.UnidadDisponible("u1", clave, 1),
                new AsignadorUnidades.UnidadDisponible("u2", clave, 2),
                new AsignadorUnidades.UnidadDisponible("u3", clave, 3)
        );

        List<AsignadorUnidades.Demanda> demandas = Arrays.asList(
                new AsignadorUnidades.Demanda("A", clave, 2, 2000, 8000, Collections.emptyList(), false),
                new AsignadorUnidades.Demanda("B", clave, 1, 4000, 10000, Collections.emptyList(), false),
                new AsignadorUnidades.Demanda("C", clave, 2, 9000, 15000, Collections.emptyList(), false)
        );

        AsignadorUnidades.Resultado r1 = AsignadorUnidades.asignar(demandas, unidades);
        AsignadorUnidades.Resultado r2 = AsignadorUnidades.asignar(demandas, unidades);

        assertEquals(r1.unidadesPorPedido, r2.unidadesPorPedido);
        assertEquals(r1.pedidosIncompletos, r2.pedidosIncompletos);
    }

    // 6. Repositorio: registrarAnticipo crea el Evento con equipoIds, pedido.eventoId y bloqueo; APARTADO no crea Evento
    @Test
    public void test6_repositorio_registrarAnticipoCreaEventoConEquipoIdsYBloqueo() {
        long inicio = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 18 * 60);
        SolicitudReserva sol = new SolicitudReserva("paq-bocina", inicio, 240, Pedido.ORIGEN_LLAMADA,
                "Boda Civil", null, "Carlos Ramos", "9611234567", "Calle Central 100", "", null);

        AtomicReference<Pedido> ref = new AtomicReference<>();
        AtomicReference<String> errRef = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String error) { errRef.set(error); }
        });

        Pedido p = ref.get();
        assertNotNull("crearReserva fallo con error: " + errRef.get(), p);
        assertEquals(Pedido.ESTADO_APARTADO, p.getEstado());
        assertNull("Un APARTADO no debe crear Evento", p.getEventoId());
        assertTrue("Un APARTADO no debe tener unidades físicas asignadas", p.getEquipoIds().isEmpty());

        // Registrar anticipo
        pedidoRepo.registrarAnticipo(p.getId(), p.getAnticipoRequerido(), Pedido.METODO_EFECTIVO, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String error) {}
        });

        Pedido confirmado = ref.get();
        assertEquals(Pedido.ESTADO_CONFIRMADO, confirmado.getEstado());
        assertNotNull("Al confirmar debe tener eventoId", confirmado.getEventoId());
        assertFalse("Debe tener equipo asignado", confirmado.getEquipoIds().isEmpty());
        assertEquals("boc-1", confirmado.getEquipoIds().get(0));

        // Verificar el Evento en EventoRepository
        AtomicReference<Evento> evRef = new AtomicReference<>();
        eventoRepo.getEventoPorId(confirmado.getEventoId(), new Callback<Evento>() {
            @Override public void onSuccess(Evento result) { evRef.set(result); }
            @Override public void onError(String error) {}
        });

        Evento ev = evRef.get();
        assertNotNull(ev);
        assertEquals(confirmado.getBloqueoInicioMillis(), ev.getBloqueoInicioMillis());
        assertEquals(confirmado.getBloqueoFinMillis(), ev.getBloqueoFinMillis());
        assertEquals(confirmado.getEquipoIds(), ev.getEquipoIds());
    }

    // 7. Repositorio: tras confirmar, proximaReserva devuelve ese evento y estadoEfectivo es RENTADO durante ventana de bloqueo y DISPONIBLE fuera
    @Test
    public void test7_repositorio_proximaReservaYEstadoEfectivoRentadoDuranteBloqueo() {
        Equipo eq = equipoRepo.listarTodos().get(0);

        // Evento de 19:00 a 23:00. Montaje desde 18:00 (buffer 60 min), Desmontaje hasta 00:00 (buffer 60 min)
        long inicioEvento = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 19 * 60);
        SolicitudReserva sol = new SolicitudReserva("paq-bocina", inicioEvento, 240, Pedido.ORIGEN_LLAMADA,
                "Fiesta Noche", null, "Ana Gomez", "9611234567", "Av Hidalgo 500", "", null);

        AtomicReference<Pedido> ref = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String error) {}
        });
        pedidoRepo.registrarAnticipo(ref.get().getId(), ref.get().getAnticipoRequerido(), Pedido.METODO_EFECTIVO, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String error) {}
        });

        // 1. Antes del montaje (12:00) -> DISPONIBLE, pero con próxima reserva
        long tMediodia = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 12 * 60);
        Evento prox = equipoRepo.proximaReserva("boc-1", tMediodia);
        assertNotNull("Debe mostrar próxima reserva", prox);
        assertEquals(ref.get().getEventoId(), prox.getId());
        assertEquals(Equipo.ESTADO_DISPONIBLE, equipoRepo.estadoEfectivo(eq, tMediodia));

        // 2. Durante el montaje (18:30) -> RENTADO
        long tMontaje = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 18 * 60 + 30);
        assertEquals(Equipo.ESTADO_RENTADO, equipoRepo.estadoEfectivo(eq, tMontaje));

        // 3. Durante el evento (20:00) -> RENTADO
        long tEvento = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 20 * 60);
        assertEquals(Equipo.ESTADO_RENTADO, equipoRepo.estadoEfectivo(eq, tEvento));

        // 4. Durante el desmontaje (23:30) -> RENTADO
        long tDesmontaje = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 23 * 60 + 30);
        assertEquals(Equipo.ESTADO_RENTADO, equipoRepo.estadoEfectivo(eq, tDesmontaje));

        // 5. Después de la ventana (01:00 día siguiente) -> DISPONIBLE
        long tDespues = TiempoUtils.construir(2026, Calendar.NOVEMBER, 21, 1 * 60);
        assertEquals(Equipo.ESTADO_DISPONIBLE, equipoRepo.estadoEfectivo(eq, tDespues));
    }

    // 8. Repositorio: cancelarReserva cancela el evento y la unidad vuelve a quedar sin próxima reserva
    @Test
    public void test8_repositorio_cancelarReservaCancelaEventoYLiberaUnidad() {
        long inicio = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 18 * 60);
        SolicitudReserva sol = new SolicitudReserva("paq-bocina", inicio, 240, Pedido.ORIGEN_LLAMADA,
                "Evento Cancelable", null, "Luis Paz", "9611234567", "Calle Central 100", "", null);

        AtomicReference<Pedido> ref = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String error) {}
        });
        pedidoRepo.registrarAnticipo(ref.get().getId(), ref.get().getAnticipoRequerido(), Pedido.METODO_EFECTIVO, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String error) {}
        });

        Pedido confirmado = ref.get();
        assertNotNull(equipoRepo.proximaReserva("boc-1", ahoraFijo));

        // Cancelar pedido
        pedidoRepo.cancelarReserva(confirmado.getId(), Pedido.CANCELA_SOCIO, "Cliente canceló", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String error) {}
        });

        Pedido cancelado = ref.get();
        assertEquals(Pedido.ESTADO_CANCELADO, cancelado.getEstado());
        assertTrue(cancelado.getEquipoIds().isEmpty());
        assertFalse(cancelado.isAsignacionIncompleta());

        // La unidad queda sin próxima reserva
        assertNull(equipoRepo.proximaReserva("boc-1", ahoraFijo));
    }

    // 9. Repositorio: reprogramarReserva actualiza fechas del evento y mantiene unidades válidas
    @Test
    public void test9_repositorio_reprogramarReservaActualizaFechasEventoYUnidades() {
        long inicio = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 14 * 60);
        SolicitudReserva sol = new SolicitudReserva("paq-bocina", inicio, 240, Pedido.ORIGEN_LLAMADA,
                "Boda Reprogramable", null, "Sofia Ruiz", "9611234567", "Calle Central 100", "", null);

        AtomicReference<Pedido> ref = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String error) {}
        });
        pedidoRepo.registrarAnticipo(ref.get().getId(), ref.get().getAnticipoRequerido(), Pedido.METODO_EFECTIVO, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String error) {}
        });

        Pedido orig = ref.get();
        assertEquals(inicio, orig.getInicioMillis());

        // Reprogramar para las 19:00
        long nuevoInicio = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 19 * 60);
        pedidoRepo.reprogramarReserva(orig.getId(), nuevoInicio, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String error) {}
        });

        Pedido reprogramado = ref.get();
        assertEquals(nuevoInicio, reprogramado.getInicioMillis());
        assertEquals("boc-1", reprogramado.getEquipoIds().get(0));

        // Verificar el evento
        AtomicReference<Evento> evRef = new AtomicReference<>();
        eventoRepo.getEventoPorId(reprogramado.getEventoId(), new Callback<Evento>() {
            @Override public void onSuccess(Evento result) { evRef.set(result); }
            @Override public void onError(String error) {}
        });
        assertEquals(nuevoInicio, evRef.get().getInicioMillis());
    }

    // 10. Repositorio: al pasar a COMPLETADO el evento queda FINALIZADO
    @Test
    public void test10_repositorio_alCompletarEventoQuedaFinalizado() {
        long inicio = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 10 * 60);
        SolicitudReserva sol = new SolicitudReserva("paq-bocina", inicio, 120, Pedido.ORIGEN_LLAMADA,
                "Evento Mañana", null, "Pedro Leon", "9611234567", "Calle Central 100", "", null);

        AtomicReference<Pedido> ref = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String error) {}
        });
        pedidoRepo.registrarAnticipo(ref.get().getId(), ref.get().getAnticipoRequerido(), Pedido.METODO_EFECTIVO, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String error) {}
        });

        Pedido confirmado = ref.get();
        String eventoId = confirmado.getEventoId();

        // Adelantar reloj pasado el fin de bloqueo (10:00 + 120m evento + 60m desmontaje = 13:00)
        reloj.setAhora(TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 14 * 60));
        pedidoRepo.liberarApartadosVencidos();

        Pedido completado = pedidoRepo.getPedidoPorId(confirmado.getId());
        assertEquals(Pedido.ESTADO_COMPLETADO, completado.getEstado());

        AtomicReference<Evento> evRef = new AtomicReference<>();
        eventoRepo.getEventoPorId(eventoId, new Callback<Evento>() {
            @Override public void onSuccess(Evento result) { evRef.set(result); }
            @Override public void onError(String error) {}
        });
        assertEquals(Evento.ESTADO_FINALIZADO, evRef.get().getEstado());
    }

    // 11. Repositorio: marcar una unidad como DANADO provoca que se reemplace por otra libre, o asignacionIncompleta=true
    @Test
    public void test11_repositorio_marcarUnidadDanadoReasignaOIncompleto() {
        long inicio = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 18 * 60);
        SolicitudReserva sol = new SolicitudReserva("paq-bocina", inicio, 240, Pedido.ORIGEN_LLAMADA,
                "Evento Reasignable", null, "Mario Diaz", "9611234567", "Calle Central 100", "", null);

        AtomicReference<Pedido> ref = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String error) {}
        });
        pedidoRepo.registrarAnticipo(ref.get().getId(), ref.get().getAnticipoRequerido(), Pedido.METODO_EFECTIVO, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String error) {}
        });

        Pedido p = pedidoRepo.getPedidoPorId(ref.get().getId());
        String asignada1 = p.getEquipoIds().get(0);
        assertFalse(p.isAsignacionIncompleta());

        // Dañar la unidad asignada -> debe reasignarse a la otra
        equipoRepo.cambiarEstado(asignada1, Equipo.ESTADO_DANADO, noop());

        p = pedidoRepo.getPedidoPorId(p.getId());
        assertFalse("No debe quedar incompleto porque había otra unidad libre", p.isAsignacionIncompleta());
        String asignada2 = p.getEquipoIds().get(0);
        assertNotEquals("Debe haber tomado la otra unidad", asignada1, asignada2);

        // Dañar la segunda unidad -> ahora sí queda con asignacionIncompleta = true
        equipoRepo.cambiarEstado(asignada2, Equipo.ESTADO_DANADO, noop());

        p = pedidoRepo.getPedidoPorId(p.getId());
        assertTrue("Ahora sí debe marcarse incompleto", p.isAsignacionIncompleta());
    }

    // 12. Repositorio: escenario A/B/C del punto 1 a través de pedidos reales (tres registrarAnticipo en orden)
    @Test
    public void test12_repositorio_escenarioABC_pedidosReales_ningunoIncompleto() {
        // Pedido A: 10:00 - 12:00 (bloqueo 09:00 - 13:00)
        long iniA = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 10 * 60);
        SolicitudReserva solA = new SolicitudReserva("paq-bocina", iniA, 120, Pedido.ORIGEN_LLAMADA,
                "Evento A", null, "Cliente A", "9611234567", "Calle Central 100", "", null);

        // Pedido B: 15:00 - 17:00 (bloqueo 14:00 - 18:00)
        long iniB = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 15 * 60);
        SolicitudReserva solB = new SolicitudReserva("paq-bocina", iniB, 120, Pedido.ORIGEN_LLAMADA,
                "Evento B", null, "Cliente B", "9611234567", "Calle Central 100", "", null);

        // Pedido C: 12:00 - 16:00 (bloqueo 11:00 - 17:00; solapa tanto con A como con B!)
        // En este caso, para que C sea viable por personal, necesitamos un 2do operador
        Usuario socio2 = new Usuario("u-socio-2", "Segundo Socio", "socio2@sonic.com", "9611234569",
                Usuario.ROL_SOCIO, "Admin#2026", true, System.currentTimeMillis());
        usuarioRepo.crearPersonal(socio2, "Admin#2026", noop());

        long iniC = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 12 * 60);
        SolicitudReserva solC = new SolicitudReserva("paq-bocina", iniC, 240, Pedido.ORIGEN_LLAMADA,
                "Evento C", null, "Cliente C", "9611234567", "Calle Central 100", "", null);

        AtomicReference<Pedido> refA = new AtomicReference<>();
        AtomicReference<Pedido> refB = new AtomicReference<>();
        AtomicReference<Pedido> refC = new AtomicReference<>();

        pedidoRepo.crearReserva(solA, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { refA.set(result); }
            @Override public void onError(String error) {}
        });
        pedidoRepo.crearReserva(solB, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { refB.set(result); }
            @Override public void onError(String error) {}
        });
        pedidoRepo.crearReserva(solC, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { refC.set(result); }
            @Override public void onError(String error) {}
        });

        // Confirmar A, luego B, luego C
        pedidoRepo.registrarAnticipo(refA.get().getId(), refA.get().getAnticipoRequerido(), Pedido.METODO_EFECTIVO, noop());
        pedidoRepo.registrarAnticipo(refB.get().getId(), refB.get().getAnticipoRequerido(), Pedido.METODO_EFECTIVO, noop());
        pedidoRepo.registrarAnticipo(refC.get().getId(), refC.get().getAnticipoRequerido(), Pedido.METODO_EFECTIVO, noop());

        Pedido pA = pedidoRepo.getPedidoPorId(refA.get().getId());
        Pedido pB = pedidoRepo.getPedidoPorId(refB.get().getId());
        Pedido pC = pedidoRepo.getPedidoPorId(refC.get().getId());

        assertFalse("A no debe quedar incompleto", pA.isAsignacionIncompleta());
        assertFalse("B no debe quedar incompleto", pB.isAsignacionIncompleta());
        assertFalse("C no debe quedar incompleto", pC.isAsignacionIncompleta());

        String unitA = pA.getEquipoIds().get(0);
        String unitB = pB.getEquipoIds().get(0);
        String unitC = pC.getEquipoIds().get(0);

        // A y C solapan en [11:00, 13:00) -> unidades distintas
        assertNotEquals(unitA, unitC);
        // B y C solapan en [14:00, 17:00) -> unidades distintas
        assertNotEquals(unitB, unitC);
    }
}
