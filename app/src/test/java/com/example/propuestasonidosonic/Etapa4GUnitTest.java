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
import com.example.propuestasonidosonic.domain.ClienteUtils;
import com.example.propuestasonidosonic.domain.DashboardUtils;
import com.example.propuestasonidosonic.domain.FinanzasUtils;
import com.example.propuestasonidosonic.util.ModeloUtils;
import com.example.propuestasonidosonic.domain.SolicitudReserva;
import com.example.propuestasonidosonic.model.CategoriaEquipo;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Evento;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Etapa4GUnitTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static class RelojFalso implements Reloj {
        long tiempo;
        RelojFalso(long tiempo) { this.tiempo = tiempo; }
        @Override public long ahora() { return tiempo; }
        public void setAhora(long tiempo) { this.tiempo = tiempo; }
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

        // 1 unidad de Bocina 15
        equipoRepo.agregarEquipo(new Equipo("boc-1", "Bocina 15", CategoriaEquipo.BOCINAS,
                1, Equipo.ESTADO_DISPONIBLE, 500.0), noop());

        Usuario socio = new Usuario("u-socio", "Carlos Socio", "carlos@sonic.com", "9611234567",
                Usuario.ROL_SOCIO, "Admin#2026", true, ahoraFijo);
        usuarioRepo.crearPersonal(socio, "Admin#2026", noop());

        paqueteBasico = new Paquete();
        paqueteBasico.setId("paq-basico");
        paqueteBasico.setNombre("Paquete Básico");
        paqueteBasico.setPrecio(3500.0);
        paqueteBasico.setDuracionBaseHoras(4);
        paqueteBasico.setBufferMontajeMin(90);
        paqueteBasico.setBufferDesmontajeMin(120);
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

    @Test
    public void test01_telefono10_y_clave_unificacionYFallback() {
        // 1. Variantes de formato telefónico producen la misma clave tel:
        assertEquals("7471234567", ClienteUtils.telefono10("+52 747 123 4567"));
        assertEquals("7471234567", ClienteUtils.telefono10("(747) 123-4567"));
        assertEquals("7471234567", ClienteUtils.telefono10("7471234567"));

        String k1 = ClienteUtils.clave(null, "+52 747 123 4567", "Juan Perez");
        String k2 = ClienteUtils.clave("id-x", "(747) 123-4567", "J. Perez");
        String k3 = ClienteUtils.clave(null, "7471234567", "Juan");
        assertEquals("tel:7471234567", k1);
        assertEquals("tel:7471234567", k2);
        assertEquals("tel:7471234567", k3);

        // 2. Sin teléfono utilizable (menos de 10 dígitos) usa id:
        String kId = ClienteUtils.clave("usr-99", "12345", "Pedro Gómez");
        assertEquals("id:usr-99", kId);

        // 3. Sin teléfono ni id usa nom: normalizado ignorando acentos y mayúsculas
        String kNom1 = ClienteUtils.clave(null, null, "José Ángel   López");
        String kNom2 = ClienteUtils.clave(null, "", "jose angel lopez");
        assertEquals("nom:jose angel lopez", kNom1);
        assertEquals("nom:jose angel lopez", kNom2);
    }

    @Test
    public void test02_resumenes_agrupaMismoTelefonoYCalculaTotales() {
        // Pedido 1: App con inicioMillis = 1000, nombre "Juan P."
        Pedido p1 = new Pedido();
        p1.setId("ped-1");
        p1.setClienteId("usr-app-1");
        p1.setClienteTelefono("747 123 4567");
        p1.setClienteNombre("Juan P.");
        p1.setInicioMillis(1000L);
        p1.setTotal(3500.0);
        p1.setAnticipoRequerido(1050.0);
        p1.setAnticipoPagado(1050.0); // saldo pendiente = 2450.0
        p1.setEstado(Pedido.ESTADO_CONFIRMADO);

        // Pedido 2: Socio con inicioMillis = 2000, nombre "Juan Pérez"
        Pedido p2 = new Pedido();
        p2.setId("ped-2");
        p2.setClienteId(null);
        p2.setClienteTelefono("(747) 123-4567");
        p2.setClienteNombre("Juan Pérez");
        p2.setInicioMillis(2000L);
        p2.setTotal(4000.0);
        p2.setAnticipoRequerido(1200.0);
        p2.setAnticipoPagado(1200.0); // saldo pendiente = 2800.0
        p2.setEstado(Pedido.ESTADO_CONFIRMADO);

        // Pagos: Anticipo ped-1 ($1050) y ped-2 ($1200), y abono ped-2 ($800)
        Pago pago1 = new Pago("pg-1", "ped-1", "usr-app-1", 1050.0, Pago.TIPO_ANTICIPO, Pago.METODO_EFECTIVO, 1000L);
        Pago pago2 = new Pago("pg-2", "ped-2", null, 1200.0, Pago.TIPO_ANTICIPO, Pago.METODO_TRANSFERENCIA, 2000L);
        Pago pago3 = new Pago("pg-3", "ped-2", null, 800.0, Pago.TIPO_ABONO, Pago.METODO_EFECTIVO, 2100L);

        // Usuario cliente coincidente
        Usuario uCliente = new Usuario("usr-app-1", "Juan Registrado", "juan@correo.com", "7471234567",
                Usuario.ROL_CLIENTE, "Pass#123", true, ahoraFijo);

        List<ClienteUtils.ResumenCliente> resumenes = ClienteUtils.resumenes(
                Arrays.asList(p1, p2),
                Arrays.asList(pago1, pago2, pago3),
                Collections.singletonList(uCliente)
        );

        // Debe agrupar en UN solo cliente
        assertEquals(1, resumenes.size());
        ClienteUtils.ResumenCliente c = resumenes.get(0);

        assertEquals("tel:7471234567", c.getClave());
        assertEquals("Juan Pérez", c.getNombre()); // Nombre del pedido más reciente (2000 > 1000)
        assertEquals("juan@correo.com", c.getCorreo());
        assertEquals("usr-app-1", c.getClienteId());
        assertEquals(2, c.getPedidosValidos());
        assertEquals(3050.0, c.getTotalPagadoNeto(), 0.001); // 1050 + 1200 + 800
        assertEquals(5250.0, c.getSaldoPendiente(), 0.001); // 2450 + 2800
        assertEquals(2000L, c.getUltimoEventoMillis());
    }

    @Test
    public void test03_resumenes_noCuentaApartadosNiCancelados_eIncluyeUsuarioSinPedidos() {
        // Pedido cancelado
        Pedido pCanc = new Pedido();
        pCanc.setId("ped-canc");
        pCanc.setClienteTelefono("7471112233");
        pCanc.setClienteNombre("Pedro Cancelado");
        pCanc.setInicioMillis(1000L);
        pCanc.setTotal(3500.0);
        pCanc.setAnticipoPagado(1050.0);
        pCanc.setEstado(Pedido.ESTADO_CANCELADO);

        // Pedido apartado
        Pedido pApart = new Pedido();
        pApart.setId("ped-apart");
        pApart.setClienteTelefono("7471112233");
        pApart.setClienteNombre("Pedro Apartado");
        pApart.setInicioMillis(2000L);
        pApart.setTotal(3500.0);
        pApart.setEstado(Pedido.ESTADO_APARTADO);

        // Usuario cliente sin pedidos
        Usuario uSinPedidos = new Usuario("usr-nuevo", "Ana Gómez", "ana@correo.com", "7479998877",
                Usuario.ROL_CLIENTE, "Pass#123", true, ahoraFijo);

        List<ClienteUtils.ResumenCliente> resumenes = ClienteUtils.resumenes(
                Arrays.asList(pCanc, pApart),
                Collections.<Pago>emptyList(),
                Collections.singletonList(uSinPedidos)
        );

        assertEquals(2, resumenes.size());

        // Ordenados alfabéticamente por nombre normalizado: Ana Gómez antes que Pedro Apartado
        ClienteUtils.ResumenCliente cAna = resumenes.get(0);
        assertEquals("Ana Gómez", cAna.getNombre());
        assertEquals(0, cAna.getPedidosValidos());
        assertEquals(0.0, cAna.getTotalPagadoNeto(), 0.001);
        assertEquals(0.0, cAna.getSaldoPendiente(), 0.001);
        assertEquals(0L, cAna.getUltimoEventoMillis());

        ClienteUtils.ResumenCliente cPedro = resumenes.get(1);
        assertEquals("Pedro Apartado", cPedro.getNombre());
        assertEquals(0, cPedro.getPedidosValidos()); // No cuenta apartados ni cancelados
        assertEquals(2000L, cPedro.getUltimoEventoMillis());
    }

    @Test
    public void test04_buscar_porNombreSinAcentos_yPorTelefonoParcial() {
        ClienteUtils.ResumenCliente c1 = new ClienteUtils.ResumenCliente("k1", "Ángel Morales", "7471234567", null, null, 1, 100, 0, 1000);
        ClienteUtils.ResumenCliente c2 = new ClienteUtils.ResumenCliente("k2", "Beatriz Soto", "5559876543", null, null, 1, 100, 0, 1000);
        List<ClienteUtils.ResumenCliente> lista = Arrays.asList(c1, c2);

        // Búsqueda por nombre sin acentos
        List<ClienteUtils.ResumenCliente> rNombre = ClienteUtils.buscar(lista, "angel");
        assertEquals(1, rNombre.size());
        assertEquals("Ángel Morales", rNombre.get(0).getNombre());

        // Búsqueda por teléfono parcial (3+ dígitos)
        List<ClienteUtils.ResumenCliente> rTel = ClienteUtils.buscar(lista, "987");
        assertEquals(1, rTel.size());
        assertEquals("Beatriz Soto", rTel.get(0).getNombre());

        // Búsqueda con menos de 3 dígitos numéricos no activa filtro de teléfono
        List<ClienteUtils.ResumenCliente> rCorto = ClienteUtils.buscar(lista, "98");
        assertTrue(rCorto.isEmpty());

        // Consulta vacía devuelve todos
        assertEquals(2, ClienteUtils.buscar(lista, "").size());
        assertEquals(2, ClienteUtils.buscar(lista, null).size());
    }

    @Test
    public void test05_pedidosDelCliente_ordenaPorInicioDescendente() {
        Pedido p1 = new Pedido();
        p1.setId("p1");
        p1.setClienteTelefono("7471234567");
        p1.setInicioMillis(1000L);

        Pedido p2 = new Pedido();
        p2.setId("p2");
        p2.setClienteTelefono("7471234567");
        p2.setInicioMillis(5000L);

        Pedido p3 = new Pedido();
        p3.setId("p3");
        p3.setClienteTelefono("7471234567");
        p3.setInicioMillis(3000L);

        Pedido otro = new Pedido();
        otro.setId("p-otro");
        otro.setClienteTelefono("5550001122");
        otro.setInicioMillis(4000L);

        List<Pedido> ordenados = ClienteUtils.pedidosDelCliente(Arrays.asList(p1, p2, p3, otro), "tel:7471234567");
        assertEquals(3, ordenados.size());
        assertEquals("p2", ordenados.get(0).getId()); // 5000
        assertEquals("p3", ordenados.get(1).getId()); // 3000
        assertEquals("p1", ordenados.get(2).getId()); // 1000
    }

    @Test
    public void test06_aprobarPedido_deEnRevisionAConfirmado_yErrorEnOtrosEstados() {
        // Crear pedido de app con menos de 48h para que requiera revisión
        long iniUrgente = ahoraFijo + 24 * 3600_000L;
        SolicitudReserva sol = new SolicitudReserva("paq-basico", iniUrgente, 240, Pedido.ORIGEN_APP,
                "Fiesta Urgente", "usr-1", "Cliente App", "7471234567", "Av. Insurgentes 123", "", 24);

        AtomicReference<Pedido> pedRef = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pedRef.set(result); }
            @Override public void onError(String error) { fail("crearReserva error: " + error); }
        });

        Pedido p = pedRef.get();
        assertNotNull(p);
        assertTrue(p.isRequiereRevision());

        // Registrar anticipo para que transicione a EN_REVISION
        pedidoRepo.registrarAnticipo(p.getId(), 1050.0, Pedido.METODO_EFECTIVO, noop());
        Pedido enRevision = pedidoRepo.getPedidoPorId(p.getId());
        assertEquals(Pedido.ESTADO_EN_REVISION, enRevision.getEstado());
        assertNotNull(enRevision.getEventoId());
        assertEquals(1, enRevision.getEquipoIds().size());

        // Aprobar pedido
        AtomicReference<Pedido> aprobadoRef = new AtomicReference<>();
        pedidoRepo.aprobarPedido(p.getId(), new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { aprobadoRef.set(result); }
            @Override public void onError(String error) {}
        });

        assertNotNull(aprobadoRef.get());
        assertEquals(Pedido.ESTADO_CONFIRMADO, aprobadoRef.get().getEstado());
        assertFalse(aprobadoRef.get().isRequiereRevision());
        assertNotNull(aprobadoRef.get().getEventoId());
        assertEquals(1, aprobadoRef.get().getEquipoIds().size());

        // Intentar aprobar cuando ya está confirmado -> error
        AtomicReference<String> errYaAprobado = new AtomicReference<>();
        pedidoRepo.aprobarPedido(p.getId(), new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String error) { errYaAprobado.set(error); }
        });
        assertEquals("El pedido no está en revisión", errYaAprobado.get());
    }

    @Test
    public void test07_rechazarPedido_reembolsoTotal_eventoCancelado_yLiberacion() {
        long iniUrgente = ahoraFijo + 24 * 3600_000L;
        SolicitudReserva sol = new SolicitudReserva("paq-basico", iniUrgente, 240, Pedido.ORIGEN_APP,
                "Boda Urgente", "usr-1", "Cliente App", "7471234567", "Av. Insurgentes 123", "", 24);

        AtomicReference<Pedido> pedRef = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pedRef.set(result); }
            @Override public void onError(String error) {}
        });
        Pedido p = pedRef.get();

        // 1. Anticipo $1,050 por transferencia
        pedidoRepo.registrarAnticipo(p.getId(), 1050.0, Pedido.METODO_TRANSFERENCIA, noop());
        // 2. Abono $500
        pedidoRepo.liquidarSaldo(p.getId(), 500.0, Pedido.METODO_TRANSFERENCIA, noop());

        Pedido rev = pedidoRepo.getPedidoPorId(p.getId());
        assertEquals(1550.0, rev.getAnticipoPagado(), 0.001);
        String eventoId = rev.getEventoId();

        // Rechazar pedido
        AtomicReference<Pedido> rechRef = new AtomicReference<>();
        pedidoRepo.rechazarPedido(p.getId(), "No hay personal suficiente", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { rechRef.set(result); }
            @Override public void onError(String error) {}
        });

        Pedido rechazado = rechRef.get();
        assertNotNull(rechazado);
        assertEquals(Pedido.ESTADO_RECHAZADO, rechazado.getEstado());
        assertEquals("No hay personal suficiente", rechazado.getMotivoRechazo());
        assertEquals(1550.0, rechazado.getReembolsoMonto(), 0.001);
        assertTrue(rechazado.getEquipoIds().isEmpty());
        assertFalse(rechazado.isAsignacionIncompleta());

        // Evento queda cancelado
        final Evento[] evBox = new Evento[1];
        eventoRepo.getEventoPorId(eventoId, new Callback<Evento>() {
            @Override
            public void onSuccess(Evento result) {
                evBox[0] = result;
            }
            @Override
            public void onError(String error) {
                fail(error);
            }
        });
        Evento ev = evBox[0];
        assertNotNull(ev);
        assertEquals(Evento.ESTADO_CANCELADO, ev.getEstado());

        // Se registró un Pago de tipo REEMBOLSO por $1550 con método TRANSFERENCIA
        List<Pago> pagos = pagoRepo.getTodosLosPagos().getValue();
        Pago reembolsoPago = null;
        for (Pago pg : pagos) {
            if (Pago.TIPO_REEMBOLSO.equals(pg.getTipo())) {
                reembolsoPago = pg;
                break;
            }
        }
        assertNotNull(reembolsoPago);
        assertEquals(1550.0, reembolsoPago.getMonto(), 0.001);
        assertEquals(Pedido.METODO_TRANSFERENCIA, reembolsoPago.getMetodo());
        assertNotNull(reembolsoPago.getFolio());
        assertTrue(reembolsoPago.getFolio().startsWith("R-"));
    }

    @Test
    public void test08_rechazarPedido_validacionesMotivoYDobleRechazo() {
        long iniUrgente = ahoraFijo + 24 * 3600_000L;
        SolicitudReserva sol = new SolicitudReserva("paq-basico", iniUrgente, 240, Pedido.ORIGEN_APP,
                "Fiesta Urgente", "usr-1", "Cliente App", "7471234567", "Av. Insurgentes 123", "", 24);

        AtomicReference<Pedido> pedRef = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pedRef.set(result); }
            @Override public void onError(String error) {}
        });
        Pedido p = pedRef.get();
        pedidoRepo.registrarAnticipo(p.getId(), 1050.0, Pedido.METODO_EFECTIVO, noop());

        // 1. Motivo nulo -> error y sin cambios
        AtomicReference<String> errNull = new AtomicReference<>();
        pedidoRepo.rechazarPedido(p.getId(), null, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String error) { errNull.set(error); }
        });
        assertNotNull(errNull.get());
        assertEquals(Pedido.ESTADO_EN_REVISION, pedidoRepo.getPedidoPorId(p.getId()).getEstado());

        // 2. Motivo vacío o < 3 chars -> error y sin cambios
        AtomicReference<String> errCorto = new AtomicReference<>();
        pedidoRepo.rechazarPedido(p.getId(), "  no ", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String error) { errCorto.set(error); }
        });
        assertNotNull(errCorto.get());
        assertEquals(Pedido.ESTADO_EN_REVISION, pedidoRepo.getPedidoPorId(p.getId()).getEstado());

        // 3. Rechazo exitoso
        pedidoRepo.rechazarPedido(p.getId(), "Motivo válido", noop());
        assertEquals(Pedido.ESTADO_RECHAZADO, pedidoRepo.getPedidoPorId(p.getId()).getEstado());

        // 4. Doble rechazo -> error
        AtomicReference<String> errDoble = new AtomicReference<>();
        pedidoRepo.rechazarPedido(p.getId(), "Otro motivo", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String error) { errDoble.set(error); }
        });
        assertEquals("El pedido no está en revisión", errDoble.get());
    }

    @Test
    public void test09_trasRechazar_reasignacionRecuperaUnidades() {
        // 2da bocina y 2do operador para permitir 2 eventos simultáneos
        equipoRepo.agregarEquipo(new Equipo("boc-2", "Bocina 15", CategoriaEquipo.BOCINAS,
                2, Equipo.ESTADO_DISPONIBLE, 500.0), noop());
        Usuario socio2 = new Usuario("u-socio-2", "Segundo Socio", "socio2@sonic.com", "9611234569",
                Usuario.ROL_SOCIO, "Admin#2026", true, ahoraFijo);
        usuarioRepo.crearPersonal(socio2, "Admin#2026", noop());

        long iniUrgente = ahoraFijo + 24 * 3600_000L;

        // Pedido 1: App urgente
        SolicitudReserva sol1 = new SolicitudReserva("paq-basico", iniUrgente, 240, Pedido.ORIGEN_APP,
                "Pedido 1 Urgente", "usr-1", "Cliente 1", "7471111111", "Av. Insurgentes 123", "", 24);
        AtomicReference<Pedido> p1Ref = new AtomicReference<>();
        pedidoRepo.crearReserva(sol1, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { p1Ref.set(result); }
            @Override public void onError(String error) { fail("sol1 error: " + error); }
        });
        pedidoRepo.registrarAnticipo(p1Ref.get().getId(), 1050.0, Pedido.METODO_EFECTIVO, noop());

        // Pedido 2: Simultáneo
        SolicitudReserva sol2 = new SolicitudReserva("paq-basico", iniUrgente, 240, Pedido.ORIGEN_LLAMADA,
                "Pedido 2 Simultáneo", null, "Cliente 2", "7472222222", "Av. Insurgentes 123", "", 24);
        AtomicReference<Pedido> p2Ref = new AtomicReference<>();
        pedidoRepo.crearReserva(sol2, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { p2Ref.set(result); }
            @Override public void onError(String error) { fail("sol2 error: " + error); }
        });
        pedidoRepo.registrarAnticipo(p2Ref.get().getId(), 1050.0, Pedido.METODO_EFECTIVO, noop());

        // Ambos tienen bocina asignada inicialmente
        Pedido p2Inicial = pedidoRepo.getPedidoPorId(p2Ref.get().getId());
        assertFalse(p2Inicial.isAsignacionIncompleta());

        // Dañamos la bocina asignada a pedido 2 -> queda incompleto
        String bocinaP2 = p2Inicial.getEquipoIds().get(0);
        equipoRepo.cambiarEstado(bocinaP2, Equipo.ESTADO_DANADO, noop());

        Pedido ped2Incompleto = pedidoRepo.getPedidoPorId(p2Ref.get().getId());
        assertTrue("El pedido 2 debía quedar incompleto al dañarse su bocina", ped2Incompleto.isAsignacionIncompleta());

        // Rechazamos el pedido 1
        pedidoRepo.rechazarPedido(p1Ref.get().getId(), "No se puede atender", noop());

        // Pedido 2 debe haber recuperado la bocina restante
        Pedido ped2Actualizado = pedidoRepo.getPedidoPorId(p2Ref.get().getId());
        assertFalse("El pedido 2 ya no debe tener asignación incompleta", ped2Actualizado.isAsignacionIncompleta());
        assertEquals(1, ped2Actualizado.getEquipoIds().size());
        assertEquals(bocinaP2.equals("boc-1") ? "boc-2" : "boc-1", ped2Actualizado.getEquipoIds().get(0));
    }

    @Test
    public void test10_finanzas_netoPedidoRechazadoQuedaEnCero_yExcluidoDePorCobrar() {
        long ini = ahoraFijo + 24 * 3600_000L;
        SolicitudReserva sol = new SolicitudReserva("paq-basico", ini, 240, Pedido.ORIGEN_APP,
                "Evento Noviembre", "usr-1", "Cliente Finanzas", "7471234567", "Av. Insurgentes 123", "", 24);
        AtomicReference<Pedido> pedRef = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pedRef.set(result); }
            @Override public void onError(String error) {}
        });
        Pedido p = pedRef.get();
        pedidoRepo.registrarAnticipo(p.getId(), 1050.0, Pedido.METODO_EFECTIVO, noop());

        // Rechazamos con reembolso total
        pedidoRepo.rechazarPedido(p.getId(), "Rechazo operativo", noop());

        List<Pago> pagos = pagoRepo.getTodosLosPagos().getValue();
        List<Pedido> pedidos = pedidoRepo.obtenerTodos();

        double[] resumen = FinanzasUtils.resumenMes(pagos, pedidos, 2026, Calendar.NOVEMBER);
        double cobrado = resumen[0];
        double reembolsos = resumen[1];
        double neto = resumen[2];
        double porCobrar = resumen[3];

        assertEquals(1050.0, cobrado, 0.001);
        assertEquals(1050.0, reembolsos, 0.001);
        assertEquals(0.0, neto, 0.001);
        assertEquals(0.0, porCobrar, 0.001); // RECHAZADO no entra en porCobrar
    }

    @Test
    public void test11_dashboardUtils_contarEnRevision() {
        Pedido p1 = new Pedido();
        p1.setEstado(Pedido.ESTADO_EN_REVISION);

        Pedido p2 = new Pedido();
        p2.setEstado(Pedido.ESTADO_CONFIRMADO);

        Pedido p3 = new Pedido();
        p3.setEstado(Pedido.ESTADO_EN_REVISION);

        Pedido p4 = new Pedido();
        p4.setEstado(Pedido.ESTADO_CANCELADO);

        List<Pedido> lista = Arrays.asList(p1, p2, p3, p4);
        assertEquals(2, DashboardUtils.contarEnRevision(lista));
        assertEquals(0, DashboardUtils.contarEnRevision(Collections.singletonList(p2)));
        assertEquals(0, DashboardUtils.contarEnRevision(null));
    }
}
