package com.example.propuestasonidosonic.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.TiempoUtils;
import com.google.android.material.color.MaterialColors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class CalendarioAdapter extends RecyclerView.Adapter<CalendarioAdapter.DiaViewHolder> {

    public interface OnDiaClickListener {
        void onDiaClick(long diaMillis);
    }

    private final List<Long> celdas = new ArrayList<>();
    private final Map<String, int[]> marcadores = new HashMap<>();
    private int mesActual0;
    private long diaSeleccionadoMillis;
    private final OnDiaClickListener listener;

    private static final Locale LOCALE_MX = new Locale("es", "MX");

    public CalendarioAdapter(OnDiaClickListener listener) {
        this.listener = listener;
    }

    public void setDatos(List<Long> nuevasCeldas, int mesActual0, long diaSeleccionadoMillis,
                         Map<String, int[]> nuevosMarcadores) {
        this.celdas.clear();
        if (nuevasCeldas != null) {
            this.celdas.addAll(nuevasCeldas);
        }
        this.mesActual0 = mesActual0;
        this.diaSeleccionadoMillis = TiempoUtils.inicioDelDia(diaSeleccionadoMillis);
        this.marcadores.clear();
        if (nuevosMarcadores != null) {
            this.marcadores.putAll(nuevosMarcadores);
        }
        notifyDataSetChanged();
    }

    public void setDiaSeleccionado(long nuevoDiaMillis) {
        this.diaSeleccionadoMillis = TiempoUtils.inicioDelDia(nuevoDiaMillis);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DiaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dia_calendario, parent, false);
        return new DiaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiaViewHolder holder, int position) {
        holder.bind(celdas.get(position), mesActual0, diaSeleccionadoMillis, marcadores, listener);
    }

    @Override
    public int getItemCount() {
        return celdas.size();
    }

    static class DiaViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout frameIndicadorDia;
        private final TextView tvNumeroDia;
        private final View dotConfirmados;
        private final View dotApartados;

        public DiaViewHolder(@NonNull View itemView) {
            super(itemView);
            frameIndicadorDia = itemView.findViewById(R.id.frameIndicadorDia);
            tvNumeroDia = itemView.findViewById(R.id.tvNumeroDia);
            dotConfirmados = itemView.findViewById(R.id.dotConfirmados);
            dotApartados = itemView.findViewById(R.id.dotApartados);
        }

        public void bind(long celdaMillis, int mesActual0, long diaSeleccionadoMillis,
                         Map<String, int[]> marcadores, OnDiaClickListener listener) {
            Context ctx = itemView.getContext();
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
            cal.setTimeInMillis(celdaMillis);

            int diaMes = cal.get(Calendar.DAY_OF_MONTH);
            int mesCelda = cal.get(Calendar.MONTH);
            boolean esMesActual = (mesCelda == mesActual0);

            tvNumeroDia.setText(String.valueOf(diaMes));

            long hoyInicio = TiempoUtils.inicioDelDia(System.currentTimeMillis());
            boolean esHoy = (celdaMillis == hoyInicio);
            boolean esSeleccionado = (celdaMillis == diaSeleccionadoMillis);

            if (esSeleccionado) {
                frameIndicadorDia.setBackgroundResource(R.drawable.bg_dia_seleccionado);
                tvNumeroDia.setTextColor(MaterialColors.getColor(tvNumeroDia,
                        com.google.android.material.R.attr.colorOnPrimary));
            } else if (esHoy) {
                frameIndicadorDia.setBackgroundResource(R.drawable.bg_dia_hoy);
                tvNumeroDia.setTextColor(MaterialColors.getColor(tvNumeroDia,
                        androidx.appcompat.R.attr.colorPrimary));
            } else {
                frameIndicadorDia.setBackground(null);
                if (esMesActual) {
                    tvNumeroDia.setTextColor(MaterialColors.getColor(tvNumeroDia,
                            com.google.android.material.R.attr.colorOnSurface));
                } else {
                    tvNumeroDia.setTextColor(MaterialColors.getColor(tvNumeroDia,
                            com.google.android.material.R.attr.colorOutline));
                }
            }

            // Marcadores
            String clave = TiempoUtils.claveDia(celdaMillis);
            int[] conteo = marcadores.get(clave);
            int confirmados = (conteo != null) ? conteo[0] : 0;
            int apartados = (conteo != null) ? conteo[1] : 0;

            dotConfirmados.setVisibility(confirmados > 0 ? View.VISIBLE : View.GONE);
            dotApartados.setVisibility(apartados > 0 ? View.VISIBLE : View.GONE);

            // Accesibilidad
            SimpleDateFormat sdfDia = new SimpleDateFormat("d 'de' MMMM", LOCALE_MX);
            sdfDia.setTimeZone(TimeZone.getTimeZone(Config.ZONA_HORARIA));
            String fechaTexto = sdfDia.format(new Date(celdaMillis));

            Resources res = ctx.getResources();
            String desc;
            if (confirmados > 0 && apartados > 0) {
                String strConf = res.getQuantityString(R.plurals.contador_eventos, confirmados, confirmados);
                String strApar = res.getQuantityString(R.plurals.contador_apartados, apartados, apartados);
                desc = ctx.getString(R.string.cd_celda_con_ambos, fechaTexto, strConf, strApar);
            } else if (confirmados > 0) {
                String strConf = res.getQuantityString(R.plurals.contador_eventos, confirmados, confirmados);
                desc = ctx.getString(R.string.cd_celda_con_eventos, fechaTexto, strConf);
            } else if (apartados > 0) {
                String strApar = res.getQuantityString(R.plurals.contador_apartados, apartados, apartados);
                desc = ctx.getString(R.string.cd_celda_con_eventos, fechaTexto, strApar);
            } else {
                desc = ctx.getString(R.string.cd_celda_sin_eventos, fechaTexto);
            }
            itemView.setContentDescription(desc);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDiaClick(celdaMillis);
                }
            });
        }
    }
}
