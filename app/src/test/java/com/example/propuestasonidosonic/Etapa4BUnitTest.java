package com.example.propuestasonidosonic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.SessionManager;
import com.example.propuestasonidosonic.data.memory.InMemoryDiaOperativoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryEquipoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPaqueteRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPedidoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryUsuarioRepository;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Etapa4BUnitTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static class RelojFalso implements Reloj {
        long tiempo;
        RelojFalso(long tiempo) { this.tiempo = tiempo; }
        @Override public long ahora() { return tiempo; }
        void avanzar(long ms) { this.tiempo += ms; }
    }

    private static final Callback NOOP = new Callback() {
        @Override public void onSuccess(Object result) {}
        @Override public void onError(String mensaje) {}
    };

    @SuppressWarnings("unchecked")
    private static <T> Callback<T> noop() {
        return (Callback<T>) NOOP;
    }

    private long ahoraFijo;
    private SessionManager sessionManager;
    private InMemoryUsuarioRepository usuarioRepository;

    @Before
    public void setUp() {
        ahoraFijo = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 10 * 60);
        sessionManager = new SessionManager();
        usuarioRepository = new InMemoryUsuarioRepository(sessionManager);
    }

    @Test
    public void test01_crearPersonalValido() {
        // Crear Asistente
        Usuario nuevoAsist = new Usuario();
        nuevoAsist.setNombre("Ana López");
        nuevoAsist.setCorreo("ana.lopez@sonidosonic.com");
        nuevoAsist.setTelefono("5551112233");
        nuevoAsist.setRol(Usuario.ROL_ASISTENTE);

        AtomicReference<Usuario> refAsist = new AtomicReference<>();
        usuarioRepository.crearPersonal(nuevoAsist, "Asistente#2026", new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) { refAsist.set(result); }
            @Override public void onError(String mensaje) {}
        });

        assertNotNull(refAsist.get());
        assertEquals("Ana López", refAsist.get().getNombre());
        assertEquals(Usuario.ROL_ASISTENTE, refAsist.get().getRol());
        assertTrue(refAsist.get().isCuentaActiva());
        assertEquals("ana.lopez@sonidosonic.com", refAsist.get().getCorreo());

        // Crear Socio
        Usuario nuevoSocio = new Usuario();
        nuevoSocio.setNombre("Carlos Socio");
        nuevoSocio.setCorreo("carlos.socio@sonidosonic.com");
        nuevoSocio.setTelefono("5552223344");
        nuevoSocio.setRol(Usuario.ROL_SOCIO);

        AtomicReference<Usuario> refSocio = new AtomicReference<>();
        usuarioRepository.crearPersonal(nuevoSocio, "Socio#2026", new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) { refSocio.set(result); }
            @Override public void onError(String mensaje) {}
        });

        assertNotNull(refSocio.get());
        assertEquals("Carlos Socio", refSocio.get().getNombre());
        assertEquals(Usuario.ROL_SOCIO, refSocio.get().getRol());
        assertTrue(refSocio.get().isCuentaActiva());

        // Verificar que crearPersonal NO inició sesión en SessionManager
        assertNull(sessionManager.getUsuarioActual());
    }

    @Test
    public void test02_crearPersonalRechaza() {
        // 1. Rol Cliente
        Usuario uCliente = new Usuario();
        uCliente.setNombre("Cliente Invalido");
        uCliente.setCorreo("cliente@sonidosonic.com");
        uCliente.setTelefono("5551234567");
        uCliente.setRol(Usuario.ROL_CLIENTE);

        AtomicReference<String> errRol = new AtomicReference<>();
        usuarioRepository.crearPersonal(uCliente, "Pass#1234", new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) {}
            @Override public void onError(String mensaje) { errRol.set(mensaje); }
        });
        assertNotNull(errRol.get());
        assertTrue(errRol.get().contains("Socio o Asistente"));

        // 2. Correo repetido (con otras mayúsculas respecto a socio semilla)
        Usuario uRepetido = new Usuario();
        uRepetido.setNombre("Otro Socio");
        uRepetido.setCorreo(Config.CUENTA_SOCIO_CORREO.toUpperCase());
        uRepetido.setTelefono("5551234567");
        uRepetido.setRol(Usuario.ROL_SOCIO);

        AtomicReference<String> errRep = new AtomicReference<>();
        usuarioRepository.crearPersonal(uRepetido, "Pass#1234", new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) {}
            @Override public void onError(String mensaje) { errRep.set(mensaje); }
        });
        assertNotNull(errRep.get());
        assertEquals("El correo ya está registrado", errRep.get());

        // 3. Contraseña débil
        Usuario uPassDebil = new Usuario();
        uPassDebil.setNombre("Asistente Debil");
        uPassDebil.setCorreo("debil@sonidosonic.com");
        uPassDebil.setTelefono("5551234567");
        uPassDebil.setRol(Usuario.ROL_ASISTENTE);

        AtomicReference<String> errPass = new AtomicReference<>();
        usuarioRepository.crearPersonal(uPassDebil, "12345", new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) {}
            @Override public void onError(String mensaje) { errPass.set(mensaje); }
        });
        assertNotNull(errPass.get());
        assertTrue(errPass.get().contains("seguridad"));

        // 4. Teléfono corto
        Usuario uTelCorto = new Usuario();
        uTelCorto.setNombre("Asistente Tel");
        uTelCorto.setCorreo("tel@sonidosonic.com");
        uTelCorto.setTelefono("12345");
        uTelCorto.setRol(Usuario.ROL_ASISTENTE);

        AtomicReference<String> errTel = new AtomicReference<>();
        usuarioRepository.crearPersonal(uTelCorto, "Pass#1234", new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) {}
            @Override public void onError(String mensaje) { errTel.set(mensaje); }
        });
        assertNotNull(errTel.get());
        assertTrue(errTel.get().contains("10 dígitos"));

        // 5. Nombre vacío
        Usuario uNomVacio = new Usuario();
        uNomVacio.setNombre("   ");
        uNomVacio.setCorreo("nom@sonidosonic.com");
        uNomVacio.setTelefono("5551234567");
        uNomVacio.setRol(Usuario.ROL_ASISTENTE);

        AtomicReference<String> errNom = new AtomicReference<>();
        usuarioRepository.crearPersonal(uNomVacio, "Pass#1234", new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) {}
            @Override public void onError(String mensaje) { errNom.set(mensaje); }
        });
        assertNotNull(errNom.get());
        assertTrue(errNom.get().contains("obligatorio"));

        // 6. Correo sin arroba o sin dominio
        Usuario uCorreoSinArroba = new Usuario();
        uCorreoSinArroba.setNombre("Sin Arroba");
        uCorreoSinArroba.setCorreo("correosinmail.com");
        uCorreoSinArroba.setTelefono("5551234567");
        uCorreoSinArroba.setRol(Usuario.ROL_ASISTENTE);

        AtomicReference<String> errSinArroba = new AtomicReference<>();
        usuarioRepository.crearPersonal(uCorreoSinArroba, "Pass#1234", new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) {}
            @Override public void onError(String mensaje) { errSinArroba.set(mensaje); }
        });
        assertNotNull(errSinArroba.get());
        assertTrue(errSinArroba.get().contains("inválido"));

        Usuario uCorreoSinDominio = new Usuario();
        uCorreoSinDominio.setNombre("Sin Dominio");
        uCorreoSinDominio.setCorreo("usuario@dominio");
        uCorreoSinDominio.setTelefono("5551234567");
        uCorreoSinDominio.setRol(Usuario.ROL_ASISTENTE);

        AtomicReference<String> errSinDom = new AtomicReference<>();
        usuarioRepository.crearPersonal(uCorreoSinDominio, "Pass#1234", new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) {}
            @Override public void onError(String mensaje) { errSinDom.set(mensaje); }
        });
        assertNotNull(errSinDom.get());
        assertTrue(errSinDom.get().contains("inválido"));
    }

    @Test
    public void test03_actualizarPersonalDatosBasicos() {
        Usuario nuevo = new Usuario();
        nuevo.setNombre("Pedro Original");
        nuevo.setCorreo("pedro@sonidosonic.com");
        nuevo.setTelefono("5551112233");
        nuevo.setRol(Usuario.ROL_ASISTENTE);

        AtomicReference<Usuario> ref = new AtomicReference<>();
        usuarioRepository.crearPersonal(nuevo, "Pass#1234", new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) { ref.set(result); }
            @Override public void onError(String mensaje) {}
        });

        Usuario creado = ref.get();
        assertNotNull(creado);

        // Actualizar nombre y teléfono
        AtomicReference<Usuario> refAct = new AtomicReference<>();
        usuarioRepository.actualizarPersonal("actor-socio", creado.getId(), "Pedro Modificado",
                "5559998877", null, null, new Callback<Usuario>() {
                    @Override public void onSuccess(Usuario result) { refAct.set(result); }
                    @Override public void onError(String mensaje) {}
                });

        assertNotNull(refAct.get());
        assertEquals("Pedro Modificado", refAct.get().getNombre());
        assertEquals("5559998877", refAct.get().getTelefono());
        // El correo y el rol no deben cambiar
        assertEquals("pedro@sonidosonic.com", refAct.get().getCorreo());
        assertEquals(Usuario.ROL_ASISTENTE, refAct.get().getRol());
    }

    @Test
    public void test04_restriccionesDesactivacion() {
        // Obtener Socio semilla
        AtomicReference<Usuario> socio1Ref = new AtomicReference<>();
        usuarioRepository.getUsuarioPorCorreo(Config.CUENTA_SOCIO_CORREO, new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) { socio1Ref.set(result); }
            @Override public void onError(String mensaje) {}
        });
        Usuario socio1 = socio1Ref.get();
        assertNotNull(socio1);

        // 1. Nadie puede desactivarse a sí mismo (actorId == usuarioId)
        AtomicReference<String> errAuto = new AtomicReference<>();
        usuarioRepository.actualizarPersonal(socio1.getId(), socio1.getId(), null, null, false, null, new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) {}
            @Override public void onError(String mensaje) { errAuto.set(mensaje); }
        });
        assertNotNull(errAuto.get());
        assertEquals("No puedes desactivar tu propia cuenta", errAuto.get());

        // 2. No se puede desactivar al último Socio activo (incluso si otro actor ficticio lo intentara)
        AtomicReference<String> errUltimo = new AtomicReference<>();
        usuarioRepository.actualizarPersonal("otro-actor", socio1.getId(), null, null, false, null, new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) {}
            @Override public void onError(String mensaje) { errUltimo.set(mensaje); }
        });
        assertNotNull(errUltimo.get());
        assertEquals("Debe quedar al menos un Socio activo", errUltimo.get());

        // 3. Crear un segundo Socio
        Usuario socio2 = new Usuario();
        socio2.setNombre("Segundo Socio");
        socio2.setCorreo("socio2@sonidosonic.com");
        socio2.setTelefono("5553334455");
        socio2.setRol(Usuario.ROL_SOCIO);
        AtomicReference<Usuario> s2Creado = new AtomicReference<>();
        usuarioRepository.crearPersonal(socio2, "Socio#2026", new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) { s2Creado.set(result); }
            @Override public void onError(String mensaje) {}
        });
        assertNotNull(s2Creado.get());

        // Con dos Socios activos, socio1 sí puede desactivar a socio2
        AtomicReference<Usuario> s2Desactivado = new AtomicReference<>();
        usuarioRepository.actualizarPersonal(socio1.getId(), s2Creado.get().getId(), null, null, false, null, new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) { s2Desactivado.set(result); }
            @Override public void onError(String mensaje) {}
        });
        assertNotNull(s2Desactivado.get());
        assertFalse(s2Desactivado.get().isCuentaActiva());
    }

    @Test
    public void test05_cuentaDesactivadaNoAutenticaYNoApareceEnActivos() {
        Usuario asist = new Usuario();
        asist.setNombre("Asistente Prueba");
        asist.setCorreo("asist.prueba@sonidosonic.com");
        asist.setTelefono("5551234567");
        asist.setRol(Usuario.ROL_ASISTENTE);
        AtomicReference<Usuario> ref = new AtomicReference<>();
        usuarioRepository.crearPersonal(asist, "Asist#2026", new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) { ref.set(result); }
            @Override public void onError(String mensaje) {}
        });
        Usuario creado = ref.get();
        assertNotNull(creado);

        // Desactivar asist
        usuarioRepository.actualizarPersonal("actor-socio", creado.getId(), null, null, false, null, noop());

        // Intentar autenticar con contraseña correcta -> error de cuenta inactiva
        AtomicReference<String> errInactiva = new AtomicReference<>();
        usuarioRepository.autenticar("asist.prueba@sonidosonic.com", "Asist#2026", new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) {}
            @Override public void onError(String mensaje) { errInactiva.set(mensaje); }
        });
        assertNotNull(errInactiva.get());
        assertEquals("Esta cuenta se encuentra desactivada", errInactiva.get());

        // Intentar autenticar con contraseña errónea -> mensaje genérico
        AtomicReference<String> errGenerico = new AtomicReference<>();
        usuarioRepository.autenticar("asist.prueba@sonidosonic.com", "PasswordErroneo", new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) {}
            @Override public void onError(String mensaje) { errGenerico.set(mensaje); }
        });
        assertNotNull(errGenerico.get());
        assertEquals("Correo o contraseña incorrectos", errGenerico.get());

        // obtenerActivosPorRol no debe incluir la cuenta desactivada
        List<Usuario> asistentesActivos = usuarioRepository.obtenerActivosPorRol(Usuario.ROL_ASISTENTE);
        boolean encontrado = false;
        for (Usuario u : asistentesActivos) {
            if (u.getId().equals(creado.getId())) {
                encontrado = true;
                break;
            }
        }
        assertFalse("Cuenta desactivada no debe aparecer en obtenerActivosPorRol", encontrado);
    }

    @Test
    public void test06_cambiarNombreActualizaSessionManager() {
        AtomicReference<Usuario> socioRef = new AtomicReference<>();
        usuarioRepository.getUsuarioPorCorreo(Config.CUENTA_SOCIO_CORREO, new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) { socioRef.set(result); }
            @Override public void onError(String mensaje) {}
        });
        Usuario socio = socioRef.get();
        assertNotNull(socio);

        // Iniciar sesión
        sessionManager.iniciarSesion(socio);
        assertEquals("Socio Principal", sessionManager.getUsuarioActual().getNombre());

        // Actualizar nombre
        usuarioRepository.actualizarPersonal(socio.getId(), socio.getId(), "Socio Nuevo Nombre",
                null, null, null, noop());

        // Verificar que SessionManager se actualizó
        assertNotNull(sessionManager.getUsuarioActual());
        assertEquals("Socio Nuevo Nombre", sessionManager.getUsuarioActual().getNombre());
    }

    @Test
    public void test07_obtenerReservasActivasDeOperador() {
        RelojFalso reloj = new RelojFalso(ahoraFijo);
        InMemoryEquipoRepository eqRepo = new InMemoryEquipoRepository();
        InMemoryPaqueteRepository paqRepo = new InMemoryPaqueteRepository(eqRepo);
        InMemoryDiaOperativoRepository diaRepo = new InMemoryDiaOperativoRepository();
        InMemoryPedidoRepository pedidoRepo = new InMemoryPedidoRepository(eqRepo, paqRepo, usuarioRepository, diaRepo, reloj);

        long ini = TiempoUtils.construir(2026, Calendar.DECEMBER, 5, 12 * 60);
        long fin = TiempoUtils.sumarMinutos(ini, 240);

        Pedido p1 = new Pedido();
        p1.setId("ped-1");
        p1.setEstado(Pedido.ESTADO_CONFIRMADO);
        p1.setOperadorIds(Collections.singletonList("op-1"));
        p1.setBloqueoInicioMillis(ini - 60 * 60_000L);
        p1.setBloqueoFinMillis(fin + 60 * 60_000L);

        // Agregar directamente a la lista del repo simulando reserva existente
        // o llamando a crearReserva
        // Creamos un pedido cancelado para el mismo operador
        Pedido pCancelado = new Pedido();
        pCancelado.setId("ped-cancelado");
        pCancelado.setEstado(Pedido.ESTADO_CANCELADO);
        pCancelado.setOperadorIds(Collections.singletonList("op-1"));
        pCancelado.setBloqueoInicioMillis(ini - 60 * 60_000L);
        pCancelado.setBloqueoFinMillis(fin + 60 * 60_000L);

        // Y un pedido activo para OTRO operador
        Pedido pOtroOp = new Pedido();
        pOtroOp.setId("ped-otro");
        pOtroOp.setEstado(Pedido.ESTADO_CONFIRMADO);
        pOtroOp.setOperadorIds(Collections.singletonList("op-2"));
        pOtroOp.setBloqueoInicioMillis(ini - 60 * 60_000L);
        pOtroOp.setBloqueoFinMillis(fin + 60 * 60_000L);

        // Acceder por reflexión o mediante paquete y solicitud
        // Podemos probar creando una reserva real en pedidoRepo:
        eqRepo.agregarEquipo(new Equipo("eq-1", "Bocina 15", CategoriaEquipo.BOCINAS, 1, Equipo.ESTADO_DISPONIBLE, 500.0), noop());
        ComponentePaquete comp = new ComponentePaquete(
                com.example.propuestasonidosonic.util.ModeloUtils.clave(CategoriaEquipo.BOCINAS, "Bocina 15"),
                "Bocina 15", CategoriaEquipo.BOCINAS, 1);
        Paquete pkg = new Paquete();
        pkg.setId("pkg-test");
        pkg.setNombre("Paquete");
        pkg.setActivo(true);
        pkg.setPrecio(1500.0);
        pkg.setDuracionBaseHoras(4);
        pkg.setBufferMontajeMin(60);
        pkg.setBufferDesmontajeMin(60);
        pkg.setOperadoresRequeridos(1);
        pkg.setComponentes(Collections.singletonList(comp));
        paqRepo.guardarPaquete(pkg, noop());

        SolicitudReserva sol = new SolicitudReserva("pkg-test", ini, 240, Pedido.ORIGEN_APP,
                "Evento", "cli-1", "Cliente", "5551234567", "Direccion valida 123", "", null);

        AtomicReference<Pedido> pedCreado = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pedCreado.set(result); }
            @Override public void onError(String mensaje) {}
        });

        assertNotNull(pedCreado.get());
        String opAsignado = pedCreado.get().getOperadorIds().get(0);

        // Consultar reservas de opAsignado que cubran ese día
        List<Pedido> res = pedidoRepo.obtenerReservasActivasDeOperador(opAsignado, ini - 3600_000L, fin + 3600_000L);
        assertEquals(1, res.size());
        assertEquals(pedCreado.get().getId(), res.get(0).getId());

        // Consultar con otro operador
        List<Pedido> resOtro = pedidoRepo.obtenerReservasActivasDeOperador("otro-op", ini - 3600_000L, fin + 3600_000L);
        assertEquals(0, resOtro.size());

        // Consultar fuera del rango de tiempo
        List<Pedido> resFuera = pedidoRepo.obtenerReservasActivasDeOperador(opAsignado, ini + 24 * 3600_000L, ini + 48 * 3600_000L);
        assertEquals(0, resFuera.size());
    }

    @Test
    public void test08_integracionConMotor() {
        RelojFalso reloj = new RelojFalso(ahoraFijo);
        InMemoryEquipoRepository eqRepo = new InMemoryEquipoRepository();
        InMemoryPaqueteRepository paqRepo = new InMemoryPaqueteRepository(eqRepo);
        InMemoryDiaOperativoRepository diaRepo = new InMemoryDiaOperativoRepository();
        InMemoryPedidoRepository pedidoRepo = new InMemoryPedidoRepository(eqRepo, paqRepo, usuarioRepository, diaRepo, reloj);

        // Equipo abundante para 3 eventos
        for (int i = 1; i <= 6; i++) {
            eqRepo.agregarEquipo(new Equipo("eq-" + i, "Bocina 15", CategoriaEquipo.BOCINAS, i, Equipo.ESTADO_DISPONIBLE, 500.0), noop());
        }

        ComponentePaquete comp = new ComponentePaquete(
                com.example.propuestasonidosonic.util.ModeloUtils.clave(CategoriaEquipo.BOCINAS, "Bocina 15"),
                "Bocina 15", CategoriaEquipo.BOCINAS, 2);

        Paquete pkg = new Paquete();
        pkg.setId("pkg-test");
        pkg.setNombre("Paquete Básico");
        pkg.setActivo(true);
        pkg.setPrecio(1500.0);
        pkg.setDuracionBaseHoras(4);
        pkg.setBufferMontajeMin(30);
        pkg.setBufferDesmontajeMin(30);
        pkg.setOperadoresRequeridos(1);
        pkg.setComponentes(Collections.singletonList(comp));
        paqRepo.guardarPaquete(pkg, noop());

        // 1. Crear segundo Socio con crearPersonal
        Usuario socio2 = new Usuario();
        socio2.setNombre("Segundo Socio");
        socio2.setCorreo("socio2.test@sonidosonic.com");
        socio2.setTelefono("5551234567");
        socio2.setRol(Usuario.ROL_SOCIO);
        AtomicReference<Usuario> s2Ref = new AtomicReference<>();
        usuarioRepository.crearPersonal(socio2, "Socio#2026", new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) { s2Ref.set(result); }
            @Override public void onError(String mensaje) {}
        });
        assertNotNull(s2Ref.get());

        long ini = TiempoUtils.construir(2026, Calendar.DECEMBER, 5, 12 * 60);

        // Primera reserva simultánea -> viable
        SolicitudReserva sol1 = new SolicitudReserva("pkg-test", ini, 240, Pedido.ORIGEN_APP,
                "Evento 1", "cli-1", "Cliente 1", "5551234567", "Direccion valida 123", "", null);
        AtomicBoolean ok1 = new AtomicBoolean(false);
        pedidoRepo.crearReserva(sol1, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ok1.set(true); }
            @Override public void onError(String mensaje) {}
        });
        assertTrue("Primera reserva debe crearse", ok1.get());

        // Segunda reserva simultánea -> viable (con el segundo Socio)
        SolicitudReserva sol2 = new SolicitudReserva("pkg-test", ini, 240, Pedido.ORIGEN_APP,
                "Evento 2", "cli-2", "Cliente 2", "5551234567", "Direccion valida 123", "", null);
        AtomicBoolean ok2 = new AtomicBoolean(false);
        pedidoRepo.crearReserva(sol2, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ok2.set(true); }
            @Override public void onError(String mensaje) {}
        });
        assertTrue("Segunda reserva debe crearse", ok2.get());

        // Tercera reserva simultánea -> falla porque solo hay 2 Socios
        SolicitudReserva sol3 = new SolicitudReserva("pkg-test", ini, 240, Pedido.ORIGEN_APP,
                "Evento 3", "cli-3", "Cliente 3", "5551234567", "Direccion valida 123", "", null);
        AtomicReference<String> err3 = new AtomicReference<>();
        pedidoRepo.crearReserva(sol3, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { err3.set(mensaje); }
        });
        assertNotNull("Tercera reserva debe fallar por falta de operadores", err3.get());

        // Crear Asistente y marcarlo disponible ese día con guardarDia
        Usuario asist = new Usuario();
        asist.setNombre("Asistente Extra");
        asist.setCorreo("asist.extra@sonidosonic.com");
        asist.setTelefono("5551234567");
        asist.setRol(Usuario.ROL_ASISTENTE);
        AtomicReference<Usuario> asistRef = new AtomicReference<>();
        usuarioRepository.crearPersonal(asist, "Asist#2026", new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) { asistRef.set(result); }
            @Override public void onError(String mensaje) {}
        });
        assertNotNull(asistRef.get());

        String fechaClave = TiempoUtils.claveDia(ini);
        DiaOperativo diaOp = new DiaOperativo(fechaClave, Collections.singletonList(asistRef.get().getId()), Collections.<String>emptyList());
        diaRepo.guardarDia(diaOp, noop());

        // Ahora la tercera reserva pasa
        AtomicBoolean ok3 = new AtomicBoolean(false);
        pedidoRepo.crearReserva(sol3, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { ok3.set(true); }
            @Override public void onError(String mensaje) {}
        });
        assertTrue("Tercera reserva ahora debe crearse con el Asistente", ok3.get());

        // Cuarta reserva simultánea debe fallar porque los 3 operadores están ocupados
        SolicitudReserva sol4 = new SolicitudReserva("pkg-test", ini, 240, Pedido.ORIGEN_APP,
                "Evento 4", "cli-4", "Cliente 4", "5551234567", "Direccion valida 123", "", null);
        AtomicReference<String> err4 = new AtomicReference<>();
        pedidoRepo.crearReserva(sol4, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { err4.set(mensaje); }
        });
        assertNotNull("Cuarta reserva simultánea debe fallar (todos los operadores ocupados)", err4.get());

        // Cancelar la tercera reserva
        List<Pedido> pedidos = pedidoRepo.obtenerTodos();
        Pedido ultimo = pedidos.get(pedidos.size() - 1);
        pedidoRepo.cancelarReserva(ultimo.getId(), "Test", noop());

        // Quitar al Asistente de los disponibles ese día
        DiaOperativo diaOpSinAsist = new DiaOperativo(fechaClave,
                Collections.<String>emptyList(),
                Collections.<String>emptyList());
        diaRepo.guardarDia(diaOpSinAsist, noop());

        // Ahora reintentar la tercera reserva debe fallar porque solo hay 2 Socios disponibles y ambos están en sol1 y sol2
        SolicitudReserva solReintento = new SolicitudReserva("pkg-test", ini, 240, Pedido.ORIGEN_APP,
                "Evento Reintento", "cli-reintento", "Cliente Reintento", "5551234567", "Direccion valida 123", "", null);
        AtomicReference<String> errReintento = new AtomicReference<>();
        pedidoRepo.crearReserva(solReintento, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) {}
            @Override public void onError(String mensaje) { errReintento.set(mensaje); }
        });
        assertNotNull("Debe fallar porque el asistente ya no está disponible ese día", errReintento.get());
    }
}

