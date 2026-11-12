package com.example.propuestasonidosonic.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

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
}
