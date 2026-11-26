package com.example.propuestasonidosonic.impresion;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Convierte el ticket a los bytes que entiende la Nelko PM290 y arma el encabezado del trabajo.
 * No depende de Android para poder probarse.
 *
 * Formato (capturado de la app oficial con una PM290C): 384 puntos de ancho = 48 bytes por renglón,
 * bit más significativo primero, 0 = punto negro y 1 = papel en blanco.
 */
public final class RasterTermico {

    public static final int ANCHO_PUNTOS = 384;
    public static final int BYTES_POR_RENGLON = ANCHO_PUNTOS / 8;
    public static final int ANCHO_PAPEL_MM = 54;

    private RasterTermico() {}

    /**
     * Pasa los píxeles ARGB a blanco y negro con difusión de error Floyd–Steinberg
     * (el logo en grises se ve con tramado; el texto ya es negro puro y no cambia).
     */
    public static byte[] desdePixeles(int[] argb, int ancho, int alto) {
        if (ancho != ANCHO_PUNTOS) {
            throw new IllegalArgumentException("El ticket debe medir " + ANCHO_PUNTOS + " puntos de ancho");
        }
        float[] lum = new float[ancho * alto];
        for (int i = 0; i < lum.length; i++) {
            int c = argb[i];
            int a = (c >>> 24) & 0xff;
            int r = (c >> 16) & 0xff;
            int g = (c >> 8) & 0xff;
            int b = c & 0xff;
            float l = 0.299f * r + 0.587f * g + 0.114f * b;
            // Lo transparente cuenta como papel en blanco
            lum[i] = l * a / 255f + 255f * (255 - a) / 255f;
        }

        byte[] salida = new byte[BYTES_POR_RENGLON * alto];
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int i = y * ancho + x;
                float viejo = lum[i];
                boolean negro = viejo < 128f;
                float error = viejo - (negro ? 0f : 255f);
                if (x + 1 < ancho) lum[i + 1] += error * 7 / 16f;
                if (y + 1 < alto) {
                    if (x > 0) lum[i + ancho - 1] += error * 3 / 16f;
                    lum[i + ancho] += error * 5 / 16f;
                    if (x + 1 < ancho) lum[i + ancho + 1] += error / 16f;
                }
                if (!negro) {
                    salida[y * BYTES_POR_RENGLON + (x >> 3)] |= (byte) (0x80 >> (x & 7));
                }
            }
        }
        return salida;
    }

    /** Encabezado tipo TSPL que manda la app oficial antes de los renglones del dibujo. */
    public static byte[] encabezado(int renglones) {
        int largoMm = (int) Math.ceil(renglones / 8.0);
        String texto = String.format(Locale.ROOT,
                "SIZE %d mm,%d mm\r\nGAP 0,0\r\nDIRECTION 0,0\r\nDENSITY 10\r\nCLS\r\nPRINT 1,1\r\nBITMAP 0,0,%d,%d,1,",
                ANCHO_PAPEL_MM, largoMm, BYTES_POR_RENGLON, renglones);
        return texto.getBytes(StandardCharsets.US_ASCII);
    }

    /** Consulta de estado: ESC ! ? CR LF. La impresora contesta 00 cuando está lista. */
    public static byte[] consultaEstado() {
        return new byte[]{0x1b, 0x21, 0x3f, 0x0d, 0x0a};
    }

    /** Aviso "f0 00 HH LL": cuántos bytes del dibujo acepta la impresora en este momento (-1 si no es aviso). */
    public static int bytesPermitidos(byte[] aviso) {
        if (aviso == null || aviso.length < 4 || (aviso[0] & 0xff) != 0xf0) return -1;
        return ((aviso[aviso.length - 2] & 0xff) << 8) | (aviso[aviso.length - 1] & 0xff);
    }

    /** Aviso "63 00": la impresora terminó el trabajo. */
    public static boolean esFin(byte[] aviso) {
        return aviso != null && aviso.length >= 2 && (aviso[0] & 0xff) == 0x63 && aviso[1] == 0x00;
    }

    public static String hex(byte[] datos) {
        if (datos == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : datos) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        }
        return sb.toString();
    }
}
