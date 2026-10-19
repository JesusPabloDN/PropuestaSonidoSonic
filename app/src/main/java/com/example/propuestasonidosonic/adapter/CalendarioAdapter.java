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

    public static final int MODO_AGENDA_SOCIO = 0;
    public static final int MODO_DISPONIBILIDAD_ASISTENTE = 1;

    private int modo = MODO_AGENDA_SOCIO;
    private final List<Long> celdas = new ArrayList<>();
    private final Map<String, int[]> marcadores = new HashMap<>();
    private final java.util.Set<String> fechasDisponibles = new java.util.HashSet<>();
    private final java.util.Set<String> fechasConEvento = new java.util.HashSet<>();
    private long ahora;
    private int mesActual0;
    private long diaSeleccionadoMillis;
    private final OnDiaClickListener listener;

    private static final Locale LOCALE_MX = new Locale("es", "MX");

    public CalendarioAdapter(OnDiaClickListener listener) {
        this.listener = listener;
    }

    public void setDatos(List<Long> nuevasCeldas, int mesActual0, long diaSeleccionadoMillis,
                         Map<String, int[]> nuevosMarcadores) {
        this.modo = MODO_AGENDA_SOCIO;
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

    public void setModoDisponibilidad(List<Long> nuevasCeldas, int mesActual0,
                                      java.util.Set<String> nuevasDisponibles,
                                      java.util.Set<String> nuevasConEvento,
                                      long ahora) {
        this.modo = MODO_DISPONIBILIDAD_ASISTENTE;
        this.celdas.clear();
        if (nuevasCeldas != null) {
            this.celdas.addAll(nuevasCeldas);
        }
        this.mesActual0 = mesActual0;
        this.fechasDisponibles.clear();
        if (nuevasDisponibles != null) {
            this.fechasDisponibles.addAll(nuevasDisponibles);
        }
        this.fechasConEvento.clear();
        if (nuevasConEvento != null) {
            this.fechasConEvento.addAll(nuevasConEvento);
        }
        this.ahora = ahora;
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
        if (modo == MODO_DISPONIBILIDAD_ASISTENTE) {
            holder.bindDisponibilidad(celdas.get(position), mesActual0, fechasDisponibles, fechasConEvento, ahora, listener);
        } else {
            holder.bind(celdas.get(position), mesActual0, diaSeleccionadoMillis, marcadores, listener);
        }
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

        public void bindDisponibilidad(long celdaMillis, int mesActual0,
                                       java.util.Set<String> fechasDisponibles,
                                       java.util.Set<String> fechasConEvento,
                                       long ahora, OnDiaClickListener listener) {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
            cal.setTimeInMillis(celdaMillis);

            int diaMes = cal.get(Calendar.DAY_OF_MONTH);
            int mesCelda = cal.get(Calendar.MONTH);
            boolean esMesActual = (mesCelda == mesActual0);

            tvNumeroDia.setText(String.valueOf(diaMes));

            SimpleDateFormat sdfDia = new SimpleDateFormat("d 'de' MMMM", LOCALE_MX);
            sdfDia.setTimeZone(TimeZone.getTimeZone(Config.ZONA_HORARIA));
            String fechaTexto = sdfDia.format(new Date(celdaMillis));

            String clave = TiempoUtils.claveDia(celdaMillis);
            long hoyInicio = TiempoUtils.inicioDelDia(ahora);
            boolean esPasado = (celdaMillis < hoyInicio);

            boolean disponible = fechasDisponibles != null && fechasDisponibles.contains(clave);
            boolean conEvento = fechasConEvento != null && fechasConEvento.contains(clave);

            dotApartados.setVisibility(View.GONE);

            if (esPasado) {
                itemView.setAlpha(0.38f);
                frameIndicadorDia.setBackground(null);
                tvNumeroDia.setTextColor(MaterialColors.getColor(tvNumeroDia,
                        com.google.android.material.R.attr.colorOutline));
                dotConfirmados.setVisibility(View.GONE);
                itemView.setOnClickListener(null);
                itemView.setContentDescription(fechaTexto + ", Día pasado");
                return;
            }

            itemView.setAlpha(1.0f);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDiaClick(celdaMillis);
                }
            });

            if (conEvento) {
                dotConfirmados.setVisibility(View.VISIBLE);
                if (disponible) {
                    frameIndicadorDia.setBackgroundResource(R.drawable.bg_dia_seleccionado);
                    tvNumeroDia.setTextColor(MaterialColors.getColor(tvNumeroDia,
                            com.google.android.material.R.attr.colorOnPrimary));
                    itemView.setContentDescription(fechaTexto + ", Con evento asignado, Disponible");
                } else {
                    frameIndicadorDia.setBackground(null);
                    tvNumeroDia.setTextColor(MaterialColors.getColor(tvNumeroDia,
                            esMesActual ? com.google.android.material.R.attr.colorOnSurface
                                        : com.google.android.material.R.attr.colorOutline));
                    itemView.setContentDescription(fechaTexto + ", Con evento asignado");
                }
            } else if (disponible) {
                dotConfirmados.setVisibility(View.GONE);
                frameIndicadorDia.setBackgroundResource(R.drawable.bg_dia_seleccionado);
                tvNumeroDia.setTextColor(MaterialColors.getColor(tvNumeroDia,
                        com.google.android.material.R.attr.colorOnPrimary));
                itemView.setContentDescription(fechaTexto + ", Disponible");
            } else {
                dotConfirmados.setVisibility(View.GONE);
                frameIndicadorDia.setBackground(null);
                tvNumeroDia.setTextColor(MaterialColors.getColor(tvNumeroDia,
                        esMesActual ? com.google.android.material.R.attr.colorOnSurface
                                    : com.google.android.material.R.attr.colorOutline));
                itemView.setContentDescription(fechaTexto + ", No disponible");
            }
        }
    }
}
