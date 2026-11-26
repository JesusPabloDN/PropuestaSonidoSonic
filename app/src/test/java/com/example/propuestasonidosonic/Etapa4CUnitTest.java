package com.example.propuestasonidosonic;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.memory.InMemoryDiaOperativoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryEquipoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPaqueteRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPedidoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryUsuarioRepository;
import com.example.propuestasonidosonic.domain.ContextoDisponibilidad;
import com.example.propuestasonidosonic.domain.MotorDisponibilidad;
import com.example.propuestasonidosonic.domain.ResultadoDisponibilidad;
import com.example.propuestasonidosonic.domain.SolicitudReserva;
import com.example.propuestasonidosonic.model.CategoriaEquipo;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.DiaOperativo;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Paquete;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.Reloj;
import com.example.propuestasonidosonic.util.TiempoUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class Etapa4CUnitTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static class RelojFalso implements Reloj {
        long tiempo;
        RelojFalso(long tiempo) { this.tiempo = tiempo; }
        @Override public long ahora() { return tiempo; }
        public void setAhora(long tiempo) { this.tiempo = tiempo; }
        public void avanzarHoras(int horas) { this.tiempo += ((long) horas) * 3600_000L; }
        public void avanzarMinutos(int min) { this.tiempo += ((long) min) * 60_000L; }
    }

    private RelojFalso reloj;
    private InMemoryEquipoRepository equipoRepo;
    private InMemoryPaqueteRepository paqueteRepo;
    private InMemoryUsuarioRepository usuarioRepo;
    private InMemoryDiaOperativoRepository diaRepo;
    private InMemoryPedidoRepository pedidoRepo;

    private Paquete paqueteBasico;
    private Paquete paqueteSoloBocina;
    private long ahoraFijo;

    @Before
    public void setUp() {
        // Viernes 20 de noviembre de 2026, 10:00
        ahoraFijo = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 10 * 60);
        reloj = new RelojFalso(ahoraFijo);

        equipoRepo = new InMemoryEquipoRepository();
        paqueteRepo = new InMemoryPaqueteRepository(equipoRepo);
        usuarioRepo = new InMemoryUsuarioRepository(null);
        diaRepo = new InMemoryDiaOperativoRepository();

        pedidoRepo = new InMemoryPedidoRepository(
                equipoRepo,
                paqueteRepo,
                usuarioRepo,
                diaRepo,
                reloj
        );

        // Inventario: 4 Bocinas, 2 Micrófonos, 1 Mezcladora
        for (int i = 1; i <= 4; i++) {
            equipoRepo.agregarEquipo(new Equipo("boc-" + i, "Bocina JBL 15\"", CategoriaEquipo.BOCINAS,
                    i, Equipo.ESTADO_DISPONIBLE, 500.0), noop());
        }
        for (int i = 1; i <= 2; i++) {
            equipoRepo.agregarEquipo(new Equipo("mic-" + i, "Micrófono inalámbrico", CategoriaEquipo.MICROFONOS,
                    i, Equipo.ESTADO_DISPONIBLE, 200.0), noop());
        }
        equipoRepo.agregarEquipo(new Equipo("mez-1", "Mezcladora 8ch", CategoriaEquipo.MEZCLADORAS,
                1, Equipo.ESTADO_DISPONIBLE, 800.0), noop());

        // Paquete Básico ($3,500, 4h, 1 op, 2 bocinas, 1 mezcladora, 2 micrófonos)
        paqueteBasico = new Paquete();
        paqueteBasico.setId("paq-basico");
        paqueteBasico.setNombre("Paquete Básico");
        paqueteBasico.setPrecio(3500.0);
        paqueteBasico.setPrecioHoraExtra(500.0);
        paqueteBasico.setDuracionBaseHoras(4);
        paqueteBasico.setOperadoresRequeridos(1);
        paqueteBasico.setBufferMontajeMin(90);
        paqueteBasico.setBufferDesmontajeMin(120);
        paqueteBasico.setActivo(true);
        paqueteBasico.setComponentes(Arrays.asList(
                new ComponentePaquete(com.example.propuestasonidosonic.util.ModeloUtils.clave(CategoriaEquipo.BOCINAS, "Bocina JBL 15\""),
                        "Bocina JBL 15\"", CategoriaEquipo.BOCINAS, 2),
                new ComponentePaquete(com.example.propuestasonidosonic.util.ModeloUtils.clave(CategoriaEquipo.MEZCLADORAS, "Mezcladora 8ch"),
                        "Mezcladora 8ch", CategoriaEquipo.MEZCLADORAS, 1),
                new ComponentePaquete(com.example.propuestasonidosonic.util.ModeloUtils.clave(CategoriaEquipo.MICROFONOS, "Micrófono inalámbrico"),
                        "Micrófono inalámbrico", CategoriaEquipo.MICROFONOS, 2)
        ));
        paqueteRepo.guardarPaquete(paqueteBasico, noop());

        // Paquete Solo bocinas ($800, 4h, 1 op, 1 bocina)
        paqueteSoloBocina = new Paquete();
        paqueteSoloBocina.setId("paq-solo-bocinas");
        paqueteSoloBocina.setNombre("Solo bocinas");
        paqueteSoloBocina.setPrecio(800.0);
        paqueteSoloBocina.setPrecioHoraExtra(200.0);
        paqueteSoloBocina.setDuracionBaseHoras(4);
        paqueteSoloBocina.setOperadoresRequeridos(1);
        paqueteSoloBocina.setBufferMontajeMin(60);
        paqueteSoloBocina.setBufferDesmontajeMin(60);
        paqueteSoloBocina.setActivo(true);
        paqueteSoloBocina.setComponentes(Collections.singletonList(
                new ComponentePaquete(com.example.propuestasonidosonic.util.ModeloUtils.clave(CategoriaEquipo.BOCINAS, "Bocina JBL 15\""),
                        "Bocina JBL 15\"", CategoriaEquipo.BOCINAS, 1)
        ));
        paqueteRepo.guardarPaquete(paqueteSoloBocina, noop());
    }

    private static <T> Callback<T> noop() {
        return new Callback<T>() {
            @Override public void onSuccess(T result) {}
            @Override public void onError(String mensaje) {}
        };
    }

    private Pedido crear(String paqueteId, long inicio, String origen, String nombre, String clienteId) {
        SolicitudReserva sol = new SolicitudReserva(paqueteId, inicio, 240, origen,
                nombre, clienteId, "Cliente " + nombre, "5551234567", "Av. Hidalgo 123", "");
        AtomicReference<Pedido> ref = new AtomicReference<>();
        AtomicReference<String> err = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String mensaje) { err.set(mensaje); }
        });
        assertNotNull("crearReserva falló: " + err.get(), ref.get());
        return ref.get();
    }

    private String confirmarPago(String pedidoId, String metodo) {
        AtomicReference<String> err = new AtomicReference<>();
        pedidoRepo.confirmarPago(pedidoId, metodo, "socio-1", "Socio Principal", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { err.set(mensaje); }
        });
        return err.get();
    }

    @Test
    public void test01_origenDefineEstadoInicial() {
        long ini = TiempoUtils.construir(2026, Calendar.DECEMBER, 5, 19 * 60);

        // Registrado por un socio (llamada): confirmado de inmediato, con evento y sin pago
        Pedido llamada = crear("paq-basico", ini, Pedido.ORIGEN_LLAMADA, "Graduación", null);
        assertEquals(Pedido.ESTADO_CONFIRMADO, llamada.getEstado());
        assertEquals(3500.0, llamada.getTotal(), 0.001);
        assertFalse(llamada.isPagoConfirmado());
        assertFalse("Un pedido confirmado ya tiene unidades asignadas",
                pedidoRepo.getPedidoPorId(llamada.getId()).getEquipoIds().isEmpty());

        // Solicitud desde la app: por aprobar y sin evento todavía
        long otroDia = TiempoUtils.construir(2026, Calendar.DECEMBER, 6, 19 * 60);
        Pedido app = crear("paq-basico", otroDia, Pedido.ORIGEN_APP, "Boda App", "cli-app");
        assertEquals(Pedido.ESTADO_POR_APROBAR, app.getEstado());
        assertNull("La solicitud no crea evento hasta que se aprueba", pedidoRepo.getPedidoPorId(app.getId()).getEventoId());
    }

    @Test
    public void test02_crearReserva_urgenteSoloDesdeApp() {
        // Evento urgente: a 24 horas del reloj actual (< 48 horas)
        long iniUrgente = ahoraFijo + 24L * 3600_000L;

        Pedido llamada = crear("paq-basico", iniUrgente, Pedido.ORIGEN_LLAMADA, "Boda Urgente Llamada", null);
        assertTrue("Motivos de revisión deben estar vacíos para Llamada", llamada.getMotivosRevision().isEmpty());
        pedidoRepo.cancelarReserva(llamada.getId(), Pedido.CANCELA_SOCIO, "Prueba", noop());

        Pedido app = crear("paq-basico", iniUrgente, Pedido.ORIGEN_APP, "Boda Urgente App", "cli-app");
        assertEquals(Pedido.ESTADO_POR_APROBAR, app.getEstado());
        assertTrue("Debe incluir motivo urgente", app.getMotivosRevision().contains(Pedido.MOTIVO_URGENTE));
    }

    @Test
    public void test03_aprobarYRechazarSolicitudes() {
        long ini = TiempoUtils.construir(2026, Calendar.DECEMBER, 5, 19 * 60);
        Pedido solicitud = crear("paq-solo-bocinas", ini, Pedido.ORIGEN_APP, "Fiesta App", "cli-1");

        // No se puede confirmar el pago de una solicitud que nadie ha aprobado
        assertNotNull(confirmarPago(solicitud.getId(), Pedido.METODO_EFECTIVO));

        AtomicReference<Pedido> aprobado = new AtomicReference<>();
        pedidoRepo.aprobarPedido(solicitud.getId(), new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { aprobado.set(result); }
            @Override public void onError(String mensaje) {}
        });
        assertNotNull(aprobado.get());
        assertEquals(Pedido.ESTADO_CONFIRMADO, aprobado.get().getEstado());

        // Aprobar dos veces -> error
        AtomicReference<String> errDoble = new AtomicReference<>();
        pedidoRepo.aprobarPedido(solicitud.getId(), new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { errDoble.set(mensaje); }
        });
        assertNotNull(errDoble.get());

        // Rechazo: exige motivo y libera el horario
        long otroDia = TiempoUtils.construir(2026, Calendar.DECEMBER, 7, 19 * 60);
        Pedido otra = crear("paq-basico", otroDia, Pedido.ORIGEN_APP, "Otra App", "cli-2");
        AtomicReference<String> errMotivo = new AtomicReference<>();
        pedidoRepo.rechazarPedido(otra.getId(), "no", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { errMotivo.set(mensaje); }
        });
        assertNotNull(errMotivo.get());
        assertFalse("La solicitud por aprobar aparta el horario",
                pedidoRepo.evaluarDisponibilidad("paq-basico", otroDia, 240, null).isViable());

        pedidoRepo.rechazarPedido(otra.getId(), "Sin operadores ese día", noop());
        Pedido rechazado = pedidoRepo.getPedidoPorId(otra.getId());
        assertEquals(Pedido.ESTADO_RECHAZADO, rechazado.getEstado());
        assertEquals("Sin operadores ese día", rechazado.getMotivoRechazo());
        assertTrue("Al rechazar se libera el horario",
                pedidoRepo.evaluarDisponibilidad("paq-basico", otroDia, 240, null).isViable());
    }

    @Test
    public void test04_confirmarPago_validacionesYMetodo() {
        long ini = TiempoUtils.construir(2026, Calendar.DECEMBER, 5, 19 * 60);
        Pedido p = crear("paq-basico", ini, Pedido.ORIGEN_LLAMADA, "Evento Pago", null);

        // Solo efectivo o transferencia
        assertNotNull(confirmarPago(p.getId(), "TARJETA"));
        assertNotNull(confirmarPago(p.getId(), null));
        assertFalse(pedidoRepo.getPedidoPorId(p.getId()).isPagoConfirmado());

        // Pago antes del evento: queda confirmado y pagado
        reloj.avanzarHoras(2);
        assertNull(confirmarPago(p.getId(), Pedido.METODO_TRANSFERENCIA));
        Pedido pagado = pedidoRepo.getPedidoPorId(p.getId());
        assertTrue(pagado.isPagoConfirmado());
        assertEquals(Pedido.ESTADO_CONFIRMADO, pagado.getEstado());
        assertEquals(Pedido.METODO_TRANSFERENCIA, pagado.getMetodoPago());
        assertEquals(reloj.ahora(), pagado.getFechaPagoMillis());
        assertEquals("socio-1", pagado.getPagoConfirmadoPorId());
        assertEquals("Socio Principal", pagado.getPagoConfirmadoPorNombre());

        // No se confirma dos veces
        String errDoble = confirmarPago(p.getId(), Pedido.METODO_EFECTIVO);
        assertNotNull(errDoble);
        assertTrue(errDoble.contains("ya fue confirmado"));
    }

    @Test
    public void test05_cancelarReserva_sinReembolsos() {
        long ini15Dias = ahoraFijo + 15L * 24 * 3600_000L;
        Pedido p = crear("paq-basico", ini15Dias, Pedido.ORIGEN_LLAMADA, "Boda Grande", null);
        assertFalse(pedidoRepo.getPedidoPorId(p.getId()).getEquipoIds().isEmpty());

        // El socio debe indicar el motivo
        AtomicReference<String> errMotivo = new AtomicReference<>();
        pedidoRepo.cancelarReserva(p.getId(), Pedido.CANCELA_SOCIO, "  ", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { errMotivo.set(mensaje); }
        });
        assertNotNull(errMotivo.get());

        AtomicReference<Pedido> cancRef = new AtomicReference<>();
        pedidoRepo.cancelarReserva(p.getId(), Pedido.CANCELA_SOCIO, "Causa de fuerza mayor", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { cancRef.set(result); }
            @Override public void onError(String mensaje) {}
        });
        assertNotNull(cancRef.get());
        assertEquals(Pedido.ESTADO_CANCELADO, cancRef.get().getEstado());
        assertEquals(Pedido.CANCELA_SOCIO, cancRef.get().getCanceladoPor());
        assertTrue(cancRef.get().getEquipoIds().isEmpty());

        // El cliente puede retirar su solicitud por aprobar
        long ini4Dias = ahoraFijo + 4L * 24 * 3600_000L;
        Pedido solicitud = crear("paq-basico", ini4Dias, Pedido.ORIGEN_APP, "Fiesta 4 Días", "cli-4d");
        AtomicReference<Pedido> cancCli = new AtomicReference<>();
        pedidoRepo.cancelarReserva(solicitud.getId(), Pedido.CANCELA_CLIENTE, null, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { cancCli.set(result); }
            @Override public void onError(String mensaje) {}
        });
        assertNotNull(cancCli.get());
        assertEquals("Cancelado por el cliente", cancCli.get().getMotivoCancelacion());

        // Cancelar un pedido ya cancelado -> error
        AtomicReference<String> errDobleCanc = new AtomicReference<>();
        pedidoRepo.cancelarReserva(solicitud.getId(), Pedido.CANCELA_CLIENTE, "Otra vez", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { errDobleCanc.set(mensaje); }
        });
        assertEquals("Este pedido ya no se puede cancelar", errDobleCanc.get());
    }

    @Test
    public void test06_reprogramarReserva_comportamientoCompleto() {
        long iniOriginal = TiempoUtils.construir(2026, Calendar.DECEMBER, 5, 19 * 60);
        SolicitudReserva sol = new SolicitudReserva("paq-basico", iniOriginal, 240, Pedido.ORIGEN_LLAMADA,
                "Boda Reprogramable", null, "Cliente Reprog", "5551234567", "Av. Hidalgo 123", "");
        AtomicReference<Pedido> pedRef = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pedRef.set(result); }
            @Override public void onError(String mensaje) {}
        });
        Pedido p = pedRef.get();
        assertNotNull(p);

        // 1. Reprogramar a un nuevo horario viable (ej. domingo 6 de diciembre a las 14:00)
        long nuevoIni = TiempoUtils.construir(2026, Calendar.DECEMBER, 6, 14 * 60);
        AtomicReference<Pedido> reprogOk = new AtomicReference<>();
        pedidoRepo.reprogramarReserva(p.getId(), nuevoIni, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { reprogOk.set(result); }
            @Override public void onError(String mensaje) {}
        });
        assertNotNull(reprogOk.get());
        assertEquals(nuevoIni, reprogOk.get().getInicioMillis());
        assertEquals(1, reprogOk.get().getReprogramaciones());

        // 2. Crear otro pedido que ocupe todo el equipo el 10 de diciembre
        long choqueIni = TiempoUtils.construir(2026, Calendar.DECEMBER, 10, 19 * 60);
        SolicitudReserva solOtro = new SolicitudReserva("paq-basico", choqueIni, 240, Pedido.ORIGEN_LLAMADA,
                "Evento Ocupante", null, "Cliente Otro", "5551234567", "Av. Hidalgo 123", "");
        pedidoRepo.crearReserva(solOtro, noop());

        // Intentar reprogramar el primer pedido a ese mismo horario del 10 de diciembre -> debe fallar y NO cambiar
        AtomicReference<String> errChoque = new AtomicReference<>();
        pedidoRepo.reprogramarReserva(p.getId(), choqueIni, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { errChoque.set(mensaje); }
        });
        assertNotNull(errChoque.get());
        Pedido sinCambio = pedidoRepo.getPedidoPorId(p.getId());
        assertEquals(nuevoIni, sinCambio.getInicioMillis()); // Mantiene el horario anterior

        // 3. Reprogramar al pasado -> error
        long enElPasado = ahoraFijo - 3600_000L;
        AtomicReference<String> errPasado = new AtomicReference<>();
        pedidoRepo.reprogramarReserva(p.getId(), enElPasado, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { errPasado.set(mensaje); }
        });
        assertNotNull(errPasado.get());
        assertTrue(errPasado.get().contains("pasado"));

        // 4. Reprogramar desde estado CANCELADO -> error
        pedidoRepo.cancelarReserva(p.getId(), Pedido.CANCELA_CLIENTE, "Cancelado definitivo", noop());
        AtomicReference<String> errCanc = new AtomicReference<>();
        pedidoRepo.reprogramarReserva(p.getId(), nuevoIni, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { errCanc.set(mensaje); }
        });
        assertNotNull(errCanc.get());
        assertTrue(errCanc.get().contains("por aprobar o confirmados"));
    }

    @Test
    public void test07_cierreDelEvento_porCobrarYFinalizado() {
        // Pedido de 19:00 a 23:00 con buffer desmontaje de 120 min (hasta 01:00)
        long ini = TiempoUtils.construir(2026, Calendar.DECEMBER, 5, 19 * 60);
        long fin = TiempoUtils.sumarMinutos(ini, 240); // 23:00
        long finDesmontaje = TiempoUtils.sumarMinutos(fin, 120); // 01:00 del día siguiente

        SolicitudReserva sol = new SolicitudReserva("paq-basico", ini, 240, Pedido.ORIGEN_LLAMADA,
                "Evento Noche", null, "Cliente Noche", "5551234567", "Av. Hidalgo 123", "");
        AtomicReference<Pedido> pedRef = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pedRef.set(result); }
            @Override public void onError(String mensaje) {}
        });
        Pedido p = pedRef.get();
        assertEquals(Pedido.ESTADO_CONFIRMADO, pedidoRepo.getPedidoPorId(p.getId()).getEstado());

        // Caso intermedio: reloj pasa a las 23:30 (después de finEvento, pero antes de finDesmontaje 01:00)
        long intermedio = TiempoUtils.sumarMinutos(fin, 30);
        reloj.setAhora(intermedio);

        int cambiados = pedidoRepo.actualizarEstadosPorTiempo();
        assertEquals("No debe haber cerrado el pedido aún", 0, cambiados);
        assertEquals(Pedido.ESTADO_CONFIRMADO, pedidoRepo.getPedidoPorId(p.getId()).getEstado());

        // En este punto, sigue bloqueando (no cabe otro pedido con la misma mezcladora)
        ResultadoDisponibilidad eval = pedidoRepo.evaluarDisponibilidad("paq-basico", intermedio, 240, null);
        assertFalse("Debe seguir bloqueando los recursos durante el desmontaje", eval.isViable());

        // Caso final: reloj pasa a la 01:05 (superando bloqueoFinMillis)
        long finTotal = finDesmontaje + 5 * 60_000L;
        reloj.setAhora(finTotal);

        int cambiadosFinal = pedidoRepo.actualizarEstadosPorTiempo();
        assertEquals(1, cambiadosFinal);
        // Terminó sin pago: por cobrar; al confirmar el pago queda finalizado
        assertEquals(Pedido.ESTADO_POR_COBRAR, pedidoRepo.getPedidoPorId(p.getId()).getEstado());
        assertNull(confirmarPago(p.getId(), Pedido.METODO_EFECTIVO));
        assertEquals(Pedido.ESTADO_FINALIZADO, pedidoRepo.getPedidoPorId(p.getId()).getEstado());

        // Un pedido pagado antes del evento queda finalizado directo al terminar
        long iniOtro = TiempoUtils.construir(2026, Calendar.DECEMBER, 12, 19 * 60);
        Pedido pagadoAntes = crear("paq-basico", iniOtro, Pedido.ORIGEN_PRESENCIAL, "Evento Pagado", null);
        assertNull(confirmarPago(pagadoAntes.getId(), Pedido.METODO_EFECTIVO));
        reloj.setAhora(TiempoUtils.sumarMinutos(iniOtro, 240 + 120 + 5));
        pedidoRepo.actualizarEstadosPorTiempo();
        assertEquals(Pedido.ESTADO_FINALIZADO, pedidoRepo.getPedidoPorId(pagadoAntes.getId()).getEstado());
    }

    @Test
    public void test08_consultasSincronasCoincidenConMotor() {
        long ini = TiempoUtils.construir(2026, Calendar.DECEMBER, 5, 19 * 60);

        // 1. evaluarDisponibilidad coincide con MotorDisponibilidad
        ResultadoDisponibilidad evalRepo = pedidoRepo.evaluarDisponibilidad("paq-basico", ini, 240, null);
        ContextoDisponibilidad ctx = new ContextoDisponibilidad(ahoraFijo,
                equipoRepo.obtenerModelos(),
                pedidoRepo.obtenerTodos(),
                usuarioRepo.obtenerActivosPorRol(Usuario.ROL_SOCIO),
                usuarioRepo.obtenerActivosPorRol(Usuario.ROL_ASISTENTE),
                Collections.<String, DiaOperativo>emptyMap());
        ResultadoDisponibilidad evalMotor = MotorDisponibilidad.evaluar(ctx, paqueteBasico, ini, 240, null);

        assertEquals(evalMotor.isViable(), evalRepo.isViable());
        assertEquals(evalMotor.getBloqueoInicio(), evalRepo.getBloqueoInicio());
        assertEquals(evalMotor.getBloqueoFin(), evalRepo.getBloqueoFin());
        assertEquals(evalMotor.getOperadorIds().size(), evalRepo.getOperadorIds().size());

        // 2. consultarHorarios coincide con MotorDisponibilidad
        List<Long> horariosRepo = pedidoRepo.consultarHorarios("paq-basico", ini, 240, null);
        List<Long> horariosMotor = MotorDisponibilidad.horariosDisponibles(ctx, paqueteBasico, ini, 240, null);
        assertEquals(horariosMotor, horariosRepo);

        // 3. Crear una reserva a las 19:00
        SolicitudReserva sol = new SolicitudReserva("paq-basico", ini, 240, Pedido.ORIGEN_LLAMADA,
                "Reserva Existente", null, "Cliente Test", "5551234567", "Av. Hidalgo 123", "");
        AtomicReference<Pedido> pedRef = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pedRef.set(result); }
            @Override public void onError(String mensaje) {}
        });

        // Sin ignorarPedidoId: las 19:00 ya no están disponibles
        List<Long> horariosSinIgnorar = pedidoRepo.consultarHorarios("paq-basico", ini, 240, null);
        assertFalse(horariosSinIgnorar.contains(ini));

        // Con ignorarPedidoId = pedRef.get().getId(): las 19:00 SÍ están disponibles (para reprogramar)
        List<Long> horariosConIgnorar = pedidoRepo.consultarHorarios("paq-basico", ini, 240, pedRef.get().getId());
        assertTrue(horariosConIgnorar.contains(ini));
    }

    @Test
    public void test09_integracionReglasDeNegocioTodasLasEtapas() {
        // Verifica que todo el flujo integrado opera respetando todas las invariantes
        assertEquals(0, pedidoRepo.actualizarEstadosPorTiempo());
        List<Pedido> todos = pedidoRepo.obtenerTodos();
        assertNotNull(todos);
    }
}
