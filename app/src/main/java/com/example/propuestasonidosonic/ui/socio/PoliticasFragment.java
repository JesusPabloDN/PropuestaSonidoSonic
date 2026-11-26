package com.example.propuestasonidosonic.ui.socio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.domain.PoliticasNegocio;
import com.example.propuestasonidosonic.util.FormatUtils;

import java.util.Locale;

public class PoliticasFragment extends Fragment {

    public PoliticasFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_politicas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PoliticasNegocio politicas = PoliticasNegocio.actual();

        TextView tvReservas = view.findViewById(R.id.tvPoliticasReservasDesc);
        TextView tvCancelaciones = view.findViewById(R.id.tvPoliticasCancelacionesDesc);
        TextView tvOperacion = view.findViewById(R.id.tvPoliticasOperacionDesc);
        TextView tvPersonal = view.findViewById(R.id.tvPoliticasPersonalDesc);
        TextView tvPagos = view.findViewById(R.id.tvPoliticasPagosDesc);

        // 1. Solicitudes y aprobación
        tvReservas.setText(getString(R.string.politicas_reservas_aprobacion_desc,
                politicas.getHorasRevision(),
                politicas.getSolicitudesPorAprobarMax()));

        // 2. Cancelaciones (sin reembolsos: los pagos se reciben fuera de la app)
        tvCancelaciones.setText(R.string.politicas_cancelaciones_desc);

        // 3. Operación
        int primerMin = politicas.getPrimerInicioMin();
        int ultimoMin = politicas.getUltimoInicioMin();
        String primerHorario = String.format(Locale.getDefault(), "%02d:%02d", primerMin / 60, primerMin % 60);
        String ultimoHorario = String.format(Locale.getDefault(), "%02d:%02d", ultimoMin / 60, ultimoMin % 60);
        String duracionMin = FormatUtils.tiempoRestante(politicas.getDuracionMinMin() * 60_000L);
        String duracionMax = FormatUtils.tiempoRestante(politicas.getDuracionMaxMin() * 60_000L);

        String operacionTexto = getString(R.string.politicas_operacion_desc,
                politicas.getBufferMontajeMin(),
                politicas.getBufferDesmontajeMin(),
                politicas.getMaxEventosSimultaneos(),
                primerHorario,
                ultimoHorario,
                politicas.getPasoHorarioMin(),
                duracionMin,
                duracionMax);
        tvOperacion.setText(operacionTexto);

        // Personal y Pagos tienen textos informativos en strings.xml
        tvPersonal.setText(R.string.politicas_personal_desc);
        tvPagos.setText(R.string.politicas_pagos_desc);
    }
}
