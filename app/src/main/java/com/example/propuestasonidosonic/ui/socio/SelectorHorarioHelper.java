package com.example.propuestasonidosonic.ui.socio;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.example.propuestasonidosonic.util.TiempoUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class SelectorHorarioHelper {

    public interface OnHorarioSeleccionadoListener {
        void onHorarioSeleccionado(long inicioMillis);
        void onHorarioLimpiado();
    }

    private final Context context;
    private final FragmentManager fragmentManager;
    private final PedidoRepository pedidoRepository;
    private final TextView tvFechaBoton;
    private final ChipGroup chipGroupHorarios;
    private final View layoutSinHorarios;
    private final ChipGroup chipGroupAlternativas;
    private final OnHorarioSeleccionadoListener listener;

    private long fechaSeleccionadaMillis = 0;
    private Long horarioSeleccionadoMillis = null;
    private String paqueteId;
    private int duracionMin;
    private String ignorarPedidoId;

    public SelectorHorarioHelper(Context context,
                                 FragmentManager fragmentManager,
                                 PedidoRepository pedidoRepository,
                                 TextView tvFechaBoton,
                                 ChipGroup chipGroupHorarios,
                                 View layoutSinHorarios,
                                 ChipGroup chipGroupAlternativas,
                                 OnHorarioSeleccionadoListener listener) {
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.pedidoRepository = pedidoRepository;
        this.tvFechaBoton = tvFechaBoton;
        this.chipGroupHorarios = chipGroupHorarios;
        this.layoutSinHorarios = layoutSinHorarios;
        this.chipGroupAlternativas = chipGroupAlternativas;
        this.listener = listener;

        configurarBotonFecha();
    }

    public void setConfiguracion(String paqueteId, int duracionMin, String ignorarPedidoId) {
        this.paqueteId = paqueteId;
        this.duracionMin = duracionMin;
        this.ignorarPedidoId = ignorarPedidoId;
        if (fechaSeleccionadaMillis > 0) {
            actualizarHorarios();
        }
    }

    public void setFechaSeleccionada(long fechaMillis, Long horarioMillis) {
        this.fechaSeleccionadaMillis = TiempoUtils.inicioDelDia(fechaMillis);
        this.horarioSeleccionadoMillis = horarioMillis;
        actualizarTextoFecha();
        actualizarHorarios();
    }

    public long getFechaSeleccionadaMillis() {
        return fechaSeleccionadaMillis;
    }

    public Long getHorarioSeleccionadoMillis() {
        return horarioSeleccionadoMillis;
    }

    private void configurarBotonFecha() {
        tvFechaBoton.setOnClickListener(v -> abrirSelectorFecha());
    }

    public void abrirSelectorFecha() {
        long hoyInicio = TiempoUtils.inicioDelDia(System.currentTimeMillis());

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.from(hoyInicio))
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.label_fecha_evento)
                .setSelection(fechaSeleccionadaMillis > 0 ? fechaSeleccionadaMillis : hoyInicio)
                .setCalendarConstraints(constraints)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) return;
            Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);
            utcCal.setTimeInMillis(selection);
            int y = utcCal.get(Calendar.YEAR);
            int m = utcCal.get(Calendar.MONTH);
            int d = utcCal.get(Calendar.DAY_OF_MONTH);

            this.fechaSeleccionadaMillis = TiempoUtils.construir(y, m, d, 0);
            this.horarioSeleccionadoMillis = null;
            if (listener != null) listener.onHorarioLimpiado();

            actualizarTextoFecha();
            actualizarHorarios();
        });

        picker.show(fragmentManager, "PICKER_FECHA_EVENTO");
    }

    private void actualizarTextoFecha() {
        if (fechaSeleccionadaMillis <= 0) {
            tvFechaBoton.setText(R.string.label_fecha_evento);
        } else {
            tvFechaBoton.setText(FormatUtils.fechaLarga(fechaSeleccionadaMillis));
        }
    }

    public void actualizarHorarios() {
        chipGroupHorarios.removeAllViews();
        if (chipGroupAlternativas != null) chipGroupAlternativas.removeAllViews();
        if (layoutSinHorarios != null) layoutSinHorarios.setVisibility(View.GONE);

        if (paqueteId == null || duracionMin <= 0 || fechaSeleccionadaMillis <= 0) {
            return;
        }

        List<Long> viables = pedidoRepository.consultarHorarios(paqueteId, fechaSeleccionadaMillis,
                duracionMin, ignorarPedidoId);

        SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm", new Locale("es", "MX"));
        sdfHora.setTimeZone(TimeZone.getTimeZone(Config.ZONA_HORARIA));

        if (viables.isEmpty()) {
            if (layoutSinHorarios != null) {
                layoutSinHorarios.setVisibility(View.VISIBLE);
            }
            if (horarioSeleccionadoMillis != null) {
                horarioSeleccionadoMillis = null;
                if (listener != null) listener.onHorarioLimpiado();
            }

            // Alternativas
            if (chipGroupAlternativas != null) {
                List<Long> alternativas = pedidoRepository.consultarAlternativas(paqueteId,
                        fechaSeleccionadaMillis, duracionMin, 5);

                for (Long altMillis : alternativas) {
                    Chip altChip = new Chip(context);
                    String textoAlt = FormatUtils.fechaHoraCorta(altMillis);
                    altChip.setText(textoAlt);
                    altChip.setCheckable(false);
                    altChip.setClickable(true);
                    altChip.setOnClickListener(v -> {
                        setFechaSeleccionada(altMillis, altMillis);
                        if (listener != null) listener.onHorarioSeleccionado(altMillis);
                    });
                    chipGroupAlternativas.addView(altChip);
                }
            }
            return;
        }

        boolean horarioAunValido = false;
        chipGroupHorarios.setSingleSelection(true);

        for (Long hMillis : viables) {
            Chip chip = new Chip(context);
            chip.setText(sdfHora.format(new Date(hMillis)));
            chip.setCheckable(true);
            chip.setClickable(true);

            if (horarioSeleccionadoMillis != null && horarioSeleccionadoMillis.equals(hMillis)) {
                chip.setChecked(true);
                horarioAunValido = true;
            }

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    horarioSeleccionadoMillis = hMillis;
                    if (listener != null) listener.onHorarioSeleccionado(hMillis);
                } else if (horarioSeleccionadoMillis != null && horarioSeleccionadoMillis.equals(hMillis)) {
                    horarioSeleccionadoMillis = null;
                    if (listener != null) listener.onHorarioLimpiado();
                }
            });

            chipGroupHorarios.addView(chip);
        }

        if (!horarioAunValido && horarioSeleccionadoMillis != null) {
            horarioSeleccionadoMillis = null;
            if (listener != null) listener.onHorarioLimpiado();
        }
    }
}
