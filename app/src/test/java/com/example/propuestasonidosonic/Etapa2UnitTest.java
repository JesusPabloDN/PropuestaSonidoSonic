package com.example.propuestasonidosonic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.memory.InMemoryEquipoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryEventoRepository;
import com.example.propuestasonidosonic.model.CategoriaEquipo;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Evento;
import com.example.propuestasonidosonic.util.FormatUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Etapa2UnitTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private InMemoryEquipoRepository equipoRepository;
    private InMemoryEventoRepository eventoRepository;

    @Before
    public void setUp() {
        equipoRepository = new InMemoryEquipoRepository();
        eventoRepository = new InMemoryEventoRepository();
        equipoRepository.setEventoRepository(eventoRepository);
    }

    @Test
    public void testCriterio2_NumeracionConsecutivaUnidades() {
        // Crear 3 unidades de "Bocina JBL 15\""
        AtomicReference<List<Equipo>> primerasRef = new AtomicReference<>();
        equipoRepository.agregarUnidades("Bocina JBL 15\"", CategoriaEquipo.BOCINAS, 500.0,
                Equipo.ESTADO_DISPONIBLE, 3, "content://foto/bocina.jpg", new Callback<List<Equipo>>() {
                    @Override
                    public void onSuccess(List<Equipo> result) {
                        primerasRef.set(result);
                    }

                    @Override
                    public void onError(String error) {}
                });

        List<Equipo> primeras = primerasRef.get();
        assertNotNull(primeras);
        assertEquals(3, primeras.size());
        assertEquals(1, primeras.get(0).getNumeroUnidad());
        assertEquals(2, primeras.get(1).getNumeroUnidad());
        assertEquals(3, primeras.get(2).getNumeroUnidad());
        assertEquals("content://foto/bocina.jpg", primeras.get(0).getFotoUri());

        // Crear 2 más con diferente capitalización debe continuar en 4 y 5
        AtomicReference<List<Equipo>> segundasRef = new AtomicReference<>();
        equipoRepository.agregarUnidades("bocina jbl 15\"", CategoriaEquipo.BOCINAS, 500.0,
                Equipo.ESTADO_DISPONIBLE, 2, null, new Callback<List<Equipo>>() {
                    @Override
                    public void onSuccess(List<Equipo> result) {
                        segundasRef.set(result);
                    }

                    @Override
                    public void onError(String error) {}
                });

        List<Equipo> segundas = segundasRef.get();
        assertNotNull(segundas);
        assertEquals(2, segundas.size());
        assertEquals(4, segundas.get(0).getNumeroUnidad());
        assertEquals(5, segundas.get(1).getNumeroUnidad());
    }

    @Test
    public void testCriterio3_ImagenesPorDefectoDeCategoria() {
        assertEquals(R.drawable.bocinajbl, CategoriaEquipo.getImagenPorDefecto(CategoriaEquipo.BOCINAS));
        assertEquals(R.drawable.subwoofer, CategoriaEquipo.getImagenPorDefecto(CategoriaEquipo.SUBWOOFERS));
        assertEquals(R.drawable.microfonosm, CategoriaEquipo.getImagenPorDefecto(CategoriaEquipo.MICROFONOS));
        assertEquals(R.drawable.mezcladora, CategoriaEquipo.getImagenPorDefecto(CategoriaEquipo.MEZCLADORAS));
        assertEquals(R.drawable.controlador, CategoriaEquipo.getImagenPorDefecto(CategoriaEquipo.CONTROLADORES));
        assertEquals(R.drawable.lucesled, CategoriaEquipo.getImagenPorDefecto(CategoriaEquipo.LUCES));

        // Genéricos para Cableado, Accesorios y Otro
        assertEquals(R.drawable.ic_equipo_generico, CategoriaEquipo.getImagenPorDefecto(CategoriaEquipo.CABLEADO));
        assertEquals(R.drawable.ic_equipo_generico, CategoriaEquipo.getImagenPorDefecto(CategoriaEquipo.ACCESORIOS));
        assertEquals(R.drawable.ic_equipo_generico, CategoriaEquipo.getImagenPorDefecto(CategoriaEquipo.OTRO));
        assertEquals(R.drawable.ic_equipo_generico, CategoriaEquipo.getImagenPorDefecto("Desconocido"));
    }

    @Test
    public void testCriterio4_FiltroBusquedaNormalizaAcentosYMayusculas() {
        assertEquals("microfono", FormatUtils.quitarAcentos("Micrófono"));
        assertEquals("bocina", FormatUtils.quitarAcentos("BOCINA"));
        assertEquals("mezcladora", FormatUtils.quitarAcentos("MÉZCLÁDÓRÁ"));

        String query = FormatUtils.quitarAcentos("micro");
        String equipoNombre = FormatUtils.quitarAcentos("Micrófono Shure SM58");
        assertTrue(equipoNombre.contains(query));
    }

    @Test
    public void testCriterio5_EstadoEfectivoYDerivadoRentado() {
        long ahora = System.currentTimeMillis();

        Equipo eq = new Equipo("eq-1", "Bocina 15", CategoriaEquipo.BOCINAS, 1, Equipo.ESTADO_DISPONIBLE, 500.0);
        equipoRepository.agregarEquipo(eq, new Callback<Equipo>() {
            @Override public void onSuccess(Equipo r) {}
            @Override public void onError(String e) {}
        });

        // Sin evento: disponible
        assertEquals(Equipo.ESTADO_DISPONIBLE, equipoRepository.estadoEfectivo(eq, ahora));

        // Evento activo hoy
        Evento evHoy = new Evento();
        evHoy.setId("ev-hoy");
        evHoy.setNombre("Boda en curso");
        evHoy.setInicioMillis(ahora - 3600000);
        evHoy.setFinMillis(ahora + 3600000);
        evHoy.setEquipoIds(Collections.singletonList("eq-1"));
        evHoy.setEstado(Evento.ESTADO_PROGRAMADO);

        eventoRepository.crearEvento(evHoy, new Callback<Evento>() {
            @Override public void onSuccess(Evento r) {}
            @Override public void onError(String e) {}
        });

        // Con evento hoy no cancelado: derivado RENTADO
        assertEquals(Equipo.ESTADO_RENTADO, equipoRepository.estadoEfectivo(eq, ahora));

        // Si el estado físico es EN_REPARACION o DANADO, ese manda sobre RENTADO
        eq.setEstadoFisico(Equipo.ESTADO_EN_REPARACION);
        assertEquals(Equipo.ESTADO_EN_REPARACION, equipoRepository.estadoEfectivo(eq, ahora));

        eq.setEstadoFisico(Equipo.ESTADO_DANADO);
        assertEquals(Equipo.ESTADO_DANADO, equipoRepository.estadoEfectivo(eq, ahora));
    }

    @Test
    public void testProximaReservaFutura() {
        long ahora = System.currentTimeMillis();

        Equipo eq = new Equipo("eq-2", "Micrófono SM58", CategoriaEquipo.MICROFONOS, 1, Equipo.ESTADO_DISPONIBLE, 200.0);
        equipoRepository.agregarEquipo(eq, new Callback<Equipo>() {
            @Override public void onSuccess(Equipo r) {}
            @Override public void onError(String e) {}
        });

        assertNull(equipoRepository.proximaReserva("eq-2", ahora));

        // Agregar evento futuro
        Evento evFuturo = new Evento();
        evFuturo.setId("ev-futuro");
        evFuturo.setNombre("XV Años Sofía");
        evFuturo.setInicioMillis(ahora + 86400000L); // Mañana
        evFuturo.setFinMillis(ahora + 86400000L + 10000L);
        evFuturo.setEquipoIds(Collections.singletonList("eq-2"));
        evFuturo.setEstado(Evento.ESTADO_PROGRAMADO);

        eventoRepository.crearEvento(evFuturo, new Callback<Evento>() {
            @Override public void onSuccess(Evento r) {}
            @Override public void onError(String e) {}
        });

        Evento proxima = equipoRepository.proximaReserva("eq-2", ahora);
        assertNotNull(proxima);
        assertEquals("XV Años Sofía", proxima.getNombre());
    }

    @Test
    public void testCriterio6_EliminarUnidadBloqueadoSiTieneEventoActivo() {
        Equipo eq = new Equipo("eq-3", "Mezcladora Pioneer", CategoriaEquipo.MEZCLADORAS, 1, Equipo.ESTADO_DISPONIBLE, 800.0);
        equipoRepository.agregarEquipo(eq, new Callback<Equipo>() {
            @Override public void onSuccess(Equipo r) {}
            @Override public void onError(String e) {}
        });

        Evento ev = new Evento();
        ev.setId("ev-activo");
        ev.setNombre("Graduación Tec");
        ev.setInicioMillis(System.currentTimeMillis() + 500000L);
        ev.setFinMillis(System.currentTimeMillis() + 900000L);
        ev.setEquipoIds(Collections.singletonList("eq-3"));
        ev.setEstado(Evento.ESTADO_PROGRAMADO);

        eventoRepository.crearEvento(ev, new Callback<Evento>() {
            @Override public void onSuccess(Evento r) {}
            @Override public void onError(String e) {}
        });

        // Intentar eliminar debe dar error y mencionar el evento
        AtomicReference<String> errorRef = new AtomicReference<>();
        AtomicBoolean successRef = new AtomicBoolean(false);

        equipoRepository.eliminar("eq-3", new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                successRef.set(true);
            }

            @Override
            public void onError(String error) {
                errorRef.set(error);
            }
        });

        assertFalse(successRef.get());
        assertNotNull(errorRef.get());
        assertTrue(errorRef.get().contains("Graduación Tec"));

        // Cancelar el evento y reintentar la eliminación
        eventoRepository.cancelarEvento("ev-activo", new Callback<Evento>() {
            @Override public void onSuccess(Evento r) {}
            @Override public void onError(String e) {}
        });

        AtomicBoolean successCancelado = new AtomicBoolean(false);
        equipoRepository.eliminar("eq-3", new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                successCancelado.set(true);
            }

            @Override
            public void onError(String error) {}
        });

        assertTrue("Tras cancelar el evento, la unidad debe poder eliminarse", successCancelado.get());
    }
}
