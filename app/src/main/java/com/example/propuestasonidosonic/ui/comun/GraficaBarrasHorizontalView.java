package com.example.propuestasonidosonic.ui.comun;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.example.propuestasonidosonic.R;
import com.google.android.material.color.MaterialColors;

import java.util.Arrays;

public class GraficaBarrasHorizontalView extends View {

    private String[] etiquetas = new String[0];
    private float[] valores = new float[0];
    private String[] textosValor = new String[0];
    private float progreso = 1f;

    private final Paint pincelPista = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pincelBarra = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pincelResaltado = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint pincelNombre = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint pincelValor = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private final float densidad;
    private final float altoFila;
    private final float altoBarra;
    private ValueAnimator animador;

    public GraficaBarrasHorizontalView(Context context) {
        this(context, null);
    }

    public GraficaBarrasHorizontalView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        densidad = getResources().getDisplayMetrics().density;
        altoFila = 52 * densidad;
        altoBarra = 10 * densidad;

        Typeface regular = ResourcesCompat.getFont(context, R.font.montserrat_regular);
        Typeface semiBold = ResourcesCompat.getFont(context, R.font.montserrat_semi_bold);

        pincelPista.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerHigh));
        pincelBarra.setColor(MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary));
        pincelResaltado.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorTertiary));

        pincelNombre.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface));
        pincelNombre.setTextSize(sp(14));
        pincelNombre.setTypeface(regular);

        pincelValor.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant));
        pincelValor.setTextSize(sp(12));
        pincelValor.setTypeface(semiBold);
        pincelValor.setTextAlign(Paint.Align.RIGHT);
    }

    private float sp(float valor) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, valor, getResources().getDisplayMetrics());
    }

    public void setDatos(String[] etiquetas, float[] valores, String[] textosValor) {
        boolean cambio = !Arrays.equals(this.valores, valores) || !Arrays.equals(this.etiquetas, etiquetas);
        this.etiquetas = etiquetas;
        this.valores = valores;
        this.textosValor = textosValor;

        StringBuilder descripcion = new StringBuilder();
        for (int i = 0; i < etiquetas.length; i++) {
            if (i > 0) descripcion.append(", ");
            descripcion.append(etiquetas[i]).append(": ").append(textosValor[i]);
        }
        setContentDescription(descripcion.toString());

        requestLayout();
        if (cambio) {
            if (animador != null) animador.cancel();
            animador = ValueAnimator.ofFloat(0f, 1f);
            animador.setDuration(700);
            animador.setInterpolator(new DecelerateInterpolator());
            animador.addUpdateListener(a -> {
                progreso = (float) a.getAnimatedValue();
                invalidate();
            });
            animador.start();
        } else {
            invalidate();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animador != null) animador.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int alto = (int) (valores.length * altoFila) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize(alto, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = valores.length;
        if (n == 0) return;

        float izquierda = getPaddingLeft();
        float derecha = getWidth() - getPaddingRight();
        float maximo = 0f;
        for (float v : valores) maximo = Math.max(maximo, v);
        if (maximo <= 0f) maximo = 1f;

        float radio = altoBarra / 2f;
        for (int i = 0; i < n; i++) {
            float y = getPaddingTop() + altoFila * i;
            float lineaTexto = y + pincelNombre.getTextSize() + 4 * densidad;

            float anchoValor = pincelValor.measureText(textosValor[i]) + 12 * densidad;
            CharSequence nombre = TextUtils.ellipsize(etiquetas[i], pincelNombre, derecha - izquierda - anchoValor, TextUtils.TruncateAt.END);
            canvas.drawText(nombre, 0, nombre.length(), izquierda, lineaTexto, pincelNombre);
            canvas.drawText(textosValor[i], derecha, lineaTexto, pincelValor);

            float arriba = lineaTexto + 10 * densidad;
            canvas.drawRoundRect(izquierda, arriba, derecha, arriba + altoBarra, radio, radio, pincelPista);
            float largo = (derecha - izquierda) * (valores[i] / maximo) * progreso;
            if (largo > 0f) {
                canvas.drawRoundRect(izquierda, arriba, izquierda + Math.max(largo, altoBarra), arriba + altoBarra, radio, radio,
                        i == 0 ? pincelResaltado : pincelBarra);
            }
        }
    }
}
