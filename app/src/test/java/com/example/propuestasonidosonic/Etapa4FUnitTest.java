package com.example.propuestasonidosonic;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.memory.InMemoryDiaOperativoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryEquipoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryEventoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPaqueteRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPedidoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryUsuarioRepository;
import com.example.propuestasonidosonic.domain.DashboardUtils;
import com.example.propuestasonidosonic.domain.FinanzasUtils;
import com.example.propuestasonidosonic.domain.MoneyUtils;
import com.example.propuestasonidosonic.domain.EstadosPedido;
import com.example.propuestasonidosonic.domain.TicketPago;
import com.example.propuestasonidosonic.domain.SolicitudReserva;
import com.example.propuestasonidosonic.model.CategoriaEquipo;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.DiaOperativo;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Paquete;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.Config;
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
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class Etapa4FUnitTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static class RelojFalso implements Reloj {
        long tiempo;
        RelojFalso(long tiempo) { this.tiempo = tiempo; }
        @Override public long ahora() { return tiempo; }
        public void setAhora(long tiempo) { this.tiempo = tiempo; }
        public void avanzarHoras(int horas) { this.tiempo += ((long) horas) * 3600_000L; }
        public void avanzarDias(int dias) { this.tiempo += ((long) dias) * 86_400_000L; }
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
    private InMemoryEventoRepository eventoRepo;
    private InMemoryPedidoRepository pedidoRepo;

    private Paquete paqueteBasico;
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

        for (int i = 1; i <= 2; i++) {
            equipoRepo.agregarEquipo(new Equipo("boc-" + i, "Bocina 15", CategoriaEquipo.BOCINAS,
                    i, Equipo.ESTADO_DISPONIBLE, 500.0), noop());
        }

        Usuario socio = new Usuario("u-socio", "Carlos Socio", "carlos@sonic.com", "9611234567",
                Usuario.ROL_SOCIO, "Admin#2026", true, ahoraFijo);
        usuarioRepo.crearPersonal(socio, "Admin#2026", noop());

        Usuario asistente = new Usuario("u-asistente", "Pedro Asistente", "pedro@sonic.com", "9611234568",
                Usuario.ROL_ASISTENTE, "Admin#2026", true, ahoraFijo);
        usuarioRepo.crearPersonal(asistente, "Admin#2026", noop());

        String claveDia = TiempoUtils.claveDia(ahoraFijo);
        diaRepo.guardarDia(new DiaOperativo(claveDia, Collections.singletonList("u-asistente"), Collections.emptyList()), noop());

        paqueteBasico = new Paquete();
        paqueteBasico.setId("paq-1000");
        paqueteBasico.setNombre("Paquete Básico");
        paqueteBasico.setPrecio(1000.0);
        paqueteBasico.setDuracionBaseHoras(4);
        paqueteBasico.setBufferMontajeMin(60);
        paqueteBasico.setBufferDesmontajeMin(60);
        paqueteBasico.setOperadoresRequeridos(1);
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

    private static final long HORA = 3600_000L;

    /** Pedido registrado por un socio (queda confirmado) el 20 de noviembre de 16:00 a 20:00. */
    private Pedido crearPedidoConfirmado() {
        long inicioEvento = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 16 * 60);
        SolicitudReserva sol = new SolicitudReserva();
        sol.setPaqueteId(paqueteBasico.getId());
        sol.setInicioMillis(inicioEvento);
        sol.setDuracionMin(240);
        sol.setNombreEvento("Fiesta de Prueba");
        sol.setClienteNombre("Ana López");
        sol.setClienteTelefono("9611112233");
        sol.setDireccion("Av. Central 123, Tuxtla");
        sol.setOrigen(Pedido.ORIGEN_LLAMADA);

        AtomicReference<Pedido> ref = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String error) {}
        });

        Pedido p = ref.get();
        assertNotNull(p);
        assertEquals(Pedido.ESTADO_CONFIRMADO, p.getEstado());
        return p;
    }

    private String confirmarPago(String pedidoId, String metodo, String socioNombre) {
        AtomicReference<String> err = new AtomicReference<>();
        pedidoRepo.confirmarPago(pedidoId, metodo, "u-socio", socioNombre, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String error) { err.set(error); }
        });
        return err.get();
    }

    private static Pedido pedido(String id, String estado, long inicio, double total) {
        Pedido p = new Pedido();
        p.setId(id);
        p.setNumero(Math.abs(id.hashCode() % 1000));
        p.setEstado(estado);
        p.setInicioMillis(inicio);
        p.setFinMillis(inicio + 4 * HORA);
        p.setTotal(total);
        return p;
    }

    private static Pedido pagado(String id, long fechaPago, double total, String metodo) {
        Pedido p = pedido(id, Pedido.ESTADO_FINALIZADO, fechaPago - 24 * HORA, total);
        p.setPagoConfirmado(true);
        p.setFechaPagoMillis(fechaPago);
        p.setMetodoPago(metodo);
        return p;
    }

    private static long fecha(int anio, int mes0, int dia, int h, int min, int seg, int ms) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        cal.clear();
        cal.set(anio, mes0, dia, h, min, seg);
        cal.set(Calendar.MILLISECOND, ms);
        return cal.getTimeInMillis();
    }

    /**
     * 1. El pago se puede confirmar antes del evento: el pedido sigue confirmado y, al terminar, queda finalizado.
     */
    @Test
    public void test01_pagoAntesDelEvento() {
        Pedido p = crearPedidoConfirmado();
        assertEquals(1000.0, EstadosPedido.montoPendiente(p), 0.001);

        reloj.avanzarHoras(2); // 10:00
        assertNull(confirmarPago(p.getId(), Pedido.METODO_EFECTIVO, "Carlos Socio"));

        Pedido pagado = pedidoRepo.getPedidoPorId(p.getId());
        assertEquals(Pedido.ESTADO_CONFIRMADO, pagado.getEstado());
        assertTrue(pagado.isPagoConfirmado());
        assertEquals(reloj.ahora(), pagado.getFechaPagoMillis());
        assertEquals("Carlos Socio", pagado.getPagoConfirmadoPorNombre());
        assertEquals(0.0, EstadosPedido.montoPendiente(pagado), 0.001);

        // 16:00 + 4 h de evento + 60 min de desmontaje = 21:00
        reloj.setAhora(TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 21 * 60 + 1));
        assertEquals(1, pedidoRepo.actualizarEstadosPorTiempo());
        assertEquals(Pedido.ESTADO_FINALIZADO, pedidoRepo.getPedidoPorId(p.getId()).getEstado());
    }

    /**
     * 2. Si el evento termina sin pago queda "Por cobrar"; al confirmar el pago queda "Finalizado".
     */
    @Test
    public void test02_eventoTerminadoSinPago() {
        Pedido p = crearPedidoConfirmado();
        reloj.setAhora(TiempoUtils.construir(2026, Calendar.NOVEMBER, 21, 10 * 60));
        pedidoRepo.actualizarEstadosPorTiempo();

        Pedido porCobrar = pedidoRepo.getPedidoPorId(p.getId());
        assertEquals(Pedido.ESTADO_POR_COBRAR, porCobrar.getEstado());
        assertTrue(EstadosPedido.puedeConfirmarPago(porCobrar));
        assertEquals(1, FinanzasUtils.pedidosPorCobrar(pedidoRepo.obtenerTodos()).size());

        assertNull(confirmarPago(p.getId(), Pedido.METODO_TRANSFERENCIA, "Carlos Socio"));
        Pedido finalizado = pedidoRepo.getPedidoPorId(p.getId());
        assertEquals(Pedido.ESTADO_FINALIZADO, finalizado.getEstado());
        assertEquals(Pedido.METODO_TRANSFERENCIA, finalizado.getMetodoPago());
        assertEquals(reloj.ahora(), finalizado.getFechaPagoMillis());
        assertTrue(FinanzasUtils.pedidosPorCobrar(pedidoRepo.obtenerTodos()).isEmpty());
    }

    /**
     * 3. Validaciones de confirmarPago.
     */
    @Test
    public void test03_validacionesConfirmarPago() {
        AtomicReference<String> errNoExiste = new AtomicReference<>();
        pedidoRepo.confirmarPago("no-existe", Pedido.METODO_EFECTIVO, "u", "U", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String error) { errNoExiste.set(error); }
        });
        assertEquals("Pedido no encontrado", errNoExiste.get());

        Pedido p = crearPedidoConfirmado();
        assertNotNull("Tarjeta ya no es un método válido", confirmarPago(p.getId(), "TARJETA", "Carlos"));
        assertNotNull(confirmarPago(p.getId(), "", "Carlos"));
        assertNull(confirmarPago(p.getId(), Pedido.METODO_EFECTIVO, "Carlos"));
        assertNotNull("No se confirma dos veces", confirmarPago(p.getId(), Pedido.METODO_EFECTIVO, "Carlos"));

        // Un pedido cancelado no se cobra
        Pedido cancelado = pedido("x", Pedido.ESTADO_CANCELADO, ahoraFijo, 500.0);
        assertFalse(EstadosPedido.puedeConfirmarPago(cancelado));
        assertEquals(0.0, EstadosPedido.montoPendiente(cancelado), 0.001);
    }

    /**
     * 4. resumenMes: 1 de octubre 00:00 entra; 31 de octubre 23:59 entra; 1 de noviembre 00:00 no entra.
     *    Se separa lo cobrado en efectivo y por transferencia.
     */
    @Test
    public void test04_resumenMesLimitesYMetodos() {
        long t1Oct00 = fecha(2026, Calendar.OCTOBER, 1, 0, 0, 0, 0);
        long t31Oct = fecha(2026, Calendar.OCTOBER, 31, 23, 59, 59, 999);
        long t1Nov00 = fecha(2026, Calendar.NOVEMBER, 1, 0, 0, 0, 0);

        List<Pedido> pedidos = Arrays.asList(
                pagado("ped1", t1Oct00, 100.0, Pedido.METODO_EFECTIVO),
                pagado("ped2", t31Oct, 200.0, Pedido.METODO_TRANSFERENCIA),
                pagado("ped3", t1Nov00, 400.0, Pedido.METODO_EFECTIVO));

        double[] oct = FinanzasUtils.resumenMes(pedidos, 2026, Calendar.OCTOBER);
        assertEquals(300.0, oct[0], 0.001); // cobrado
        assertEquals(0.0, oct[1], 0.001);   // por cobrar
        assertEquals(100.0, oct[2], 0.001); // efectivo
        assertEquals(200.0, oct[3], 0.001); // transferencia

        double[] nov = FinanzasUtils.resumenMes(pedidos, 2026, Calendar.NOVEMBER);
        assertEquals(400.0, nov[0], 0.001);
        assertEquals(400.0, nov[2], 0.001);
        assertEquals(400.0, FinanzasUtils.cobradoDelMes(pedidos, 2026, Calendar.NOVEMBER), 0.001);
    }

    /**
     * 5. resumenMes.porCobrar: solo pedidos aceptados sin pago con evento en el mes.
     */
    @Test
    public void test05_resumenMesPorCobrar() {
        long fechaOct = fecha(2026, Calendar.OCTOBER, 15, 12, 0, 0, 0);
        long fechaNov = fecha(2026, Calendar.NOVEMBER, 15, 12, 0, 0, 0);

        List<Pedido> pedidos = Arrays.asList(
                pedido("conf", Pedido.ESTADO_CONFIRMADO, fechaOct, 1000.0),
                pedido("cobrar", Pedido.ESTADO_POR_COBRAR, fechaOct, 300.0),
                pedido("aprobar", Pedido.ESTADO_POR_APROBAR, fechaOct, 1000.0),
                pedido("canc", Pedido.ESTADO_CANCELADO, fechaOct, 1000.0),
                pedido("rech", Pedido.ESTADO_RECHAZADO, fechaOct, 1000.0),
                pedido("venc", Pedido.ESTADO_VENCIDO, fechaOct, 1000.0),
                pedido("otroMes", Pedido.ESTADO_CONFIRMADO, fechaNov, 800.0));

        double[] resumen = FinanzasUtils.resumenMes(pedidos, 2026, Calendar.OCTOBER);
        // Solo "conf" (1000) + "cobrar" (300)
        assertEquals(1300.0, resumen[1], 0.001);
        assertEquals(0.0, resumen[0], 0.001);
    }

    /**
     * 6. movimientosDelMes: primero lo pendiente (evento más antiguo arriba), luego lo cobrado (más reciente arriba).
     */
    @Test
    public void test06_movimientosDelMesFiltroYOrden() {
        long t5 = fecha(2026, Calendar.OCTOBER, 5, 10, 0, 0, 0);
        long t10 = fecha(2026, Calendar.OCTOBER, 10, 10, 0, 0, 0);
        long t20 = fecha(2026, Calendar.OCTOBER, 20, 10, 0, 0, 0);

        Pedido c1 = pagado("c1", t5, 100.0, Pedido.METODO_EFECTIVO);
        Pedido c2 = pagado("c2", t10, 200.0, Pedido.METODO_TRANSFERENCIA);
        Pedido pend20 = pedido("p20", Pedido.ESTADO_CONFIRMADO, t20, 300.0);
        Pedido pend5 = pedido("p5", Pedido.ESTADO_POR_COBRAR, t5, 400.0);
        List<Pedido> pedidos = Arrays.asList(c1, pend20, c2, pend5);

        List<Pedido> todos = FinanzasUtils.movimientosDelMes(pedidos, 2026, Calendar.OCTOBER, FinanzasUtils.FILTRO_TODOS);
        assertEquals(4, todos.size());
        assertEquals("p5", todos.get(0).getId());
        assertEquals("p20", todos.get(1).getId());
        assertEquals("c2", todos.get(2).getId());
        assertEquals("c1", todos.get(3).getId());

        List<Pedido> cobrados = FinanzasUtils.movimientosDelMes(pedidos, 2026, Calendar.OCTOBER, FinanzasUtils.FILTRO_COBRADOS);
        assertEquals(2, cobrados.size());
        assertEquals("c2", cobrados.get(0).getId());

        List<Pedido> porCobrar = FinanzasUtils.movimientosDelMes(pedidos, 2026, Calendar.OCTOBER, FinanzasUtils.FILTRO_POR_COBRAR);
        assertEquals(2, porCobrar.size());
        assertEquals("p5", porCobrar.get(0).getId());

        assertTrue(FinanzasUtils.movimientosDelMes(null, 2026, Calendar.OCTOBER, FinanzasUtils.FILTRO_TODOS).isEmpty());
    }

    /**
     * 7. pedidosPorCobrar solo incluye eventos terminados sin pago; sumaTotales redondea a centavos.
     */
    @Test
    public void test07_pedidosPorCobrarYSuma() {
        Pedido a = pedido("a", Pedido.ESTADO_POR_COBRAR, ahoraFijo - 48 * HORA, 0.1);
        Pedido b = pedido("b", Pedido.ESTADO_POR_COBRAR, ahoraFijo - 72 * HORA, 0.2);
        Pedido c = pedido("c", Pedido.ESTADO_CONFIRMADO, ahoraFijo + 48 * HORA, 1000.0);
        List<Pedido> lista = FinanzasUtils.pedidosPorCobrar(Arrays.asList(a, b, c));
        assertEquals(2, lista.size());
        assertEquals("b", lista.get(0).getId()); // el más antiguo primero
        assertEquals(0.3, FinanzasUtils.sumaTotales(lista), 0.0000001);
    }

    /**
     * 8. Ticket térmico: logo y encabezado, fecha y hora de confirmación, paquete, monto, IVA 0 % y total.
     */
    @Test
    public void test08_ticketDePago() {
        Pedido p = crearPedidoConfirmado();
        reloj.setAhora(TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 14 * 60 + 35));
        assertNull(confirmarPago(p.getId(), Pedido.METODO_EFECTIVO, "Carlos Socio"));
        Pedido pagado = pedidoRepo.getPedidoPorId(p.getId());

        List<TicketPago.Linea> lineas = TicketPago.construir(pagado);
        StringBuilder sb = new StringBuilder();
        int idxIva = -1;
        int idxTotal = -1;
        for (int i = 0; i < lineas.size(); i++) {
            TicketPago.Linea l = lineas.get(i);
            sb.append(l.izquierda).append('|').append(l.derecha).append('\n');
            if (l.izquierda.startsWith("IVA")) idxIva = i;
            if (l.tipo == TicketPago.TIPO_TOTAL) idxTotal = i;
        }
        String texto = sb.toString();

        assertEquals(TicketPago.TIPO_TITULO, lineas.get(0).tipo);
        assertEquals(String.format(Locale.ROOT, "%06d", pagado.getNumero()), TicketPago.folio(pagado));
        assertTrue("Fecha y hora del pago en la misma línea", texto.contains("Fecha: 20/11/2026|Hora: 14:35"));
        assertTrue(texto.contains("San Rafael Norte Ixtapa"));
        assertTrue(texto.contains("Mza 10 LT 6 CP 39016"));
        assertTrue(texto.contains("Tel. 747 109 0459"));
        assertTrue(texto.contains("Lugar: Av. Central 123, Tuxtla"));
        assertTrue(texto.contains("Fecha del evento:|20/11/2026"));
        assertTrue(texto.contains("Hora de inicio:|16:00"));
        assertTrue(texto.contains("Paquete Básico"));
        assertTrue(texto.contains("IVA (0%)|" + TicketPago.moneda(0.0)));
        assertTrue(texto.contains("TOTAL|" + TicketPago.moneda(1000.0)));
        assertTrue(texto.contains("Forma de pago:|Efectivo"));
        assertTrue(texto.contains("Recibió: Carlos Socio"));
        assertTrue("El IVA va antes del total", idxIva >= 0 && idxIva < idxTotal);
        assertEquals(1000.0, TicketPago.subtotal(1000.0), 0.001);
        assertEquals(0.0, TicketPago.iva(1000.0), 0.001);
        assertEquals("ticket_" + TicketPago.folio(pagado) + ".pdf", TicketPago.nombreArchivo(pagado));
    }

    /**
     * 9. Cancelar un pedido ya pagado no genera reembolsos: el pago confirmado se conserva en Finanzas.
     */
    @Test
    public void test09_cancelarPedidoPagadoConservaPago() {
        Pedido p = crearPedidoConfirmado();
        assertNull(confirmarPago(p.getId(), Pedido.METODO_EFECTIVO, "Carlos Socio"));

        pedidoRepo.cancelarReserva(p.getId(), Pedido.CANCELA_SOCIO, "El cliente lo pidió", noop());
        Pedido cancelado = pedidoRepo.getPedidoPorId(p.getId());
        assertEquals(Pedido.ESTADO_CANCELADO, cancelado.getEstado());
        assertTrue(cancelado.isPagoConfirmado());

        double[] resumen = FinanzasUtils.resumenMes(pedidoRepo.obtenerTodos(), 2026, Calendar.NOVEMBER);
        assertEquals(1000.0, resumen[0], 0.001);
        assertEquals(0.0, resumen[1], 0.001);
    }

    /**
     * 10. DashboardUtils.contadores con pedidos de hoy, de mañana, dentro de 8 días,
     *     solicitudes por aprobar vigentes/vencidas y asignacionIncompleta.
     */
    @Test
    public void test10_dashboardContadores() {
        long hoy = ahoraFijo; // 20 nov 08:00
        long manana = TiempoUtils.construir(2026, Calendar.NOVEMBER, 21, 14 * 60);
        long en8Dias = TiempoUtils.construir(2026, Calendar.NOVEMBER, 28, 14 * 60);

        Pedido p1Hoy = pedido("1", Pedido.ESTADO_CONFIRMADO, hoy, 1);
        Pedido p2Manana = pedido("2", Pedido.ESTADO_CONFIRMADO, manana, 1);
        Pedido p3En8Dias = pedido("3", Pedido.ESTADO_CONFIRMADO, en8Dias, 1);
        Pedido p4PorAprobar = pedido("4", Pedido.ESTADO_POR_APROBAR, manana, 1);
        Pedido p5PorAprobarPasado = pedido("5", Pedido.ESTADO_POR_APROBAR, hoy - HORA, 1);
        Pedido p6AsigIncompleta = pedido("6", Pedido.ESTADO_CONFIRMADO, en8Dias, 1);
        p6AsigIncompleta.setAsignacionIncompleta(true);

        List<Pedido> lista = Arrays.asList(p1Hoy, p2Manana, p3En8Dias, p4PorAprobar, p5PorAprobarPasado, p6AsigIncompleta);

        int[] c = DashboardUtils.contadores(lista, hoy);
        assertEquals(1, c[0]); // eventos hoy
        assertEquals(1, c[1]); // próximos 7 días (la solicitud por aprobar no cuenta)
        assertEquals(1, c[2]); // solicitudes por aprobar vigentes
        assertEquals(1, c[3]); // asignación incompleta

        List<Pedido> solicitudes = DashboardUtils.solicitudesPorAprobar(lista, hoy);
        assertEquals(1, solicitudes.size());
        assertEquals("4", solicitudes.get(0).getId());
    }
}
