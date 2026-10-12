package com.example.propuestasonidosonic.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import com.example.propuestasonidosonic.domain.ReciboUtils;
import com.example.propuestasonidosonic.model.Pago;
import com.example.propuestasonidosonic.model.Pedido;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public final class ReciboPdf {

    private static final int PAGE_WIDTH = 595; // A4 pt
    private static final int PAGE_HEIGHT = 842; // A4 pt
    private static final int MARGIN = 40;

    private ReciboPdf() {}

    /**
     * Genera un recibo en PDF de 1 página A4 y lo guarda en cacheDir/recibos/recibo_<folio>.pdf.
     */
    public static File generar(Context context, Pedido pedido, Pago pago, List<Pago> pagosDelPedido) throws IOException {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        int contentWidth = PAGE_WIDTH - (MARGIN * 2);
        float y = MARGIN + 20;

        // 1. Encabezado principal
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(22f);
        paint.setColor(Color.rgb(20, 30, 55));
        canvas.drawText("Sonido Sonic", MARGIN, y, paint);

        // Título del recibo
        String titulo = ReciboUtils.tituloRecibo(pago.getTipo());
        paint.setTextSize(14f);
        paint.setColor(Color.rgb(80, 90, 110));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        y += 22;
        canvas.drawText(titulo, MARGIN, y, paint);

        // Folio y Fecha a la derecha
        String folio = pago.getFolio() != null ? pago.getFolio() : "SIN FOLIO";
        String fechaPago = FormatUtils.fechaHoraCorta(pago.getFecha());
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(14f);
        paint.setColor(Color.rgb(20, 30, 55));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Folio: " + folio, PAGE_WIDTH - MARGIN, MARGIN + 20, paint);

        paint.setTextSize(10f);
        paint.setColor(Color.rgb(100, 110, 125));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText(fechaPago, PAGE_WIDTH - MARGIN, MARGIN + 38, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        // Línea divisoria
        y += 18;
        paint.setColor(Color.rgb(220, 225, 235));
        paint.setStrokeWidth(1.5f);
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint);

        // 2. Datos del Cliente y Evento
        y += 26;
        paint.setTextSize(12f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setColor(Color.rgb(40, 50, 70));
        canvas.drawText("DATOS DEL CLIENTE Y EVENTO", MARGIN, y, paint);

        y += 18;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(11f);
        paint.setColor(Color.rgb(60, 70, 85));

        String clienteNom = pedido != null && pedido.getClienteNombre() != null ? pedido.getClienteNombre() : "Cliente";
        String clienteTel = pedido != null && pedido.getClienteTelefono() != null ? pedido.getClienteTelefono() : "";
        canvas.drawText("Cliente: " + clienteNom + (clienteTel.isEmpty() ? "" : "  (" + clienteTel + ")"), MARGIN, y, paint);

        y += 16;
        String eventoNom = pedido != null && pedido.getNombreEvento() != null ? pedido.getNombreEvento() : "Evento";
        canvas.drawText("Evento: " + eventoNom, MARGIN, y, paint);

        if (pedido != null && pedido.getInicioMillis() > 0) {
            y += 16;
            String fechaEv = FormatUtils.fechaLarga(pedido.getInicioMillis());
            String horarioEv = FormatUtils.rangoHorario(pedido.getInicioMillis(), pedido.getFinMillis());
            canvas.drawText("Fecha y horario: " + fechaEv + " (" + horarioEv + ")", MARGIN, y, paint);
        }

        if (pedido != null && pedido.getPaqueteNombre() != null) {
            y += 16;
            canvas.drawText("Paquete: " + pedido.getPaqueteNombre(), MARGIN, y, paint);
        }

        if (pedido != null && pedido.getDireccion() != null && !pedido.getDireccion().trim().isEmpty()) {
            y += 16;
            canvas.drawText("Ubicación: " + pedido.getDireccion(), MARGIN, y, paint);
        }

        // Línea divisoria
        y += 20;
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint);

        // 3. Detalle del Movimiento (Caja destacada)
        y += 24;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(12f);
        paint.setColor(Color.rgb(40, 50, 70));
        canvas.drawText("DETALLE DEL MOVIMIENTO", MARGIN, y, paint);

        y += 16;
        // Rectángulo fondo suave
        paint.setColor(Color.rgb(245, 247, 250));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 60, paint);

        // Borde
        paint.setColor(Color.rgb(220, 225, 235));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 60, paint);
        paint.setStyle(Paint.Style.FILL);

        // Textos dentro de la caja
        paint.setColor(Color.rgb(60, 70, 85));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(11f);
        canvas.drawText("Tipo: " + titulo, MARGIN + 16, y + 24, paint);
        String metodo = pago.getMetodo() != null ? pago.getMetodo() : "EFECTIVO";
        canvas.drawText("Método: " + metodo, MARGIN + 16, y + 44, paint);

        // Monto destacado
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(18f);
        if (Pago.TIPO_REEMBOLSO.equals(pago.getTipo())) {
            paint.setColor(Color.rgb(180, 40, 40));
            canvas.drawText("− " + FormatUtils.formatoMoneda(pago.getMonto()), PAGE_WIDTH - MARGIN - 16, y + 36, paint);
        } else {
            paint.setColor(Color.rgb(25, 120, 60));
            canvas.drawText("+ " + FormatUtils.formatoMoneda(pago.getMonto()), PAGE_WIDTH - MARGIN - 16, y + 36, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);

        y += 80;

        // 4. Bloque de Resumen del Pedido
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(12f);
        paint.setColor(Color.rgb(40, 50, 70));
        canvas.drawText("ESTADO DE LA CUENTA", MARGIN, y, paint);

        y += 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(11f);
        paint.setColor(Color.rgb(60, 70, 85));

        boolean esReembolso = Pago.TIPO_REEMBOLSO.equals(pago.getTipo());
        if (esReembolso) {
            canvas.drawText("Monto reembolsado:", MARGIN, y, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(FormatUtils.formatoMoneda(pago.getMonto()), PAGE_WIDTH - MARGIN, y, paint);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

            String motivo = pedido != null ? pedido.getMotivoCancelacion() : "";
            if (motivo != null && !motivo.trim().isEmpty()) {
                y += 18;
                canvas.drawText("Motivo del reembolso: " + motivo.trim(), MARGIN, y, paint);
            }
        } else {
            double totalPedido = pedido != null ? pedido.getTotal() : 0.0;
            double pagadoHasta = ReciboUtils.totalPagadoHasta(pagosDelPedido, pago.getId());
            double saldoRestante = ReciboUtils.saldoDespuesDe(totalPedido, pagosDelPedido, pago.getId());

            // Fila Total
            canvas.drawText("Total del servicio:", MARGIN, y, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(FormatUtils.formatoMoneda(totalPedido), PAGE_WIDTH - MARGIN, y, paint);
            paint.setTextAlign(Paint.Align.LEFT);

            // Fila Pagado a la fecha
            y += 18;
            canvas.drawText("Pagado a la fecha de este recibo:", MARGIN, y, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(FormatUtils.formatoMoneda(pagadoHasta), PAGE_WIDTH - MARGIN, y, paint);
            paint.setTextAlign(Paint.Align.LEFT);

            // Fila Saldo restante
            y += 18;
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setColor(Color.rgb(30, 40, 60));
            canvas.drawText("Saldo restante:", MARGIN, y, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(FormatUtils.formatoMoneda(saldoRestante), PAGE_WIDTH - MARGIN, y, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        // Línea divisoria antes del pie
        y += 30;
        paint.setColor(Color.rgb(220, 225, 235));
        paint.setStrokeWidth(1f);
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint);

        // 5. Pie de página
        float pieY = PAGE_HEIGHT - MARGIN - 10;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        paint.setTextSize(10f);
        paint.setColor(Color.rgb(130, 140, 155));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Este documento no es un comprobante fiscal.", PAGE_WIDTH / 2f, pieY, paint);

        document.finishPage(page);

        // Guardar archivo
        File dir = new File(context.getCacheDir(), "recibos");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String nombreArchivo = "recibo_" + (pago.getFolio() != null ? pago.getFolio() : pago.getId()) + ".pdf";
        File archivo = new File(dir, nombreArchivo);

        FileOutputStream fos = new FileOutputStream(archivo);
        document.writeTo(fos);
        fos.close();
        document.close();

        return archivo;
    }
}
