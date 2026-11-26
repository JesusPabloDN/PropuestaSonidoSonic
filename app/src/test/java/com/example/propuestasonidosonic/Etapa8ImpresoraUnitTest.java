package com.example.propuestasonidosonic;

import com.example.propuestasonidosonic.impresion.RasterTermico;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Formato de los datos que se envían a la impresora térmica Nelko PM290 (384 puntos de ancho). */
public class Etapa8ImpresoraUnitTest {

    private static final int BLANCO = 0xFFFFFFFF;
    private static final int NEGRO = 0xFF000000;

    @Test
    public void test01_rasterBlancoYNegro_bitMasSignificativoPrimero() {
        int ancho = RasterTermico.ANCHO_PUNTOS;
        int[] px = new int[ancho * 2];
        Arrays.fill(px, BLANCO);
        px[0] = NEGRO;           // renglón 0, primer punto
        px[9] = NEGRO;           // renglón 0, segundo byte, segundo bit
        px[ancho + 383] = NEGRO; // renglón 1, último punto

        byte[] r = RasterTermico.desdePixeles(px, ancho, 2);
        assertEquals(48 * 2, r.length);
        // 0 = negro, 1 = blanco
        assertEquals(0x7F, r[0] & 0xff);
        assertEquals(0xBF, r[1] & 0xff);
        assertEquals(0xFF, r[2] & 0xff);
        assertEquals(0xFE, r[48 + 47] & 0xff);
    }

    @Test
    public void test02_transparenteCuentaComoPapel() {
        int ancho = RasterTermico.ANCHO_PUNTOS;
        int[] px = new int[ancho];
        Arrays.fill(px, 0x00000000);
        byte[] r = RasterTermico.desdePixeles(px, ancho, 1);
        for (byte b : r) assertEquals(0xFF, b & 0xff);
    }

    @Test
    public void test03_grisSeTramaConPuntosNegrosYBlancos() {
        int ancho = RasterTermico.ANCHO_PUNTOS;
        int[] px = new int[ancho * 8];
        Arrays.fill(px, 0xFF808080);
        byte[] r = RasterTermico.desdePixeles(px, ancho, 8);
        int negros = 0;
        for (byte b : r) negros += 8 - Integer.bitCount(b & 0xff);
        double proporcion = negros / (double) (ancho * 8);
        assertTrue("Un gris medio debe quedar con cerca de la mitad de puntos negros: " + proporcion,
                proporcion > 0.4 && proporcion < 0.6);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test04_anchoIncorrectoSeRechaza() {
        RasterTermico.desdePixeles(new int[100], 100, 1);
    }

    @Test
    public void test05_encabezadoComoLaAppOficial() {
        String h = new String(RasterTermico.encabezado(432), StandardCharsets.US_ASCII);
        assertEquals("SIZE 54 mm,54 mm\r\nGAP 0,0\r\nDIRECTION 0,0\r\nDENSITY 10\r\nCLS\r\nPRINT 1,1\r\nBITMAP 0,0,48,432,1,", h);
        // Largo en mm redondeado hacia arriba
        assertTrue(new String(RasterTermico.encabezado(1001), StandardCharsets.US_ASCII).startsWith("SIZE 54 mm,126 mm"));
    }

    @Test
    public void test06_avisosDeLaImpresora() {
        assertEquals(18000, RasterTermico.bytesPermitidos(new byte[]{(byte) 0xf0, 0x00, 0x46, 0x50}));
        assertEquals(2736, RasterTermico.bytesPermitidos(new byte[]{(byte) 0xf0, 0x00, 0x0a, (byte) 0xb0}));
        assertEquals(-1, RasterTermico.bytesPermitidos(new byte[]{0x63, 0x00}));
        assertTrue(RasterTermico.esFin(new byte[]{0x63, 0x00}));
        assertFalse(RasterTermico.esFin(new byte[]{0x00}));
        assertEquals("1b 21 3f 0d 0a", RasterTermico.hex(RasterTermico.consultaEstado()));
    }
}
