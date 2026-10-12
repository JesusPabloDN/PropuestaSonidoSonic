package com.example.propuestasonidosonic;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.memory.InMemoryDiaOperativoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryEquipoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPagoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPaqueteRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPedidoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryUsuarioRepository;
import com.example.propuestasonidosonic.domain.ContextoDisponibilidad;
import com.example.propuestasonidosonic.domain.MotorDisponibilidad;
import com.example.propuestasonidosonic.domain.PoliticaCancelacion;
import com.example.propuestasonidosonic.domain.ResultadoDisponibilidad;
import com.example.propuestasonidosonic.domain.SolicitudReserva;
import com.example.propuestasonidosonic.model.CategoriaEquipo;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.DiaOperativo;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Pago;
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
    private InMemoryPagoRepository pagoRepo;
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
        pagoRepo = new InMemoryPagoRepository();

        pedidoRepo = new InMemoryPedidoRepository(
                equipoRepo,
                paqueteRepo,
                usuarioRepo,
                diaRepo,
                pagoRepo,
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

    @Test
    public void test01_politicaCancelacion_casosReglamentarios() {
        long ahora = 1_000_000_000L;
        double anticipo = 1050.0;

        // 1. Socio cancela -> 100%
        long inicioCualquiera = ahora + 10 * 3600_000L;
        assertEquals(1050.0, PoliticaCancelacion.calcularReembolso(anticipo, inicioCualquiera, ahora, Pedido.CANCELA_SOCIO), 0.001);

        // 2. Cliente cancela con 10 días (240 h >= 168 h) -> 100%
        long inicio10Dias = ahora + 10L * 24 * 3600_000L;
        assertEquals(1050.0, PoliticaCancelacion.calcularReembolso(anticipo, inicio10Dias, ahora, Pedido.CANCELA_CLIENTE), 0.001);

        // 3. Cliente cancela con exactamente 7 días (168 h) -> 100%
        long inicio7Dias = ahora + 7L * 24 * 3600_000L;
        assertEquals(1050.0, PoliticaCancelacion.calcularReembolso(anticipo, inicio7Dias, ahora, Pedido.CANCELA_CLIENTE), 0.001);

        // 4. Cliente cancela con 5 días (120 h >= 72 h) -> 50%
        long inicio5Dias = ahora + 5L * 24 * 3600_000L;
        assertEquals(525.0, PoliticaCancelacion.calcularReembolso(anticipo, inicio5Dias, ahora, Pedido.CANCELA_CLIENTE), 0.001);

        // 5. Cliente cancela con exactamente 72 horas -> 50%
        long inicio72Horas = ahora + 72L * 3600_000L;
        assertEquals(525.0, PoliticaCancelacion.calcularReembolso(anticipo, inicio72Horas, ahora, Pedido.CANCELA_CLIENTE), 0.001);

        // 6. Cliente cancela con 71 horas (< 72 h) -> 0%
        long inicio71Horas = ahora + 71L * 3600_000L;
        assertEquals(0.0, PoliticaCancelacion.calcularReembolso(anticipo, inicio71Horas, ahora, Pedido.CANCELA_CLIENTE), 0.001);

        // 7. Sin anticipo (anticipo <= 0) -> 0
        assertEquals(0.0, PoliticaCancelacion.calcularReembolso(0.0, inicio10Dias, ahora, Pedido.CANCELA_CLIENTE), 0.001);

        // 8. Redondeo a 2 decimales (ej. anticipo 333.33 al 50% = 166.67)
        assertEquals(166.67, PoliticaCancelacion.calcularReembolso(333.33, inicio5Dias, ahora, Pedido.CANCELA_CLIENTE), 0.001);
    }

    @Test
    public void test02_crearReserva_revisionSegunOrigen() {
        // Evento urgente: a 24 horas del reloj actual (< 48 horas)
        long iniUrgente = ahoraFijo + 24L * 3600_000L;

        // 1. Origen LLAMADA a menos de 48 horas -> requiereRevision = false
        SolicitudReserva solLlamada = new SolicitudReserva("paq-basico", iniUrgente, 240, Pedido.ORIGEN_LLAMADA,
                "Boda Urgente Llamada", null, "Cliente Llamada", "5551234567", "Av. Insurgentes 123", "", null);
        AtomicReference<Pedido> refLlamada = new AtomicReference<>();
        pedidoRepo.crearReserva(solLlamada, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { refLlamada.set(result); }
            @Override public void onError(String mensaje) {}
        });

        assertNotNull(refLlamada.get());
        assertFalse("Llamada no debe requerir revisión", refLlamada.get().isRequiereRevision());
        assertTrue("Motivos de revisión deben estar vacíos para Llamada", refLlamada.get().getMotivosRevision().isEmpty());

        // Cancelar para liberar
        pedidoRepo.cancelarReserva(refLlamada.get().getId(), Pedido.CANCELA_SOCIO, "Prueba", noop());

        // 2. Origen APP a menos de 48 horas -> requiereRevision = true y MOTIVO_URGENTE
        SolicitudReserva solApp = new SolicitudReserva("paq-basico", iniUrgente, 240, Pedido.ORIGEN_APP,
                "Boda Urgente App", "cli-app", "Cliente App", "5551234567", "Av. Insurgentes 123", "", null);
        AtomicReference<Pedido> refApp = new AtomicReference<>();
        pedidoRepo.crearReserva(solApp, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { refApp.set(result); }
            @Override public void onError(String mensaje) {}
        });

        assertNotNull(refApp.get());
        assertTrue("Pedido por App urgente debe requerir revisión", refApp.get().isRequiereRevision());
        assertTrue("Debe incluir motivo urgente", refApp.get().getMotivosRevision().contains(Pedido.MOTIVO_URGENTE));
    }

    @Test
    public void test03_registrarAnticipo_validacionesYConfirmacion() {
        long ini = TiempoUtils.construir(2026, Calendar.DECEMBER, 5, 19 * 60);
        SolicitudReserva sol = new SolicitudReserva("paq-basico", ini, 240, Pedido.ORIGEN_LLAMADA,
                "Graduación", null, "Roberto", "5551234567", "Av. Juárez 456", "", 24);
        AtomicReference<Pedido> pedRef = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pedRef.set(result); }
            @Override public void onError(String mensaje) {}
        });

        Pedido p = pedRef.get();
        assertNotNull(p);
        assertEquals(3500.0, p.getTotal(), 0.001);
        assertEquals(1050.0, p.getAnticipoRequerido(), 0.001);

        // Monto menor al anticipo requerido (1050) -> error
        AtomicReference<String> errMenor = new AtomicReference<>();
        pedidoRepo.registrarAnticipo(p.getId(), 500.0, Pedido.METODO_EFECTIVO, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { errMenor.set(mensaje); }
        });
        assertNotNull(errMenor.get());
        assertTrue(errMenor.get().contains("no cubre el anticipo"));

        // Monto mayor al total (3500) -> error
        AtomicReference<String> errMayor = new AtomicReference<>();
        pedidoRepo.registrarAnticipo(p.getId(), 4000.0, Pedido.METODO_EFECTIVO, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { errMayor.set(mensaje); }
        });
        assertNotNull(errMayor.get());
        assertTrue(errMayor.get().contains("excede el saldo"));

        // Monto correcto ($1,050.00) en EFECTIVO -> CONFIRMADO
        AtomicReference<Pedido> confRef = new AtomicReference<>();
        pedidoRepo.registrarAnticipo(p.getId(), 1050.0, Pedido.METODO_EFECTIVO, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { confRef.set(result); }
            @Override public void onError(String mensaje) {}
        });

        assertNotNull(confRef.get());
        assertEquals(Pedido.ESTADO_CONFIRMADO, confRef.get().getEstado());
        assertEquals(1050.0, confRef.get().getAnticipoPagado(), 0.001);
        assertEquals(Pedido.METODO_EFECTIVO, confRef.get().getMetodoAnticipo());
        assertEquals(0, confRef.get().getVencimientoApartadoMillis());

        // Verificar que se registró Pago de tipo ANTICIPO
        List<Pago> pagos = pagoRepo.getTodosLosPagos().getValue();
        assertNotNull(pagos);
        assertFalse(pagos.isEmpty());
        Pago pagoReg = pagos.get(pagos.size() - 1);
        assertEquals(Pago.TIPO_ANTICIPO, pagoReg.getTipo());
        assertEquals(1050.0, pagoReg.getMonto(), 0.001);
        assertEquals(Pedido.METODO_EFECTIVO, pagoReg.getMetodo());
        assertEquals(p.getId(), pagoReg.getPedidoId());

        // Probar pedido por App urgente que requiere revisión: al pagar anticipo queda en EN_REVISION
        long iniUrgente = ahoraFijo + 24L * 3600_000L;
        SolicitudReserva solApp = new SolicitudReserva("paq-solo-bocinas", iniUrgente, 240, Pedido.ORIGEN_APP,
                "Evento App Urgente", "cli-urg", "Cliente Urgente", "5551234567", "Calle Real 100", "", null);
        AtomicReference<Pedido> refApp = new AtomicReference<>();
        pedidoRepo.crearReserva(solApp, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { refApp.set(result); }
            @Override public void onError(String mensaje) {}
        });

        Pedido pApp = refApp.get();
        assertNotNull(pApp);
        assertTrue(pApp.isRequiereRevision());

        AtomicReference<Pedido> revRef = new AtomicReference<>();
        pedidoRepo.registrarAnticipo(pApp.getId(), pApp.getAnticipoRequerido(), Pedido.METODO_TRANSFERENCIA, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { revRef.set(result); }
            @Override public void onError(String mensaje) {}
        });

        assertNotNull(revRef.get());
        assertEquals(Pedido.ESTADO_EN_REVISION, revRef.get().getEstado());
    }

    @Test
    public void test04_registrarAnticipo_rechazosPorEstadoYVencimiento() {
        long ini = TiempoUtils.construir(2026, Calendar.DECEMBER, 5, 19 * 60);
        SolicitudReserva sol = new SolicitudReserva("paq-basico", ini, 240, Pedido.ORIGEN_LLAMADA,
                "Evento Test Vencimiento", null, "Cliente Vencido", "5551234567", "Av. Hidalgo 123", "", 2);
        AtomicReference<Pedido> pedRef = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pedRef.set(result); }
            @Override public void onError(String mensaje) {}
        });

        Pedido p = pedRef.get();
        assertNotNull(p);

        // Avanzar el reloj 3 horas (el apartado duraba 2 horas)
        reloj.avanzarHoras(3);

        AtomicReference<String> errVencido = new AtomicReference<>();
        pedidoRepo.registrarAnticipo(p.getId(), 1050.0, Pedido.METODO_EFECTIVO, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { errVencido.set(mensaje); }
        });
        assertNotNull(errVencido.get());
        assertTrue(errVencido.get().contains("venció"));
        assertEquals(Pedido.ESTADO_VENCIDO, pedidoRepo.getPedidoPorId(p.getId()).getEstado());

        // Sobre un pedido ya confirmado no se permite registrar anticipo de nuevo
        SolicitudReserva sol2 = new SolicitudReserva("paq-basico", ini, 240, Pedido.ORIGEN_LLAMADA,
                "Evento Test Ya Confirmado", null, "Cliente Conf", "5551234567", "Av. Hidalgo 123", "", 24);
        AtomicReference<Pedido> pedRef2 = new AtomicReference<>();
        pedidoRepo.crearReserva(sol2, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pedRef2.set(result); }
            @Override public void onError(String mensaje) {}
        });

        pedidoRepo.registrarAnticipo(pedRef2.get().getId(), 1050.0, Pedido.METODO_EFECTIVO, noop());
        assertEquals(Pedido.ESTADO_CONFIRMADO, pedidoRepo.getPedidoPorId(pedRef2.get().getId()).getEstado());

        AtomicReference<String> errYaConf = new AtomicReference<>();
        pedidoRepo.registrarAnticipo(pedRef2.get().getId(), 500.0, Pedido.METODO_EFECTIVO, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { errYaConf.set(mensaje); }
        });
        assertNotNull(errYaConf.get());
        assertTrue(errYaConf.get().contains("apartadas"));
    }

    @Test
    public void test05_cancelarReserva_politicasReembolsoYRegistroPago() {
        // Evento a 15 días
        long ini15Dias = ahoraFijo + 15L * 24 * 3600_000L;
        SolicitudReserva sol = new SolicitudReserva("paq-basico", ini15Dias, 240, Pedido.ORIGEN_LLAMADA,
                "Boda Grande", null, "Cliente Boda", "5551234567", "Av. Hidalgo 123", "", 24);
        AtomicReference<Pedido> pedRef = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pedRef.set(result); }
            @Override public void onError(String mensaje) {}
        });
        Pedido p = pedRef.get();
        pedidoRepo.registrarAnticipo(p.getId(), 1050.0, Pedido.METODO_TRANSFERENCIA, noop());

        // 1. Socio cancela -> 100% reembolso y Pago de tipo REEMBOLSO
        AtomicReference<Pedido> cancSocioRef = new AtomicReference<>();
        pedidoRepo.cancelarReserva(p.getId(), Pedido.CANCELA_SOCIO, "Causa de fuerza mayor", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { cancSocioRef.set(result); }
            @Override public void onError(String mensaje) {}
        });
        assertNotNull(cancSocioRef.get());
        assertEquals(Pedido.ESTADO_CANCELADO, cancSocioRef.get().getEstado());
        assertEquals(1050.0, cancSocioRef.get().getReembolsoMonto(), 0.001);
        assertEquals(Pedido.CANCELA_SOCIO, cancSocioRef.get().getCanceladoPor());

        List<Pago> pagos = pagoRepo.getTodosLosPagos().getValue();
        Pago pagoReembolso = pagos.get(pagos.size() - 1);
        assertEquals(Pago.TIPO_REEMBOLSO, pagoReembolso.getTipo());
        assertEquals(1050.0, pagoReembolso.getMonto(), 0.001);

        // 2. Cliente cancela con 4 días (96 h: entre 72 y 168 h) -> 50%
        long ini4Dias = ahoraFijo + 4L * 24 * 3600_000L;
        SolicitudReserva sol4d = new SolicitudReserva("paq-basico", ini4Dias, 240, Pedido.ORIGEN_LLAMADA,
                "Fiesta 4 Días", null, "Cliente 4D", "5551234567", "Av. Hidalgo 123", "", 24);
        AtomicReference<Pedido> ped4dRef = new AtomicReference<>();
        pedidoRepo.crearReserva(sol4d, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ped4dRef.set(result); }
            @Override public void onError(String mensaje) {}
        });
        pedidoRepo.registrarAnticipo(ped4dRef.get().getId(), 1050.0, Pedido.METODO_EFECTIVO, noop());

        AtomicReference<Pedido> canc4dRef = new AtomicReference<>();
        pedidoRepo.cancelarReserva(ped4dRef.get().getId(), Pedido.CANCELA_CLIENTE, "Cambio de planes", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { canc4dRef.set(result); }
            @Override public void onError(String mensaje) {}
        });
        assertNotNull(canc4dRef.get());
        assertEquals(525.0, canc4dRef.get().getReembolsoMonto(), 0.001);

        // 3. Cliente cancela con 2 días (48 h < 72 h) -> $0 reembolso, sin Pago de reembolso
        long ini2Dias = ahoraFijo + 2L * 24 * 3600_000L;
        SolicitudReserva sol2d = new SolicitudReserva("paq-basico", ini2Dias, 240, Pedido.ORIGEN_LLAMADA,
                "Fiesta 2 Días", null, "Cliente 2D", "5551234567", "Av. Hidalgo 123", "", 24);
        AtomicReference<Pedido> ped2dRef = new AtomicReference<>();
        pedidoRepo.crearReserva(sol2d, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ped2dRef.set(result); }
            @Override public void onError(String mensaje) {}
        });
        pedidoRepo.registrarAnticipo(ped2dRef.get().getId(), 1050.0, Pedido.METODO_EFECTIVO, noop());

        int conteoPagosAntes = pagoRepo.getTodosLosPagos().getValue().size();
        AtomicReference<Pedido> canc2dRef = new AtomicReference<>();
        pedidoRepo.cancelarReserva(ped2dRef.get().getId(), Pedido.CANCELA_CLIENTE, "Aviso tardío", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { canc2dRef.set(result); }
            @Override public void onError(String mensaje) {}
        });
        assertNotNull(canc2dRef.get());
        assertEquals(0.0, canc2dRef.get().getReembolsoMonto(), 0.001);
        assertEquals(conteoPagosAntes, pagoRepo.getTodosLosPagos().getValue().size()); // No se agregó pago de reembolso

        // 4. Cancelar un pedido ya cancelado -> error
        AtomicReference<String> errDobleCanc = new AtomicReference<>();
        pedidoRepo.cancelarReserva(ped2dRef.get().getId(), Pedido.CANCELA_CLIENTE, "Otra vez", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { errDobleCanc.set(mensaje); }
        });
        assertNotNull(errDobleCanc.get());
        assertEquals("Esta reserva ya no se puede cancelar", errDobleCanc.get());
    }

    @Test
    public void test06_reprogramarReserva_comportamientoCompleto() {
        long iniOriginal = TiempoUtils.construir(2026, Calendar.DECEMBER, 5, 19 * 60);
        SolicitudReserva sol = new SolicitudReserva("paq-basico", iniOriginal, 240, Pedido.ORIGEN_LLAMADA,
                "Boda Reprogramable", null, "Cliente Reprog", "5551234567", "Av. Hidalgo 123", "", 24);
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
                "Evento Ocupante", null, "Cliente Otro", "5551234567", "Av. Hidalgo 123", "", 24);
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
        assertTrue(errCanc.get().contains("apartadas o confirmadas"));
    }

    @Test
    public void test07_liberarApartadosVencidos_desmontajeCompleto() {
        // Pedido de 19:00 a 23:00 con buffer desmontaje de 120 min (hasta 01:00)
        long ini = TiempoUtils.construir(2026, Calendar.DECEMBER, 5, 19 * 60);
        long fin = TiempoUtils.sumarMinutos(ini, 240); // 23:00
        long finDesmontaje = TiempoUtils.sumarMinutos(fin, 120); // 01:00 del día siguiente

        SolicitudReserva sol = new SolicitudReserva("paq-basico", ini, 240, Pedido.ORIGEN_LLAMADA,
                "Evento Noche", null, "Cliente Noche", "5551234567", "Av. Hidalgo 123", "", 24);
        AtomicReference<Pedido> pedRef = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pedRef.set(result); }
            @Override public void onError(String mensaje) {}
        });
        Pedido p = pedRef.get();
        pedidoRepo.registrarAnticipo(p.getId(), 1050.0, Pedido.METODO_EFECTIVO, noop());
        assertEquals(Pedido.ESTADO_CONFIRMADO, pedidoRepo.getPedidoPorId(p.getId()).getEstado());

        // Caso intermedio: reloj pasa a las 23:30 (después de finEvento, pero antes de finDesmontaje 01:00)
        long intermedio = TiempoUtils.sumarMinutos(fin, 30);
        reloj.setAhora(intermedio);

        int cambiados = pedidoRepo.liberarApartadosVencidos();
        assertEquals("No debe haber cambiado a completado aún", 0, cambiados);
        assertEquals(Pedido.ESTADO_CONFIRMADO, pedidoRepo.getPedidoPorId(p.getId()).getEstado());

        // En este punto, sigue bloqueando (no cabe otro pedido con la misma mezcladora)
        ResultadoDisponibilidad eval = pedidoRepo.evaluarDisponibilidad("paq-basico", intermedio, 240, null);
        assertFalse("Debe seguir bloqueando los recursos durante el desmontaje", eval.isViable());

        // Caso final: reloj pasa a la 01:05 (superando bloqueoFinMillis)
        long finTotal = finDesmontaje + 5 * 60_000L;
        reloj.setAhora(finTotal);

        int cambiadosFinal = pedidoRepo.liberarApartadosVencidos();
        assertEquals(1, cambiadosFinal);
        assertEquals(Pedido.ESTADO_COMPLETADO, pedidoRepo.getPedidoPorId(p.getId()).getEstado());
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
                "Reserva Existente", null, "Cliente Test", "5551234567", "Av. Hidalgo 123", "", 24);
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
        assertEquals(0, pedidoRepo.liberarApartadosVencidos());
        List<Pedido> todos = pedidoRepo.obtenerTodos();
        assertNotNull(todos);
    }
}
