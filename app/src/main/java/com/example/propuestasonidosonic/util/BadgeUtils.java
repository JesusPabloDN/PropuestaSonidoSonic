package com.example.propuestasonidosonic.util;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

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
}
