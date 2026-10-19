package com.example.propuestasonidosonic;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.memory.InMemoryEquipoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryUsuarioRepository;
import com.example.propuestasonidosonic.domain.ActividadRecienteUtils;
import com.example.propuestasonidosonic.domain.PerfilUtils;
import com.example.propuestasonidosonic.domain.PoliticasNegocio;
import com.example.propuestasonidosonic.model.CategoriaEquipo;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Pago;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Etapa7UnitTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static class RelojFalso implements Reloj {
        long tiempo;

        RelojFalso(long tiempo) {
            this.tiempo = tiempo;
        }

        @Override
        public long ahora() {
            return tiempo;
        }

        public void setAhora(long tiempo) {
            this.tiempo = tiempo;
        }
    }

    private RelojFalso reloj;
    private InMemoryUsuarioRepository usuarioRepo;
    private InMemoryEquipoRepository equipoRepo;

    @Before
    public void setUp() {
        // Miércoles 21 de Octubre de 2026, 12:00:00 GMT (06:00 CDMX)
        long baseTiempo = TiempoUtils.construir(2026, 9, 21, 12 * 60);
        reloj = new RelojFalso(baseTiempo);
        usuarioRepo = new InMemoryUsuarioRepository();
        equipoRepo = new InMemoryEquipoRepository(reloj);
    }

    private Usuario obtenerSeedSocio() {
        List<Usuario> list = usuarioRepo.getUsuarios().getValue();
        if (list != null) {
            for (Usuario u : list) {
                if (Config.CUENTA_SOCIO_CORREO.equalsIgnoreCase(u.getCorreo())) {
                    return u;
                }
            }
        }
        return null;
    }

    // 1. PerfilUtils.iniciales
    @Test
    public void testIniciales() {
        assertEquals("SP", PerfilUtils.iniciales("Socio Principal"));
        assertEquals("JP", PerfilUtils.iniciales("  juan   pablo   damián "));
        assertEquals("Á", PerfilUtils.iniciales("ángel"));
        assertEquals("C", PerfilUtils.iniciales("Carlos"));
        assertEquals("?", PerfilUtils.iniciales(""));
        assertEquals("?", PerfilUtils.iniciales("   "));
        assertEquals("?", PerfilUtils.iniciales(null));
    }

    // 2. actualizarPerfil
    @Test
    public void testActualizarPerfilExitoso() {
        Usuario seed = obtenerSeedSocio();
        assertNotNull(seed);
        String socioId = seed.getId();

        Usuario original = usuarioRepo.getUsuarioPorId(socioId);
        assertNotNull(original);
        String correoOriginal = original.getCorreo();
        String rolOriginal = original.getRol();
        String passOriginal = original.getContrasena();
        boolean activaOriginal = original.isCuentaActiva();

        AtomicReference<Usuario> usuarioActualizadoRef = new AtomicReference<>();
        usuarioRepo.actualizarPerfil(socioId, "  Juan Carlos Pérez  ", " 7471234567 ", new Callback<Usuario>() {
            @Override
            public void onSuccess(Usuario result) {
                usuarioActualizadoRef.set(result);
            }

            @Override
            public void onError(String error) {
                fail("No debió fallar: " + error);
            }
        });

        Usuario actualizado = usuarioActualizadoRef.get();
        assertNotNull(actualizado);
        assertEquals("Juan Carlos Pérez", actualizado.getNombre());
        assertEquals("7471234567", actualizado.getTelefono());
        assertEquals(correoOriginal, actualizado.getCorreo());
        assertEquals(rolOriginal, actualizado.getRol());
        assertEquals(passOriginal, actualizado.getContrasena());
        assertEquals(activaOriginal, actualizado.isCuentaActiva());

        // Verificar que en el repositorio se persistió
        Usuario persistido = usuarioRepo.getUsuarioPorId(socioId);
        assertEquals("Juan Carlos Pérez", persistido.getNombre());
        assertEquals("7471234567", persistido.getTelefono());
    }

    @Test
    public void testActualizarPerfilValidaciones() {
        Usuario seed = obtenerSeedSocio();
        assertNotNull(seed);
        String socioId = seed.getId();

        Usuario antes = usuarioRepo.getUsuarioPorId(socioId);
        String nombreAntes = antes.getNombre();
        String telAntes = antes.getTelefono();

        // 1. Nombre vacío
        AtomicBoolean falloNombre = new AtomicBoolean(false);
        usuarioRepo.actualizarPerfil(socioId, "   ", "7471234567", new Callback<Usuario>() {
            @Override
            public void onSuccess(Usuario result) {
                fail("No debió aceptar nombre vacío");
            }

            @Override
            public void onError(String error) {
                falloNombre.set(true);
            }
        });
        assertTrue(falloNombre.get());

        // 2. Teléfono con menos de 10 dígitos
        AtomicBoolean falloTelefono = new AtomicBoolean(false);
        usuarioRepo.actualizarPerfil(socioId, "Carlos", "123456789", new Callback<Usuario>() {
            @Override
            public void onSuccess(Usuario result) {
                fail("No debió aceptar teléfono de 9 dígitos");
            }

            @Override
            public void onError(String error) {
                falloTelefono.set(true);
            }
        });
        assertTrue(falloTelefono.get());

        // 3. Usuario inexistente
        AtomicBoolean falloInexistente = new AtomicBoolean(false);
        usuarioRepo.actualizarPerfil("id-fantasma-999", "Carlos", "7471234567", new Callback<Usuario>() {
            @Override
            public void onSuccess(Usuario result) {
                fail("No debió aceptar usuario inexistente");
            }

            @Override
            public void onError(String error) {
                falloInexistente.set(true);
            }
        });
        assertTrue(falloInexistente.get());

        // Verificar que el usuario no cambió
        Usuario despues = usuarioRepo.getUsuarioPorId(socioId);
        assertEquals(nombreAntes, despues.getNombre());
        assertEquals(telAntes, despues.getTelefono());
    }

    // 3. cambiarContrasena
    @Test
    public void testCambiarContrasenaValidacionesYExito() {
        Usuario seed = obtenerSeedSocio();
        assertNotNull(seed);
        String socioId = seed.getId();
        String actual = Config.CUENTA_SOCIO_CONTRASENA;

        // A. Actual incorrecta
        AtomicBoolean falloActual = new AtomicBoolean(false);
        usuarioRepo.cambiarContrasena(socioId, "erronea999", "NuevaPass123!", new Callback<Void>() {
            @Override public void onSuccess(Void result) { fail("No debió cambiar con contraseña actual incorrecta"); }
            @Override public void onError(String error) { falloActual.set(true); }
        });
        assertTrue(falloActual.get());

        // B. Nueva débil (menos de 8 caracteres o sin mayúscula/número/símbolo)
        AtomicBoolean falloDebil = new AtomicBoolean(false);
        usuarioRepo.cambiarContrasena(socioId, actual, "debil", new Callback<Void>() {
            @Override public void onSuccess(Void result) { fail("No debió aceptar contraseña débil"); }
            @Override public void onError(String error) { falloDebil.set(true); }
        });
        assertTrue(falloDebil.get());

        // C. Nueva igual a la actual
        AtomicBoolean falloIgual = new AtomicBoolean(false);
        usuarioRepo.cambiarContrasena(socioId, actual, actual, new Callback<Void>() {
            @Override public void onSuccess(Void result) { fail("No debió aceptar contraseña igual a la actual"); }
            @Override public void onError(String error) { falloIgual.set(true); }
        });
        assertTrue(falloIgual.get());

        // D. Cambio exitoso
        AtomicBoolean exitoCambio = new AtomicBoolean(false);
        usuarioRepo.cambiarContrasena(socioId, actual, "NuevaFuerte2026!", new Callback<Void>() {
            @Override public void onSuccess(Void result) { exitoCambio.set(true); }
            @Override public void onError(String error) { fail("No debió fallar: " + error); }
        });
        assertTrue(exitoCambio.get());

        // E. Autenticación funciona con la nueva y falla con la vieja
        AtomicBoolean authNueva = new AtomicBoolean(false);
        usuarioRepo.autenticar("socio@sonidosonic.com", "NuevaFuerte2026!", new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) { authNueva.set(true); }
            @Override public void onError(String error) {}
        });
        assertTrue(authNueva.get());

        AtomicBoolean authVieja = new AtomicBoolean(false);
        usuarioRepo.autenticar("socio@sonidosonic.com", actual, new Callback<Usuario>() {
            @Override public void onSuccess(Usuario result) { authVieja.set(true); }
            @Override public void onError(String error) {}
        });
        assertFalse(authVieja.get());
    }

    // 4. PoliticasNegocio.actual()
    @Test
    public void testPoliticasNegocioReflejaConfig() {
        PoliticasNegocio p = PoliticasNegocio.actual();
        assertNotNull(p);
        assertEquals(30, p.getPorcentajeAnticipo());
        assertEquals(30, p.getMinutosApartadoApp());
        assertEquals(48, p.getHorasRevision());
        assertEquals(7, p.getDiasReembolsoTotal());
        assertEquals(72, p.getHorasTramoParcial());
        assertEquals(50, p.getPorcentajeReembolsoParcial());
        assertEquals(Config.MAX_EVENTOS_SIMULTANEOS, p.getMaxEventosSimultaneos());
        assertEquals(Config.PRIMER_INICIO_MIN, p.getPrimerInicioMin());
        assertEquals(Config.ULTIMO_INICIO_MIN, p.getUltimoInicioMin());
        assertEquals(Config.PASO_HORARIO_MIN, p.getPasoHorarioMin());
        assertEquals(Config.DURACION_MIN_MIN, p.getDuracionMinMin());
        assertEquals(Config.DURACION_MAX_MIN, p.getDuracionMaxMin());
        assertEquals(Config.PREFERIR_ASISTENTES, p.isPreferirAsistentes());
        assertEquals(Config.BUFFER_MONTAJE_MIN_DEFAULT, p.getBufferMontajeMin());
        assertEquals(Config.BUFFER_DESMONTAJE_MIN_DEFAULT, p.getBufferDesmontajeMin());

        int[] opcionesManual = p.getOpcionesHorasApartadoManual();
        assertNotNull(opcionesManual);
        assertEquals(Config.APARTADO_MANUAL_OPCIONES_HORAS.length, opcionesManual.length);
    }

    // 5. ActividadRecienteUtils.ultimoPedido
    @Test
    public void testUltimoPedido() {
        assertNull(ActividadRecienteUtils.ultimoPedido(null));
        assertNull(ActividadRecienteUtils.ultimoPedido(Collections.emptyList()));

        Pedido p1 = new Pedido();
        p1.setId("p1");
        p1.setNumero(1);
        p1.setFechaCreacion(1000L);

        Pedido p2 = new Pedido();
        p2.setId("p2");
        p2.setNumero(2);
        p2.setFechaCreacion(2000L);

        Pedido p3 = new Pedido();
        p3.setId("p3");
        p3.setNumero(3);
        p3.setFechaCreacion(2000L); // Misma fecha que p2, pero numero mayor

        Pedido p4 = new Pedido();
        p4.setId("p4");
        p4.setNumero(4);
        p4.setFechaCreacion(1500L);

        List<Pedido> lista = Arrays.asList(p1, p4, p2, p3);
        Pedido resultado = ActividadRecienteUtils.ultimoPedido(lista);
        assertNotNull(resultado);
        assertEquals("p3", resultado.getId());
        assertEquals(3, resultado.getNumero());
    }

    // 6. ActividadRecienteUtils.proximoEvento
    @Test
    public void testProximoEvento() {
        long ahora = 100_000L;

        // Caso 1: Vacío o nulo
        assertNull(ActividadRecienteUtils.proximoEvento(null, ahora));
        assertNull(ActividadRecienteUtils.proximoEvento(Collections.emptyList(), ahora));

        // Pedidos excluidos por estado
        Pedido pedApartado = new Pedido();
        pedApartado.setId("apartado");
        pedApartado.setEstado(Pedido.ESTADO_APARTADO);
        pedApartado.setInicioMillis(110_000L);
        pedApartado.setFinMillis(150_000L);

        Pedido pedCancelado = new Pedido();
        pedCancelado.setId("cancelado");
        pedCancelado.setEstado(Pedido.ESTADO_CANCELADO);
        pedCancelado.setInicioMillis(110_000L);
        pedCancelado.setFinMillis(150_000L);

        Pedido pedRechazado = new Pedido();
        pedRechazado.setId("rechazado");
        pedRechazado.setEstado(Pedido.ESTADO_RECHAZADO);
        pedRechazado.setInicioMillis(110_000L);
        pedRechazado.setFinMillis(150_000L);

        Pedido pedVencido = new Pedido();
        pedVencido.setId("vencido");
        pedVencido.setEstado(Pedido.ESTADO_VENCIDO);
        pedVencido.setInicioMillis(110_000L);
        pedVencido.setFinMillis(150_000L);

        Pedido pedCompletado = new Pedido();
        pedCompletado.setId("completado");
        pedCompletado.setEstado(Pedido.ESTADO_COMPLETADO);
        pedCompletado.setInicioMillis(110_000L);
        pedCompletado.setFinMillis(150_000L);

        List<Pedido> soloExcluidos = Arrays.asList(pedApartado, pedCancelado, pedRechazado, pedVencido, pedCompletado);
        assertNull(ActividadRecienteUtils.proximoEvento(soloExcluidos, ahora));

        // Evento que ya finalizó (finMillis <= ahora)
        Pedido pedPasado = new Pedido();
        pedPasado.setId("pasado");
        pedPasado.setEstado(Pedido.ESTADO_CONFIRMADO);
        pedPasado.setInicioMillis(50_000L);
        pedPasado.setFinMillis(90_000L);

        // Evento en curso (inicio <= ahora && fin > ahora)
        Pedido pedEnCurso = new Pedido();
        pedEnCurso.setId("en_curso");
        pedEnCurso.setEstado(Pedido.ESTADO_PAGADO);
        pedEnCurso.setInicioMillis(95_000L);
        pedEnCurso.setFinMillis(120_000L);

        // Evento futuro cercano
        Pedido pedFuturoCercano = new Pedido();
        pedFuturoCercano.setId("futuro_cercano");
        pedFuturoCercano.setEstado(Pedido.ESTADO_EN_REVISION);
        pedFuturoCercano.setInicioMillis(130_000L);
        pedFuturoCercano.setFinMillis(160_000L);

        // Evento futuro lejano
        Pedido pedFuturoLejano = new Pedido();
        pedFuturoLejano.setId("futuro_lejano");
        pedFuturoLejano.setEstado(Pedido.ESTADO_CONFIRMADO);
        pedFuturoLejano.setInicioMillis(200_000L);
        pedFuturoLejano.setFinMillis(240_000L);

        List<Pedido> lista = Arrays.asList(pedPasado, pedFuturoLejano, pedEnCurso, pedFuturoCercano);
        Pedido prox = ActividadRecienteUtils.proximoEvento(lista, ahora);
        assertNotNull(prox);
        // pedEnCurso tiene el inicioMillis menor (95_000 vs 130_000) y su finMillis (120_000) > ahora (100_000)
        assertEquals("en_curso", prox.getId());

        // Si removemos el en curso, el más cercano es futuro cercano
        List<Pedido> sinEnCurso = Arrays.asList(pedPasado, pedFuturoLejano, pedFuturoCercano);
        Pedido prox2 = ActividadRecienteUtils.proximoEvento(sinEnCurso, ahora);
        assertNotNull(prox2);
        assertEquals("futuro_cercano", prox2.getId());
    }

    // 7. ultimoPago y ultimoEquipoAgregado
    @Test
    public void testUltimoPago() {
        assertNull(ActividadRecienteUtils.ultimoPago(null));
        assertNull(ActividadRecienteUtils.ultimoPago(Collections.emptyList()));

        Pago pagoAnticipo = new Pago("pg1", "ped1", "cli1", 1000.0, Pago.TIPO_ANTICIPO, "Efectivo", 5000L);
        Pago pagoLiquidacion = new Pago("pg2", "ped1", "cli1", 2000.0, Pago.TIPO_LIQUIDACION, "Tarjeta", 8000L);
        Pago pagoReembolso = new Pago("pg3", "ped2", "cli2", 1000.0, Pago.TIPO_REEMBOLSO, "Transferencia", 12000L);
        Pago pagoAbono = new Pago("pg4", "ped3", "cli3", 500.0, Pago.TIPO_ABONO, "Efectivo", 7000L);

        List<Pago> pagos = Arrays.asList(pagoAnticipo, pagoReembolso, pagoLiquidacion, pagoAbono);
        Pago ult = ActividadRecienteUtils.ultimoPago(pagos);
        assertNotNull(ult);
        // Debe ser el reembolso con fecha 12000L
        assertEquals("pg3", ult.getId());
        assertEquals(Pago.TIPO_REEMBOLSO, ult.getTipo());
    }

    @Test
    public void testUltimoEquipoAgregado() {
        assertNull(ActividadRecienteUtils.ultimoEquipoAgregado(null));
        assertNull(ActividadRecienteUtils.ultimoEquipoAgregado(Collections.emptyList()));

        Equipo e1 = new Equipo("eq1", "Bafle", CategoriaEquipo.BOCINAS, 1, Equipo.ESTADO_DISPONIBLE, 1000L);
        Equipo e2 = new Equipo("eq2", "Micro", CategoriaEquipo.MICROFONOS, 1, Equipo.ESTADO_DISPONIBLE, 3000L);
        Equipo e3 = new Equipo("eq3", "Micro", CategoriaEquipo.MICROFONOS, 2, Equipo.ESTADO_DISPONIBLE, 3000L); // Misma fecha, mayor unidad
        Equipo e4 = new Equipo("eq4", "Luces", CategoriaEquipo.LUCES, 1, Equipo.ESTADO_DISPONIBLE, 2000L);

        List<Equipo> equipos = Arrays.asList(e1, e4, e2, e3);
        Equipo ult = ActividadRecienteUtils.ultimoEquipoAgregado(equipos);
        assertNotNull(ult);
        assertEquals("eq3", ult.getId());
        assertEquals(2, ult.getNumeroUnidad());
    }

    @Test
    public void testEquipoRepositorioAsignaFechaAlta() {
        // Al crear unidades en InMemoryEquipoRepository, se debe asignar la fechaAlta del Reloj
        reloj.setAhora(987654321L);
        equipoRepo.agregarUnidades("Bocina Nueva", CategoriaEquipo.BOCINAS, 500.0, Equipo.ESTADO_DISPONIBLE, 2, new Callback<List<Equipo>>() {
            @Override
            public void onSuccess(List<Equipo> result) {
                assertNotNull(result);
                assertEquals(2, result.size());
                for (Equipo eq : result) {
                    assertEquals(987654321L, eq.getFechaAlta());
                }
            }

            @Override
            public void onError(String error) {
                fail("No debió fallar: " + error);
            }
        });
    }

    // 8. ActividadRecienteUtils.tiempoRelativo
    @Test
    public void testTiempoRelativo() {
        // Base: 21 de octubre de 2026, 12:00:00 CDMX
        long ahora = TiempoUtils.construir(2026, 9, 21, 12 * 60);

        // A. 30 s atrás -> AHORA
        long momento30s = ahora - 30_000L;
        ActividadRecienteUtils.TiempoRelativo tr30s = ActividadRecienteUtils.tiempoRelativo(ahora, momento30s);
        assertEquals(ActividadRecienteUtils.TiempoRelativo.AHORA, tr30s.getTipo());

        // B. 5 min atrás -> MINUTOS 5
        long momento5min = ahora - (5 * 60_000L);
        ActividadRecienteUtils.TiempoRelativo tr5min = ActividadRecienteUtils.tiempoRelativo(ahora, momento5min);
        assertEquals(ActividadRecienteUtils.TiempoRelativo.MINUTOS, tr5min.getTipo());
        assertEquals(5, tr5min.getValor());

        // C. 59 min atrás -> MINUTOS 59
        long momento59min = ahora - (59 * 60_000L);
        ActividadRecienteUtils.TiempoRelativo tr59min = ActividadRecienteUtils.tiempoRelativo(ahora, momento59min);
        assertEquals(ActividadRecienteUtils.TiempoRelativo.MINUTOS, tr59min.getTipo());
        assertEquals(59, tr59min.getValor());

        // D. 60 min atrás -> HORAS 1
        long momento60min = ahora - (60 * 60_000L);
        ActividadRecienteUtils.TiempoRelativo tr60min = ActividadRecienteUtils.tiempoRelativo(ahora, momento60min);
        assertEquals(ActividadRecienteUtils.TiempoRelativo.HORAS, tr60min.getTipo());
        assertEquals(1, tr60min.getValor());

        // E. 23 h atrás dentro del mismo día calendario -> HORAS 23
        long ahoraFinDia = TiempoUtils.construir(2026, 9, 21, 23 * 60 + 30);
        long momento23h = ahoraFinDia - (23 * 3600_000L);
        ActividadRecienteUtils.TiempoRelativo tr23h = ActividadRecienteUtils.tiempoRelativo(ahoraFinDia, momento23h);
        assertEquals(ActividadRecienteUtils.TiempoRelativo.HORAS, tr23h.getTipo());
        assertEquals(23, tr23h.getValor());

        // F. Ayer a las 23:50 visto hoy a las 00:10 -> AYER
        // 20 de octubre a las 23:50
        long momentoAyer = TiempoUtils.construir(2026, 9, 20, 23 * 60 + 50);
        // 21 de octubre a las 00:10 (diferencia de 20 minutos, pero día anterior calendario)
        long ahora0010 = TiempoUtils.construir(2026, 9, 21, 10);
        ActividadRecienteUtils.TiempoRelativo trAyer = ActividadRecienteUtils.tiempoRelativo(ahora0010, momentoAyer);
        assertEquals(ActividadRecienteUtils.TiempoRelativo.AYER, trAyer.getTipo());
        assertEquals(1, trAyer.getValor());

        // G. 3 días atrás -> DIAS 3
        long momento3dias = ahora - (3L * 86400_000L);
        ActividadRecienteUtils.TiempoRelativo tr3dias = ActividadRecienteUtils.tiempoRelativo(ahora, momento3dias);
        assertEquals(ActividadRecienteUtils.TiempoRelativo.DIAS, tr3dias.getTipo());
        assertEquals(3, tr3dias.getValor());

        // H. 8 días atrás -> FECHA
        long momento8dias = ahora - (8L * 86400_000L);
        ActividadRecienteUtils.TiempoRelativo tr8dias = ActividadRecienteUtils.tiempoRelativo(ahora, momento8dias);
        assertEquals(ActividadRecienteUtils.TiempoRelativo.FECHA, tr8dias.getTipo());

        // I. Momento futuro -> AHORA
        long momentoFuturo = ahora + 120_000L;
        ActividadRecienteUtils.TiempoRelativo trFuturo = ActividadRecienteUtils.tiempoRelativo(ahora, momentoFuturo);
        assertEquals(ActividadRecienteUtils.TiempoRelativo.AHORA, trFuturo.getTipo());
    }
}
