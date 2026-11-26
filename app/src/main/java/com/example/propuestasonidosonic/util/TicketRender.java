package com.example.propuestasonidosonic.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.domain.TicketPago;
import com.example.propuestasonidosonic.model.Pedido;

import java.util.ArrayList;
import java.util.List;

/**
 * Diseño del ticket medido en puntos de la impresora térmica (8 puntos por mm, 203 dpi).
 * El área de impresión de la Nelko PM290 es de 384 puntos (48 mm) sobre papel de 54 mm,
 * así que el ticket se dibuja a ese ancho. Lo usan el PDF (escalado a puntos PDF) y la
 * impresión directa por Bluetooth (mapa de bits de 384 píxeles de ancho).
 */
public final class TicketRender {

    public static final float PUNTOS_POR_MM = 8f;
    public static final int ANCHO_PUNTOS = 384;          // 48 mm de área de impresión
    public static final int ALTO_MAXIMO_PUNTOS = 1200;   // 150 mm: largo máximo comprobado en la PM290

    private static final float MARGEN = 8f;              // 1 mm por lado
    private static final float ANCHO_CONTENIDO = ANCHO_PUNTOS - 2 * MARGEN;

    private static final float TAM_TEXTO = 19f;          // ~2.4 mm, unas 32 columnas
    private static final float TAM_TITULO = 27f;
    private static final float TAM_TOTAL = 23f;
    private static final float TAM_NOTA = 16f;
    private static final float INTERLINEADO = 1.3f;
    private static final float ESPACIO_SEPARADOR = 24f;
    private static final float ANCHO_LOGO = 0.72f;
    private static final float ESPACIO_LOGO = 12f;
    private static final float ESPACIO_FINAL = 40f;      // 5 mm en blanco para cortar el papel

    private TicketRender() {}

    /** Alto total en puntos (1 punto = 1/8 mm). */
    public static int altoPuntos(List<TicketPago.Linea> lineas, Bitmap logo) {
        float alto = MARGEN + altoLogo(logo) + (logo != null ? ESPACIO_LOGO : 0f);
        for (TicketPago.Linea l : lineas) {
            alto += altoLinea(l, true);
        }
        return (int) Math.ceil(alto + ESPACIO_FINAL);
    }

    /**
     * Dibuja el ticket en un lienzo cuyas unidades son puntos de impresora.
     * Con suavizado = false el texto queda nítido para convertirlo a blanco y negro puro.
     */
    public static void dibujar(Canvas canvas, List<TicketPago.Linea> lineas, Bitmap logo, boolean suavizado) {
        canvas.drawColor(Color.WHITE);
        float y = MARGEN;
        if (logo != null) {
            float ancho = ANCHO_CONTENIDO * ANCHO_LOGO;
            float alto = altoLogo(logo);
            float x = (ANCHO_PUNTOS - ancho) / 2f;
            Paint pLogo = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
            ColorMatrix gris = new ColorMatrix();
            gris.setSaturation(0f);
            pLogo.setColorFilter(new ColorMatrixColorFilter(gris));
            canvas.drawBitmap(logo, null, new RectF(x, y, x + ancho, y + alto), pLogo);
            y += alto + ESPACIO_LOGO;
        }
        for (TicketPago.Linea l : lineas) {
            y = dibujarLinea(canvas, l, y, suavizado);
        }
    }

    /** Ticket como mapa de bits de 384 píxeles de ancho, listo para la impresora térmica. */
    public static Bitmap generarBitmap(Context context, Pedido pedido) {
        List<TicketPago.Linea> lineas = TicketPago.construir(pedido);
        Bitmap logo = cargarLogo(context);
        int alto = altoPuntos(lineas, logo);
        Bitmap bmp = Bitmap.createBitmap(ANCHO_PUNTOS, alto, Bitmap.Config.ARGB_8888);
        dibujar(new Canvas(bmp), lineas, logo, false);
        if (logo != null) logo.recycle();
        return bmp;
    }

    private static float altoLogo(Bitmap logo) {
        if (logo == null) return 0f;
        float ancho = ANCHO_CONTENIDO * ANCHO_LOGO;
        return ancho * logo.getHeight() / (float) logo.getWidth();
    }

    private static Paint pintura(TicketPago.Linea l, boolean suavizado) {
        Paint p = new Paint();
        p.setAntiAlias(suavizado);
        p.setColor(Color.BLACK);
        p.setTypeface(Typeface.create(Typeface.MONOSPACE, l.negritas ? Typeface.BOLD : Typeface.NORMAL));
        switch (l.tipo) {
            case TicketPago.TIPO_TITULO:
                p.setTextSize(TAM_TITULO);
                break;
            case TicketPago.TIPO_TOTAL:
                p.setTextSize(TAM_TOTAL);
                break;
            case TicketPago.TIPO_NOTA:
                p.setTextSize(TAM_NOTA);
                break;
            default:
                p.setTextSize(TAM_TEXTO);
                break;
        }
        return p;
    }

    private static float altoLinea(TicketPago.Linea l, boolean suavizado) {
        if (l.tipo == TicketPago.TIPO_SEPARADOR) return ESPACIO_SEPARADOR;
        Paint p = pintura(l, suavizado);
        return p.getTextSize() * INTERLINEADO * renglones(l, p).size();
    }

    private static List<String> renglones(TicketPago.Linea l, Paint p) {
        float ancho = ANCHO_CONTENIDO;
        if (l.tipo == TicketPago.TIPO_COLUMNAS || l.tipo == TicketPago.TIPO_TOTAL) {
            ancho = ANCHO_CONTENIDO - p.measureText(l.derecha) - p.measureText(" ");
        }
        return partir(l.izquierda, p, Math.max(ancho, ANCHO_CONTENIDO * 0.3f));
    }

    private static float dibujarLinea(Canvas canvas, TicketPago.Linea l, float y, boolean suavizado) {
        Paint p = pintura(l, suavizado);
        if (l.tipo == TicketPago.TIPO_SEPARADOR) {
            float base = y + ESPACIO_SEPARADOR * 0.62f;
            int n = (int) (ANCHO_CONTENIDO / p.measureText("-"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) sb.append('-');
            p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(sb.toString(), ANCHO_PUNTOS / 2f, base, p);
            return y + ESPACIO_SEPARADOR;
        }

        float renglon = p.getTextSize() * INTERLINEADO;
        List<String> partes = renglones(l, p);
        boolean centrado = l.tipo == TicketPago.TIPO_TITULO
                || l.tipo == TicketPago.TIPO_CENTRADO
                || l.tipo == TicketPago.TIPO_NOTA;
        for (int i = 0; i < partes.size(); i++) {
            float base = y + p.getTextSize();
            if (centrado) {
                p.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(partes.get(i), ANCHO_PUNTOS / 2f, base, p);
            } else {
                p.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(partes.get(i), MARGEN, base, p);
                if (i == 0 && !l.derecha.isEmpty()) {
                    p.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText(l.derecha, ANCHO_PUNTOS - MARGEN, base, p);
                }
            }
            y += renglon;
        }
        return y;
    }

    /** Parte el texto por palabras (o por letras si una palabra no cabe sola), respetando la sangría. */
    private static List<String> partir(String texto, Paint p, float ancho) {
        List<String> res = new ArrayList<>();
        if (texto == null || texto.isEmpty()) {
            res.add("");
            return res;
        }
        int sangria = 0;
        while (sangria < texto.length() && texto.charAt(sangria) == ' ') sangria++;
        String prefijo = texto.substring(0, sangria);

        StringBuilder actual = new StringBuilder(prefijo);
        for (String palabra : texto.substring(sangria).split(" ")) {
            if (palabra.isEmpty()) continue;
            String prueba = actual.length() > prefijo.length() ? actual + " " + palabra : actual + palabra;
            if (p.measureText(prueba) <= ancho) {
                actual = new StringBuilder(prueba);
                continue;
            }
            if (actual.length() > prefijo.length()) {
                res.add(actual.toString());
                actual = new StringBuilder(prefijo);
            }
            String resto = palabra;
            while (p.measureText(prefijo + resto) > ancho && resto.length() > 1) {
                int n = Math.max(1, p.breakText(resto, true, ancho - p.measureText(prefijo), null));
                res.add(prefijo + resto.substring(0, n));
                resto = resto.substring(n);
            }
            actual = new StringBuilder(prefijo + resto);
        }
        if (actual.length() > 0 || res.isEmpty()) {
            res.add(actual.toString());
        }
        return res;
    }

    /** Logo sin el margen transparente y sobre fondo blanco (el papel térmico no tiene transparencia). */
    public static Bitmap cargarLogo(Context context) {
        try {
            Bitmap original = BitmapFactory.decodeResource(context.getResources(), R.drawable.sonidosonic);
            if (original == null) return null;
            Rect limites = limitesOpacos(original);
            Bitmap recortado = limites != null
                    ? Bitmap.createBitmap(original, limites.left, limites.top, limites.width(), limites.height())
                    : original;
            Bitmap conFondo = Bitmap.createBitmap(recortado.getWidth(), recortado.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(conFondo);
            c.drawColor(Color.WHITE);
            c.drawBitmap(recortado, 0, 0, null);
            if (recortado != original) recortado.recycle();
            original.recycle();
            return conFondo;
        } catch (Exception e) {
            return null;
        }
    }

    private static Rect limitesOpacos(Bitmap bmp) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int[] px = new int[w * h];
        bmp.getPixels(px, 0, w, 0, 0, w, h);
        int izq = w, der = -1, arr = h, aba = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = px[y * w + x];
                boolean visible = Color.alpha(c) > 16
                        && !(Color.red(c) > 245 && Color.green(c) > 245 && Color.blue(c) > 245);
                if (visible) {
                    if (x < izq) izq = x;
                    if (x > der) der = x;
                    if (y < arr) arr = y;
                    if (y > aba) aba = y;
                }
            }
        }
        if (der < izq || aba < arr) return null;
        return new Rect(izq, arr, der + 1, aba + 1);
    }
}
