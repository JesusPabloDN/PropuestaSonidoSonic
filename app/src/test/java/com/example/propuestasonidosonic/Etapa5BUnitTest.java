package com.example.propuestasonidosonic;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.memory.InMemoryDiaOperativoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryEquipoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryEventoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPaqueteRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPedidoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryUsuarioRepository;
import com.example.propuestasonidosonic.domain.ContactoUtils;
import com.example.propuestasonidosonic.domain.PedidosClienteUtils;
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Etapa5BUnitTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static class RelojFalso implements Reloj {
        long tiempo;
        RelojFalso(long tiempo) { this.tiempo = tiempo; }
        @Override public long ahora() { return tiempo; }
        public void setAhora(long tiempo) { this.tiempo = tiempo; }
        public void avanzarMinutos(int minutos) { this.tiempo += ((long) minutos) * 60_000L; }
        public void avanzarHoras(int horas) { this.tiempo += ((long) horas) * 3600_000L; }
    }

    private static <T> Callback<T> noop() {
        return new Callback<T>() {
            @Override public void onSuccess(T result) {}
            @Override public void onError(String error) {}
        };
    }

    private RelojFalso reloj;
    private InMemoryEquipoRepository equipoRepo;
    private InMemoryPaqueteRepository paqueteRepo;
    private InMemoryUsuarioRepository usuarioRepo;
    private InMemoryDiaOperativoRepository diaRepo;
    private InMemoryEventoRepository eventoRepo;
    private InMemoryPedidoRepository pedidoRepo;

    private Paquete paqueteBasico;
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
        eventoRepo = new InMemoryEventoRepository();

        equipoRepo.setEventoRepository(eventoRepo);

        pedidoRepo = new InMemoryPedidoRepository(
                equipoRepo,
                paqueteRepo,
                usuarioRepo,
                diaRepo,
                eventoRepo,
                reloj
        );

        equipoRepo.setOnInventarioCambio(() -> pedidoRepo.reasignarUnidades(null));

        // Unidades de equipo
        equipoRepo.agregarEquipo(new Equipo("boc-1", "Bocina 15", CategoriaEquipo.BOCINAS,
                1, Equipo.ESTADO_DISPONIBLE, 500.0), noop());
        equipoRepo.agregarEquipo(new Equipo("boc-2", "Bocina 15", CategoriaEquipo.BOCINAS,
                2, Equipo.ESTADO_DISPONIBLE, 500.0), noop());

        // Operadores
        Usuario socio = new Usuario("u-socio", "Carlos Socio", "carlos@sonic.com", "9611234567",
                Usuario.ROL_SOCIO, "Admin#2026", true, ahoraFijo);
        usuarioRepo.crearPersonal(socio, "Admin#2026", noop());

        Usuario asistente = new Usuario("u-asist", "Pedro Asistente", "pedro@sonic.com", "9617654321",
                Usuario.ROL_ASISTENTE, "Admin#2026", true, ahoraFijo);
        usuarioRepo.crearPersonal(asistente, "Admin#2026", noop());

        // Paquete básico: 1 bocina, 4 horas, precio base $3,500
        paqueteBasico = new Paquete();
        paqueteBasico.setId("paq-basico");
        paqueteBasico.setNombre("Paquete Básico");
        paqueteBasico.setDescripcion("Paquete para eventos pequeños");
        paqueteBasico.setPrecio(3500.0);
        paqueteBasico.setDuracionBaseHoras(4);
        paqueteBasico.setPrecioHoraExtra(500.0);
        paqueteBasico.setOperadoresRequeridos(1);
        paqueteBasico.setBufferMontajeMin(90);
        paqueteBasico.setBufferDesmontajeMin(120);
        paqueteBasico.setActivo(true);
        paqueteBasico.setComponentes(Collections.singletonList(
                new ComponentePaquete(
                        ModeloUtils.clave(CategoriaEquipo.BOCINAS, "Bocina 15"),
                        "Bocina 15",
                        CategoriaEquipo.BOCINAS,
                        1
                )
        ));
        paqueteRepo.guardarPaquete(paqueteBasico, noop());
    }

    // 1. delCliente: no devuelve pedidos de otros clientes; id nulo o vacío -> lista vacía
    @Test
    public void test01_delCliente() {
        Pedido p1 = new Pedido();
        p1.setId("p1");
        p1.setClienteId("cli-1");

        Pedido p2 = new Pedido();
        p2.setId("p2");
        p2.setClienteId("cli-2");

        Pedido p3 = new Pedido();
        p3.setId("p3");
        p3.setClienteId("cli-1");

        List<Pedido> todos = Arrays.asList(p1, p2, p3);

        List<Pedido> cli1 = PedidosClienteUtils.delCliente(todos, "cli-1");
        assertEquals(2, cli1.size());
        assertEquals("p1", cli1.get(0).getId());
        assertEquals("p3", cli1.get(1).getId());

        assertTrue(PedidosClienteUtils.delCliente(todos, null).isEmpty());
        assertTrue(PedidosClienteUtils.delCliente(todos, "").isEmpty());
        assertTrue(PedidosClienteUtils.delCliente(todos, "   ").isEmpty());
        assertTrue(PedidosClienteUtils.delCliente(null, "cli-1").isEmpty());
    }

    // 2. esVisible/activos/historial:
    //    solicitud por aprobar vigente y confirmados futuros son activos;
    //    confirmado cuyo bloqueo ya terminó va al historial;
    //    cancelado y rechazado van al historial;
    //    vencido y solicitud cuyo evento ya empezó no aparecen en ninguna;
    //    orden ascendente en activos y descendente en historial.
    @Test
    public void test02_esVisible_activos_historial() {
        // A. Solicitud por aprobar con evento en 3 días -> visible y activa
        Pedido pApartadoVigente = new Pedido();
        pApartadoVigente.setId("p-ap-vig");
        pApartadoVigente.setEstado(Pedido.ESTADO_POR_APROBAR);
        pApartadoVigente.setInicioMillis(ahoraFijo + 72 * 3600_000L);
        pApartadoVigente.setFinMillis(ahoraFijo + 76 * 3600_000L);

        // B. Solicitud por aprobar cuyo evento ya empezó (está por vencer) -> invisible
        Pedido pApartadoVencido = new Pedido();
        pApartadoVencido.setId("p-ap-venc");
        pApartadoVencido.setEstado(Pedido.ESTADO_POR_APROBAR);
        pApartadoVencido.setInicioMillis(ahoraFijo - 5 * 60_000L);
        pApartadoVencido.setFinMillis(ahoraFijo + 4 * 3600_000L);

        // C. Vencido explícito -> invisible
        Pedido pVencido = new Pedido();
        pVencido.setId("p-vencido");
        pVencido.setEstado(Pedido.ESTADO_VENCIDO);

        // D. Confirmado futuro 1 (en 4 días) -> visible y activo
        Pedido pConfFuturo1 = new Pedido();
        pConfFuturo1.setId("p-conf-4d");
        pConfFuturo1.setEstado(Pedido.ESTADO_CONFIRMADO);
        pConfFuturo1.setInicioMillis(ahoraFijo + 96 * 3600_000L);
        pConfFuturo1.setFinMillis(ahoraFijo + 100 * 3600_000L);
        pConfFuturo1.setBloqueoFinMillis(ahoraFijo + 101 * 3600_000L);

        // E. Confirmado futuro 2 (en 2 días) -> visible y activo
        Pedido pConfFuturo2 = new Pedido();
        pConfFuturo2.setId("p-conf-2d");
        pConfFuturo2.setEstado(Pedido.ESTADO_CONFIRMADO);
        pConfFuturo2.setInicioMillis(ahoraFijo + 48 * 3600_000L);
        pConfFuturo2.setFinMillis(ahoraFijo + 52 * 3600_000L);
        pConfFuturo2.setBloqueoFinMillis(ahoraFijo + 53 * 3600_000L);

        // F. Confirmado pasado cuyo bloqueo ya terminó -> visible y en historial
        Pedido pConfPasado = new Pedido();
        pConfPasado.setId("p-conf-pasado");
        pConfPasado.setEstado(Pedido.ESTADO_CONFIRMADO);
        pConfPasado.setInicioMillis(ahoraFijo - 24 * 3600_000L);
        pConfPasado.setFinMillis(ahoraFijo - 20 * 3600_000L);
        pConfPasado.setBloqueoFinMillis(ahoraFijo - 19 * 3600_000L);

        // G. Cancelado -> visible y en historial
        Pedido pCancelado = new Pedido();
        pCancelado.setId("p-cancelado");
        pCancelado.setEstado(Pedido.ESTADO_CANCELADO);
        pCancelado.setInicioMillis(ahoraFijo - 10 * 3600_000L);

        // H. Rechazado -> visible y en historial
        Pedido pRechazado = new Pedido();
        pRechazado.setId("p-rechazado");
        pRechazado.setEstado(Pedido.ESTADO_RECHAZADO);
        pRechazado.setInicioMillis(ahoraFijo - 5 * 3600_000L);

        List<Pedido> todos = Arrays.asList(
                pApartadoVigente, pApartadoVencido, pVencido,
                pConfFuturo1, pConfFuturo2, pConfPasado,
                pCancelado, pRechazado
        );

        // Visibilidad individual
        assertTrue(PedidosClienteUtils.esVisible(pApartadoVigente, ahoraFijo));
        assertFalse(PedidosClienteUtils.esVisible(pApartadoVencido, ahoraFijo));
        assertFalse(PedidosClienteUtils.esVisible(pVencido, ahoraFijo));
        assertTrue(PedidosClienteUtils.esVisible(pConfFuturo1, ahoraFijo));
        assertTrue(PedidosClienteUtils.esVisible(pConfPasado, ahoraFijo));
        assertTrue(PedidosClienteUtils.esVisible(pCancelado, ahoraFijo));
        assertTrue(PedidosClienteUtils.esVisible(pRechazado, ahoraFijo));

        // Activos
        List<Pedido> activos = PedidosClienteUtils.activos(todos, ahoraFijo);
        assertEquals(3, activos.size());
        // Orden ascendente por inicioMillis: pConfFuturo2 (48h), pApartadoVigente (72h), pConfFuturo1 (96h)
        assertEquals("p-conf-2d", activos.get(0).getId());
        assertEquals("p-ap-vig", activos.get(1).getId());
        assertEquals("p-conf-4d", activos.get(2).getId());

        // Historial
        List<Pedido> historial = PedidosClienteUtils.historial(todos, ahoraFijo);
        assertEquals(3, historial.size());
        // Orden descendente por inicioMillis: pRechazado (-5h), pCancelado (-10h), pConfPasado (-24h)
        assertEquals("p-rechazado", historial.get(0).getId());
        assertEquals("p-cancelado", historial.get(1).getId());
        assertEquals("p-conf-pasado", historial.get(2).getId());
    }

    // 3. puedeCancelar: cierto para solicitud por aprobar y confirmado antes de iniciar;
    //    falso al iniciar el evento; falso para por cobrar, finalizado, cancelado, rechazado y vencido.
    @Test
    public void test03_puedeCancelar() {
        long futuro = ahoraFijo + 10 * 3600_000L;
        long pasado = ahoraFijo - 1000L;

        Pedido pSolicitud = new Pedido();
        pSolicitud.setEstado(Pedido.ESTADO_POR_APROBAR);
        pSolicitud.setInicioMillis(futuro);
        assertTrue(PedidosClienteUtils.puedeCancelar(pSolicitud, ahoraFijo));

        Pedido pConf = new Pedido();
        pConf.setEstado(Pedido.ESTADO_CONFIRMADO);
        pConf.setInicioMillis(futuro);
        assertTrue(PedidosClienteUtils.puedeCancelar(pConf, ahoraFijo));

        // Pagado por adelantado: también se puede cancelar (la devolución se acuerda fuera de la app)
        Pedido pPagado = new Pedido();
        pPagado.setEstado(Pedido.ESTADO_CONFIRMADO);
        pPagado.setPagoConfirmado(true);
        pPagado.setInicioMillis(futuro);
        assertTrue(PedidosClienteUtils.puedeCancelar(pPagado, ahoraFijo));

        // Evento ya iniciado (inicio <= ahora)
        Pedido pIniciado = new Pedido();
        pIniciado.setEstado(Pedido.ESTADO_CONFIRMADO);
        pIniciado.setInicioMillis(ahoraFijo);
        assertFalse(PedidosClienteUtils.puedeCancelar(pIniciado, ahoraFijo));

        Pedido pPasado = new Pedido();
        pPasado.setEstado(Pedido.ESTADO_CONFIRMADO);
        pPasado.setInicioMillis(pasado);
        assertFalse(PedidosClienteUtils.puedeCancelar(pPasado, ahoraFijo));

        for (String estado : Arrays.asList(Pedido.ESTADO_POR_COBRAR, Pedido.ESTADO_FINALIZADO,
                Pedido.ESTADO_CANCELADO, Pedido.ESTADO_RECHAZADO, Pedido.ESTADO_VENCIDO)) {
            Pedido p = new Pedido();
            p.setEstado(estado);
            p.setInicioMillis(futuro);
            assertFalse("No se cancela en estado " + estado, PedidosClienteUtils.puedeCancelar(p, ahoraFijo));
        }
        assertFalse(PedidosClienteUtils.puedeCancelar(null, ahoraFijo));
    }

    // 5. etiquetaEstado para el cliente
    @Test
    public void test05_etiquetaEstado() {
        assertEquals("Por aprobar", PedidosClienteUtils.etiquetaEstado(Pedido.ESTADO_POR_APROBAR));
        assertEquals("Confirmado", PedidosClienteUtils.etiquetaEstado(Pedido.ESTADO_CONFIRMADO));
        assertEquals("Pago pendiente", PedidosClienteUtils.etiquetaEstado(Pedido.ESTADO_POR_COBRAR));
        assertEquals("Realizado", PedidosClienteUtils.etiquetaEstado(Pedido.ESTADO_FINALIZADO));
        assertEquals("Cancelado", PedidosClienteUtils.etiquetaEstado(Pedido.ESTADO_CANCELADO));
        assertEquals("No aprobado", PedidosClienteUtils.etiquetaEstado(Pedido.ESTADO_RECHAZADO));
        assertEquals("Vencido", PedidosClienteUtils.etiquetaEstado(Pedido.ESTADO_VENCIDO));
        assertEquals("ESTADO_INVENTADO", PedidosClienteUtils.etiquetaEstado("ESTADO_INVENTADO"));
        assertEquals("", PedidosClienteUtils.etiquetaEstado(null));
    }

    // 6. urlWhatsApp: acepta '747 123 4567' y '+52 (747) 123-4567', codifica espacios y acentos, teléfono corto -> vacío
    @Test
    public void test06_contactoUtils_urlWhatsApp_y_mensajes() {
        String url1 = ContactoUtils.urlWhatsApp("747 123 4567", "Hola mundo");
        String url2 = ContactoUtils.urlWhatsApp("+52 (747) 123-4567", "Hola mundo");
        assertEquals("https://wa.me/527471234567?text=Hola%20mundo", url1);
        assertEquals(url1, url2);

        // Acentos
        String urlAcentos = ContactoUtils.urlWhatsApp("7471234567", "Duda sobre mi canción");
        assertEquals("https://wa.me/527471234567?text=Duda%20sobre%20mi%20canci%C3%B3n", urlAcentos);

        // Teléfono corto -> cadena vacía
        assertEquals("", ContactoUtils.urlWhatsApp("12345", "Hola"));
        assertEquals("", ContactoUtils.urlWhatsApp(null, "Hola"));

        // Mensaje vacío
        assertEquals("https://wa.me/527471234567", ContactoUtils.urlWhatsApp("7471234567", ""));

        // Mensajes predefinidos
        assertEquals("Hola, quiero cambiar la fecha de mi pedido #1001 (Boda de Ana).",
                ContactoUtils.mensajeCambioFecha(1001, "Boda de Ana"));
        assertEquals("Hola, tengo una duda sobre mi pedido #1001 (Boda de Ana).",
                ContactoUtils.mensajeConsulta(1001, "Boda de Ana"));
    }

    private Pedido solicitar(String nombre, String clienteId, long inicio) {
        SolicitudReserva sol = new SolicitudReserva("paq-basico", inicio, 240, Pedido.ORIGEN_APP,
                nombre, clienteId, "Cliente " + clienteId, "7471111111", "Av. Central 1234, Col. Centro", "");
        AtomicReference<Pedido> ref = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { ref.set(r); }
            @Override public void onError(String error) { fail("Error crearReserva: " + error); }
        });
        assertNotNull(ref.get());
        return ref.get();
    }

    // 7. Repositorio: el cliente cancela un pedido confirmado: queda CANCELADO, el evento se cancela,
    //    se liberan las unidades y no hay reembolsos que calcular.
    @Test
    public void test07_repositorio_cancelarConfirmado_liberaEventoYUnidades() {
        long ini8d = ahoraFijo + 8 * 24 * 3600_000L;
        Pedido p = solicitar("Boda 8 días", "cli-7a", ini8d);
        pedidoRepo.aprobarPedido(p.getId(), noop());

        Pedido confirmado = pedidoRepo.getPedidoPorId(p.getId());
        assertEquals(Pedido.ESTADO_CONFIRMADO, confirmado.getEstado());
        String eventoId = confirmado.getEventoId();
        assertNotNull(eventoId);

        AtomicReference<Pedido> cancelRef = new AtomicReference<>();
        pedidoRepo.cancelarReserva(p.getId(), Pedido.CANCELA_CLIENTE, "Ya no lo necesito", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { cancelRef.set(r); }
            @Override public void onError(String error) { fail("Error cancel: " + error); }
        });

        assertEquals(Pedido.ESTADO_CANCELADO, cancelRef.get().getEstado());
        assertEquals("Ya no lo necesito", cancelRef.get().getMotivoCancelacion());
        assertTrue(cancelRef.get().getEquipoIds().isEmpty());

        AtomicReference<Evento> evRef = new AtomicReference<>();
        eventoRepo.getEventoPorId(eventoId, new Callback<Evento>() {
            @Override public void onSuccess(Evento r) { evRef.set(r); }
            @Override public void onError(String error) { fail("Error al obtener evento: " + error); }
        });
        assertNotNull(evRef.get());
        assertEquals(Evento.ESTADO_CANCELADO, evRef.get().getEstado());
    }

    // 8. Repositorio: el cliente retira una solicitud por aprobar y puede enviar otra de inmediato.
    @Test
    public void test08_repositorio_retirarSolicitud_permiteOtra() {
        long ini = ahoraFijo + 72 * 3600_000L;
        Pedido p = solicitar("Solicitud", "cli-ap-canc", ini);
        assertEquals(Pedido.ESTADO_POR_APROBAR, p.getEstado());

        AtomicReference<Pedido> cancelRef = new AtomicReference<>();
        pedidoRepo.cancelarReserva(p.getId(), Pedido.CANCELA_CLIENTE, "", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { cancelRef.set(r); }
            @Override public void onError(String error) { fail("Error cancel: " + error); }
        });
        assertEquals(Pedido.ESTADO_CANCELADO, cancelRef.get().getEstado());
        assertTrue(pedidoRepo.evaluarDisponibilidad("paq-basico", ini, 240, null).isViable());

        Pedido nueva = solicitar("Nueva Solicitud", "cli-ap-canc", ini);
        assertEquals(Pedido.ESTADO_POR_APROBAR, nueva.getEstado());
    }

    // 9. Repositorio: el cliente ve su pedido como pagado cuando un socio confirma el pago,
    //    y una vez realizado el evento pasa al historial.
    @Test
    public void test09_repositorio_pedidoPagadoYRealizado() {
        long ini = ahoraFijo + 30 * 3600_000L;
        Pedido p = solicitar("Fiesta", "cli-pago", ini);
        pedidoRepo.aprobarPedido(p.getId(), noop());
        pedidoRepo.confirmarPago(p.getId(), Pedido.METODO_TRANSFERENCIA, "u-socio", "Carlos Socio", noop());

        List<Pedido> delCliente = PedidosClienteUtils.delCliente(pedidoRepo.obtenerTodos(), "cli-pago");
        assertEquals(1, PedidosClienteUtils.activos(delCliente, reloj.ahora()).size());
        assertTrue(delCliente.get(0).isPagoConfirmado());

        reloj.setAhora(ini + 10 * 3600_000L);
        pedidoRepo.actualizarEstadosPorTiempo();
        delCliente = PedidosClienteUtils.delCliente(pedidoRepo.obtenerTodos(), "cli-pago");
        assertEquals(Pedido.ESTADO_FINALIZADO, delCliente.get(0).getEstado());
        assertTrue(PedidosClienteUtils.activos(delCliente, reloj.ahora()).isEmpty());
        assertEquals(1, PedidosClienteUtils.historial(delCliente, reloj.ahora()).size());
    }

    // 10. Repositorio: cancelar como cliente con motivo vacío funciona y guarda 'Cancelado por el cliente';
    //     cancelar como Socio con motivo vacío sigue fallando.
    @Test
    public void test10_repositorio_motivoCancelacion_clienteOpcional_socioObligatorio() {
        long ini = ahoraFijo + 72 * 3600_000L;

        // Caso Cliente: motivo vacío -> guarda "Cancelado por el cliente"
        SolicitudReserva solCli = new SolicitudReserva("paq-basico", ini, 240, Pedido.ORIGEN_APP,
                "Reserva Cliente", "cli-motivo", "Cliente M", "7471111111", "Calle Guerrero 123, Col. Centro", "");
        AtomicReference<Pedido> pCliRef = new AtomicReference<>();
        pedidoRepo.crearReserva(solCli, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { pCliRef.set(r); }
            @Override public void onError(String error) { fail("Error solCli: " + error); }
        });

        AtomicReference<Pedido> pCliCancel = new AtomicReference<>();
        pedidoRepo.cancelarReserva(pCliRef.get().getId(), Pedido.CANCELA_CLIENTE, "", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { pCliCancel.set(r); }
            @Override public void onError(String error) { fail("Cliente con motivo vacío debe tener éxito: " + error); }
        });

        assertNotNull(pCliCancel.get());
        assertEquals("Cancelado por el cliente", pCliCancel.get().getMotivoCancelacion());

        // Caso Socio: motivo vacío -> falla
        SolicitudReserva solSocio = new SolicitudReserva("paq-basico", ini + 24 * 3600_000L, 240, Pedido.ORIGEN_PRESENCIAL,
                "Reserva Socio", "cli-socio", "Cliente S", "7472222222", "Calle Guerrero 456, Col. Centro", "");
        AtomicReference<Pedido> pSocioRef = new AtomicReference<>();
        pedidoRepo.crearReserva(solSocio, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { pSocioRef.set(r); }
            @Override public void onError(String error) { fail("Error solSocio: " + error); }
        });

        AtomicBoolean falloMotivoSocio = new AtomicBoolean(false);
        pedidoRepo.cancelarReserva(pSocioRef.get().getId(), Pedido.CANCELA_SOCIO, "   ", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { fail("Socio con motivo vacío no debe permitirse"); }
            @Override public void onError(String error) { falloMotivoSocio.set(true); }
        });
        assertTrue(falloMotivoSocio.get());
    }

    // 11. Repositorio: un pedido ya CANCELADO o FINALIZADO no se puede cancelar.
    @Test
    public void test11_repositorio_pedidosNoCancelables() {
        long ini = ahoraFijo + 72 * 3600_000L;
        SolicitudReserva sol = new SolicitudReserva("paq-basico", ini, 240, Pedido.ORIGEN_APP,
                "Reserva Para Cancelar", "cli-nocanc", "Cliente NC", "7471111111", "Calle Zaragoza 123, Col. Centro", "");
        AtomicReference<Pedido> pedRef = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { pedRef.set(r); }
            @Override public void onError(String error) { fail("Error sol: " + error); }
        });

        // Cancelamos por primera vez
        pedidoRepo.cancelarReserva(pedRef.get().getId(), Pedido.CANCELA_CLIENTE, "Primera", noop());
        assertEquals(Pedido.ESTADO_CANCELADO, pedidoRepo.getPedidoPorId(pedRef.get().getId()).getEstado());

        // Intento de cancelar de nuevo pedido ya CANCELADO
        AtomicBoolean falloReintento = new AtomicBoolean(false);
        pedidoRepo.cancelarReserva(pedRef.get().getId(), Pedido.CANCELA_CLIENTE, "Segunda", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { fail("No debe permitir cancelar pedido ya cancelado"); }
            @Override public void onError(String error) { falloReintento.set(true); }
        });
        assertTrue(falloReintento.get());

        // Pedido COMPLETADO
        Pedido pComp = new Pedido();
        pComp.setId("p-comp-nocanc");
        pComp.setEstado(Pedido.ESTADO_FINALIZADO);
        pComp.setClienteId("cli-nocanc");
        pedidoRepo.obtenerTodos().add(pComp);

        AtomicBoolean falloCompletado = new AtomicBoolean(false);
        pedidoRepo.cancelarReserva(pComp.getId(), Pedido.CANCELA_CLIENTE, "No se puede", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { fail("No debe permitir cancelar pedido completado"); }
            @Override public void onError(String error) { falloCompletado.set(true); }
        });
        assertTrue(falloCompletado.get());
    }
}
