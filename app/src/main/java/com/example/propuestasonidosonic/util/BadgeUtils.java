package com.example.propuestasonidosonic.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.domain.EstadosPedido;
import com.example.propuestasonidosonic.model.Pedido;
import com.google.android.material.color.MaterialColors;

public final class BadgeUtils {
    private BadgeUtils() {}

    public static void pintar(TextView textView, int colorTexto, int colorFondo) {
        if (textView == null) return;
        Context context = textView.getContext();
        float radiusPx = 8 * context.getResources().getDisplayMetrics().density;
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(radiusPx);
        bg.setColor(colorFondo);
        textView.setBackground(bg);
        textView.setTextColor(colorTexto);
    }

    public static int colorError(Context context) {
        if (context == null) return Color.TRANSPARENT;
        return colorAtributo(context, androidx.appcompat.R.attr.colorError);
    }

    public static int colorAtributo(Context context, int atributo) {
        return MaterialColors.getColor(context, atributo, Color.TRANSPARENT);
    }

    /** Pinta la insignia de estado de un pedido con su texto y su par de colores contenedor/sobre-contenedor. */
    public static void pintarEstadoPedido(TextView textView, String estado) {
        if (textView == null) return;
        Context ctx = textView.getContext();
        int texto;
        int fondo;
        String e = estado != null ? estado : "";
        switch (e) {
            case Pedido.ESTADO_POR_APROBAR:
                texto = colorAtributo(ctx, R.attr.colorOnEstadoPendienteContainer);
                fondo = colorAtributo(ctx, R.attr.colorEstadoPendienteContainer);
                break;
            case Pedido.ESTADO_CONFIRMADO:
                texto = colorAtributo(ctx, R.attr.colorOnEstadoDisponibleContainer);
                fondo = colorAtributo(ctx, R.attr.colorEstadoDisponibleContainer);
                break;
            case Pedido.ESTADO_POR_COBRAR:
                texto = colorAtributo(ctx, com.google.android.material.R.attr.colorOnTertiaryContainer);
                fondo = colorAtributo(ctx, com.google.android.material.R.attr.colorTertiaryContainer);
                break;
            case Pedido.ESTADO_FINALIZADO:
                texto = colorAtributo(ctx, R.attr.colorOnEstadoRentadoContainer);
                fondo = colorAtributo(ctx, R.attr.colorEstadoRentadoContainer);
                break;
            case Pedido.ESTADO_CANCELADO:
            case Pedido.ESTADO_RECHAZADO:
                texto = colorAtributo(ctx, R.attr.colorOnEstadoDanadoContainer);
                fondo = colorAtributo(ctx, R.attr.colorEstadoDanadoContainer);
                break;
            case Pedido.ESTADO_VENCIDO:
            default:
                texto = colorAtributo(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant);
                fondo = colorAtributo(ctx, com.google.android.material.R.attr.colorSurfaceContainerHighest);
                break;
        }
        textView.setText(EstadosPedido.etiqueta(estado));
        pintar(textView, texto, fondo);
    }

    /** Insignia "Pagado" (pago confirmado por un socio). */
    public static void pintarPagado(TextView textView) {
        if (textView == null) return;
        Context ctx = textView.getContext();
        pintar(textView,
                colorAtributo(ctx, R.attr.colorOnEstadoDisponibleContainer),
                colorAtributo(ctx, R.attr.colorEstadoDisponibleContainer));
    }
}
