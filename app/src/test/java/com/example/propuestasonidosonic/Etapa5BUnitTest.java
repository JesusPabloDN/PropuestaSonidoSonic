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
import com.example.propuestasonidosonic.domain.ContactoUtils;
import com.example.propuestasonidosonic.domain.PedidosClienteUtils;
import com.example.propuestasonidosonic.domain.SolicitudReserva;
import com.example.propuestasonidosonic.model.CategoriaEquipo;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.DiaOperativo;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Evento;
import com.example.propuestasonidosonic.model.Pago;
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
    private InMemoryPagoRepository pagoRepo;
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
        pagoRepo = new InMemoryPagoRepository(reloj);
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
    //    apartado vigente y confirmados futuros son activos;
    //    confirmado cuyo bloqueo ya terminó va al historial;
    //    cancelado y rechazado van al historial;
    //    vencido y apartado vencido no aparecen en ninguna;
    //    orden ascendente en activos y descendente en historial.
    @Test
    public void test02_esVisible_activos_historial() {
        // A. Apartado vigente (vence en 20 min) -> visible y activo
        Pedido pApartadoVigente = new Pedido();
        pApartadoVigente.setId("p-ap-vig");
        pApartadoVigente.setEstado(Pedido.ESTADO_APARTADO);
        pApartadoVigente.setVencimientoApartadoMillis(ahoraFijo + 20 * 60_000L);
        pApartadoVigente.setInicioMillis(ahoraFijo + 72 * 3600_000L);
        pApartadoVigente.setFinMillis(ahoraFijo + 76 * 3600_000L);

        // B. Apartado vencido (venció hace 5 min) -> invisible
        Pedido pApartadoVencido = new Pedido();
        pApartadoVencido.setId("p-ap-venc");
        pApartadoVencido.setEstado(Pedido.ESTADO_APARTADO);
        pApartadoVencido.setVencimientoApartadoMillis(ahoraFijo - 5 * 60_000L);
        pApartadoVencido.setInicioMillis(ahoraFijo + 72 * 3600_000L);

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

    // 3. puedeCancelar:
    //    cierto para apartado vigente, confirmado, pagado y en revisión antes de iniciar;
    //    falso al iniciar el evento; falso para realizado, cancelado y rechazado.
    @Test
    public void test03_puedeCancelar() {
        long futuro = ahoraFijo + 10 * 3600_000L;
        long pasado = ahoraFijo - 1000L;

        // Apartado vigente futuro
        Pedido pAp = new Pedido();
        pAp.setEstado(Pedido.ESTADO_APARTADO);
        pAp.setInicioMillis(futuro);
        pAp.setVencimientoApartadoMillis(ahoraFijo + 10_000L);
        assertTrue(PedidosClienteUtils.puedeCancelar(pAp, ahoraFijo));

        // Apartado con vencimiento expirado
        Pedido pApVenc = new Pedido();
        pApVenc.setEstado(Pedido.ESTADO_APARTADO);
        pApVenc.setInicioMillis(futuro);
        pApVenc.setVencimientoApartadoMillis(ahoraFijo - 1000L);
        assertFalse(PedidosClienteUtils.puedeCancelar(pApVenc, ahoraFijo));

        // Confirmado futuro
        Pedido pConf = new Pedido();
        pConf.setEstado(Pedido.ESTADO_CONFIRMADO);
        pConf.setInicioMillis(futuro);
        assertTrue(PedidosClienteUtils.puedeCancelar(pConf, ahoraFijo));

        // Pagado futuro
        Pedido pPag = new Pedido();
        pPag.setEstado(Pedido.ESTADO_PAGADO);
        pPag.setInicioMillis(futuro);
        assertTrue(PedidosClienteUtils.puedeCancelar(pPag, ahoraFijo));

        // En revisión futuro
        Pedido pRev = new Pedido();
        pRev.setEstado(Pedido.ESTADO_EN_REVISION);
        pRev.setInicioMillis(futuro);
        assertTrue(PedidosClienteUtils.puedeCancelar(pRev, ahoraFijo));

        // Evento ya iniciado (inicio <= ahora)
        Pedido pIniciado = new Pedido();
        pIniciado.setEstado(Pedido.ESTADO_CONFIRMADO);
        pIniciado.setInicioMillis(ahoraFijo);
        assertFalse(PedidosClienteUtils.puedeCancelar(pIniciado, ahoraFijo));

        Pedido pPasado = new Pedido();
        pPasado.setEstado(Pedido.ESTADO_CONFIRMADO);
        pPasado.setInicioMillis(pasado);
        assertFalse(PedidosClienteUtils.puedeCancelar(pPasado, ahoraFijo));

        // Completado, Cancelado, Rechazado
        Pedido pComp = new Pedido();
        pComp.setEstado(Pedido.ESTADO_COMPLETADO);
        pComp.setInicioMillis(futuro);
        assertFalse(PedidosClienteUtils.puedeCancelar(pComp, ahoraFijo));

        Pedido pCanc = new Pedido();
        pCanc.setEstado(Pedido.ESTADO_CANCELADO);
        pCanc.setInicioMillis(futuro);
        assertFalse(PedidosClienteUtils.puedeCancelar(pCanc, ahoraFijo));

        Pedido pRech = new Pedido();
        pRech.setEstado(Pedido.ESTADO_RECHAZADO);
        pRech.setInicioMillis(futuro);
        assertFalse(PedidosClienteUtils.puedeCancelar(pRech, ahoraFijo));
    }

    // 4. puedePagarAnticipo: solo apartado vigente
    @Test
    public void test04_puedePagarAnticipo() {
        Pedido pApVig = new Pedido();
        pApVig.setEstado(Pedido.ESTADO_APARTADO);
        pApVig.setVencimientoApartadoMillis(ahoraFijo + 15 * 60_000L);
        assertTrue(PedidosClienteUtils.puedePagarAnticipo(pApVig, ahoraFijo));

        Pedido pApVenc = new Pedido();
        pApVenc.setEstado(Pedido.ESTADO_APARTADO);
        pApVenc.setVencimientoApartadoMillis(ahoraFijo - 1000L);
        assertFalse(PedidosClienteUtils.puedePagarAnticipo(pApVenc, ahoraFijo));

        Pedido pConf = new Pedido();
        pConf.setEstado(Pedido.ESTADO_CONFIRMADO);
        assertFalse(PedidosClienteUtils.puedePagarAnticipo(pConf, ahoraFijo));

        assertFalse(PedidosClienteUtils.puedePagarAnticipo(null, ahoraFijo));
    }

    // 5. etiquetaEstado y textoVencimiento
    @Test
    public void test05_etiquetaEstado_y_textoVencimiento() {
        assertEquals("Pendiente de pago", PedidosClienteUtils.etiquetaEstado(Pedido.ESTADO_APARTADO));
        assertEquals("Confirmado", PedidosClienteUtils.etiquetaEstado(Pedido.ESTADO_CONFIRMADO));
        assertEquals("Confirmado", PedidosClienteUtils.etiquetaEstado(Pedido.ESTADO_PAGADO));
        assertEquals("En revisión", PedidosClienteUtils.etiquetaEstado(Pedido.ESTADO_EN_REVISION));
        assertEquals("Realizado", PedidosClienteUtils.etiquetaEstado(Pedido.ESTADO_COMPLETADO));
        assertEquals("Cancelado", PedidosClienteUtils.etiquetaEstado(Pedido.ESTADO_CANCELADO));
        assertEquals("No aprobado", PedidosClienteUtils.etiquetaEstado(Pedido.ESTADO_RECHAZADO));
        assertEquals("ESTADO_INVENTADO", PedidosClienteUtils.etiquetaEstado("ESTADO_INVENTADO"));
        assertEquals("", PedidosClienteUtils.etiquetaEstado(null));

        // textoVencimiento: 10s -> "Vence en 1 min"; 12 min -> "Vence en 12 min"; pasado/igual -> "Vencida"
        assertEquals("Vence en 1 min", PedidosClienteUtils.textoVencimiento(ahoraFijo + 10_000L, ahoraFijo));
        assertEquals("Vence en 12 min", PedidosClienteUtils.textoVencimiento(ahoraFijo + 12 * 60_000L, ahoraFijo));
        assertEquals("Vencida", PedidosClienteUtils.textoVencimiento(ahoraFijo, ahoraFijo));
        assertEquals("Vencida", PedidosClienteUtils.textoVencimiento(ahoraFijo - 5000L, ahoraFijo));
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

    // 7. Repositorio: el cliente cancela un pedido CONFIRMADO con más de 7 días:
    //    evento queda CANCELADO, unidades liberadas y reembolso del 100% con folio;
    //    entre 72h y 7 días: 50%; con menos de 72h: 0% y no se crea Pago de reembolso.
    @Test
    public void test07_repositorio_cancelarConfirmado_politicaReembolso() {
        // Caso A: Más de 7 días (8 días) -> Reembolso 100%
        long ini8d = ahoraFijo + 8 * 24 * 3600_000L;
        SolicitudReserva solA = new SolicitudReserva("paq-basico", ini8d, 240, Pedido.ORIGEN_APP,
                "Boda 8 días", "cli-7a", "Cliente 7A", "7471111111", "Av. Central 1234, Col. Centro", "", null);
        AtomicReference<Pedido> pedARef = new AtomicReference<>();
        pedidoRepo.crearReserva(solA, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { pedARef.set(r); }
            @Override public void onError(String error) { fail("Error solA: " + error); }
        });
        pedidoRepo.registrarAnticipo(pedARef.get().getId(), pedARef.get().getAnticipoRequerido(),
                Pedido.METODO_TARJETA, noop());

        Pedido pAConfirmado = pedidoRepo.getPedidoPorId(pedARef.get().getId());
        assertEquals(Pedido.ESTADO_CONFIRMADO, pAConfirmado.getEstado());
        double anticipoA = pAConfirmado.getAnticipoPagado();
        String eventoIdA = pAConfirmado.getEventoId();
        assertNotNull(eventoIdA);

        AtomicReference<Pedido> cancelARef = new AtomicReference<>();
        pedidoRepo.cancelarReserva(pAConfirmado.getId(), Pedido.CANCELA_CLIENTE, "Ya no lo necesito", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { cancelARef.set(r); }
            @Override public void onError(String error) { fail("Error cancel A: " + error); }
        });

        assertEquals(Pedido.ESTADO_CANCELADO, cancelARef.get().getEstado());
        assertEquals(anticipoA, cancelARef.get().getReembolsoMonto(), 0.001);
        // Evento cancelado y unidades liberadas
        AtomicReference<Evento> evARef = new AtomicReference<>();
        eventoRepo.getEventoPorId(eventoIdA, new Callback<Evento>() {
            @Override public void onSuccess(Evento r) { evARef.set(r); }
            @Override public void onError(String error) { fail("Error al obtener evento: " + error); }
        });
        assertNotNull(evARef.get());
        assertEquals(Evento.ESTADO_CANCELADO, evARef.get().getEstado());
        assertTrue(cancelARef.get().getEquipoIds().isEmpty());

        // Verificamos el Pago de reembolso
        List<Pago> pagosA = pagoRepo.getPagosPorPedido(pAConfirmado.getId()).getValue();
        assertEquals(2, pagosA.size()); // Anticipo + Reembolso
        Pago pagoReembolsoA = pagosA.get(1);
        assertEquals(Pago.TIPO_REEMBOLSO, pagoReembolsoA.getTipo());
        assertEquals(anticipoA, pagoReembolsoA.getMonto(), 0.001);
        assertNotNull(pagoReembolsoA.getFolio());

        // Caso B: Entre 72 h y 7 días (4 días = 96 h) -> Reembolso 50%
        long ini4d = ahoraFijo + 4 * 24 * 3600_000L;
        SolicitudReserva solB = new SolicitudReserva("paq-basico", ini4d, 240, Pedido.ORIGEN_APP,
                "Boda 4 días", "cli-7b", "Cliente 7B", "7472222222", "Av. Central 5678, Col. Centro", "", null);
        AtomicReference<Pedido> pedBRef = new AtomicReference<>();
        pedidoRepo.crearReserva(solB, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { pedBRef.set(r); }
            @Override public void onError(String error) { fail("Error solB: " + error); }
        });
        pedidoRepo.registrarAnticipo(pedBRef.get().getId(), pedBRef.get().getAnticipoRequerido(),
                Pedido.METODO_TARJETA, noop());

        Pedido pBConfirmado = pedidoRepo.getPedidoPorId(pedBRef.get().getId());
        double esperado50 = Math.round(pBConfirmado.getAnticipoPagado() * 0.5 * 100.0) / 100.0;

        AtomicReference<Pedido> cancelBRef = new AtomicReference<>();
        pedidoRepo.cancelarReserva(pBConfirmado.getId(), Pedido.CANCELA_CLIENTE, "Cambio", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { cancelBRef.set(r); }
            @Override public void onError(String error) { fail("Error cancel B: " + error); }
        });

        assertEquals(esperado50, cancelBRef.get().getReembolsoMonto(), 0.001);
        List<Pago> pagosB = pagoRepo.getPagosPorPedido(pBConfirmado.getId()).getValue();
        assertEquals(2, pagosB.size());
        assertEquals(esperado50, pagosB.get(1).getMonto(), 0.001);

        // Caso C: Menos de 72 h (48 h) -> Reembolso 0% y NO se crea Pago
        long ini48h = ahoraFijo + 48 * 3600_000L;
        SolicitudReserva solC = new SolicitudReserva("paq-basico", ini48h, 240, Pedido.ORIGEN_APP,
                "Boda 48h", "cli-7c", "Cliente 7C", "7473333333", "Av. Central 9012, Col. Centro", "", null);
        AtomicReference<Pedido> pedCRef = new AtomicReference<>();
        pedidoRepo.crearReserva(solC, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { pedCRef.set(r); }
            @Override public void onError(String error) { fail("Error solC: " + error); }
        });
        pedidoRepo.registrarAnticipo(pedCRef.get().getId(), pedCRef.get().getAnticipoRequerido(),
                Pedido.METODO_TARJETA, noop());

        Pedido pCConfirmado = pedidoRepo.getPedidoPorId(pedCRef.get().getId());
        AtomicReference<Pedido> cancelCRef = new AtomicReference<>();
        pedidoRepo.cancelarReserva(pCConfirmado.getId(), Pedido.CANCELA_CLIENTE, "Tarde", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { cancelCRef.set(r); }
            @Override public void onError(String error) { fail("Error cancel C: " + error); }
        });

        assertEquals(0.0, cancelCRef.get().getReembolsoMonto(), 0.001);
        List<Pago> pagosC = pagoRepo.getPagosPorPedido(pCConfirmado.getId()).getValue();
        // Solo debe estar el anticipo, NO hay pago de reembolso
        assertEquals(1, pagosC.size());
        assertEquals(Pago.TIPO_ANTICIPO, pagosC.get(0).getTipo());
    }

    // 8. Repositorio: el cliente cancela un pedido EN_REVISION a menos de 72 h:
    //    reembolso del 100%. calcularReembolsoPrevio devuelve el mismo monto que cancelarReserva en cada caso.
    @Test
    public void test08_repositorio_cancelarEnRevision_reembolso100_y_calcularReembolsoPrevio() {
        // Evento a menos de 48 h (24 h) -> entra EN_REVISION tras pagar anticipo
        long ini24h = ahoraFijo + 24 * 3600_000L;
        SolicitudReserva sol = new SolicitudReserva("paq-basico", ini24h, 240, Pedido.ORIGEN_APP,
                "Urgente en revisión", "cli-rev", "Cliente Rev", "7471111111", "Av. Insurgentes 123, Piso 4", "", null);

        AtomicReference<Pedido> pedRef = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { pedRef.set(r); }
            @Override public void onError(String error) { fail("Error sol: " + error); }
        });

        pedidoRepo.registrarAnticipo(pedRef.get().getId(), pedRef.get().getAnticipoRequerido(),
                Pedido.METODO_TARJETA, noop());

        Pedido pEnRevision = pedidoRepo.getPedidoPorId(pedRef.get().getId());
        assertEquals(Pedido.ESTADO_EN_REVISION, pEnRevision.getEstado());
        double pagado = pEnRevision.getAnticipoPagado();
        assertTrue(pagado > 0);

        // calcularReembolsoPrevio debe indicar 100% para cliente en pedido EN_REVISION aunque falten < 72h
        double reembolsoPrevio = pedidoRepo.calcularReembolsoPrevio(pEnRevision.getId(), Pedido.CANCELA_CLIENTE);
        assertEquals(pagado, reembolsoPrevio, 0.001);

        AtomicReference<Pedido> cancelRef = new AtomicReference<>();
        pedidoRepo.cancelarReserva(pEnRevision.getId(), Pedido.CANCELA_CLIENTE, "Prefiero cancelar", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { cancelRef.set(r); }
            @Override public void onError(String error) { fail("Error cancelar: " + error); }
        });

        assertEquals(Pedido.ESTADO_CANCELADO, cancelRef.get().getEstado());
        assertEquals(reembolsoPrevio, cancelRef.get().getReembolsoMonto(), 0.001);

        List<Pago> pagos = pagoRepo.getPagosPorPedido(pEnRevision.getId()).getValue();
        assertEquals(2, pagos.size());
        assertEquals(Pago.TIPO_REEMBOLSO, pagos.get(1).getTipo());
        assertEquals(pagado, pagos.get(1).getMonto(), 0.001);
    }

    // 9. Repositorio: el cliente cancela un APARTADO sin pagos: queda CANCELADO, sin Pago de reembolso,
    //    y puede volver a reservar de inmediato (la regla de un apartado a la vez deja de bloquearlo).
    @Test
    public void test09_repositorio_cancelarApartadoSinPago_liberaInmediatamente() {
        long ini = ahoraFijo + 72 * 3600_000L;
        SolicitudReserva sol = new SolicitudReserva("paq-basico", ini, 240, Pedido.ORIGEN_APP,
                "Apartado sin pago", "cli-ap-canc", "Cliente Ap", "7471111111", "Calle Morelos 123, Col. Centro", "", null);

        AtomicReference<Pedido> pedRef = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { pedRef.set(r); }
            @Override public void onError(String error) { fail("Error sol: " + error); }
        });

        Pedido pApartado = pedRef.get();
        assertEquals(Pedido.ESTADO_APARTADO, pApartado.getEstado());

        // Cancelar apartado sin pagar
        AtomicReference<Pedido> cancelRef = new AtomicReference<>();
        pedidoRepo.cancelarReserva(pApartado.getId(), Pedido.CANCELA_CLIENTE, "", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { cancelRef.set(r); }
            @Override public void onError(String error) { fail("Error cancel: " + error); }
        });

        assertEquals(Pedido.ESTADO_CANCELADO, cancelRef.get().getEstado());
        assertEquals(0.0, cancelRef.get().getReembolsoMonto(), 0.001);

        List<Pago> pagos = pagoRepo.getPagosPorPedido(pApartado.getId()).getValue();
        assertTrue(pagos == null || pagos.isEmpty());

        // Ahora el mismo cliente puede reservar de inmediato sin esperar a los 30 min
        SolicitudReserva solNueva = new SolicitudReserva("paq-basico", ini, 240, Pedido.ORIGEN_APP,
                "Nueva Reserva Inmediata", "cli-ap-canc", "Cliente Ap", "7471111111", "Calle Morelos 123, Col. Centro", "", null);
        AtomicReference<Pedido> pedNuevoRef = new AtomicReference<>();
        pedidoRepo.crearReserva(solNueva, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido r) { pedNuevoRef.set(r); }
            @Override public void onError(String error) { fail("Debe permitir reservar de nuevo: " + error); }
        });

        assertNotNull(pedNuevoRef.get());
        assertEquals(Pedido.ESTADO_APARTADO, pedNuevoRef.get().getEstado());
    }

    // 10. Repositorio: cancelar como cliente con motivo vacío funciona y guarda 'Cancelado por el cliente';
    //     cancelar como Socio con motivo vacío sigue fallando.
    @Test
    public void test10_repositorio_motivoCancelacion_clienteOpcional_socioObligatorio() {
        long ini = ahoraFijo + 72 * 3600_000L;

        // Caso Cliente: motivo vacío -> guarda "Cancelado por el cliente"
        SolicitudReserva solCli = new SolicitudReserva("paq-basico", ini, 240, Pedido.ORIGEN_APP,
                "Reserva Cliente", "cli-motivo", "Cliente M", "7471111111", "Calle Guerrero 123, Col. Centro", "", null);
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
                "Reserva Socio", "cli-socio", "Cliente S", "7472222222", "Calle Guerrero 456, Col. Centro", "", 24);
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

    // 11. Repositorio: un pedido ya CANCELADO, RECHAZADO o COMPLETADO no se puede cancelar.
    @Test
    public void test11_repositorio_pedidosNoCancelables() {
        long ini = ahoraFijo + 72 * 3600_000L;
        SolicitudReserva sol = new SolicitudReserva("paq-basico", ini, 240, Pedido.ORIGEN_APP,
                "Reserva Para Cancelar", "cli-nocanc", "Cliente NC", "7471111111", "Calle Zaragoza 123, Col. Centro", "", null);
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
        pComp.setEstado(Pedido.ESTADO_COMPLETADO);
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
