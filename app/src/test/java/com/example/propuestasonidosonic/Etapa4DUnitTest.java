package com.example.propuestasonidosonic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.propuestasonidosonic.domain.AgendaUtils;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.util.TiempoUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Etapa4DUnitTest {

    private Pedido crearPedido(String id, String estado, long inicioMillis, long finMillis, long vencimientoMillis) {
        Pedido p = new Pedido();
        p.setId(id);
        p.setEstado(estado);
        p.setInicioMillis(inicioMillis);
        p.setFinMillis(finMillis);
        p.setVencimientoApartadoMillis(vencimientoMillis);
        p.setNombreEvento("Evento " + id);
        p.setPaqueteNombre("Paquete Prueba");
        p.setClienteNombre("Cliente Prueba");
        return p;
    }

    @Test
    public void test01_apareceEnAgenda() {
        long ahora = TiempoUtils.construir(2026, 9, 12, 12 * 60);

        // Estados que siempre aparecen
        Pedido pConfirmado = crearPedido("1", Pedido.ESTADO_CONFIRMADO, ahora, ahora + 3600_000L, 0);
        Pedido pPagado = crearPedido("2", Pedido.ESTADO_PAGADO, ahora, ahora + 3600_000L, 0);
        Pedido pEnRevision = crearPedido("3", Pedido.ESTADO_EN_REVISION, ahora, ahora + 3600_000L, 0);
        Pedido pCompletado = crearPedido("4", Pedido.ESTADO_COMPLETADO, ahora, ahora + 3600_000L, 0);

        assertTrue("Confirmado debe aparecer", AgendaUtils.apareceEnAgenda(pConfirmado, ahora));
        assertTrue("Pagado debe aparecer", AgendaUtils.apareceEnAgenda(pPagado, ahora));
        assertTrue("En revisión debe aparecer", AgendaUtils.apareceEnAgenda(pEnRevision, ahora));
        assertTrue("Completado debe aparecer", AgendaUtils.apareceEnAgenda(pCompletado, ahora));

        // APARTADO vigente vs vencido según parámetro 'ahora'
        Pedido pApartadoVigente = crearPedido("5", Pedido.ESTADO_APARTADO, ahora + 86400_000L, ahora + 90000_000L, ahora + 3600_000L);
        Pedido pApartadoVencido = crearPedido("6", Pedido.ESTADO_APARTADO, ahora + 86400_000L, ahora + 90000_000L, ahora - 60_000L);
        Pedido pApartadoJustoVencido = crearPedido("7", Pedido.ESTADO_APARTADO, ahora + 86400_000L, ahora + 90000_000L, ahora);

        assertTrue("Apartado con vencimiento > ahora debe aparecer", AgendaUtils.apareceEnAgenda(pApartadoVigente, ahora));
        assertFalse("Apartado con vencimiento < ahora NO debe aparecer", AgendaUtils.apareceEnAgenda(pApartadoVencido, ahora));
        assertFalse("Apartado con vencimiento == ahora NO debe aparecer", AgendaUtils.apareceEnAgenda(pApartadoJustoVencido, ahora));

        // Estados excluidos
        Pedido pCancelado = crearPedido("8", Pedido.ESTADO_CANCELADO, ahora, ahora + 3600_000L, 0);
        Pedido pRechazado = crearPedido("9", Pedido.ESTADO_RECHAZADO, ahora, ahora + 3600_000L, 0);
        Pedido pVencido = crearPedido("10", Pedido.ESTADO_VENCIDO, ahora, ahora + 3600_000L, 0);

        assertFalse("Cancelado no debe aparecer", AgendaUtils.apareceEnAgenda(pCancelado, ahora));
        assertFalse("Rechazado no debe aparecer", AgendaUtils.apareceEnAgenda(pRechazado, ahora));
        assertFalse("Vencido no debe aparecer", AgendaUtils.apareceEnAgenda(pVencido, ahora));
        assertFalse("Nulo no debe aparecer", AgendaUtils.apareceEnAgenda(null, ahora));
    }

    @Test
    public void test02_agruparPorDia_inicioLocalYOrdenAscendente() {
        long ahora = TiempoUtils.construir(2026, 9, 1, 0);

        // 12 de octubre de 2026: evento a las 10:00 y evento a las 23:30 (termina a las 03:30 del 13)
        long oct12_10h = TiempoUtils.construir(2026, 9, 12, 10 * 60);
        long oct12_23h30 = TiempoUtils.construir(2026, 9, 12, 23 * 60 + 30);
        long oct13_03h30 = TiempoUtils.construir(2026, 9, 13, 3 * 60 + 30);

        // 13 de octubre de 2026: evento a las 00:15
        long oct13_00h15 = TiempoUtils.construir(2026, 9, 13, 15);
        long oct13_04h = TiempoUtils.construir(2026, 9, 13, 4 * 60);

        Pedido p1 = crearPedido("P1", Pedido.ESTADO_CONFIRMADO, oct12_23h30, oct13_03h30, 0);
        Pedido p2 = crearPedido("P2", Pedido.ESTADO_CONFIRMADO, oct12_10h, oct12_10h + 3600_000L, 0);
        Pedido p3 = crearPedido("P3", Pedido.ESTADO_CONFIRMADO, oct13_00h15, oct13_04h, 0);
        // Pedido cancelado que no debe entrar
        Pedido pCancel = crearPedido("PC", Pedido.ESTADO_CANCELADO, oct12_10h, oct12_10h + 3600_000L, 0);

        List<Pedido> lista = Arrays.asList(p1, p2, p3, pCancel);
        Map<String, List<Pedido>> mapa = AgendaUtils.agruparPorDia(lista, ahora);

        assertEquals("Debe tener 2 días con eventos", 2, mapa.size());

        List<Pedido> del12 = mapa.get("2026-10-12");
        assertNotNull("Día 12 debe existir", del12);
        assertEquals("Día 12 debe tener 2 pedidos válidos", 2, del12.size());
        assertEquals("El primero del día 12 debe ser el de las 10:00", "P2", del12.get(0).getId());
        assertEquals("El segundo del día 12 debe ser el de las 23:30", "P1", del12.get(1).getId());

        List<Pedido> del13 = mapa.get("2026-10-13");
        assertNotNull("Día 13 debe existir", del13);
        assertEquals("Día 13 debe tener 1 pedido", 1, del13.size());
        assertEquals("El pedido de las 00:15 pertenece al día 13", "P3", del13.get(0).getId());
    }

    @Test
    public void test03_delMes_filtradoYOrdenado() {
        long ahora = TiempoUtils.construir(2026, 9, 1, 0);

        // Mes 9 = octubre de 2026
        long sep30 = TiempoUtils.construir(2026, 8, 30, 20 * 60);
        long oct1 = TiempoUtils.construir(2026, 9, 1, 10 * 60);
        long oct15 = TiempoUtils.construir(2026, 9, 15, 14 * 60);
        long oct31 = TiempoUtils.construir(2026, 9, 31, 23 * 60);
        long nov1 = TiempoUtils.construir(2026, 10, 1, 0);

        Pedido pSep = crearPedido("P_SEP", Pedido.ESTADO_CONFIRMADO, sep30, sep30 + 3600_000L, 0);
        Pedido pOct15 = crearPedido("P_OCT15", Pedido.ESTADO_CONFIRMADO, oct15, oct15 + 3600_000L, 0);
        Pedido pOct1 = crearPedido("P_OCT1", Pedido.ESTADO_CONFIRMADO, oct1, oct1 + 3600_000L, 0);
        Pedido pOct31 = crearPedido("P_OCT31", Pedido.ESTADO_CONFIRMADO, oct31, oct31 + 3600_000L, 0);
        Pedido pNov1 = crearPedido("P_NOV1", Pedido.ESTADO_CONFIRMADO, nov1, nov1 + 3600_000L, 0);

        List<Pedido> pedidos = Arrays.asList(pSep, pOct15, pOct1, pOct31, pNov1);
        List<Pedido> resultado = AgendaUtils.delMes(pedidos, 2026, 9, ahora);

        assertEquals("Debe incluir exactamente los 3 pedidos de octubre", 3, resultado.size());
        assertEquals("Primero: 1 de octubre", "P_OCT1", resultado.get(0).getId());
        assertEquals("Segundo: 15 de octubre", "P_OCT15", resultado.get(1).getId());
        assertEquals("Tercero: 31 de octubre", "P_OCT31", resultado.get(2).getId());
    }

    @Test
    public void test04_marcadoresDia() {
        long ini = TiempoUtils.construir(2026, 9, 12, 10 * 60);
        Pedido c1 = crearPedido("C1", Pedido.ESTADO_CONFIRMADO, ini, ini + 3600_000L, 0);
        Pedido c2 = crearPedido("C2", Pedido.ESTADO_COMPLETADO, ini, ini + 3600_000L, 0);
        Pedido c3 = crearPedido("C3", Pedido.ESTADO_PAGADO, ini, ini + 3600_000L, 0);
        Pedido a1 = crearPedido("A1", Pedido.ESTADO_APARTADO, ini, ini + 3600_000L, ini + 7200_000L);
        Pedido a2 = crearPedido("A2", Pedido.ESTADO_APARTADO, ini, ini + 3600_000L, ini + 7200_000L);

        List<Pedido> diaMix = Arrays.asList(c1, c2, c3, a1, a2);
        int[] marcadores = AgendaUtils.marcadoresDia(diaMix);

        assertEquals("Confirmados/completados/pagados deben ser 3", 3, marcadores[0]);
        assertEquals("Apartados deben ser 2", 2, marcadores[1]);

        int[] vacio = AgendaUtils.marcadoresDia(new ArrayList<>());
        assertEquals(0, vacio[0]);
        assertEquals(0, vacio[1]);
    }

    @Test
    public void test05_celdasDelMes_cuadricula42YCasosExactos() {
        // Octubre 2026: anio=2026, mes0=9
        List<Long> celdasOct = AgendaUtils.celdasDelMes(2026, 9);
        assertEquals("Siempre debe devolver 42 celdas", 42, celdasOct.size());

        // Octubre 2026 empieza el lunes 28 de septiembre (el 1 de octubre es índice 3)
        long sep28 = TiempoUtils.construir(2026, 8, 28, 0);
        long oct1 = TiempoUtils.construir(2026, 9, 1, 0);
        assertEquals("Celda 0 debe ser lunes 28 sep 2026", sep28, (long) celdasOct.get(0));
        assertEquals("Celda 3 debe ser jueves 1 oct 2026", oct1, (long) celdasOct.get(3));

        // Noviembre 2026: anio=2026, mes0=10
        // Noviembre 2026 empieza el lunes 26 de octubre (el 1 de noviembre es índice 6)
        List<Long> celdasNov = AgendaUtils.celdasDelMes(2026, 10);
        assertEquals(42, celdasNov.size());
        long oct26 = TiempoUtils.construir(2026, 9, 26, 0);
        long nov1 = TiempoUtils.construir(2026, 10, 1, 0);
        assertEquals("Celda 0 debe ser lunes 26 oct 2026", oct26, (long) celdasNov.get(0));
        assertEquals("Celda 6 debe ser domingo 1 nov 2026", nov1, (long) celdasNov.get(6));

        // Febrero 2027: anio=2027, mes0=1
        // Febrero 2027 empieza el lunes 1 de febrero (índice 0)
        List<Long> celdasFeb = AgendaUtils.celdasDelMes(2027, 1);
        assertEquals(42, celdasFeb.size());
        long feb1 = TiempoUtils.construir(2027, 1, 1, 0);
        assertEquals("Celda 0 debe ser lunes 1 feb 2027", feb1, (long) celdasFeb.get(0));
    }
}
