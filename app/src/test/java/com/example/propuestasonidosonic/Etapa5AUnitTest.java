package com.example.propuestasonidosonic;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.memory.InMemoryDiaOperativoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryEquipoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryEventoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPaqueteRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPedidoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryUsuarioRepository;
import com.example.propuestasonidosonic.domain.MotorDisponibilidad;
import com.example.propuestasonidosonic.domain.ReservaClienteUtils;
import com.example.propuestasonidosonic.domain.ResultadoDisponibilidad;
import com.example.propuestasonidosonic.domain.SolicitudReserva;
import com.example.propuestasonidosonic.model.CategoriaEquipo;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.DiaOperativo;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Evento;
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

public class Etapa5AUnitTest {

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

        // Paquete básico
        paqueteBasico = new Paquete();
        paqueteBasico.setId("paq-basico");
        paqueteBasico.setNombre("Paquete Básico");
        paqueteBasico.setPrecio(3500.0);
        paqueteBasico.setPrecioHoraExtra(500.0);
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

    // 6. ReservaClienteUtils.esUrgente: 47 h 59 min -> urgente; exactamente 48 h -> no; 10 días -> no
    @Test
    public void test06_reservaClienteUtils_esUrgente() {
        long ini47h59m = ahoraFijo + (47L * 3600_000L) + (59L * 60_000L);
        assertTrue(ReservaClienteUtils.esUrgente(ini47h59m, ahoraFijo));

        long iniExacto48h = ahoraFijo + (48L * 3600_000L);
        assertFalse(ReservaClienteUtils.esUrgente(iniExacto48h, ahoraFijo));

        long ini10dias = ahoraFijo + (10L * 24L * 3600_000L);
        assertFalse(ReservaClienteUtils.esUrgente(ini10dias, ahoraFijo));
    }

    // 8. mensajeAmigable: cada motivoNoViable real no revela palabras internas y desconocido da genérico
    @Test
    public void test08_reservaClienteUtils_mensajeAmigable() {
        List<String> motivos = Arrays.asList(
                ResultadoDisponibilidad.PAQUETE_INACTIVO,
                ResultadoDisponibilidad.PAQUETE_INCOMPLETO,
                ResultadoDisponibilidad.DURACION_INVALIDA,
                ResultadoDisponibilidad.EN_EL_PASADO,
                ResultadoDisponibilidad.FUERA_DE_HORARIO,
                ResultadoDisponibilidad.SIN_STOCK,
                ResultadoDisponibilidad.MAXIMO_SIMULTANEOS,
                ResultadoDisponibilidad.SIN_OPERADORES
        );

        for (String m : motivos) {
            String msg = ReservaClienteUtils.mensajeAmigable(m);
            assertNotNull(msg);
            String lower = msg.toLowerCase();
            assertFalse("No debe revelar 'stock': " + msg, lower.contains("stock"));
            assertFalse("No debe revelar 'operador': " + msg, lower.contains("operador"));
            assertFalse("No debe revelar 'capacidad': " + msg, lower.contains("capacidad"));
        }

        // Desconocido -> genérico
        String generico = ReservaClienteUtils.mensajeAmigable("MOTIVO_INVENTADO_123");
        assertEquals("Ese horario ya no está disponible. Elige otro, por favor.", generico);

        // Errores de validación de formulario se respetan
        String errNom = "El nombre del evento es obligatorio y debe tener máximo 80 caracteres";
        assertEquals(errNom, ReservaClienteUtils.mensajeAmigable(errNom));
    }

    // 9. paquetesVisibles: excluye inactivos y ordena por precio asc y luego por nombre
    @Test
    public void test09_reservaClienteUtils_paquetesVisibles() {
        Paquete pInactivo = new Paquete();
        pInactivo.setId("p-inact");
        pInactivo.setNombre("Paquete Inactivo");
        pInactivo.setPrecio(1000.0);
        pInactivo.setActivo(false);

        Paquete pCaro = new Paquete();
        pCaro.setId("p-caro");
        pCaro.setNombre("Paquete VIP");
        pCaro.setPrecio(6000.0);
        pCaro.setActivo(true);

        Paquete pMedioB = new Paquete();
        pMedioB.setId("p-m-b");
        pMedioB.setNombre("Paquete Boda");
        pMedioB.setPrecio(3000.0);
        pMedioB.setActivo(true);

        Paquete pMedioA = new Paquete();
        pMedioA.setId("p-m-a");
        pMedioA.setNombre("Paquete Aniversario");
        pMedioA.setPrecio(3000.0);
        pMedioA.setActivo(true);

        List<Paquete> visibles = ReservaClienteUtils.paquetesVisibles(Arrays.asList(pInactivo, pCaro, pMedioB, pMedioA));

        assertEquals(3, visibles.size());
        assertEquals("Paquete Aniversario", visibles.get(0).getNombre());
        assertEquals("Paquete Boda", visibles.get(1).getNombre());
        assertEquals("Paquete VIP", visibles.get(2).getNombre());
    }

    private Pedido solicitar(String nombre, String clienteId, long inicio) {
        SolicitudReserva sol = new SolicitudReserva("paq-basico", inicio, 240, Pedido.ORIGEN_APP,
                nombre, clienteId, "Cliente " + clienteId, "7471111111", "Av. Central 123", "");
        AtomicReference<Pedido> ref = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String error) { fail("Error crearReserva: " + error); }
        });
        assertNotNull(ref.get());
        return ref.get();
    }

    // 10. Repositorio: segunda solicitud de la app del mismo cliente con otra por aprobar -> error;
    //     con otro cliente -> permitido; con ORIGEN_PRESENCIAL -> permitido
    @Test
    public void test10_repositorio_reglaAntiAbuso() {
        long ini = ahoraFijo + 72 * 3600_000L;
        Pedido p1 = solicitar("Boda Cliente 1", "cli-1", ini);
        assertEquals(Pedido.ESTADO_POR_APROBAR, p1.getEstado());

        // Segunda solicitud del mismo cliente cli-1 -> error
        SolicitudReserva sol1Bis = new SolicitudReserva("paq-basico", ini + 24 * 3600_000L, 240, Pedido.ORIGEN_APP,
                "Boda Cliente 1 Bis", "cli-1", "Cliente Uno", "7471111111", "Av. Central 123", "");
        AtomicReference<String> errBis = new AtomicReference<>();
        pedidoRepo.crearReserva(sol1Bis, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { fail("No debía permitir una segunda solicitud por aprobar"); }
            @Override public void onError(String error) { errBis.set(error); }
        });
        assertNotNull(errBis.get());
        assertTrue(errBis.get().contains("solicitud por aprobar"));
        // El mensaje se muestra tal cual al cliente
        assertEquals(errBis.get(), ReservaClienteUtils.mensajeAmigable(errBis.get()));

        // Con otro cliente cli-2 -> permitido
        assertNotNull(solicitar("Boda Cliente 2", "cli-2", ini));

        // Con ORIGEN_PRESENCIAL del mismo cliente cli-1 -> permitido (lo registra un socio)
        long iniPres = ini + 48 * 3600_000L;
        SolicitudReserva solPresencial = new SolicitudReserva("paq-basico", iniPres, 240, Pedido.ORIGEN_PRESENCIAL,
                "Boda Presencial cli-1", "cli-1", "Cliente Uno", "7471111111", "Av. Central 123", "");
        AtomicReference<Pedido> pPresRef = new AtomicReference<>();
        pedidoRepo.crearReserva(solPresencial, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pPresRef.set(result); }
            @Override public void onError(String error) { fail("Error presencial: " + error); }
        });
        assertEquals(Pedido.ESTADO_CONFIRMADO, pPresRef.get().getEstado());
    }

    // 11. Repositorio: cuando la solicitud se aprueba, se rechaza o vence, el cliente puede enviar otra.
    @Test
    public void test11_repositorio_solicitudResuelta_permiteNuevaSolicitud() {
        long ini = ahoraFijo + 72 * 3600_000L;

        // Aprobada
        Pedido p1 = solicitar("Evento 1", "cli-x", ini);
        pedidoRepo.aprobarPedido(p1.getId(), noop());
        assertEquals(Pedido.ESTADO_CONFIRMADO, pedidoRepo.getPedidoPorId(p1.getId()).getEstado());

        // Rechazada
        Pedido p2 = solicitar("Evento 2", "cli-x", ini + 24 * 3600_000L);
        pedidoRepo.rechazarPedido(p2.getId(), "Fecha no disponible", noop());
        assertEquals(Pedido.ESTADO_RECHAZADO, pedidoRepo.getPedidoPorId(p2.getId()).getEstado());

        // Vencida: nadie la aprueba antes del inicio del evento
        long iniCercano = ahoraFijo + 5 * 3600_000L;
        Pedido p3 = solicitar("Evento 3", "cli-x", iniCercano);
        reloj.avanzarHoras(6);
        pedidoRepo.actualizarEstadosPorTiempo();
        assertEquals(Pedido.ESTADO_VENCIDO, pedidoRepo.getPedidoPorId(p3.getId()).getEstado());

        // Puede volver a solicitar
        assertNotNull(solicitar("Evento 4", "cli-x", ini + 48 * 3600_000L));
    }

    // 12. Flujo completo: solicitud (app) -> por aprobar -> socio aprueba -> confirmado con evento y unidades
    //     -> el socio confirma el pago en efectivo. Una solicitud a menos de 48 h se marca como urgente.
    @Test
    public void test12_flujoCompleto_solicitudAprobacionYPago() {
        long iniNormal = ahoraFijo + 72 * 3600_000L;
        Pedido pNorm = solicitar("Evento Normal", "cli-flujo-1", iniNormal);
        assertEquals(Pedido.ESTADO_POR_APROBAR, pNorm.getEstado());
        assertTrue(pNorm.getMotivosRevision().isEmpty());
        assertNull(pNorm.getEventoId());

        AtomicReference<Pedido> aprobado = new AtomicReference<>();
        pedidoRepo.aprobarPedido(pNorm.getId(), new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { aprobado.set(result); }
            @Override public void onError(String error) { fail("Error aprobar: " + error); }
        });
        assertEquals(Pedido.ESTADO_CONFIRMADO, aprobado.get().getEstado());
        Pedido conEvento = pedidoRepo.getPedidoPorId(pNorm.getId());
        assertNotNull(conEvento.getEventoId());
        assertFalse(conEvento.getEquipoIds().isEmpty());

        AtomicReference<Evento> ev = new AtomicReference<>();
        eventoRepo.getEventoPorId(conEvento.getEventoId(), new Callback<Evento>() {
            @Override public void onSuccess(Evento result) { ev.set(result); }
            @Override public void onError(String error) {}
        });
        assertNotNull(ev.get());
        assertEquals(Evento.ESTADO_PROGRAMADO, ev.get().getEstado());

        AtomicReference<Pedido> pagado = new AtomicReference<>();
        pedidoRepo.confirmarPago(pNorm.getId(), Pedido.METODO_EFECTIVO, "u-socio", "Carlos Socio", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pagado.set(result); }
            @Override public void onError(String error) { fail("Error confirmarPago: " + error); }
        });
        assertTrue(pagado.get().isPagoConfirmado());
        assertEquals(Pedido.METODO_EFECTIVO, pagado.get().getMetodoPago());
        assertEquals(Pedido.ESTADO_CONFIRMADO, pagado.get().getEstado());

        // Solicitud urgente (< 48 h)
        long iniUrgente = ahoraFijo + 24 * 3600_000L;
        Pedido pUrg = solicitar("Evento Urgente", "cli-flujo-2", iniUrgente);
        assertEquals(Pedido.ESTADO_POR_APROBAR, pUrg.getEstado());
        assertTrue(pUrg.getMotivosRevision().contains(Pedido.MOTIVO_URGENTE));
    }
}
