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

    public static int colorError(Context context) {
        if (context == null) return 0xFFC53030;
        android.util.TypedValue tv = new android.util.TypedValue();
        if (context.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorError, tv, true)) {
            return tv.data;
        }
        return androidx.core.content.ContextCompat.getColor(context, com.example.propuestasonidosonic.R.color.status_danado);
    }
}
