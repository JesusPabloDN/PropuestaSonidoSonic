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
import com.example.propuestasonidosonic.domain.DashboardUtils;
import com.example.propuestasonidosonic.domain.FinanzasUtils;
import com.example.propuestasonidosonic.domain.MoneyUtils;
import com.example.propuestasonidosonic.domain.ReciboUtils;
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
    private InMemoryPagoRepository pagoRepo;
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

    private Pedido crearYConfirmarPedido(double anticipo) {
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
        sol.setHorasApartado(48);

        AtomicReference<Pedido> ref = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String error) {}
        });

        Pedido p = ref.get();
        assertNotNull(p);

        AtomicReference<Pedido> refConf = new AtomicReference<>();
        pedidoRepo.registrarAnticipo(p.getId(), anticipo, Pedido.METODO_EFECTIVO, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { refConf.set(result); }
            @Override public void onError(String error) {}
        });

        Pedido conf = refConf.get();
        assertNotNull(conf);
        return conf;
    }

    /**
     * 1. liquidarSaldo parcial -> TIPO_ABONO, saldo baja, cobrado == false.
     */
    @Test
    public void test01_liquidarSaldoParcial() {
        Pedido p = crearYConfirmarPedido(300.0); // total 1000, anticipo 300, saldo 700
        assertEquals(700.0, p.getSaldoPendiente(), 0.001);
        assertFalse(p.isCobrado());

        AtomicReference<Pago> refPago = new AtomicReference<>();
        pedidoRepo.liquidarSaldo(p.getId(), 250.0, Pedido.METODO_EFECTIVO, new Callback<Pago>() {
            @Override public void onSuccess(Pago result) { refPago.set(result); }
            @Override public void onError(String error) {}
        });

        Pago pago = refPago.get();
        assertNotNull(pago);
        assertEquals(Pago.TIPO_ABONO, pago.getTipo());
        assertEquals(250.0, pago.getMonto(), 0.001);

        Pedido actualizado = pedidoRepo.getPedidoPorId(p.getId());
        assertEquals(550.0, actualizado.getAnticipoPagado(), 0.001);
        assertEquals(450.0, actualizado.getSaldoPendiente(), 0.001);
        assertFalse(actualizado.isCobrado());
        assertFalse(actualizado.estaLiquidado());
        assertEquals(Pedido.ESTADO_CONFIRMADO, actualizado.getEstado());
    }

    /**
     * 2. liquidarSaldo por el saldo exacto -> TIPO_TOTAL, saldo 0, cobrado == true, fechaCobro correcta, estado no cambia.
     */
    @Test
    public void test02_liquidarSaldoTotalExacto() {
        Pedido p = crearYConfirmarPedido(300.0);
        assertEquals(700.0, p.getSaldoPendiente(), 0.001);

        long fechaPago = ahoraFijo + 10_000L;
        reloj.setAhora(fechaPago);

        AtomicReference<Pago> refPago = new AtomicReference<>();
        pedidoRepo.liquidarSaldo(p.getId(), 700.0, Pedido.METODO_TRANSFERENCIA, new Callback<Pago>() {
            @Override public void onSuccess(Pago result) { refPago.set(result); }
            @Override public void onError(String error) {}
        });

        Pago pago = refPago.get();
        assertNotNull(pago);
        assertEquals(Pago.TIPO_TOTAL, pago.getTipo());
        assertEquals(700.0, pago.getMonto(), 0.001);

        Pedido actualizado = pedidoRepo.getPedidoPorId(p.getId());
        assertEquals(1000.0, actualizado.getAnticipoPagado(), 0.001);
        assertEquals(0.0, actualizado.getSaldoPendiente(), 0.001);
        assertTrue(actualizado.isCobrado());
        assertTrue(actualizado.estaLiquidado());
        assertEquals(fechaPago, actualizado.getFechaCobro());
        assertEquals(Pedido.ESTADO_CONFIRMADO, actualizado.getEstado());
    }

    /**
     * 3. Validaciones: monto 0 o negativo, mayor al saldo, método inválido, estados no permitidos, saldo ya en 0.
     */
    @Test
    public void test03_validacionesLiquidarSaldo() {
        Pedido p = crearYConfirmarPedido(300.0); // saldo 700

        // Monto 0
        AtomicReference<String> error = new AtomicReference<>();
        pedidoRepo.liquidarSaldo(p.getId(), 0.0, Pedido.METODO_EFECTIVO, new Callback<Pago>() {
            @Override public void onSuccess(Pago result) {}
            @Override public void onError(String err) { error.set(err); }
        });
        assertNotNull(error.get());

        // Monto negativo
        error.set(null);
        pedidoRepo.liquidarSaldo(p.getId(), -50.0, Pedido.METODO_EFECTIVO, new Callback<Pago>() {
            @Override public void onSuccess(Pago result) {}
            @Override public void onError(String err) { error.set(err); }
        });
        assertNotNull(error.get());

        // Monto mayor al saldo
        error.set(null);
        pedidoRepo.liquidarSaldo(p.getId(), 750.0, Pedido.METODO_EFECTIVO, new Callback<Pago>() {
            @Override public void onSuccess(Pago result) {}
            @Override public void onError(String err) { error.set(err); }
        });
        assertNotNull(error.get());

        // Método inválido
        error.set(null);
        pedidoRepo.liquidarSaldo(p.getId(), 100.0, "BITCOIN", new Callback<Pago>() {
            @Override public void onSuccess(Pago result) {}
            @Override public void onError(String err) { error.set(err); }
        });
        assertNotNull(error.get());

        // Liquidar todo el saldo
        pedidoRepo.liquidarSaldo(p.getId(), 700.0, Pedido.METODO_EFECTIVO, noop());

        // Saldo ya en 0
        error.set(null);
        pedidoRepo.liquidarSaldo(p.getId(), 50.0, Pedido.METODO_EFECTIVO, new Callback<Pago>() {
            @Override public void onSuccess(Pago result) {}
            @Override public void onError(String err) { error.set(err); }
        });
        assertNotNull(error.get());
    }

    /**
     * 4. Un pedido COMPLETADO con saldo sí puede liquidarse.
     */
    @Test
    public void test04_pedidoCompletadoPuedeLiquidarSaldo() {
        Pedido p = crearYConfirmarPedido(300.0); // saldo 700

        // Avanzar el reloj para que el evento termine y pase a COMPLETADO
        reloj.avanzarDias(2);
        pedidoRepo.liberarApartadosVencidos();

        Pedido comp = pedidoRepo.getPedidoPorId(p.getId());
        assertEquals(Pedido.ESTADO_COMPLETADO, comp.getEstado());
        assertEquals(700.0, comp.getSaldoPendiente(), 0.001);

        AtomicReference<Pago> refPago = new AtomicReference<>();
        pedidoRepo.liquidarSaldo(comp.getId(), 700.0, Pedido.METODO_EFECTIVO, new Callback<Pago>() {
            @Override public void onSuccess(Pago result) { refPago.set(result); }
            @Override public void onError(String error) {}
        });

        assertNotNull(refPago.get());
        Pedido liquidado = pedidoRepo.getPedidoPorId(p.getId());
        assertTrue(liquidado.estaLiquidado());
        assertTrue(liquidado.isCobrado());
        assertEquals(Pedido.ESTADO_COMPLETADO, liquidado.getEstado());
    }

    /**
     * 5. Dos pagos consecutivos reciben folios R-0001 y R-0002, únicos.
     */
    @Test
    public void test05_foliosConsecutivosUnicos() {
        AtomicReference<Pago> ref1 = new AtomicReference<>();
        AtomicReference<Pago> ref2 = new AtomicReference<>();

        Pago p1 = new Pago("pg-1", "ped-1", "c-1", 100.0, Pago.TIPO_ANTICIPO, Pago.METODO_EFECTIVO, ahoraFijo);
        pagoRepo.registrarPago(p1, new Callback<Pago>() {
            @Override public void onSuccess(Pago result) { ref1.set(result); }
            @Override public void onError(String error) {}
        });

        Pago p2 = new Pago("pg-2", "ped-1", "c-1", 200.0, Pago.TIPO_ABONO, Pago.METODO_TRANSFERENCIA, ahoraFijo);
        pagoRepo.registrarPago(p2, new Callback<Pago>() {
            @Override public void onSuccess(Pago result) { ref2.set(result); }
            @Override public void onError(String error) {}
        });

        assertNotNull(ref1.get());
        assertNotNull(ref2.get());
        assertEquals("R-0001", ref1.get().getFolio());
        assertEquals("R-0002", ref2.get().getFolio());
        assertNotEquals(ref1.get().getFolio(), ref2.get().getFolio());
    }

    /**
     * 6. registrarAnticipo con el 100% del total deja cobrado == true sin cambiar el estado.
     */
    @Test
    public void test06_registrarAnticipo100PorCiento() {
        long inicioEvento = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 16 * 60);
        SolicitudReserva sol = new SolicitudReserva();
        sol.setPaqueteId(paqueteBasico.getId());
        sol.setInicioMillis(inicioEvento);
        sol.setDuracionMin(240);
        sol.setNombreEvento("Boda 100%");
        sol.setClienteNombre("Beatriz");
        sol.setClienteTelefono("9611234455");
        sol.setDireccion("Calle 5 de Mayo #4");
        sol.setOrigen(Pedido.ORIGEN_LLAMADA);

        AtomicReference<Pedido> ref = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String error) {}
        });
        Pedido p = ref.get();
        assertNotNull(p);

        pedidoRepo.registrarAnticipo(p.getId(), 1000.0, Pedido.METODO_TARJETA, noop());

        Pedido conf = pedidoRepo.getPedidoPorId(p.getId());
        assertEquals(Pedido.ESTADO_CONFIRMADO, conf.getEstado());
        assertTrue(conf.isCobrado());
        assertTrue(conf.estaLiquidado());
        assertEquals(0.0, conf.getSaldoPendiente(), 0.001);
        assertEquals(ahoraFijo, conf.getFechaCobro());
    }

    /**
     * 7. Redondeo: montos como 1/3 del total o 0.1 + 0.2 no dejan saldos tipo 0.0000001; saldo termina en 0.
     */
    @Test
    public void test07_redondeoSinErroresDePuntoFlotante() {
        assertEquals(0.3, MoneyUtils.redondear2(0.1 + 0.2), 0.0001);
        assertEquals(0.33, MoneyUtils.redondear2(1.0 / 3.0), 0.0001);

        Pedido p = crearYConfirmarPedido(333.33); // total 1000.0, anticipo 333.33 -> saldo 666.67
        assertEquals(666.67, p.getSaldoPendiente(), 0.001);

        pedidoRepo.liquidarSaldo(p.getId(), 333.33, Pedido.METODO_EFECTIVO, noop());
        Pedido p2 = pedidoRepo.getPedidoPorId(p.getId());
        assertEquals(333.34, p2.getSaldoPendiente(), 0.001);

        pedidoRepo.liquidarSaldo(p.getId(), 333.34, Pedido.METODO_EFECTIVO, noop());
        Pedido p3 = pedidoRepo.getPedidoPorId(p.getId());
        assertEquals(0.0, p3.getSaldoPendiente(), 0.0);
        assertTrue(p3.estaLiquidado());
    }

    /**
     * 8. FinanzasUtils.resumenMes: 1 de octubre 00:00 entra; 31 de octubre 23:59 entra; 1 de noviembre 00:00 no entra.
     */
    @Test
    public void test08_resumenMesLimitesMensuales() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        cal.clear();
        cal.set(2026, Calendar.OCTOBER, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long t1Oct00 = cal.getTimeInMillis();

        cal.clear();
        cal.set(2026, Calendar.OCTOBER, 31, 23, 59, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long t31Oct2359 = cal.getTimeInMillis();

        cal.clear();
        cal.set(2026, Calendar.NOVEMBER, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long t1Nov00 = cal.getTimeInMillis();

        List<Pago> pagos = Arrays.asList(
                new Pago("p1", "ped1", "c1", 100.0, Pago.TIPO_ANTICIPO, "EFECTIVO", t1Oct00),
                new Pago("p2", "ped2", "c2", 200.0, Pago.TIPO_ABONO, "EFECTIVO", t31Oct2359),
                new Pago("p3", "ped3", "c3", 400.0, Pago.TIPO_TOTAL, "EFECTIVO", t1Nov00)
        );

        double[] resumenOct = FinanzasUtils.resumenMes(pagos, Collections.emptyList(), 2026, Calendar.OCTOBER);
        assertEquals(300.0, resumenOct[0], 0.001); // 100 + 200
        assertEquals(0.0, resumenOct[1], 0.001);
        assertEquals(300.0, resumenOct[2], 0.001);

        double[] resumenNov = FinanzasUtils.resumenMes(pagos, Collections.emptyList(), 2026, Calendar.NOVEMBER);
        assertEquals(400.0, resumenNov[0], 0.001);
    }

    /**
     * 9. resumenMes.porCobrar excluye apartados, cancelados, rechazados, vencidos y pedidos de otros meses;
     *    incluye completados con saldo.
     */
    @Test
    public void test09_resumenMesPorCobrar() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        cal.clear();
        cal.set(2026, Calendar.OCTOBER, 15, 12, 0, 0);
        long fechaOct = cal.getTimeInMillis();

        cal.clear();
        cal.set(2026, Calendar.NOVEMBER, 15, 12, 0, 0);
        long fechaNov = cal.getTimeInMillis();

        Pedido pConf = new Pedido();
        pConf.setEstado(Pedido.ESTADO_CONFIRMADO);
        pConf.setInicioMillis(fechaOct);
        pConf.setTotal(1000.0);
        pConf.setAnticipoPagado(500.0); // saldo 500

        Pedido pComp = new Pedido();
        pComp.setEstado(Pedido.ESTADO_COMPLETADO);
        pComp.setInicioMillis(fechaOct);
        pComp.setTotal(1000.0);
        pComp.setAnticipoPagado(700.0); // saldo 300

        Pedido pApar = new Pedido();
        pApar.setEstado(Pedido.ESTADO_APARTADO);
        pApar.setInicioMillis(fechaOct);
        pApar.setTotal(1000.0);
        pApar.setAnticipoPagado(0.0);

        Pedido pCanc = new Pedido();
        pCanc.setEstado(Pedido.ESTADO_CANCELADO);
        pCanc.setInicioMillis(fechaOct);
        pCanc.setTotal(1000.0);
        pCanc.setAnticipoPagado(300.0);

        Pedido pRech = new Pedido();
        pRech.setEstado(Pedido.ESTADO_RECHAZADO);
        pRech.setInicioMillis(fechaOct);
        pRech.setTotal(1000.0);

        Pedido pVenc = new Pedido();
        pVenc.setEstado(Pedido.ESTADO_VENCIDO);
        pVenc.setInicioMillis(fechaOct);
        pVenc.setTotal(1000.0);

        Pedido pOtroMes = new Pedido();
        pOtroMes.setEstado(Pedido.ESTADO_CONFIRMADO);
        pOtroMes.setInicioMillis(fechaNov);
        pOtroMes.setTotal(1000.0);
        pOtroMes.setAnticipoPagado(200.0); // saldo 800

        List<Pedido> pedidos = Arrays.asList(pConf, pComp, pApar, pCanc, pRech, pVenc, pOtroMes);

        double[] resumen = FinanzasUtils.resumenMes(Collections.emptyList(), pedidos, 2026, Calendar.OCTOBER);
        // Debe sumar solo pConf (500) + pComp (300) = 800.0
        assertEquals(800.0, resumen[3], 0.001);
    }

    /**
     * 10. movimientosDelMes con cada filtro y orden descendente.
     */
    @Test
    public void test10_movimientosDelMesFiltroYOrden() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        cal.clear();
        cal.set(2026, Calendar.OCTOBER, 5, 10, 0, 0);
        long t1 = cal.getTimeInMillis();
        cal.set(2026, Calendar.OCTOBER, 10, 10, 0, 0);
        long t2 = cal.getTimeInMillis();

        Pago p1 = new Pago("1", "ped1", "c1", 100.0, Pago.TIPO_ANTICIPO, "EFECTIVO", t1);
        p1.setFolio("R-0001");
        Pago p2 = new Pago("2", "ped2", "c2", 50.0, Pago.TIPO_REEMBOLSO, "EFECTIVO", t2);
        p2.setFolio("R-0002");
        Pago p3 = new Pago("3", "ped3", "c3", 200.0, Pago.TIPO_ABONO, "TRANSFERENCIA", t2);
        p3.setFolio("R-0003");

        List<Pago> pagos = Arrays.asList(p1, p2, p3);

        // Filtro TODOS: t2 desc (p3 con R-0003 antes de p2 con R-0002), luego t1 (p1)
        List<Pago> todos = FinanzasUtils.movimientosDelMes(pagos, 2026, Calendar.OCTOBER, FinanzasUtils.FILTRO_TODOS);
        assertEquals(3, todos.size());
        assertEquals("3", todos.get(0).getId());
        assertEquals("2", todos.get(1).getId());
        assertEquals("1", todos.get(2).getId());

        // Filtro INGRESOS
        List<Pago> ingresos = FinanzasUtils.movimientosDelMes(pagos, 2026, Calendar.OCTOBER, FinanzasUtils.FILTRO_INGRESOS);
        assertEquals(2, ingresos.size());
        assertEquals("3", ingresos.get(0).getId());
        assertEquals("1", ingresos.get(1).getId());

        // Filtro REEMBOLSOS
        List<Pago> reembolsos = FinanzasUtils.movimientosDelMes(pagos, 2026, Calendar.OCTOBER, FinanzasUtils.FILTRO_REEMBOLSOS);
        assertEquals(1, reembolsos.size());
        assertEquals("2", reembolsos.get(0).getId());
    }

    /**
     * 11. ReciboUtils.saldoDespuesDe y totalPagadoHasta con anticipo, abono, total y reembolso.
     */
    @Test
    public void test11_reciboUtilsCalculos() {
        double totalPedido = 1000.0;
        Pago p1 = new Pago("p1", "ped", "c", 300.0, Pago.TIPO_ANTICIPO, "EFECTIVO", 1000L);
        Pago p2 = new Pago("p2", "ped", "c", 200.0, Pago.TIPO_ABONO, "EFECTIVO", 2000L);
        Pago p3 = new Pago("p3", "ped", "c", 500.0, Pago.TIPO_TOTAL, "EFECTIVO", 3000L);
        Pago p4 = new Pago("p4", "ped", "c", 1000.0, Pago.TIPO_REEMBOLSO, "EFECTIVO", 4000L);

        List<Pago> historial = Arrays.asList(p1, p2, p3, p4);

        // p1 (anticipo 300)
        assertEquals(300.0, ReciboUtils.totalPagadoHasta(historial, "p1"), 0.001);
        assertEquals(700.0, ReciboUtils.saldoDespuesDe(totalPedido, historial, "p1"), 0.001);

        // p2 (abono 200 -> neto 500)
        assertEquals(500.0, ReciboUtils.totalPagadoHasta(historial, "p2"), 0.001);
        assertEquals(500.0, ReciboUtils.saldoDespuesDe(totalPedido, historial, "p2"), 0.001);

        // p3 (total 500 -> neto 1000)
        assertEquals(1000.0, ReciboUtils.totalPagadoHasta(historial, "p3"), 0.001);
        assertEquals(0.0, ReciboUtils.saldoDespuesDe(totalPedido, historial, "p3"), 0.001);

        // p4 (reembolso 1000 -> neto 0)
        assertEquals(0.0, ReciboUtils.totalPagadoHasta(historial, "p4"), 0.001);
        assertEquals(1000.0, ReciboUtils.saldoDespuesDe(totalPedido, historial, "p4"), 0.001);

        // Títulos de recibos
        assertEquals("Recibo de anticipo", ReciboUtils.tituloRecibo(Pago.TIPO_ANTICIPO));
        assertEquals("Recibo de abono", ReciboUtils.tituloRecibo(Pago.TIPO_ABONO));
        assertEquals("Recibo de pago total", ReciboUtils.tituloRecibo(Pago.TIPO_TOTAL));
        assertEquals("Comprobante de reembolso", ReciboUtils.tituloRecibo(Pago.TIPO_REEMBOLSO));
    }

    /**
     * 12. Cancelar con el Socio un pedido liquidado reembolsa el 100% del total pagado y genera un Pago REEMBOLSO con folio.
     */
    @Test
    public void test12_cancelarPedidoLiquidadoReembolsa100PorCiento() {
        Pedido p = crearYConfirmarPedido(300.0);
        pedidoRepo.liquidarSaldo(p.getId(), 700.0, Pedido.METODO_EFECTIVO, noop());

        Pedido liquidado = pedidoRepo.getPedidoPorId(p.getId());
        assertTrue(liquidado.estaLiquidado());
        assertEquals(1000.0, liquidado.getAnticipoPagado(), 0.001);

        AtomicReference<Pedido> refCanc = new AtomicReference<>();
        pedidoRepo.cancelarReserva(liquidado.getId(), Pedido.CANCELA_SOCIO, "Imprevisto técnico", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { refCanc.set(result); }
            @Override public void onError(String error) {}
        });

        Pedido cancelado = refCanc.get();
        assertNotNull(cancelado);
        assertEquals(Pedido.ESTADO_CANCELADO, cancelado.getEstado());
        assertEquals(1000.0, cancelado.getReembolsoMonto(), 0.001);

        List<Pago> pagos = pagoRepo.getPagosPorPedido(liquidado.getId()).getValue();
        Pago pagoReembolso = null;
        for (Pago pg : pagos) {
            if (Pago.TIPO_REEMBOLSO.equals(pg.getTipo())) {
                pagoReembolso = pg;
                break;
            }
        }
        assertNotNull(pagoReembolso);
        assertEquals(1000.0, pagoReembolso.getMonto(), 0.001);
        assertNotNull(pagoReembolso.getFolio());
        assertTrue(pagoReembolso.getFolio().startsWith("R-"));
    }

    /**
     * 13. DashboardUtils.contadores con pedidos de hoy, de mañana, dentro de 8 días, apartados vigentes/vencidos y asignacionIncompleta.
     */
    @Test
    public void test13_dashboardContadores() {
        long hoy = ahoraFijo; // 20 nov 08:00
        long manana = TiempoUtils.construir(2026, Calendar.NOVEMBER, 21, 14 * 60);
        long en8Dias = TiempoUtils.construir(2026, Calendar.NOVEMBER, 28, 14 * 60);

        Pedido p1Hoy = new Pedido();
        p1Hoy.setEstado(Pedido.ESTADO_CONFIRMADO);
        p1Hoy.setInicioMillis(hoy);

        Pedido p2Manana = new Pedido();
        p2Manana.setEstado(Pedido.ESTADO_CONFIRMADO);
        p2Manana.setInicioMillis(manana);

        Pedido p3En8Dias = new Pedido();
        p3En8Dias.setEstado(Pedido.ESTADO_CONFIRMADO);
        p3En8Dias.setInicioMillis(en8Dias);

        Pedido p4ApartadoVigente = new Pedido();
        p4ApartadoVigente.setEstado(Pedido.ESTADO_APARTADO);
        p4ApartadoVigente.setVencimientoApartadoMillis(hoy + 3600_000L);

        Pedido p5ApartadoVencido = new Pedido();
        p5ApartadoVencido.setEstado(Pedido.ESTADO_APARTADO);
        p5ApartadoVencido.setVencimientoApartadoMillis(hoy - 3600_000L);

        Pedido p6AsigIncompleta = new Pedido();
        p6AsigIncompleta.setEstado(Pedido.ESTADO_CONFIRMADO);
        p6AsigIncompleta.setInicioMillis(en8Dias);
        p6AsigIncompleta.setAsignacionIncompleta(true);

        List<Pedido> lista = Arrays.asList(p1Hoy, p2Manana, p3En8Dias, p4ApartadoVigente, p5ApartadoVencido, p6AsigIncompleta);

        int[] c = DashboardUtils.contadores(lista, hoy);
        // [0] eventosHoy = 1 (p1Hoy)
        // [1] proximos7Dias = 1 (p2Manana)
        // [2] apartadosPorPagar = 1 (p4ApartadoVigente)
        // [3] asignacionIncompleta = 1 (p6AsigIncompleta)
        assertEquals(1, c[0]);
        assertEquals(1, c[1]);
        assertEquals(1, c[2]);
        assertEquals(1, c[3]);
    }
}
