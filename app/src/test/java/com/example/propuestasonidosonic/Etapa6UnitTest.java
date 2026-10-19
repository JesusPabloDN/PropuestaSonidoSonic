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
import com.example.propuestasonidosonic.domain.AsistenteUtils;
import com.example.propuestasonidosonic.domain.DisponibilidadAsistenteUtils;
import com.example.propuestasonidosonic.domain.ResultadoDisponibilidad;
import com.example.propuestasonidosonic.domain.SolicitudReserva;
import com.example.propuestasonidosonic.model.CategoriaEquipo;
import com.example.propuestasonidosonic.model.ChecklistItem;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Etapa6UnitTest {

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

        Usuario asistente1 = new Usuario("u-asist1", "Pedro Asistente", "pedro@sonic.com", "9617654321",
                Usuario.ROL_ASISTENTE, "Admin#2026", true, ahoraFijo);
        usuarioRepo.crearPersonal(asistente1, "Admin#2026", noop());

        Usuario asistente2 = new Usuario("u-asist2", "Juan Asistente", "juan@sonic.com", "9619876543",
                Usuario.ROL_ASISTENTE, "Admin#2026", true, ahoraFijo);
        usuarioRepo.crearPersonal(asistente2, "Admin#2026", noop());

        // Paquete básico: 1 bocina, 4 horas, buffer 90 montaje / 120 desmontaje
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

    // 1. eventosDelAsistente: solo pedidos CONFIRMADO/PAGADO/COMPLETADO con su id en operadorIds;
    //    excluye apartado, revisión, cancelado, rechazado, vencido y de otros operadores; id nulo -> vacío.
    @Test
    public void test01_eventosDelAsistente() {
        Pedido pConf = new Pedido();
        pConf.setId("p1");
        pConf.setEstado(Pedido.ESTADO_CONFIRMADO);
        pConf.setOperadorIds(Collections.singletonList("u-asist1"));

        Pedido pPag = new Pedido();
        pPag.setId("p2");
        pPag.setEstado(Pedido.ESTADO_PAGADO);
        pPag.setOperadorIds(Arrays.asList("u-asist1", "u-socio"));

        Pedido pComp = new Pedido();
        pComp.setId("p3");
        pComp.setEstado(Pedido.ESTADO_COMPLETADO);
        pComp.setOperadorIds(Collections.singletonList("u-asist1"));

        Pedido pApar = new Pedido();
        pApar.setId("p4");
        pApar.setEstado(Pedido.ESTADO_APARTADO);
        pApar.setOperadorIds(Collections.singletonList("u-asist1"));

        Pedido pRev = new Pedido();
        pRev.setId("p5");
        pRev.setEstado(Pedido.ESTADO_EN_REVISION);
        pRev.setOperadorIds(Collections.singletonList("u-asist1"));

        Pedido pCanc = new Pedido();
        pCanc.setId("p6");
        pCanc.setEstado(Pedido.ESTADO_CANCELADO);
        pCanc.setOperadorIds(Collections.singletonList("u-asist1"));

        Pedido pRech = new Pedido();
        pRech.setId("p7");
        pRech.setEstado(Pedido.ESTADO_RECHAZADO);
        pRech.setOperadorIds(Collections.singletonList("u-asist1"));

        Pedido pVenc = new Pedido();
        pVenc.setId("p8");
        pVenc.setEstado(Pedido.ESTADO_VENCIDO);
        pVenc.setOperadorIds(Collections.singletonList("u-asist1"));

        Pedido pOtroOp = new Pedido();
        pOtroOp.setId("p9");
        pOtroOp.setEstado(Pedido.ESTADO_CONFIRMADO);
        pOtroOp.setOperadorIds(Collections.singletonList("u-asist2"));

        List<Pedido> todos = Arrays.asList(pConf, pPag, pComp, pApar, pRev, pCanc, pRech, pVenc, pOtroOp);

        List<Pedido> res = AsistenteUtils.eventosDelAsistente(todos, "u-asist1");
        assertEquals(3, res.size());
        assertTrue(res.contains(pConf));
        assertTrue(res.contains(pPag));
        assertTrue(res.contains(pComp));

        assertTrue(AsistenteUtils.eventosDelAsistente(todos, null).isEmpty());
        assertTrue(AsistenteUtils.eventosDelAsistente(todos, "").isEmpty());
        assertTrue(AsistenteUtils.eventosDelAsistente(null, "u-asist1").isEmpty());
    }

    // 2. proximos/pasados: bordes de ahora contra fin de bloqueo, COMPLETADO va a pasados, orden ascendente/descendente
    @Test
    public void test02_proximos_y_pasados() {
        // Pedido 1: fin de bloqueo en ahora + 2h (próximo)
        Pedido p1 = new Pedido();
        p1.setId("p1");
        p1.setEstado(Pedido.ESTADO_CONFIRMADO);
        p1.setInicioMillis(ahoraFijo + 3600_000L);
        p1.setFinMillis(ahoraFijo + 5 * 3600_000L);
        p1.setBloqueoInicioMillis(ahoraFijo);
        p1.setBloqueoFinMillis(ahoraFijo + 7 * 3600_000L);

        // Pedido 2: fin de bloqueo en ahora + 10h (próximo, llegada después de p1)
        Pedido p2 = new Pedido();
        p2.setId("p2");
        p2.setEstado(Pedido.ESTADO_CONFIRMADO);
        p2.setInicioMillis(ahoraFijo + 12 * 3600_000L);
        p2.setFinMillis(ahoraFijo + 16 * 3600_000L);
        p2.setBloqueoInicioMillis(ahoraFijo + 10 * 3600_000L);
        p2.setBloqueoFinMillis(ahoraFijo + 18 * 3600_000L);

        // Pedido 3: fin de bloqueo igual a ahora (borde exacto -> va a pasados)
        Pedido p3 = new Pedido();
        p3.setId("p3");
        p3.setEstado(Pedido.ESTADO_CONFIRMADO);
        p3.setInicioMillis(ahoraFijo - 4 * 3600_000L);
        p3.setFinMillis(ahoraFijo - 3600_000L);
        p3.setBloqueoInicioMillis(ahoraFijo - 5 * 3600_000L);
        p3.setBloqueoFinMillis(ahoraFijo); // finBloqueo == ahora

        // Pedido 4: futuro pero COMPLETADO -> va a pasados
        Pedido p4 = new Pedido();
        p4.setId("p4");
        p4.setEstado(Pedido.ESTADO_COMPLETADO);
        p4.setInicioMillis(ahoraFijo + 24 * 3600_000L);
        p4.setFinMillis(ahoraFijo + 28 * 3600_000L);
        p4.setBloqueoInicioMillis(ahoraFijo + 22 * 3600_000L);
        p4.setBloqueoFinMillis(ahoraFijo + 30 * 3600_000L);

        List<Pedido> lista = Arrays.asList(p2, p3, p1, p4);

        List<Pedido> prox = AsistenteUtils.proximos(lista, ahoraFijo);
        assertEquals(2, prox.size());
        assertEquals("p1", prox.get(0).getId()); // llegada menor primero
        assertEquals("p2", prox.get(1).getId());

        List<Pedido> pas = AsistenteUtils.pasados(lista, ahoraFijo);
        assertEquals(2, pas.size());
        // En pasados, orden descendente por llegada
        assertEquals("p4", pas.get(0).getId()); // llegada más reciente primero
        assertEquals("p3", pas.get(1).getId());
    }

    // 3. llegadaMillis con y sin bloqueo; esHoy con evento a las 23:30 y 00:15 del día siguiente
    @Test
    public void test03_llegadaMillis_y_esHoy() {
        Pedido pConBloqueo = new Pedido();
        pConBloqueo.setInicioMillis(5000L);
        pConBloqueo.setBloqueoInicioMillis(3000L);
        pConBloqueo.setFinMillis(8000L);
        pConBloqueo.setBloqueoFinMillis(10000L);
        assertEquals(3000L, AsistenteUtils.llegadaMillis(pConBloqueo));
        assertEquals(10000L, AsistenteUtils.finBloqueoMillis(pConBloqueo));

        Pedido pSinBloqueo = new Pedido();
        pSinBloqueo.setInicioMillis(5000L);
        pSinBloqueo.setBloqueoInicioMillis(0L);
        pSinBloqueo.setFinMillis(8000L);
        pSinBloqueo.setBloqueoFinMillis(0L);
        assertEquals(5000L, AsistenteUtils.llegadaMillis(pSinBloqueo));
        assertEquals(8000L, AsistenteUtils.finBloqueoMillis(pSinBloqueo));

        // esHoy: Viernes 20 de noviembre de 2026
        long eventoHoy2330 = TiempoUtils.construir(2026, Calendar.NOVEMBER, 20, 23 * 60 + 30);
        Pedido pHoy = new Pedido();
        pHoy.setInicioMillis(eventoHoy2330);
        assertTrue(AsistenteUtils.esHoy(pHoy, ahoraFijo));

        long eventoManana0015 = TiempoUtils.construir(2026, Calendar.NOVEMBER, 21, 15);
        Pedido pManana = new Pedido();
        pManana.setInicioMillis(eventoManana0015);
        assertFalse(AsistenteUtils.esHoy(pManana, ahoraFijo));
    }

    // 4. contactoVisible: justo 24h antes (sí), 24h y 1min antes (no), durante el evento (sí) y después del bloqueo (no)
    @Test
    public void test04_contactoVisible() {
        long llegada = ahoraFijo + 24 * 3600_000L; // Llega exactamente en 24h
        long finBloqueo = llegada + 8 * 3600_000L;

        Pedido p = new Pedido();
        p.setBloqueoInicioMillis(llegada);
        p.setBloqueoFinMillis(finBloqueo);

        // Justo 24h antes -> llegada - 24h == ahoraFijo -> true
        assertTrue(AsistenteUtils.contactoVisible(p, ahoraFijo));

        // 24h y 1 minuto antes -> ahoraFijo - 1 min -> false
        assertFalse(AsistenteUtils.contactoVisible(p, ahoraFijo - 60_000L));

        // Durante el evento -> true
        assertTrue(AsistenteUtils.contactoVisible(p, llegada + 2 * 3600_000L));

        // En el fin de bloqueo exacto -> true
        assertTrue(AsistenteUtils.contactoVisible(p, finBloqueo));

        // 1 minuto después del fin de bloqueo -> false
        assertFalse(AsistenteUtils.contactoVisible(p, finBloqueo + 60_000L));
    }

    // 5. checklistEditable: bordes de 3h antes y de 24h después
    @Test
    public void test05_checklistEditable() {
        long llegada = ahoraFijo + 3 * 3600_000L;
        long finBloqueo = llegada + 6 * 3600_000L;

        Pedido p = new Pedido();
        p.setBloqueoInicioMillis(llegada);
        p.setBloqueoFinMillis(finBloqueo);

        // 3h y 1 min antes de la llegada -> false
        assertFalse(AsistenteUtils.checklistEditable(p, llegada - 3 * 3600_000L - 60_000L));

        // Justo 3h antes de la llegada -> true
        assertTrue(AsistenteUtils.checklistEditable(p, llegada - 3 * 3600_000L));

        // Durante el evento -> true
        assertTrue(AsistenteUtils.checklistEditable(p, llegada + 3600_000L));

        // Justo 24h después del fin de bloqueo -> true
        assertTrue(AsistenteUtils.checklistEditable(p, finBloqueo + 24 * 3600_000L));

        // 24h y 1 min después del fin de bloqueo -> false
        assertFalse(AsistenteUtils.checklistEditable(p, finBloqueo + 24 * 3600_000L + 60_000L));
    }

    // 6. checklistInicial: nombres con número, orden, todo en falso, Unidad eliminada
    @Test
    public void test06_checklistInicial() {
        Map<String, Equipo> equiposMap = new HashMap<>();
        equiposMap.put("boc-1", new Equipo("boc-1", "Bocina 15", CategoriaEquipo.BOCINAS, 1, Equipo.ESTADO_DISPONIBLE, 500.0));
        equiposMap.put("boc-2", new Equipo("boc-2", "Bocina 15", CategoriaEquipo.BOCINAS, 2, Equipo.ESTADO_DISPONIBLE, 500.0));

        List<String> ids = Arrays.asList("boc-2", "boc-1", "id-inexistente");
        List<ChecklistItem> items = AsistenteUtils.checklistInicial(ids, equiposMap);

        assertEquals(3, items.size());
        // Orden alfabético: Bocina 15 (1) < Bocina 15 (2) < Unidad eliminada
        assertEquals("boc-1", items.get(0).getEquipoId());
        assertEquals("Bocina 15 (1)", items.get(0).getNombre());
        assertFalse(items.get(0).isSalio());
        assertFalse(items.get(0).isRegreso());
        assertEquals("", items.get(0).getObservacion());

        assertEquals("boc-2", items.get(1).getEquipoId());
        assertEquals("Bocina 15 (2)", items.get(1).getNombre());

        assertEquals("id-inexistente", items.get(2).getEquipoId());
        assertEquals("Unidad eliminada", items.get(2).getNombre());
    }

    // 7. sincronizarChecklist: conserva marcas de unidades que siguen, agrega nuevas,
    //    quita las removidas sin marcar y conserva las removidas con salio == true.
    @Test
    public void test07_sincronizarChecklist() {
        Map<String, Equipo> equiposMap = new HashMap<>();
        equiposMap.put("u1", new Equipo("u1", "Bocina", CategoriaEquipo.BOCINAS, 1, Equipo.ESTADO_DISPONIBLE, 500.0));
        equiposMap.put("u2", new Equipo("u2", "Consola", CategoriaEquipo.MEZCLADORAS, 1, Equipo.ESTADO_DISPONIBLE, 500.0));
        equiposMap.put("u3", new Equipo("u3", "Micrófono", CategoriaEquipo.MICROFONOS, 1, Equipo.ESTADO_DISPONIBLE, 200.0));
        equiposMap.put("u4", new Equipo("u4", "Luces", CategoriaEquipo.LUCES, 1, Equipo.ESTADO_DISPONIBLE, 400.0));

        List<ChecklistItem> actual = new ArrayList<>();
        actual.add(new ChecklistItem("u1", "Bocina (1)", true, true, "Todo bien"));
        actual.add(new ChecklistItem("u2", "Consola (1)", true, false, "Raspón")); // Ya no asignada pero salio == true
        actual.add(new ChecklistItem("u3", "Micrófono (1)", false, false, "")); // Ya no asignada y no salió

        List<String> nuevaAsignacion = Arrays.asList("u1", "u4");

        List<ChecklistItem> sinc = AsistenteUtils.sincronizarChecklist(actual, nuevaAsignacion, equiposMap);

        assertEquals(3, sinc.size());

        // u1 sigue y conserva sus marcas
        ChecklistItem it1 = sinc.get(0); // Bocina (1)
        assertEquals("u1", it1.getEquipoId());
        assertTrue(it1.isSalio());
        assertTrue(it1.isRegreso());
        assertEquals("Todo bien", it1.getObservacion());

        // u2 no está en nuevaAsignacion pero como salió se conserva
        ChecklistItem it2 = sinc.get(1); // Consola (1)
        assertEquals("u2", it2.getEquipoId());
        assertTrue(it2.isSalio());
        assertFalse(it2.isRegreso());
        assertEquals("Raspón", it2.getObservacion());

        // u4 es nueva unidad agregada
        ChecklistItem it4 = sinc.get(2); // Luces (1)
        assertEquals("u4", it4.getEquipoId());
        assertFalse(it4.isSalio());
        assertFalse(it4.isRegreso());

        // u3 fue quitada porque no estaba en nuevaAsignacion y no había salido
        for (ChecklistItem it : sinc) {
            assertFalse("u3 no debe estar en la lista sincronizada", "u3".equals(it.getEquipoId()));
        }
    }

    // 8. marcar: regreso sin salida no cambia nada; desmarcar salida desmarca regreso; lista original no se modifica; índices fuera de rango no fallan
    @Test
    public void test08_marcar() {
        List<ChecklistItem> original = Collections.singletonList(
                new ChecklistItem("u1", "Bocina (1)", false, false, "")
        );

        // Intento de marcar regreso sin haber salido -> no cambia nada
        List<ChecklistItem> intentoRegreso = AsistenteUtils.marcar(original, 0, "regreso", true);
        assertFalse(intentoRegreso.get(0).isRegreso());
        assertFalse(intentoRegreso.get(0).isSalio());

        // Marcar salida -> ok
        List<ChecklistItem> conSalida = AsistenteUtils.marcar(original, 0, "salio", true);
        assertTrue(conSalida.get(0).isSalio());
        assertFalse(conSalida.get(0).isRegreso());
        // Original sin cambios
        assertFalse(original.get(0).isSalio());

        // Ahora marcar regreso -> ok
        List<ChecklistItem> conRegreso = AsistenteUtils.marcar(conSalida, 0, "regreso", true);
        assertTrue(conRegreso.get(0).isSalio());
        assertTrue(conRegreso.get(0).isRegreso());

        // Desmarcar salida -> debe desmarcar regreso también
        List<ChecklistItem> sinSalida = AsistenteUtils.marcar(conRegreso, 0, "salio", false);
        assertFalse(sinSalida.get(0).isSalio());
        assertFalse(sinSalida.get(0).isRegreso());

        // Índices fuera de rango
        List<ChecklistItem> fuera1 = AsistenteUtils.marcar(original, -1, "salio", true);
        assertEquals(1, fuera1.size());
        List<ChecklistItem> fuera2 = AsistenteUtils.marcar(original, 99, "salio", true);
        assertEquals(1, fuera2.size());
    }

    // 9. conObservacion recorta y limita a 200 chars; resumenChecklist y estadoChecklist en los 4 estados
    @Test
    public void test09_conObservacion_resumenChecklist_y_estadoChecklist() {
        List<ChecklistItem> lista = Collections.singletonList(
                new ChecklistItem("u1", "Bocina (1)", false, false, "")
        );

        StringBuilder sbLargo = new StringBuilder();
        for (int i = 0; i < 250; i++) sbLargo.append("A");

        List<ChecklistItem> conObs = AsistenteUtils.conObservacion(lista, 0, "  " + sbLargo.toString() + "  ");
        assertEquals(200, conObs.get(0).getObservacion().length());

        // 4 Estados de Checklist
        // A: Sin equipo (lista vacía)
        assertEquals("Sin equipo", AsistenteUtils.estadoChecklist(Collections.emptyList()));

        // B: Pendiente (ninguno salió)
        List<ChecklistItem> pendiente = Arrays.asList(
                new ChecklistItem("u1", "Bocina (1)", false, false, ""),
                new ChecklistItem("u2", "Bocina (2)", false, false, "")
        );
        assertEquals("Pendiente", AsistenteUtils.estadoChecklist(pendiente));
        int[] resPendiente = AsistenteUtils.resumenChecklist(pendiente);
        assertEquals(2, resPendiente[0]);
        assertEquals(0, resPendiente[1]);
        assertEquals(0, resPendiente[2]);
        assertEquals(0, resPendiente[3]);

        // C: En evento (uno salió y no todos regresaron)
        List<ChecklistItem> enEvento = Arrays.asList(
                new ChecklistItem("u1", "Bocina (1)", true, true, "Todo bien"),
                new ChecklistItem("u2", "Bocina (2)", true, false, "En uso")
        );
        assertEquals("En evento", AsistenteUtils.estadoChecklist(enEvento));
        int[] resEvento = AsistenteUtils.resumenChecklist(enEvento);
        assertEquals(2, resEvento[0]); // total
        assertEquals(2, resEvento[1]); // salieron
        assertEquals(1, resEvento[2]); // regresaron
        assertEquals(2, resEvento[3]); // con observacion

        // D: Completo (todos regresaron)
        List<ChecklistItem> completo = Arrays.asList(
                new ChecklistItem("u1", "Bocina (1)", true, true, ""),
                new ChecklistItem("u2", "Bocina (2)", true, true, "")
        );
        assertEquals("Completo", AsistenteUtils.estadoChecklist(completo));
    }

    // 10. DisponibilidadAsistenteUtils.puedeMarcar / puedeDesmarcar: día pasado, día con evento, día sin evento
    @Test
    public void test10_disponibilidadAsistenteUtils_puedeMarcar_y_puedeDesmarcar() {
        // Base: 2026-11-20
        // Día pasado: 2026-11-19
        assertFalse(DisponibilidadAsistenteUtils.puedeMarcar("2026-11-19", ahoraFijo));
        assertFalse(DisponibilidadAsistenteUtils.puedeDesmarcar("2026-11-19", ahoraFijo, Collections.emptyList()));

        // Día futuro sin evento: 2026-11-25
        assertTrue(DisponibilidadAsistenteUtils.puedeMarcar("2026-11-25", ahoraFijo));
        assertTrue(DisponibilidadAsistenteUtils.puedeDesmarcar("2026-11-25", ahoraFijo, Collections.emptyList()));

        // Día futuro con evento activo asignado que inicia ese día
        long iniEvento25 = TiempoUtils.construir(2026, Calendar.NOVEMBER, 25, 18 * 60);
        Pedido pActivo = new Pedido();
        pActivo.setId("p-activo");
        pActivo.setEstado(Pedido.ESTADO_CONFIRMADO);
        pActivo.setInicioMillis(iniEvento25);

        assertFalse(DisponibilidadAsistenteUtils.puedeDesmarcar("2026-11-25", ahoraFijo, Collections.singletonList(pActivo)));

        // Evento cancelado en ese día no bloquea
        Pedido pCancelado = new Pedido();
        pCancelado.setId("p-canc");
        pCancelado.setEstado(Pedido.ESTADO_CANCELADO);
        pCancelado.setInicioMillis(iniEvento25);

        assertTrue(DisponibilidadAsistenteUtils.puedeDesmarcar("2026-11-25", ahoraFijo, Collections.singletonList(pCancelado)));
    }

    // 11. clavesFinesDeSemana: octubre de 2026 desde el jueves 15 devuelve 16, 17, 18, 23, 24, 25, 30 y 31; y desde el 1 de octubre incluye también el 2, 3 y 4.
    @Test
    public void test11_clavesFinesDeSemana() {
        // Jueves 15 de octubre de 2026, 10:00
        long jueves15Oct = TiempoUtils.construir(2026, Calendar.OCTOBER, 15, 10 * 60);
        List<String> desde15 = DisponibilidadAsistenteUtils.clavesFinesDeSemana(2026, Calendar.OCTOBER, jueves15Oct);

        List<String> esperadoDesde15 = Arrays.asList(
                "2026-10-16", "2026-10-17", "2026-10-18",
                "2026-10-23", "2026-10-24", "2026-10-25",
                "2026-10-30", "2026-10-31"
        );
        assertEquals(esperadoDesde15, desde15);

        // Jueves 1 de octubre de 2026, 10:00
        long jueves1Oct = TiempoUtils.construir(2026, Calendar.OCTOBER, 1, 10 * 60);
        List<String> desde1 = DisponibilidadAsistenteUtils.clavesFinesDeSemana(2026, Calendar.OCTOBER, jueves1Oct);
        assertTrue(desde1.contains("2026-10-02"));
        assertTrue(desde1.contains("2026-10-03"));
        assertTrue(desde1.contains("2026-10-04"));
        assertTrue(desde1.containsAll(esperadoDesde15));
    }

    // 12. InMemoryDiaOperativoRepository: establecerDisponibilidadAsistente agrega y quita sin borrar
    //     a otros asistentes ni sociosNoDisponiblesIds, es idempotente, y ...Varios marca varios días a la vez.
    @Test
    public void test12_repositorio_disponibilidad_atomica() {
        String fecha = "2026-11-25";

        // Creamos día inicial con u-socio no disponible y u-asist1 disponible
        DiaOperativo dInit = new DiaOperativo(fecha,
                Collections.singletonList("u-asist1"),
                Collections.singletonList("u-socio"));
        diaRepo.guardarDia(dInit, noop());

        // Agregamos a u-asist2 de forma atómica
        AtomicReference<DiaOperativo> diaRef = new AtomicReference<>();
        diaRepo.establecerDisponibilidadAsistente(fecha, "u-asist2", true, new Callback<DiaOperativo>() {
            @Override public void onSuccess(DiaOperativo result) { diaRef.set(result); }
            @Override public void onError(String error) { fail("Error al agregar: " + error); }
        });

        assertNotNull(diaRef.get());
        assertTrue(diaRef.get().getAsistentesDisponiblesIds().contains("u-asist1"));
        assertTrue(diaRef.get().getAsistentesDisponiblesIds().contains("u-asist2"));
        assertTrue(diaRef.get().getSociosNoDisponiblesIds().contains("u-socio"));

        // Idempotencia: volver a agregar u-asist2 no genera duplicados
        diaRepo.establecerDisponibilidadAsistente(fecha, "u-asist2", true, new Callback<DiaOperativo>() {
            @Override public void onSuccess(DiaOperativo result) { diaRef.set(result); }
            @Override public void onError(String error) { fail("Error idempotente: " + error); }
        });
        int countAsist2 = 0;
        for (String id : diaRef.get().getAsistentesDisponiblesIds()) {
            if ("u-asist2".equals(id)) countAsist2++;
        }
        assertEquals(1, countAsist2);

        // Quitar u-asist1 sin tocar u-asist2 ni u-socio
        diaRepo.establecerDisponibilidadAsistente(fecha, "u-asist1", false, new Callback<DiaOperativo>() {
            @Override public void onSuccess(DiaOperativo result) { diaRef.set(result); }
            @Override public void onError(String error) { fail("Error al quitar: " + error); }
        });

        assertFalse(diaRef.get().getAsistentesDisponiblesIds().contains("u-asist1"));
        assertTrue(diaRef.get().getAsistentesDisponiblesIds().contains("u-asist2"));
        assertTrue(diaRef.get().getSociosNoDisponiblesIds().contains("u-socio"));

        // ...Varios: marcar 3 días distintos a la vez
        List<String> variosDias = Arrays.asList("2026-11-26", "2026-11-27", "2026-11-28");
        diaRepo.establecerDisponibilidadAsistenteVarios(variosDias, "u-asist1", true, noop());

        for (String f : variosDias) {
            DiaOperativo d = diaRepo.obtenerDia(f);
            assertNotNull(d);
            assertTrue(d.getAsistentesDisponiblesIds().contains("u-asist1"));
        }
    }

    // 13. Integración: con socios no disponibles un día y un Asistente que lo marca,
    //     una reserva ese día es viable y su operadorIds incluye al Asistente;
    //     al desmarcar (antes de reservar) deja de ser viable. Una vez confirmada, eventosDelAsistente incluye ese pedido.
    @Test
    public void test13_integracion_disponibilidad_motor_y_eventosDelAsistente() {
        // Viernes 27 de noviembre de 2026, 18:00
        long ini27 = TiempoUtils.construir(2026, Calendar.NOVEMBER, 27, 18 * 60);
        String clave27 = TiempoUtils.claveDia(ini27);

        // Bloqueamos a todos los socios en esa fecha
        List<String> sociosIds = new ArrayList<>();
        for (Usuario u : usuarioRepo.obtenerActivosPorRol(Usuario.ROL_SOCIO)) {
            sociosIds.add(u.getId());
        }
        DiaOperativo dBloqSocio = new DiaOperativo(clave27,
                Collections.emptyList(),
                sociosIds);
        diaRepo.guardarDia(dBloqSocio, noop());

        // Sin ningún asistente disponible -> la reserva no es viable
        ResultadoDisponibilidad evalSinAsistente = pedidoRepo.evaluarDisponibilidad("paq-basico", ini27, 240, null);
        assertFalse(evalSinAsistente.isViable());

        // El asistente u-asist1 marca disponibilidad ese día
        diaRepo.establecerDisponibilidadAsistente(clave27, "u-asist1", true, noop());

        // Ahora es viable!
        ResultadoDisponibilidad evalConAsistente = pedidoRepo.evaluarDisponibilidad("paq-basico", ini27, 240, null);
        assertTrue(evalConAsistente.isViable());
        assertTrue(evalConAsistente.getOperadorIds().contains("u-asist1"));

        // Si se desmarca antes de reservar, vuelve a ser no viable
        diaRepo.establecerDisponibilidadAsistente(clave27, "u-asist1", false, noop());
        assertFalse(pedidoRepo.evaluarDisponibilidad("paq-basico", ini27, 240, null).isViable());

        // Vuelve a marcar y creamos la reserva
        diaRepo.establecerDisponibilidadAsistente(clave27, "u-asist1", true, noop());
        SolicitudReserva sol = new SolicitudReserva("paq-basico", ini27, 240, Pedido.ORIGEN_APP,
                "Boda Asistente", "cli-asist", "Cliente Asist", "7471234567", "Av. Principal 1234, Centro", "", null);

        AtomicReference<Pedido> pedRef = new AtomicReference<>();
        pedidoRepo.crearReserva(sol, new Callback<Pedido>() {
            @Override public void onSuccess(Pedido result) { pedRef.set(result); }
            @Override public void onError(String error) { fail("Error crearReserva: " + error); }
        });

        assertNotNull(pedRef.get());
        assertTrue(pedRef.get().getOperadorIds().contains("u-asist1"));

        // Al registrar anticipo pasa a CONFIRMADO
        pedidoRepo.registrarAnticipo(pedRef.get().getId(), pedRef.get().getAnticipoRequerido(),
                Pedido.METODO_TARJETA, noop());
        Pedido pConfirmado = pedidoRepo.getPedidoPorId(pedRef.get().getId());
        assertEquals(Pedido.ESTADO_CONFIRMADO, pConfirmado.getEstado());

        // Ahora eventosDelAsistente incluye este pedido para u-asist1
        List<Pedido> pedidosAsist1 = AsistenteUtils.eventosDelAsistente(pedidoRepo.obtenerTodos(), "u-asist1");
        assertEquals(1, pedidosAsist1.size());
        assertEquals(pConfirmado.getId(), pedidosAsist1.get(0).getId());

        // Y no aparece para u-asist2
        List<Pedido> pedidosAsist2 = AsistenteUtils.eventosDelAsistente(pedidoRepo.obtenerTodos(), "u-asist2");
        assertTrue(pedidosAsist2.isEmpty());
    }

    // 14. EventoRepository.actualizarChecklist guarda el checklist y funciona con callback null
    @Test
    public void test14_eventoRepository_actualizarChecklist_nullSafe() {
        Evento ev = new Evento();
        ev.setId("ev-checklist-test");
        ev.setNombre("Evento Checklist");
        ev.setInicioMillis(ahoraFijo);
        ev.setFinMillis(ahoraFijo + 4 * 3600_000L);
        ev.setEstado(Evento.ESTADO_PROGRAMADO);

        eventoRepo.crearEvento(ev, noop());

        List<ChecklistItem> cl = Arrays.asList(
                new ChecklistItem("boc-1", "Bocina 15 (1)", true, false, "Sin detalles"),
                new ChecklistItem("boc-2", "Bocina 15 (2)", true, true, "Todo en orden")
        );

        // Invocación con callback null -> NO debe lanzar NullPointerException
        try {
            eventoRepo.actualizarChecklist("ev-checklist-test", cl, null);
        } catch (Exception e) {
            fail("actualizarChecklist con callback null no debe fallar: " + e.getMessage());
        }

        // Verificar que se guardó
        AtomicReference<Evento> obtenidoRef = new AtomicReference<>();
        eventoRepo.getEventoPorId("ev-checklist-test", new Callback<Evento>() {
            @Override public void onSuccess(Evento result) { obtenidoRef.set(result); }
            @Override public void onError(String error) { fail("Error al obtener: " + error); }
        });

        assertNotNull(obtenidoRef.get());
        assertNotNull(obtenidoRef.get().getChecklist());
        assertEquals(2, obtenidoRef.get().getChecklist().size());
        assertEquals("Sin detalles", obtenidoRef.get().getChecklist().get(0).getObservacion());
    }
}
