package com.example.propuestasonidosonic;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.PasarelaPago;
import com.example.propuestasonidosonic.data.memory.InMemoryDiaOperativoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryEquipoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryEventoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPagoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPaqueteRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPedidoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryUsuarioRepository;
import com.example.propuestasonidosonic.data.memory.PasarelaSimulada;
import com.example.propuestasonidosonic.domain.MotorDisponibilidad;
import com.example.propuestasonidosonic.domain.ReservaClienteUtils;
import com.example.propuestasonidosonic.domain.ResultadoDisponibilidad;
import com.example.propuestasonidosonic.domain.SolicitudReserva;
import com.example.propuestasonidosonic.domain.TarjetaUtils;
import com.example.propuestasonidosonic.model.CategoriaEquipo;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.DiaOperativo;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Evento;
import com.example.propuestasonidosonic.model.Pago;
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
    private InMemoryPagoRepository pagoRepo;
    private InMemoryEventoRepository eventoRepo;
    private InMemoryPedidoRepository pedidoRepo;
    private PasarelaSimulada pasarelaSimulada;

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
        pasarelaSimulada = new PasarelaSimulada();

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

    // 1. TarjetaUtils.luhnValido y numeroValido
    @Test
    public void test01_tarjetaUtils_luhn_y_numeroValido() {
        assertTrue(TarjetaUtils.luhnValido("4242424242424242"));
        assertFalse(TarjetaUtils.luhnValido("4242424242424241"));

        assertTrue(TarjetaUtils.numeroValido("4242424242424242"));
        assertFalse(TarjetaUtils.numeroValido("4242424242424241"));

        // Amex válida 15 dígitos
        assertTrue(TarjetaUtils.numeroValido("378282246310005"));

        // Longitudes incorrectas
        assertFalse(TarjetaUtils.numeroValido("42424242424242")); // 14 dígitos
        assertFalse(TarjetaUtils.numeroValido("42424242424242424")); // 17 dígitos
        assertFalse(TarjetaUtils.numeroValido(""));
        assertFalse(TarjetaUtils.numeroValido(null));
    }

    // 2. marca para Visa, Mastercard, Amex, desconocida; cvvValido por marca
    @Test
    public void test02_tarjetaUtils_marca_y_cvvValido() {
        assertEquals("VISA", TarjetaUtils.marca("4123456789012345"));
        assertEquals("MASTERCARD", TarjetaUtils.marca("5123456789012345"));
        assertEquals("MASTERCARD", TarjetaUtils.marca("5523456789012345"));
        assertEquals("MASTERCARD", TarjetaUtils.marca("2221000000000000"));
        assertEquals("MASTERCARD", TarjetaUtils.marca("2720999999999999"));
        assertEquals("AMEX", TarjetaUtils.marca("340000000000000"));
        assertEquals("AMEX", TarjetaUtils.marca("370000000000000"));
        assertEquals("DESCONOCIDA", TarjetaUtils.marca("6011000000000000"));
        assertEquals("DESCONOCIDA", TarjetaUtils.marca(""));

        // CVV según marca
        assertTrue(TarjetaUtils.cvvValido("1234", "AMEX"));
        assertFalse(TarjetaUtils.cvvValido("123", "AMEX"));
        assertFalse(TarjetaUtils.cvvValido("12345", "AMEX"));

        assertTrue(TarjetaUtils.cvvValido("123", "VISA"));
        assertFalse(TarjetaUtils.cvvValido("1234", "VISA"));
        assertTrue(TarjetaUtils.cvvValido("456", "MASTERCARD"));
        assertFalse(TarjetaUtils.cvvValido("45a", "MASTERCARD"));
    }

    // 3. vencimientoValido: mes actual vale, anterior no, mes 13 y 0 no, año futuro sí
    @Test
    public void test03_tarjetaUtils_vencimientoValido() {
        // En noviembre de 2026:
        // Mes actual: 11/26 -> válido
        assertTrue(TarjetaUtils.vencimientoValido(11, 26, ahoraFijo));

        // Mes anterior: 10/26 -> vencido
        assertFalse(TarjetaUtils.vencimientoValido(10, 26, ahoraFijo));

        // Meses inválidos
        assertFalse(TarjetaUtils.vencimientoValido(0, 26, ahoraFijo));
        assertFalse(TarjetaUtils.vencimientoValido(13, 26, ahoraFijo));

        // Año futuro: 05/27 -> válido
        assertTrue(TarjetaUtils.vencimientoValido(5, 27, ahoraFijo));

        // Año pasado: 11/25 -> vencido
        assertFalse(TarjetaUtils.vencimientoValido(11, 25, ahoraFijo));
    }

    // 4. formatearNumero para Visa (4-4-4-4) y Amex (4-6-5); titularValido con acentos y vacío
    @Test
    public void test04_tarjetaUtils_formatearNumero_y_titularValido() {
        assertEquals("4242 4242 4242 4242", TarjetaUtils.formatearNumero("4242424242424242"));
        assertEquals("3782 822463 10005", TarjetaUtils.formatearNumero("378282246310005"));

        // Titular
        assertTrue(TarjetaUtils.titularValido("José María Peña"));
        assertTrue(TarjetaUtils.titularValido("Ana Gomez"));
        assertTrue(TarjetaUtils.titularValido("GÜERO PÉREZ"));
        assertFalse(TarjetaUtils.titularValido("Jo")); // menos de 3 chars
        assertFalse(TarjetaUtils.titularValido(""));
        assertFalse(TarjetaUtils.titularValido(null));
        assertFalse(TarjetaUtils.titularValido("Carlos123")); // números no permitidos
    }

    // 5. PasarelaSimulada: aprobada 4242…, rechazada 4000…0002 y 4000…9995, referencia SIM- y ultimos4
    @Test
    public void test05_pasarelaSimulada() {
        AtomicReference<PasarelaPago.ResultadoPago> refAprobada = new AtomicReference<>();
        PasarelaPago.TarjetaDatos tAprobada = new PasarelaPago.TarjetaDatos("Carlos", "4242424242424242", 12, 28, "123");
        pasarelaSimulada.cobrar(1050.0, tAprobada, new Callback<PasarelaPago.ResultadoPago>() {
            @Override public void onSuccess(PasarelaPago.ResultadoPago r) { refAprobada.set(r); }
            @Override public void onError(String error) { fail("Error inesperado: " + error); }
        });

        assertNotNull(refAprobada.get());
        assertTrue(refAprobada.get().isAprobado());
        assertNotNull(refAprobada.get().getReferencia());
        assertTrue(refAprobada.get().getReferencia().startsWith("SIM-"));
        assertEquals("4242", refAprobada.get().getUltimos4());
        assertNull(refAprobada.get().getMotivoRechazo());

        // Rechazo 4000000000000002
        AtomicReference<PasarelaPago.ResultadoPago> refRechazo1 = new AtomicReference<>();
        PasarelaPago.TarjetaDatos tRechazo1 = new PasarelaPago.TarjetaDatos("Carlos", "4000000000000002", 12, 28, "123");
        pasarelaSimulada.cobrar(1050.0, tRechazo1, new Callback<PasarelaPago.ResultadoPago>() {
            @Override public void onSuccess(PasarelaPago.ResultadoPago r) { refRechazo1.set(r); }
            @Override public void onError(String error) { fail("Error inesperado: " + error); }
        });
        assertNotNull(refRechazo1.get());
        assertFalse(refRechazo1.get().isAprobado());
        assertEquals("La tarjeta fue rechazada por el banco", refRechazo1.get().getMotivoRechazo());
        assertEquals("0002", refRechazo1.get().getUltimos4());

        // Rechazo 4000000000009995
        AtomicReference<PasarelaPago.ResultadoPago> refRechazo2 = new AtomicReference<>();
        PasarelaPago.TarjetaDatos tRechazo2 = new PasarelaPago.TarjetaDatos("Carlos", "4000000000009995", 12, 28, "123");
        pasarelaSimulada.cobrar(1050.0, tRechazo2, new Callback<PasarelaPago.ResultadoPago>() {
            @Override public void onSuccess(PasarelaPago.ResultadoPago r) { refRechazo2.set(r); }
            @Override public void onError(String error) { fail("Error inesperado: " + error); }
        });
        assertNotNull(refRechazo2.get());
        assertFalse(refRechazo2.get().isAprobado());
        assertEquals("Fondos insuficientes", refRechazo2.get().getMotivoRechazo());
        assertEquals("9995", refRechazo2.get().getUltimos4());
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

    // 7. segundosRestantes (nunca negativo) y formatoCuentaRegresiva (1661 -> 27:41, 5 -> 00:05)
    @Test
    public void test07_segundosRestantes_y_formatoCuentaRegresiva() {
        long vencimiento = ahoraFijo + 1661_000L;
        assertEquals(1661L, ReservaClienteUtils.segundosRestantes(vencimiento, ahoraFijo));

        long vencido = ahoraFijo - 10_000L;
        assertEquals(0L, ReservaClienteUtils.segundosRestantes(vencido, ahoraFijo));

        assertEquals("27:41", ReservaClienteUtils.formatoCuentaRegresiva(1661L));
        assertEquals("00:05", ReservaClienteUtils.formatoCuentaRegresiva(5L));
        assertEquals("00:00", ReservaClienteUtils.formatoCuentaRegresiva(0L));
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

    // 10. Repositorio: segundo crearReserva ORIGEN_APP del mismo cliente con apartado vigente -> error;
    //     con otro cliente -> permitido; con ORIGEN_PRESENCIAL -> permitido
    @Test
    public void test10_repositorio_reglaAntiAbuso() {
        long ini = ahoraFijo + 72 * 3600_000L;
        SolicitudReserva sol1 = new SolicitudReserva("paq-basico", ini, 240, Pedido.ORIGEN_APP,
                "Boda Cliente 1", "cli-1", "Cliente Uno", "7471111111", "Av. Central 123", "", null);

        AtomicReference<Pedido> p1Ref = new AtomicReference<>();
        pedidoRepo.crearReserva(sol1, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { p1Ref.set(result); }
            @Override public void onError(String error) { fail("Error sol1: " + error); }
        });
        assertNotNull(p1Ref.get());
        assertEquals(Pedido.ESTADO_APARTADO, p1Ref.get().getEstado());

        // Segundo intento ORIGEN_APP del mismo cliente cli-1 -> error
        SolicitudReserva sol1Bis = new SolicitudReserva("paq-basico", ini + 3600_000L, 240, Pedido.ORIGEN_APP,
                "Boda Cliente 1 Bis", "cli-1", "Cliente Uno", "7471111111", "Av. Central 123", "", null);
        AtomicReference<String> errBis = new AtomicReference<>();
        pedidoRepo.crearReserva(sol1Bis, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { fail("No debía permitir segundo apartado"); }
            @Override public void onError(String error) { errBis.set(error); }
        });
        assertEquals("Ya tienes una reserva pendiente de pago. Págala o espera a que venza.", errBis.get());

        // Con otro cliente cli-2 -> permitido
        SolicitudReserva sol2 = new SolicitudReserva("paq-basico", ini, 240, Pedido.ORIGEN_APP,
                "Boda Cliente 2", "cli-2", "Cliente Dos", "7472222222", "Av. Central 456", "", null);
        AtomicReference<Pedido> p2Ref = new AtomicReference<>();
        pedidoRepo.crearReserva(sol2, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { p2Ref.set(result); }
            @Override public void onError(String error) { fail("Error sol2: " + error); }
        });
        assertNotNull(p2Ref.get());

        // Con ORIGEN_PRESENCIAL del mismo cliente cli-1 -> permitido (en horario disponible sin colisión de stock)
        long iniPres = ini + 24 * 3600_000L;
        SolicitudReserva solPresencial = new SolicitudReserva("paq-basico", iniPres, 240, Pedido.ORIGEN_PRESENCIAL,
                "Boda Presencial cli-1", "cli-1", "Cliente Uno", "7471111111", "Av. Central 123", "", 24);
        AtomicReference<Pedido> pPresRef = new AtomicReference<>();
        pedidoRepo.crearReserva(solPresencial, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pPresRef.set(result); }
            @Override public void onError(String error) { fail("Error presencial: " + error); }
        });
        assertNotNull(pPresRef.get());
    }

    // 11. Repositorio: tras vencer el apartado (avanza reloj > 30 min y liberarApartadosVencidos),
    //     el mismo cliente puede reservar de nuevo; y tras pagar anticipo (CONFIRMADO) también.
    @Test
    public void test11_repositorio_apartadoVencidoOPagado_permiteNuevaReserva() {
        long ini = ahoraFijo + 72 * 3600_000L;
        SolicitudReserva sol1 = new SolicitudReserva("paq-basico", ini, 240, Pedido.ORIGEN_APP,
                "Evento 1", "cli-vence", "Cliente Vence", "7471111111", "Av. Central 123", "", null);

        AtomicReference<Pedido> p1Ref = new AtomicReference<>();
        pedidoRepo.crearReserva(sol1, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { p1Ref.set(result); }
            @Override public void onError(String error) { fail("Error sol1: " + error); }
        });

        // Avanzar reloj 35 minutos y liberar apartados
        reloj.avanzarMinutos(35);
        pedidoRepo.liberarApartadosVencidos();
        assertEquals(Pedido.ESTADO_VENCIDO, pedidoRepo.getPedidoPorId(p1Ref.get().getId()).getEstado());

        // Ahora el mismo cliente puede reservar de nuevo
        SolicitudReserva sol2 = new SolicitudReserva("paq-basico", ini, 240, Pedido.ORIGEN_APP,
                "Evento 2 Reintento", "cli-vence", "Cliente Vence", "7471111111", "Av. Central 123", "", null);
        AtomicReference<Pedido> p2Ref = new AtomicReference<>();
        pedidoRepo.crearReserva(sol2, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { p2Ref.set(result); }
            @Override public void onError(String error) { fail("Error sol2: " + error); }
        });
        assertNotNull(p2Ref.get());

        // Pagamos el anticipo de este segundo pedido
        pedidoRepo.registrarAnticipo(p2Ref.get().getId(), p2Ref.get().getAnticipoRequerido(), Pedido.METODO_TARJETA, noop());
        assertEquals(Pedido.ESTADO_CONFIRMADO, pedidoRepo.getPedidoPorId(p2Ref.get().getId()).getEstado());

        // Ahora que está CONFIRMADO, el mismo cliente puede volver a reservar
        SolicitudReserva sol3 = new SolicitudReserva("paq-basico", ini + 24 * 3600_000L, 240, Pedido.ORIGEN_APP,
                "Evento 3 Posterior", "cli-vence", "Cliente Vence", "7471111111", "Av. Central 123", "", null);
        AtomicReference<Pedido> p3Ref = new AtomicReference<>();
        pedidoRepo.crearReserva(sol3, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { p3Ref.set(result); }
            @Override public void onError(String error) { fail("Error sol3: " + error); }
        });
        assertNotNull(p3Ref.get());
    }

    // 12. Flujo completo: crearReserva (app, evento > 48 h) -> registrarAnticipo(anticipoRequerido, METODO_TARJETA)
    //     -> CONFIRMADO, Pago ANTICIPO con TARJETA, Evento con unidades asignadas. Mismo flujo < 48 h -> EN_REVISION.
    @Test
    public void test12_flujoCompleto_anticipoTarjeta_confirmado_y_enRevision() {
        // Caso A: Evento a más de 48 h (72 h)
        long iniNormal = ahoraFijo + 72 * 3600_000L;
        SolicitudReserva solNormal = new SolicitudReserva("paq-basico", iniNormal, 240, Pedido.ORIGEN_APP,
                "Evento Normal", "cli-flujo-1", "Cliente Normal", "7471111111", "Calle Las Flores 10", "", null);

        AtomicReference<Pedido> pedNormRef = new AtomicReference<>();
        pedidoRepo.crearReserva(solNormal, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pedNormRef.set(result); }
            @Override public void onError(String error) { fail("Error solNormal: " + error); }
        });

        Pedido pNorm = pedNormRef.get();
        assertNotNull(pNorm);
        assertEquals(Pedido.ESTADO_APARTADO, pNorm.getEstado());
        assertFalse(pNorm.isRequiereRevision());

        AtomicReference<Pedido> pNormPagado = new AtomicReference<>();
        pedidoRepo.registrarAnticipo(pNorm.getId(), pNorm.getAnticipoRequerido(), Pedido.METODO_TARJETA, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pNormPagado.set(result); }
            @Override public void onError(String error) { fail("Error anticipo: " + error); }
        });

        assertEquals(Pedido.ESTADO_CONFIRMADO, pNormPagado.get().getEstado());
        assertEquals(Pedido.METODO_TARJETA, pNormPagado.get().getMetodoAnticipo());
        assertEquals(pNorm.getAnticipoRequerido(), pNormPagado.get().getAnticipoPagado(), 0.001);
        assertNotNull(pNormPagado.get().getEventoId());
        assertFalse(pNormPagado.get().getEquipoIds().isEmpty());

        // Verificamos el Pago en pagoRepo
        List<Pago> pagos = pagoRepo.getPagosPorPedido(pNorm.getId()).getValue();
        assertEquals(1, pagos.size());
        Pago pagoAnt = pagos.get(0);
        assertEquals(Pago.TIPO_ANTICIPO, pagoAnt.getTipo());
        assertEquals(Pedido.METODO_TARJETA, pagoAnt.getMetodo());
        assertEquals(pNorm.getAnticipoRequerido(), pagoAnt.getMonto(), 0.001);

        // Caso B: Evento a menos de 48 h (24 h) -> EN_REVISION
        long iniUrgente = ahoraFijo + 24 * 3600_000L;
        SolicitudReserva solUrgente = new SolicitudReserva("paq-basico", iniUrgente, 240, Pedido.ORIGEN_APP,
                "Evento Urgente", "cli-flujo-2", "Cliente Urgente", "7472222222", "Calle Morelos 20", "", null);

        AtomicReference<Pedido> pedUrgRef = new AtomicReference<>();
        pedidoRepo.crearReserva(solUrgente, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pedUrgRef.set(result); }
            @Override public void onError(String error) { fail("Error solUrgente: " + error); }
        });

        Pedido pUrg = pedUrgRef.get();
        assertNotNull(pUrg);
        assertTrue(pUrg.isRequiereRevision());

        AtomicReference<Pedido> pUrgPagado = new AtomicReference<>();
        pedidoRepo.registrarAnticipo(pUrg.getId(), pUrg.getAnticipoRequerido(), Pedido.METODO_TARJETA, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pUrgPagado.set(result); }
            @Override public void onError(String error) { fail("Error anticipo urgente: " + error); }
        });

        assertEquals(Pedido.ESTADO_EN_REVISION, pUrgPagado.get().getEstado());
        assertEquals(Pedido.METODO_TARJETA, pUrgPagado.get().getMetodoAnticipo());
    }
}
