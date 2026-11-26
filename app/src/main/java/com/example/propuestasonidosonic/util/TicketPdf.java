package com.example.propuestasonidosonic.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;

import com.example.propuestasonidosonic.domain.TicketPago;
import com.example.propuestasonidosonic.model.Pedido;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Guarda el ticket en PDF con el ancho del área de impresión de la impresora térmica (48 mm)
 * y el largo que necesite el contenido. El diseño es el mismo que se imprime por Bluetooth.
 */
public final class TicketPdf {

    // Puntos PDF por punto de impresora: (72 / 25.4) / 8
    private static final float ESCALA = (72f / 25.4f) / TicketRender.PUNTOS_POR_MM;

    private TicketPdf() {}

    /** Genera el ticket y lo guarda en cacheDir/recibos/ticket_<folio>.pdf. */
    public static File generar(Context context, Pedido pedido) throws IOException {
        List<TicketPago.Linea> lineas = TicketPago.construir(pedido);
        Bitmap logo = TicketRender.cargarLogo(context);
        int alto = TicketRender.altoPuntos(lineas, logo);

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(
                Math.round(TicketRender.ANCHO_PUNTOS * ESCALA),
                (int) Math.ceil(alto * ESCALA), 1).create();
        PdfDocument.Page page = document.startPage(info);
        Canvas canvas = page.getCanvas();
        canvas.scale(ESCALA, ESCALA);
        TicketRender.dibujar(canvas, lineas, logo, true);
        document.finishPage(page);
        if (logo != null) logo.recycle();

        File dir = new File(context.getCacheDir(), "recibos");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File archivo = new File(dir, TicketPago.nombreArchivo(pedido));
        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            document.writeTo(fos);
        } finally {
            document.close();
        }
        return archivo;
    }
}
