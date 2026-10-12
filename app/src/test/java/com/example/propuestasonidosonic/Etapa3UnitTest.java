package com.example.propuestasonidosonic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.memory.InMemoryEquipoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPaqueteRepository;
import com.example.propuestasonidosonic.model.CategoriaEquipo;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Paquete;
import com.example.propuestasonidosonic.util.ModeloUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Etapa3UnitTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private InMemoryEquipoRepository equipoRepository;
    private InMemoryPaqueteRepository paqueteRepository;

    @Before
    public void setUp() {
        equipoRepository = new InMemoryEquipoRepository();
        paqueteRepository = new InMemoryPaqueteRepository(equipoRepository);

        // Registrar equipo base para pruebas:
        // Bocina JBL 15" × 4
        equipoRepository.agregarUnidades("Bocina JBL 15\"", CategoriaEquipo.BOCINAS, 500.0,
                Equipo.ESTADO_DISPONIBLE, 4, null, new Callback<List<Equipo>>() {
                    @Override public void onSuccess(List<Equipo> result) {}
                    @Override public void onError(String error) {}
                });

        // Mezcladora Behringer × 1
        equipoRepository.agregarUnidades("Mezcladora Behringer", CategoriaEquipo.MEZCLADORAS, 800.0,
                Equipo.ESTADO_DISPONIBLE, 1, null, new Callback<List<Equipo>>() {
                    @Override public void onSuccess(List<Equipo> result) {}
                    @Override public void onError(String error) {}
                });
    }

    @Test
    public void testGuardadoCorrectoYCopiasDefensivas() {
        String claveBocina = ModeloUtils.clave(CategoriaEquipo.BOCINAS, "Bocina JBL 15\"");
        String claveMezcladora = ModeloUtils.clave(CategoriaEquipo.MEZCLADORAS, "Mezcladora Behringer");

        Paquete p = new Paquete();
        p.setNombre("Paquete Básico");
        p.setDescripcion("Para eventos pequeños");
        p.setPrecio(1800.0);
        p.setDuracionBaseHoras(4);
        p.setOperadoresRequeridos(1);

        List<ComponentePaquete> comps = new ArrayList<>();
        comps.add(new ComponentePaquete(claveBocina, "Bocina JBL 15\"", CategoriaEquipo.BOCINAS, 2));
        comps.add(new ComponentePaquete(claveMezcladora, "Mezcladora Behringer", CategoriaEquipo.MEZCLADORAS, 1));
        p.setComponentes(comps);

        AtomicReference<Paquete> resultRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>();

        paqueteRepository.guardarPaquete(p, new Callback<Paquete>() {
            @Override
            public void onSuccess(Paquete result) {
                resultRef.set(result);
            }

            @Override
            public void onError(String error) {
                errorRef.set(error);
            }
        });

        assertNull(errorRef.get());
        assertNotNull(resultRef.get());
        assertEquals("Paquete Básico", resultRef.get().getNombre());
        assertEquals(2, resultRef.get().getComponentes().size());

        // Verificar copia defensiva: modificar objeto local no altera lo guardado en repo
        p.setNombre("Nombre Modificado");
        Paquete recuperado = paqueteRepository.getPaquetePorId(resultRef.get().getId());
        assertNotNull(recuperado);
        assertEquals("Paquete Básico", recuperado.getNombre());
    }

    @Test
    public void testValidacionNombreRepetidoCaseInsensitive() {
        String claveBocina = ModeloUtils.clave(CategoriaEquipo.BOCINAS, "Bocina JBL 15\"");

        Paquete p1 = new Paquete();
        p1.setNombre("Paquete Básico");
        p1.setPrecio(1500.0);
        p1.setComponentes(Collections.singletonList(new ComponentePaquete(claveBocina, "Bocina JBL 15\"", CategoriaEquipo.BOCINAS, 1)));

        paqueteRepository.guardarPaquete(p1, new Callback<Paquete>() {
            @Override public void onSuccess(Paquete result) {}
            @Override public void onError(String error) {}
        });

        // Intentar guardar con minúsculas "paquete básico"
        Paquete p2 = new Paquete();
        p2.setNombre("paquete básico");
        p2.setPrecio(1600.0);
        p2.setComponentes(Collections.singletonList(new ComponentePaquete(claveBocina, "Bocina JBL 15\"", CategoriaEquipo.BOCINAS, 1)));

        AtomicReference<String> errorRef = new AtomicReference<>();
        paqueteRepository.guardarPaquete(p2, new Callback<Paquete>() {
            @Override
            public void onSuccess(Paquete result) {}

            @Override
            public void onError(String error) {
                errorRef.set(error);
            }
        });

        assertNotNull(errorRef.get());
        assertTrue(errorRef.get().contains("Ya existe un paquete"));
    }

    @Test
    public void testValidacionCantidadMayorAlTotalDelModelo() {
        String claveBocina = ModeloUtils.clave(CategoriaEquipo.BOCINAS, "Bocina JBL 15\"");

        // Solo hay 4 bocinas en inventario, pedir 5 debe fallar
        Paquete p = new Paquete();
        p.setNombre("Paquete Grande");
        p.setPrecio(3000.0);
        p.setComponentes(Collections.singletonList(new ComponentePaquete(claveBocina, "Bocina JBL 15\"", CategoriaEquipo.BOCINAS, 5)));

        AtomicReference<String> errorRef = new AtomicReference<>();
        paqueteRepository.guardarPaquete(p, new Callback<Paquete>() {
            @Override
            public void onSuccess(Paquete result) {}

            @Override
            public void onError(String error) {
                errorRef.set(error);
            }
        });

        assertNotNull(errorRef.get());
        assertTrue(errorRef.get().contains("Solo hay 4 unidades"));
    }

    @Test
    public void testValidacionSinComponentes() {
        Paquete p = new Paquete();
        p.setNombre("Paquete Vacío");
        p.setPrecio(1000.0);
        p.setComponentes(new ArrayList<>());

        AtomicReference<String> errorRef = new AtomicReference<>();
        paqueteRepository.guardarPaquete(p, new Callback<Paquete>() {
            @Override
            public void onSuccess(Paquete result) {}

            @Override
            public void onError(String error) {
                errorRef.set(error);
            }
        });

        assertNotNull(errorRef.get());
        assertTrue(errorRef.get().contains("al menos un componente"));
    }

    @Test
    public void testEliminarPaquete() {
        String claveBocina = ModeloUtils.clave(CategoriaEquipo.BOCINAS, "Bocina JBL 15\"");

        Paquete p = new Paquete();
        p.setNombre("Paquete Temporal");
        p.setPrecio(1200.0);
        p.setComponentes(Collections.singletonList(new ComponentePaquete(claveBocina, "Bocina JBL 15\"", CategoriaEquipo.BOCINAS, 1)));

        AtomicReference<Paquete> ref = new AtomicReference<>();
        paqueteRepository.guardarPaquete(p, new Callback<Paquete>() {
            @Override public void onSuccess(Paquete result) { ref.set(result); }
            @Override public void onError(String error) {}
        });

        assertNotNull(ref.get());

        AtomicBoolean eliminado = new AtomicBoolean(false);
        paqueteRepository.eliminarPaquete(ref.get().getId(), new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                eliminado.set(true);
            }

            @Override
            public void onError(String error) {}
        });

        assertTrue(eliminado.get());
        assertNull(paqueteRepository.getPaquetePorId(ref.get().getId()));
    }
}
