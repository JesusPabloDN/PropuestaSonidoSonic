package com.example.propuestasonidosonic.ui.comun;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.example.propuestasonidosonic.R;
import com.google.android.material.color.MaterialColors;

import java.util.Arrays;

public class GraficaBarrasView extends View {

    private String[] etiquetas = new String[0];
    private float[] valores = new float[0];
    private String[] textosValor = new String[0];
    private int indiceResaltado = -1;
    private float progreso = 1f;

    private final Paint pincelBarra = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pincelResaltado = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pincelEje = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pincelEtiqueta = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pincelEtiquetaResaltada = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pincelValor = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pincelValorResaltado = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final float densidad;
    private ValueAnimator animador;

    public GraficaBarrasView(Context context) {
        this(context, null);
    }

    public GraficaBarrasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        densidad = getResources().getDisplayMetrics().density;

        Typeface regular = ResourcesCompat.getFont(context, R.font.montserrat_regular);
        Typeface medium = ResourcesCompat.getFont(context, R.font.montserrat_medium);
        Typeface semiBold = ResourcesCompat.getFont(context, R.font.montserrat_semi_bold);

        pincelBarra.setColor(MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary));
        pincelResaltado.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorTertiary));
        pincelEje.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant));
        pincelEje.setStrokeWidth(densidad);

        pincelEtiqueta.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant));
        pincelEtiqueta.setTextSize(sp(12));
        pincelEtiqueta.setTypeface(regular);
        pincelEtiqueta.setTextAlign(Paint.Align.CENTER);

        pincelEtiquetaResaltada.set(pincelEtiqueta);
        pincelEtiquetaResaltada.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface));
        pincelEtiquetaResaltada.setTypeface(semiBold);

        pincelValor.set(pincelEtiqueta);
        pincelValor.setTypeface(medium);
        pincelValor.setTextSize(sp(11));

        pincelValorResaltado.set(pincelValor);
        pincelValorResaltado.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorTertiary));
        pincelValorResaltado.setTypeface(semiBold);
    }

    private float sp(float valor) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, valor, getResources().getDisplayMetrics());
    }

    public void setDatos(String[] etiquetas, float[] valores, String[] textosValor, int indiceResaltado) {
        boolean cambio = !Arrays.equals(this.valores, valores)
                || !Arrays.equals(this.etiquetas, etiquetas)
                || this.indiceResaltado != indiceResaltado;
        this.etiquetas = etiquetas;
        this.valores = valores;
        this.textosValor = textosValor;
        this.indiceResaltado = indiceResaltado;

        StringBuilder descripcion = new StringBuilder();
        for (int i = 0; i < etiquetas.length; i++) {
            if (i > 0) descripcion.append(", ");
            descripcion.append(etiquetas[i]).append(": ").append(textosValor[i]);
        }
        setContentDescription(descripcion.toString());

        if (cambio) {
            animar();
        } else {
            invalidate();
        }
    }

    private void animar() {
        if (animador != null) animador.cancel();
        animador = ValueAnimator.ofFloat(0f, 1f);
        animador.setDuration(700);
        animador.setInterpolator(new DecelerateInterpolator());
        animador.addUpdateListener(a -> {
            progreso = (float) a.getAnimatedValue();
            invalidate();
        });
        animador.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animador != null) animador.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int alto = resolveSize((int) (190 * densidad), heightMeasureSpec);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), alto);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = valores.length;
        if (n == 0) return;

        float izquierda = getPaddingLeft();
        float derecha = getWidth() - getPaddingRight();
        float altoEtiqueta = pincelEtiqueta.getTextSize() + 10 * densidad;
        float altoValor = pincelValor.getTextSize() + 8 * densidad;
        float base = getHeight() - getPaddingBottom() - altoEtiqueta;
        float tope = getPaddingTop() + altoValor;

        float maximo = 0f;
        for (float v : valores) maximo = Math.max(maximo, v);
        if (maximo <= 0f) maximo = 1f;

        float ancho = (derecha - izquierda) / n;
        float anchoBarra = Math.min(ancho * 0.56f, 40 * densidad);
        float radio = Math.min(anchoBarra / 2f, 10 * densidad);

        canvas.drawLine(izquierda, base, derecha, base, pincelEje);

        for (int i = 0; i < n; i++) {
            boolean resaltado = i == indiceResaltado;
            float centro = izquierda + ancho * i + ancho / 2f;
            float alto = (valores[i] / maximo) * (base - tope) * progreso;

            if (valores[i] > 0f) {
                float arriba = base - Math.max(alto, radio);
                Paint pincel = resaltado ? pincelResaltado : pincelBarra;
                canvas.drawRoundRect(centro - anchoBarra / 2f, arriba, centro + anchoBarra / 2f, base, radio, radio, pincel);
                canvas.drawRect(centro - anchoBarra / 2f, Math.min(arriba + radio, base), centro + anchoBarra / 2f, base, pincel);

                Paint pincelTexto = resaltado ? pincelValorResaltado : pincelValor;
                canvas.drawText(textosValor[i], centro, arriba - 6 * densidad, pincelTexto);
            }

            Paint pincelEt = resaltado ? pincelEtiquetaResaltada : pincelEtiqueta;
            canvas.drawText(etiquetas[i], centro, base + altoEtiqueta - 2 * densidad, pincelEt);
        }
    }
}
