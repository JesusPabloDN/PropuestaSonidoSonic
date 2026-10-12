package com.example.propuestasonidosonic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.DiaOperativoRepository;
import com.example.propuestasonidosonic.data.EquipoRepository;
import com.example.propuestasonidosonic.data.PaqueteRepository;
import com.example.propuestasonidosonic.data.UsuarioRepository;
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
import com.example.propuestasonidosonic.model.ModeloInventario;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Etapa4AMotorTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    // Fecha fija de referencia: viernes 20 de noviembre de 2026, 10:00
    private long ahoraFijo;
    // Sábado 5 de diciembre de 2026
    private int anioEvento = 2026;
    private int mesEvento0 = Calendar.DECEMBER; // mes base 0
    private int diaEvento = 5;

    @Before
    public void setUp() {
        ahoraFijo = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 10 * 60);
    }

    // --- Helpers de prueba ---
    private static class RelojFalso implements Reloj {
        long tiempo;
        RelojFalso(long tiempo) { this.tiempo = tiempo; }
        @Override public long ahora() { return tiempo; }
        void avanzar(long ms) { this.tiempo += ms; }
    }

    private Paquete crearPaquete(String id, String nombre, double precio, int duracionBaseHoras,
                                 int bufferMontajeMin, int bufferDesmontajeMin,
                                 int operadoresRequeridos, List<ComponentePaquete> componentes) {
        Paquete p = new Paquete();
        p.setId(id);
        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setPrecioHoraExtra(100.0);
        p.setDuracionBaseHoras(duracionBaseHoras);
        p.setBufferMontajeMin(bufferMontajeMin);
        p.setBufferDesmontajeMin(bufferDesmontajeMin);
        p.setOperadoresRequeridos(operadoresRequeridos);
        p.setActivo(true);
        p.setComponentes(componentes);
        return p;
    }

    private ModeloInventario crearModelo(String clave, String nombre, String categoria, int disponibles) {
        return new ModeloInventario(clave, nombre, categoria, disponibles, disponibles, 500.0);
    }

    private Usuario crearUsuario(String id, String nombre, String rol, boolean activo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setRol(rol);
        u.setCuentaActiva(activo);
        return u;
    }

    private Pedido crearReserva(String id, long inicio, int duracionMin, int bufferMontajeMin, int bufferDesmontajeMin,
                                List<ComponentePaquete> componentes, List<String> operadorIds,
                                String estado, long vencimiento) {
        Pedido r = new Pedido();
        r.setId(id);
        r.setInicioMillis(inicio);
        long fin = TiempoUtils.sumarMinutos(inicio, duracionMin);
        r.setFinMillis(fin);
        r.setBloqueoInicioMillis(inicio - ((long) bufferMontajeMin) * 60_000L);
        r.setBloqueoFinMillis(fin + ((long) bufferDesmontajeMin) * 60_000L);
        r.setComponentes(componentes);
        r.setOperadorIds(operadorIds);
        r.setEstado(estado);
        r.setVencimientoApartadoMillis(vencimiento);
        return r;
    }

    // =========================================================================
    // PRUEBAS DEL MOTOR (1 a 16)
    // =========================================================================

    @Test
    public void test01_horarioLibre_unSocio_viable() {
        ModeloInventario mod = crearModelo("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 2);
        ComponentePaquete comp = new ComponentePaquete("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Paquete pkg = crearPaquete("pkg1", "Básico", 1500.0, 4, 60, 60, 1, Collections.singletonList(comp));

        Usuario socio = crearUsuario("socio-1", "Socio 1", Usuario.ROL_SOCIO, true);
        ContextoDisponibilidad ctx = new ContextoDisponibilidad(ahoraFijo,
                Collections.singletonList(mod), Collections.<Pedido>emptyList(),
                Collections.singletonList(socio), Collections.<Usuario>emptyList(),
                Collections.<String, DiaOperativo>emptyMap());

        long inicio = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 12 * 60);
        ResultadoDisponibilidad res = MotorDisponibilidad.evaluar(ctx, pkg, inicio, 240, null);

        assertTrue("Debe ser viable", res.isViable());
        assertEquals("Debe asignar 1 operador", 1, res.getOperadorIds().size());
        assertEquals("socio-1", res.getOperadorIds().get(0));
    }

    @Test
    public void test02_buffersMontajeYDesmontaje() {
        // Reserva A de 19:00 a 22:00 (montaje 90, desmontaje 120 -> bloqueo 17:30 a 00:00)
        long iniA = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 19 * 60);
        ComponentePaquete comp = new ComponentePaquete("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Pedido resA = crearReserva("res-A", iniA, 180, 90, 120,
                Collections.singletonList(comp), Collections.singletonList("socio-1"),
                Pedido.ESTADO_CONFIRMADO, 0);

        ModeloInventario mod = crearModelo("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 10);
        Paquete pkgB = crearPaquete("pkgB", "Básico", 1500.0, 3, 90, 120, 1, Collections.singletonList(comp));
        Usuario socio = crearUsuario("socio-1", "Socio 1", Usuario.ROL_SOCIO, true);

        ContextoDisponibilidad ctx = new ContextoDisponibilidad(ahoraFijo,
                Collections.singletonList(mod), Collections.singletonList(resA),
                Collections.singletonList(socio), Collections.<Usuario>emptyList(),
                Collections.<String, DiaOperativo>emptyMap());

        // Evento B1: 10:00 a 13:00 (montaje 90, desmontaje 120 -> bloqueo 08:30 a 15:00). No toca 17:30-00:00 -> Viable
        long iniB1 = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 10 * 60);
        ResultadoDisponibilidad r1 = MotorDisponibilidad.evaluar(ctx, pkgBB(pkgB, 90, 120), iniB1, 180, null);
        assertTrue("B1 (10:00 a 13:00) debe ser viable", r1.isViable());

        // Evento B2: 14:00 a 16:00 (montaje 90, desmontaje 120 -> bloqueo 12:30 a 18:00). Toca 17:30-00:00 -> SIN_OPERADORES
        long iniB2 = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 14 * 60);
        ResultadoDisponibilidad r2 = MotorDisponibilidad.evaluar(ctx, pkgB, iniB2, 120, null);
        assertFalse("B2 debe fallar por operadores ocupados", r2.isViable());
        assertEquals(ResultadoDisponibilidad.SIN_OPERADORES, r2.getMotivoNoViable());
    }

    private Paquete pkgBB(Paquete base, int m, int d) {
        base.setBufferMontajeMin(m);
        base.setBufferDesmontajeMin(d);
        return base;
    }

    @Test
    public void test03_extremosQueSeTocanNoSolapan() {
        // Ventana A: termina exactamente a las 15:00
        long finBloqueoA = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 15 * 60);
        long iniBloqueoA = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 10 * 60);
        assertFalse("10:00-15:00 y 15:00-18:00 no deben solapar",
                TiempoUtils.solapan(iniBloqueoA, finBloqueoA, finBloqueoA, finBloqueoA + 3 * 3600_000L));

        ComponentePaquete comp = new ComponentePaquete("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Pedido resA = new Pedido();
        resA.setId("res-A");
        resA.setBloqueoInicioMillis(iniBloqueoA);
        resA.setBloqueoFinMillis(finBloqueoA);
        resA.setOperadorIds(Collections.singletonList("socio-1"));
        resA.setEstado(Pedido.ESTADO_CONFIRMADO);
        resA.setComponentes(Collections.singletonList(comp));

        // Nuevo paquete con buffer montaje 0 y duración 180 min que empieza a las 15:00
        Paquete pkg = crearPaquete("pkg", "P", 1000.0, 3, 0, 0, 1, Collections.singletonList(comp));
        ModeloInventario mod = crearModelo("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Usuario socio = crearUsuario("socio-1", "Socio 1", Usuario.ROL_SOCIO, true);

        ContextoDisponibilidad ctx = new ContextoDisponibilidad(ahoraFijo,
                Collections.singletonList(mod), Collections.singletonList(resA),
                Collections.singletonList(socio), Collections.<Usuario>emptyList(),
                Collections.<String, DiaOperativo>emptyMap());

        ResultadoDisponibilidad res = MotorDisponibilidad.evaluar(ctx, pkg, finBloqueoA, 180, null);
        assertTrue("Extremos que se tocan deben ser viables", res.isViable());
    }

    @Test
    public void test04_existencias_sinStock() {
        // 4 bocinas disponibles; A usa 3 en la misma ventana; 2 operadores libres
        ModeloInventario mod = crearModelo("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 4);
        ComponentePaquete comp3 = new ComponentePaquete("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 3);
        ComponentePaquete comp2 = new ComponentePaquete("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 2);
        ComponentePaquete comp1 = new ComponentePaquete("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 1);

        long ini = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 12 * 60);
        Pedido resA = crearReserva("res-A", ini, 240, 60, 60,
                Collections.singletonList(comp3), Collections.singletonList("socio-1"),
                Pedido.ESTADO_CONFIRMADO, 0);

        List<Usuario> socios = new ArrayList<>();
        socios.add(crearUsuario("socio-1", "Socio 1", Usuario.ROL_SOCIO, true));
        socios.add(crearUsuario("socio-2", "Socio 2", Usuario.ROL_SOCIO, true));

        ContextoDisponibilidad ctx = new ContextoDisponibilidad(ahoraFijo,
                Collections.singletonList(mod), Collections.singletonList(resA),
                socios, Collections.<Usuario>emptyList(),
                Collections.<String, DiaOperativo>emptyMap());

        // B pide 2 -> SIN_STOCK (3 + 2 = 5 > 4)
        Paquete pkgB2 = crearPaquete("pkgB2", "P2", 2000.0, 4, 60, 60, 1, Collections.singletonList(comp2));
        ResultadoDisponibilidad rFalla = MotorDisponibilidad.evaluar(ctx, pkgB2, ini, 240, null);
        assertFalse(rFalla.isViable());
        assertEquals(ResultadoDisponibilidad.SIN_STOCK, rFalla.getMotivoNoViable());
        assertTrue(rFalla.getDetalle().contains("Bocina 15"));

        // B pide 1 -> viable (3 + 1 = 4 <= 4)
        Paquete pkgB1 = crearPaquete("pkgB1", "P1", 1000.0, 4, 60, 60, 1, Collections.singletonList(comp1));
        ResultadoDisponibilidad rOk = MotorDisponibilidad.evaluar(ctx, pkgB1, ini, 240, null);
        assertTrue("3 + 1 <= 4 debe ser viable", rOk.isViable());
    }

    @Test
    public void test05_picoDeUsoNoSuma() {
        // Capacidad 4. A usa 3 de 08:00 a 12:00. C usa 3 de 18:00 a 22:00.
        ModeloInventario mod = crearModelo("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 4);
        ComponentePaquete comp3 = new ComponentePaquete("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 3);
        ComponentePaquete comp1 = new ComponentePaquete("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        ComponentePaquete comp2 = new ComponentePaquete("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 2);

        long iniA = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 8 * 60);
        Pedido resA = crearReserva("res-A", iniA, 240, 0, 0,
                Collections.singletonList(comp3), Collections.singletonList("socio-1"),
                Pedido.ESTADO_CONFIRMADO, 0);

        long iniC = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 18 * 60);
        Pedido resC = crearReserva("res-C", iniC, 240, 0, 0,
                Collections.singletonList(comp3), Collections.singletonList("socio-2"),
                Pedido.ESTADO_CONFIRMADO, 0);

        List<Pedido> reservas = new ArrayList<>();
        reservas.add(resA);
        reservas.add(resC);

        List<Usuario> socios = new ArrayList<>();
        socios.add(crearUsuario("socio-1", "S1", Usuario.ROL_SOCIO, true));
        socios.add(crearUsuario("socio-2", "S2", Usuario.ROL_SOCIO, true));
        socios.add(crearUsuario("socio-3", "S3", Usuario.ROL_SOCIO, true));

        ContextoDisponibilidad ctx = new ContextoDisponibilidad(ahoraFijo,
                Collections.singletonList(mod), reservas, socios,
                Collections.<Usuario>emptyList(), Collections.<String, DiaOperativo>emptyMap());

        // Solicitud de 09:00 a 21:00 (duración 720 min): abarca A y C.
        long iniB = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 9 * 60);

        // Con 1 bocina: pico en cualquier momento es 3 + 1 = 4 <= 4 -> VIABLE
        Paquete pkg1 = crearPaquete("pkg1", "P1", 2000.0, 12, 0, 0, 1, Collections.singletonList(comp1));
        ResultadoDisponibilidad r1 = MotorDisponibilidad.evaluar(ctx, pkg1, iniB, 720, null);
        assertTrue("Pico 3+1 <= 4 debe ser viable", r1.isViable());

        // Con 2 bocinas: pico en el intervalo de A (o C) es 3 + 2 = 5 > 4 -> SIN_STOCK
        Paquete pkg2 = crearPaquete("pkg2", "P2", 2500.0, 12, 0, 0, 1, Collections.singletonList(comp2));
        ResultadoDisponibilidad r2 = MotorDisponibilidad.evaluar(ctx, pkg2, iniB, 720, null);
        assertFalse("Pico 3+2 > 4 debe fallar", r2.isViable());
        assertEquals(ResultadoDisponibilidad.SIN_STOCK, r2.getMotivoNoViable());
    }

    @Test
    public void test06_operadores_preferenciaYAsignacion() {
        // Con 2 Socios: dos reservas simultáneas viables; tercera da SIN_OPERADORES;
        // con un Asistente disponible ese día, la tercera es viable y se le asigna a él.
        ModeloInventario mod = crearModelo("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 20);
        ComponentePaquete comp = new ComponentePaquete("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Paquete pkg = crearPaquete("pkg", "P", 1000.0, 4, 0, 0, 1, Collections.singletonList(comp));

        long ini = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 12 * 60);
        Pedido r1 = crearReserva("r1", ini, 240, 0, 0, Collections.singletonList(comp),
                Collections.singletonList("socio-1"), Pedido.ESTADO_CONFIRMADO, 0);
        Pedido r2 = crearReserva("r2", ini, 240, 0, 0, Collections.singletonList(comp),
                Collections.singletonList("socio-2"), Pedido.ESTADO_CONFIRMADO, 0);

        List<Pedido> reservas = new ArrayList<>();
        reservas.add(r1);
        reservas.add(r2);

        List<Usuario> socios = new ArrayList<>();
        socios.add(crearUsuario("socio-1", "S1", Usuario.ROL_SOCIO, true));
        socios.add(crearUsuario("socio-2", "S2", Usuario.ROL_SOCIO, true));

        ContextoDisponibilidad ctxSinAsistente = new ContextoDisponibilidad(ahoraFijo,
                Collections.singletonList(mod), reservas, socios,
                Collections.<Usuario>emptyList(), Collections.<String, DiaOperativo>emptyMap());

        ResultadoDisponibilidad resFalla = MotorDisponibilidad.evaluar(ctxSinAsistente, pkg, ini, 240, null);
        assertFalse(resFalla.isViable());
        assertEquals(ResultadoDisponibilidad.SIN_OPERADORES, resFalla.getMotivoNoViable());

        // Ahora agregar Asistente y marcarlo disponible ese día
        Usuario asist = crearUsuario("asist-1", "Asistente 1", Usuario.ROL_ASISTENTE, true);
        String fechaClave = TiempoUtils.claveDia(ini);
        DiaOperativo diaOp = new DiaOperativo(fechaClave, Collections.singletonList("asist-1"), Collections.<String>emptyList());
        Map<String, DiaOperativo> diasMap = new HashMap<>();
        diasMap.put(fechaClave, diaOp);

        ContextoDisponibilidad ctxConAsistente = new ContextoDisponibilidad(ahoraFijo,
                Collections.singletonList(mod), reservas, socios,
                Collections.singletonList(asist), diasMap);

        ResultadoDisponibilidad resOk = MotorDisponibilidad.evaluar(ctxConAsistente, pkg, ini, 240, null);
        assertTrue(resOk.isViable());
        assertEquals(1, resOk.getOperadorIds().size());
        assertEquals("asist-1", resOk.getOperadorIds().get(0));
    }

    @Test
    public void test07_dosOperadoresRequeridos() {
        ModeloInventario mod = crearModelo("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 10);
        ComponentePaquete comp = new ComponentePaquete("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Paquete pkg2Op = crearPaquete("pkg", "P Grande", 3000.0, 4, 0, 0, 2, Collections.singletonList(comp));

        long ini = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 12 * 60);

        // Caso con solo 1 operador disponible
        List<Usuario> unSocio = Collections.singletonList(crearUsuario("socio-1", "S1", Usuario.ROL_SOCIO, true));
        ContextoDisponibilidad ctx1 = new ContextoDisponibilidad(ahoraFijo,
                Collections.singletonList(mod), Collections.<Pedido>emptyList(), unSocio,
                Collections.<Usuario>emptyList(), Collections.<String, DiaOperativo>emptyMap());
        ResultadoDisponibilidad r1 = MotorDisponibilidad.evaluar(ctx1, pkg2Op, ini, 240, null);
        assertFalse(r1.isViable());
        assertEquals(ResultadoDisponibilidad.SIN_OPERADORES, r1.getMotivoNoViable());

        // Caso con 2 operadores disponibles
        List<Usuario> dosSocios = new ArrayList<>(unSocio);
        dosSocios.add(crearUsuario("socio-2", "S2", Usuario.ROL_SOCIO, true));
        ContextoDisponibilidad ctx2 = new ContextoDisponibilidad(ahoraFijo,
                Collections.singletonList(mod), Collections.<Pedido>emptyList(), dosSocios,
                Collections.<Usuario>emptyList(), Collections.<String, DiaOperativo>emptyMap());
        ResultadoDisponibilidad r2 = MotorDisponibilidad.evaluar(ctx2, pkg2Op, ini, 240, null);
        assertTrue(r2.isViable());
        assertEquals(2, r2.getOperadorIds().size());
        assertTrue(r2.getOperadorIds().contains("socio-1"));
        assertTrue(r2.getOperadorIds().contains("socio-2"));
    }

    @Test
    public void test08_diaOperativo_optInYOptOut() {
        ModeloInventario mod = crearModelo("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 10);
        ComponentePaquete comp = new ComponentePaquete("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Paquete pkg = crearPaquete("pkg", "P", 1000.0, 4, 0, 0, 1, Collections.singletonList(comp));
        long ini = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 12 * 60);
        String fechaClave = TiempoUtils.claveDia(ini);

        Usuario socio1 = crearUsuario("socio-1", "S1", Usuario.ROL_SOCIO, true);
        Usuario asist1 = crearUsuario("asist-1", "A1", Usuario.ROL_ASISTENTE, true);

        // socio-1 está marcado no disponible (opt-out), y asist-1 NO está en asistentesDisponiblesIds (opt-in)
        DiaOperativo diaOp = new DiaOperativo(fechaClave, Collections.<String>emptyList(), Collections.singletonList("socio-1"));
        Map<String, DiaOperativo> diasMap = new HashMap<>();
        diasMap.put(fechaClave, diaOp);

        ContextoDisponibilidad ctx = new ContextoDisponibilidad(ahoraFijo,
                Collections.singletonList(mod), Collections.<Pedido>emptyList(),
                Collections.singletonList(socio1), Collections.singletonList(asist1), diasMap);

        ResultadoDisponibilidad res = MotorDisponibilidad.evaluar(ctx, pkg, ini, 240, null);
        assertFalse("Nadie está disponible", res.isViable());
        assertEquals(ResultadoDisponibilidad.SIN_OPERADORES, res.getMotivoNoViable());
    }

    @Test
    public void test09_bloqueoPorEstado() {
        Pedido p = new Pedido();

        // APARTADO vigente vs vencido
        p.setEstado(Pedido.ESTADO_APARTADO);
        p.setVencimientoApartadoMillis(ahoraFijo + 10_000L);
        assertTrue(MotorDisponibilidad.bloquea(p, ahoraFijo));

        p.setVencimientoApartadoMillis(ahoraFijo - 10_000L);
        assertFalse("Apartado vencido no bloquea", MotorDisponibilidad.bloquea(p, ahoraFijo));

        // PAGADO, CONFIRMADO, EN_REVISION bloquean
        p.setEstado(Pedido.ESTADO_PAGADO);
        assertTrue(MotorDisponibilidad.bloquea(p, ahoraFijo));
        p.setEstado(Pedido.ESTADO_CONFIRMADO);
        assertTrue(MotorDisponibilidad.bloquea(p, ahoraFijo));
        p.setEstado(Pedido.ESTADO_EN_REVISION);
        assertTrue(MotorDisponibilidad.bloquea(p, ahoraFijo));

        // CANCELADO, RECHAZADO, VENCIDO no bloquean
        p.setEstado(Pedido.ESTADO_CANCELADO);
        assertFalse(MotorDisponibilidad.bloquea(p, ahoraFijo));
        p.setEstado(Pedido.ESTADO_RECHAZADO);
        assertFalse(MotorDisponibilidad.bloquea(p, ahoraFijo));
        p.setEstado(Pedido.ESTADO_VENCIDO);
        assertFalse(MotorDisponibilidad.bloquea(p, ahoraFijo));
    }

    @Test
    public void test10_maximoSimultaneos() {
        // Con 4 operadores y 3 reservas simultáneas, la cuarta da MAXIMO_SIMULTANEOS
        ModeloInventario mod = crearModelo("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 50);
        ComponentePaquete comp = new ComponentePaquete("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Paquete pkg = crearPaquete("pkg", "P", 1000.0, 4, 0, 0, 1, Collections.singletonList(comp));
        long ini = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 12 * 60);

        List<Pedido> reservas = new ArrayList<>();
        reservas.add(crearReserva("r1", ini, 240, 0, 0, Collections.singletonList(comp), Collections.singletonList("s1"), Pedido.ESTADO_CONFIRMADO, 0));
        reservas.add(crearReserva("r2", ini, 240, 0, 0, Collections.singletonList(comp), Collections.singletonList("s2"), Pedido.ESTADO_CONFIRMADO, 0));
        reservas.add(crearReserva("r3", ini, 240, 0, 0, Collections.singletonList(comp), Collections.singletonList("s3"), Pedido.ESTADO_CONFIRMADO, 0));

        List<Usuario> socios = new ArrayList<>();
        socios.add(crearUsuario("s1", "S1", Usuario.ROL_SOCIO, true));
        socios.add(crearUsuario("s2", "S2", Usuario.ROL_SOCIO, true));
        socios.add(crearUsuario("s3", "S3", Usuario.ROL_SOCIO, true));
        socios.add(crearUsuario("s4", "S4", Usuario.ROL_SOCIO, true));

        ContextoDisponibilidad ctx = new ContextoDisponibilidad(ahoraFijo,
                Collections.singletonList(mod), reservas, socios,
                Collections.<Usuario>emptyList(), Collections.<String, DiaOperativo>emptyMap());

        ResultadoDisponibilidad res = MotorDisponibilidad.evaluar(ctx, pkg, ini, 240, null);
        assertFalse(res.isViable());
        assertEquals(ResultadoDisponibilidad.MAXIMO_SIMULTANEOS, res.getMotivoNoViable());
    }

    @Test
    public void test11_validacionesReglasBasicas() {
        ModeloInventario mod = crearModelo("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 10);
        ComponentePaquete comp = new ComponentePaquete("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Paquete pkg = crearPaquete("pkg", "P", 1000.0, 4, 0, 0, 1, Collections.singletonList(comp));
        Usuario socio = crearUsuario("s1", "S1", Usuario.ROL_SOCIO, true);
        ContextoDisponibilidad ctx = new ContextoDisponibilidad(ahoraFijo,
                Collections.singletonList(mod), Collections.<Pedido>emptyList(),
                Collections.singletonList(socio), Collections.<Usuario>emptyList(),
                Collections.<String, DiaOperativo>emptyMap());

        // 1. Paquete inactivo
        pkg.setActivo(false);
        assertEquals(ResultadoDisponibilidad.PAQUETE_INACTIVO,
                MotorDisponibilidad.evaluar(ctx, pkg, TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 12 * 60), 240, null).getMotivoNoViable());
        pkg.setActivo(true);

        // 2. Duración inválida (45 min)
        assertEquals(ResultadoDisponibilidad.DURACION_INVALIDA,
                MotorDisponibilidad.evaluar(ctx, pkg, TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 12 * 60), 45, null).getMotivoNoViable());

        // 3. Fecha en el pasado
        long pasado = ahoraFijo - 3600_000L;
        assertEquals(ResultadoDisponibilidad.EN_EL_PASADO,
                MotorDisponibilidad.evaluar(ctx, pkg, pasado, 240, null).getMotivoNoViable());

        // 4. Fuera de horario: 08:00 y 22:30
        long inicio08 = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 8 * 60);
        assertEquals(ResultadoDisponibilidad.FUERA_DE_HORARIO,
                MotorDisponibilidad.evaluar(ctx, pkg, inicio08, 240, null).getMotivoNoViable());

        long inicio2230 = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 22 * 60 + 30);
        assertEquals(ResultadoDisponibilidad.FUERA_DE_HORARIO,
                MotorDisponibilidad.evaluar(ctx, pkg, inicio2230, 240, null).getMotivoNoViable());

        // 5. Modelo inexistente -> PAQUETE_INCOMPLETO
        ComponentePaquete compInexistente = new ComponentePaquete("FANTASMA", "Fantasma", CategoriaEquipo.BOCINAS, 1);
        Paquete pkgIncompleto = crearPaquete("pkgInc", "Incompleto", 1000.0, 4, 0, 0, 1,
                Collections.singletonList(compInexistente));
        assertEquals(ResultadoDisponibilidad.PAQUETE_INCOMPLETO,
                MotorDisponibilidad.evaluar(ctx, pkgIncompleto, TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 12 * 60), 240, null).getMotivoNoViable());
    }

    @Test
    public void test12_eventoUrgenteMenosDe48Horas() {
        ModeloInventario mod = crearModelo("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 10);
        ComponentePaquete comp = new ComponentePaquete("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Paquete pkg = crearPaquete("pkg", "P", 1000.0, 4, 0, 0, 1, Collections.singletonList(comp));
        Usuario socio = crearUsuario("s1", "S1", Usuario.ROL_SOCIO, true);
        ContextoDisponibilidad ctx = new ContextoDisponibilidad(ahoraFijo,
                Collections.singletonList(mod), Collections.<Pedido>emptyList(),
                Collections.singletonList(socio), Collections.<Usuario>emptyList(),
                Collections.<String, DiaOperativo>emptyMap());

        // Evento mañana a las 12:00 (a 26 horas de ahoraFijo)
        long manana12 = ahoraFijo + 26 * 3600_000L;
        ResultadoDisponibilidad res = MotorDisponibilidad.evaluar(ctx, pkg, manana12, 240, null);

        assertTrue("Debe seguir siendo viable", res.isViable());
        assertTrue("Debe requerir revisión", res.isRequiereRevision());
        assertTrue("Debe incluir MOTIVO_URGENTE", res.getMotivosRevision().contains(Pedido.MOTIVO_URGENTE));
    }

    @Test
    public void test13_horariosDisponibles() {
        // Con la reserva A del caso 2 (19:00 a 22:00, bloqueo 17:30 a 00:00) y 1 solo operador:
        // no contiene horarios cuya ventana choque, sí contiene el primero libre (09:00),
        // todos son múltiplos de 30 min y entre 09:00 y 22:00.
        long iniA = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 19 * 60);
        ComponentePaquete comp = new ComponentePaquete("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Pedido resA = crearReserva("res-A", iniA, 180, 90, 120,
                Collections.singletonList(comp), Collections.singletonList("socio-1"),
                Pedido.ESTADO_CONFIRMADO, 0);

        ModeloInventario mod = crearModelo("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 10);
        Paquete pkg = crearPaquete("pkg", "P", 1500.0, 3, 90, 120, 1, Collections.singletonList(comp));
        Usuario socio = crearUsuario("socio-1", "S1", Usuario.ROL_SOCIO, true);

        ContextoDisponibilidad ctx = new ContextoDisponibilidad(ahoraFijo,
                Collections.singletonList(mod), Collections.singletonList(resA),
                Collections.singletonList(socio), Collections.<Usuario>emptyList(),
                Collections.<String, DiaOperativo>emptyMap());

        long unMomento = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 12 * 60);
        List<Long> horarios = MotorDisponibilidad.horariosDisponibles(ctx, pkg, unMomento, 180);

        assertFalse("Debe encontrar horarios disponibles", horarios.isEmpty());

        // Primer horario disponible debe ser 09:00
        long h09 = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 9 * 60);
        assertEquals(Long.valueOf(h09), horarios.get(0));

        // No debe contener 19:00 ni 16:00
        long h19 = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 19 * 60);
        long h16 = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 16 * 60);
        assertFalse(horarios.contains(h19));
        assertFalse(horarios.contains(h16));

        // Todos deben estar dentro del rango permitido y ser múltiplos de 30 min
        for (Long h : horarios) {
            int min = TiempoUtils.minutosDelDia(h);
            assertTrue(min >= Config.PRIMER_INICIO_MIN);
            assertTrue(min <= Config.ULTIMO_INICIO_MIN);
            assertEquals(0, min % Config.PASO_HORARIO_MIN);
        }
    }

    @Test
    public void test14_ignorarPedidoId() {
        ModeloInventario mod = crearModelo("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        ComponentePaquete comp = new ComponentePaquete("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Paquete pkg = crearPaquete("pkg", "P", 1000.0, 4, 0, 0, 1, Collections.singletonList(comp));
        long ini = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 12 * 60);

        Pedido resPropia = crearReserva("pedido-123", ini, 240, 0, 0,
                Collections.singletonList(comp), Collections.singletonList("socio-1"),
                Pedido.ESTADO_CONFIRMADO, 0);

        Usuario socio = crearUsuario("socio-1", "S1", Usuario.ROL_SOCIO, true);
        ContextoDisponibilidad ctx = new ContextoDisponibilidad(ahoraFijo,
                Collections.singletonList(mod), Collections.singletonList(resPropia),
                Collections.singletonList(socio), Collections.<Usuario>emptyList(),
                Collections.<String, DiaOperativo>emptyMap());

        // Sin ignorar -> choca consigo misma
        ResultadoDisponibilidad rChoca = MotorDisponibilidad.evaluar(ctx, pkg, ini, 240, null);
        assertFalse(rChoca.isViable());

        // Ignorando "pedido-123" -> viable
        ResultadoDisponibilidad rOk = MotorDisponibilidad.evaluar(ctx, pkg, ini, 240, "pedido-123");
        assertTrue("No debe chocar consigo misma", rOk.isViable());
    }

    @Test
    public void test15_calculoTotalYAnticipo() {
        Paquete pkg = new Paquete();
        pkg.setPrecio(3500.0);
        pkg.setDuracionBaseHoras(4);
        pkg.setPrecioHoraExtra(100.0);

        // 5.5 horas = 330 min (1.5 horas extra -> redondea hacia arriba a 2 horas extra -> 200)
        double total = MotorDisponibilidad.calcularTotal(pkg, 330);
        assertEquals(3700.0, total, 0.001);

        double anticipo = MotorDisponibilidad.calcularAnticipo(total);
        assertEquals(1110.00, anticipo, 0.001);
    }

    @Test
    public void test16_proximasAlternativas() {
        ModeloInventario mod = crearModelo("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 10);
        ComponentePaquete comp = new ComponentePaquete("BOCINA", "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Paquete pkg = crearPaquete("pkg", "P", 1000.0, 4, 0, 0, 1, Collections.singletonList(comp));
        Usuario socio = crearUsuario("s1", "S1", Usuario.ROL_SOCIO, true);

        ContextoDisponibilidad ctx = new ContextoDisponibilidad(ahoraFijo,
                Collections.singletonList(mod), Collections.<Pedido>emptyList(),
                Collections.singletonList(socio), Collections.<Usuario>emptyList(),
                Collections.<String, DiaOperativo>emptyMap());

        long desde = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 15 * 60);
        List<Long> alternativas = MotorDisponibilidad.proximasAlternativas(ctx, pkg, desde, 240, 5);

        assertEquals(5, alternativas.size());
        assertTrue(alternativas.get(0) >= desde);
        for (int i = 0; i < alternativas.size() - 1; i++) {
            assertTrue("Deben estar en orden ascendente", alternativas.get(i) < alternativas.get(i + 1));
        }
    }

    // =========================================================================
    // PRUEBAS DEL REPOSITORIO (17 a 21)
    // =========================================================================

    private static final Callback NOOP = new Callback() {
        @Override public void onSuccess(Object result) {}
        @Override public void onError(String mensaje) {}
    };

    @SuppressWarnings("unchecked")
    private static <T> Callback<T> noop() {
        return (Callback<T>) NOOP;
    }

    private InMemoryPedidoRepository armarRepoConReloj(RelojFalso reloj, Paquete paquete) {
        InMemoryEquipoRepository eqRepo = new InMemoryEquipoRepository();
        // Agregar equipo disponible
        eqRepo.agregarEquipo(new Equipo("eq-1", "Bocina 15", CategoriaEquipo.BOCINAS, 1, Equipo.ESTADO_DISPONIBLE, 500.0), noop());
        eqRepo.agregarEquipo(new Equipo("eq-2", "Bocina 15", CategoriaEquipo.BOCINAS, 2, Equipo.ESTADO_DISPONIBLE, 500.0), noop());

        InMemoryPaqueteRepository paqRepo = new InMemoryPaqueteRepository(eqRepo);
        paqRepo.guardarPaquete(paquete, noop());

        InMemoryUsuarioRepository usrRepo = new InMemoryUsuarioRepository();
        // usrRepo ya tiene un Socio semilla "socio@sonidosonic.com" con ROL_SOCIO

        InMemoryDiaOperativoRepository diaRepo = new InMemoryDiaOperativoRepository();

        return new InMemoryPedidoRepository(eqRepo, paqRepo, usrRepo, diaRepo, reloj);
    }

    @Test
    public void test17_repo_crearReservaValida() {
        RelojFalso reloj = new RelojFalso(ahoraFijo);
        ComponentePaquete comp = new ComponentePaquete(
                com.example.propuestasonidosonic.util.ModeloUtils.clave(CategoriaEquipo.BOCINAS, "Bocina 15"),
                "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Paquete pkg = crearPaquete("paq-1", "Paquete Test", 2000.0, 4, 30, 30, 1, Collections.singletonList(comp));
        InMemoryPedidoRepository repo = armarRepoConReloj(reloj, pkg);

        long ini = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 12 * 60);

        // Origen APP
        SolicitudReserva solApp = new SolicitudReserva("paq-1", ini, 240, Pedido.ORIGEN_APP,
                "Boda Carlos", "cli-1", "Carlos Gomez", "5551234567", "Av Insurgentes 1234", "Notas", null);

        AtomicReference<Pedido> refApp = new AtomicReference<>();
        repo.crearReserva(solApp, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { refApp.set(result); }
            @Override public void onError(String mensaje) {}
        });

        assertNotNull(refApp.get());
        Pedido pApp = refApp.get();
        assertEquals(Pedido.ESTADO_APARTADO, pApp.getEstado());
        assertEquals(ahoraFijo + Config.APARTADO_APP_MIN * 60_000L, pApp.getVencimientoApartadoMillis());
        assertEquals(1, pApp.getOperadorIds().size());
        assertEquals(2000.0, pApp.getTotal(), 0.001);
        assertEquals(600.0, pApp.getAnticipoRequerido(), 0.001);
        assertEquals(1001, pApp.getNumero());

        // Origen LLAMADA (para otro día para no chocar)
        long iniOtroDia = TiempoUtils.sumarMinutos(ini, 24 * 60);
        SolicitudReserva solLlamada = new SolicitudReserva("paq-1", iniOtroDia, 240, Pedido.ORIGEN_LLAMADA,
                "XV Ana", null, "Ana Lopez", "5559876543", "Calle Hidalgo 5678", "", null);

        AtomicReference<Pedido> refLlamada = new AtomicReference<>();
        repo.crearReserva(solLlamada, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { refLlamada.set(result); }
            @Override public void onError(String mensaje) {}
        });

        assertNotNull(refLlamada.get());
        Pedido pLlamada = refLlamada.get();
        assertEquals(ahoraFijo + Config.APARTADO_MANUAL_HORAS * 3600_000L, pLlamada.getVencimientoApartadoMillis());
        assertEquals(1002, pLlamada.getNumero());
    }

    @Test
    public void test18_repo_segundaReservaMismoHorarioFalla() {
        RelojFalso reloj = new RelojFalso(ahoraFijo);
        ComponentePaquete comp = new ComponentePaquete(
                com.example.propuestasonidosonic.util.ModeloUtils.clave(CategoriaEquipo.BOCINAS, "Bocina 15"),
                "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Paquete pkg = crearPaquete("paq-1", "Paquete Test", 2000.0, 4, 30, 30, 1, Collections.singletonList(comp));
        InMemoryPedidoRepository repo = armarRepoConReloj(reloj, pkg);

        long ini = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 12 * 60);
        SolicitudReserva sol1 = new SolicitudReserva("paq-1", ini, 240, Pedido.ORIGEN_APP,
                "Evento 1", "cli-1", "Cliente 1", "5551234567", "Direccion valida 123", "", null);

        AtomicBoolean ok1 = new AtomicBoolean(false);
        repo.crearReserva(sol1, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ok1.set(true); }
            @Override public void onError(String mensaje) {}
        });
        assertTrue("Primera reserva debe crearse", ok1.get());

        // Segunda reserva en el mismo horario con 1 solo operador disponible en el repo
        SolicitudReserva sol2 = new SolicitudReserva("paq-1", ini, 240, Pedido.ORIGEN_APP,
                "Evento 2", "cli-2", "Cliente 2", "5551234567", "Direccion valida 123", "", null);

        AtomicReference<String> error2 = new AtomicReference<>();
        repo.crearReserva(sol2, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { error2.set(mensaje); }
        });

        assertNotNull("Segunda reserva debe fallar", error2.get());
        assertEquals("Solo debe haber 1 pedido guardado", 1, repo.obtenerTodos().size());
    }

    @Test
    public void test19_repo_cancelarReservaLibera() {
        RelojFalso reloj = new RelojFalso(ahoraFijo);
        ComponentePaquete comp = new ComponentePaquete(
                com.example.propuestasonidosonic.util.ModeloUtils.clave(CategoriaEquipo.BOCINAS, "Bocina 15"),
                "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Paquete pkg = crearPaquete("paq-1", "Paquete Test", 2000.0, 4, 30, 30, 1, Collections.singletonList(comp));
        InMemoryPedidoRepository repo = armarRepoConReloj(reloj, pkg);

        long ini = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 12 * 60);
        SolicitudReserva sol1 = new SolicitudReserva("paq-1", ini, 240, Pedido.ORIGEN_APP,
                "Evento 1", "cli-1", "Cliente 1", "5551234567", "Direccion valida 123", "", null);

        AtomicReference<Pedido> ref1 = new AtomicReference<>();
        repo.crearReserva(sol1, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref1.set(result); }
            @Override public void onError(String mensaje) {}
        });

        assertNotNull(ref1.get());
        String id1 = ref1.get().getId();

        // Cancelar reserva 1
        AtomicBoolean cancelado = new AtomicBoolean(false);
        repo.cancelarReserva(id1, "Cambio de planes", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { cancelado.set(true); }
            @Override public void onError(String mensaje) {}
        });
        assertTrue(cancelado.get());
        assertEquals(Pedido.ESTADO_CANCELADO, repo.getPedidoPorId(id1).getEstado());

        // Ahora la segunda reserva en el mismo horario sí se puede crear
        SolicitudReserva sol2 = new SolicitudReserva("paq-1", ini, 240, Pedido.ORIGEN_APP,
                "Evento 2", "cli-2", "Cliente 2", "5551234567", "Direccion valida 123", "", null);
        AtomicReference<Pedido> ref2 = new AtomicReference<>();
        repo.crearReserva(sol2, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref2.set(result); }
            @Override public void onError(String mensaje) {}
        });

        assertNotNull("Segunda reserva ahora debe tener éxito", ref2.get());
        assertEquals(Pedido.ESTADO_APARTADO, ref2.get().getEstado());
    }

    @Test
    public void test20_repo_liberarApartadosVencidos() {
        RelojFalso reloj = new RelojFalso(ahoraFijo);
        ComponentePaquete comp = new ComponentePaquete(
                com.example.propuestasonidosonic.util.ModeloUtils.clave(CategoriaEquipo.BOCINAS, "Bocina 15"),
                "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Paquete pkg = crearPaquete("paq-1", "Paquete Test", 2000.0, 4, 30, 30, 1, Collections.singletonList(comp));
        InMemoryPedidoRepository repo = armarRepoConReloj(reloj, pkg);

        long ini = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 12 * 60);
        SolicitudReserva sol = new SolicitudReserva("paq-1", ini, 240, Pedido.ORIGEN_APP,
                "Evento 1", "cli-1", "Cliente 1", "5551234567", "Direccion valida 123", "", null);

        AtomicReference<Pedido> ref = new AtomicReference<>();
        repo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ref.set(result); }
            @Override public void onError(String mensaje) {}
        });

        assertNotNull(ref.get());
        String id = ref.get().getId();

        // Avanzar el reloj 31 minutos (más allá de APARTADO_APP_MIN = 30)
        reloj.avanzar(31 * 60_000L);

        int liberados = repo.liberarApartadosVencidos();
        assertEquals(1, liberados);
        assertEquals(Pedido.ESTADO_VENCIDO, repo.getPedidoPorId(id).getEstado());

        // Cancelar una reserva vencida debe dar error
        AtomicReference<String> errorCancel = new AtomicReference<>();
        repo.cancelarReserva(id, "Tarde", new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { errorCancel.set(mensaje); }
        });
        assertNotNull("No se debe permitir cancelar una reserva vencida", errorCancel.get());
        assertEquals("Esta reserva ya no se puede cancelar", errorCancel.get());
    }

    @Test
    public void test21_repo_validacionesSolicitud() {
        RelojFalso reloj = new RelojFalso(ahoraFijo);
        ComponentePaquete comp = new ComponentePaquete(
                com.example.propuestasonidosonic.util.ModeloUtils.clave(CategoriaEquipo.BOCINAS, "Bocina 15"),
                "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Paquete pkg = crearPaquete("paq-1", "Paquete Test", 2000.0, 4, 30, 30, 1, Collections.singletonList(comp));
        InMemoryPedidoRepository repo = armarRepoConReloj(reloj, pkg);
        long ini = TiempoUtils.construir(anioEvento, mesEvento0, diaEvento, 12 * 60);

        // 1. Teléfono corto
        SolicitudReserva solTelCorto = new SolicitudReserva("paq-1", ini, 240, Pedido.ORIGEN_WHATSAPP,
                "Evento", null, "Cliente", "12345", "Direccion valida 123", "", null);
        AtomicReference<String> errTel = new AtomicReference<>();
        repo.crearReserva(solTelCorto, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { errTel.set(mensaje); }
        });
        assertNotNull(errTel.get());
        assertTrue(errTel.get().contains("10 dígitos"));

        // 2. Dirección corta (< 10 caracteres)
        SolicitudReserva solDirCorta = new SolicitudReserva("paq-1", ini, 240, Pedido.ORIGEN_WHATSAPP,
                "Evento", null, "Cliente", "5551234567", "Corta", "", null);
        AtomicReference<String> errDir = new AtomicReference<>();
        repo.crearReserva(solDirCorta, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { errDir.set(mensaje); }
        });
        assertNotNull(errDir.get());
        assertTrue(errDir.get().contains("10 caracteres"));

        // 3. ORIGEN_APP sin clienteId
        SolicitudReserva solSinCliente = new SolicitudReserva("paq-1", ini, 240, Pedido.ORIGEN_APP,
                "Evento", null, "Cliente", "5551234567", "Direccion valida 123", "", null);
        AtomicReference<String> errCli = new AtomicReference<>();
        repo.crearReserva(solSinCliente, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { errCli.set(mensaje); }
        });
        assertNotNull(errCli.get());
        assertTrue(errCli.get().contains("cliente"));

        // 4. Nombre de evento vacío
        SolicitudReserva solSinNombreEv = new SolicitudReserva("paq-1", ini, 240, Pedido.ORIGEN_WHATSAPP,
                "", null, "Cliente", "5551234567", "Direccion valida 123", "", null);
        AtomicReference<String> errEv = new AtomicReference<>();
        repo.crearReserva(solSinNombreEv, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { errEv.set(mensaje); }
        });
        assertNotNull(errEv.get());
        assertTrue(errEv.get().contains("evento"));
    }
}
