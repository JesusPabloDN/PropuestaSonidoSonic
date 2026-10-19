package com.example.propuestasonidosonic.ui.socio;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.data.EquipoRepository;
import com.example.propuestasonidosonic.data.PagoRepository;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.domain.ActividadRecienteUtils;
import com.example.propuestasonidosonic.domain.DashboardUtils;
import com.example.propuestasonidosonic.domain.FinanzasUtils;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Pago;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DashboardFragment extends Fragment {

    private static final long INTERVALO_REFRESCO_MS = 60_000L;

    private PagoRepository pagoRepository;
    private PedidoRepository pedidoRepository;
    private EquipoRepository equipoRepository;

    private TextView tvDashCobrado;
    private TextView tvDashReembolsos;
    private TextView tvDashNeto;
    private TextView tvDashPorCobrar;

    private TextView tvDashEventosHoy;
    private TextView tvDashProximos7Dias;
    private TextView tvDashApartadosPorPagar;
    private TextView tvDashEquiposPorAsignar;
    private TextView tvDashPorRevisar;

    // Sección 3: Lo más reciente
    private View rowRecientePedidos;
    private TextView tvRecientePedidosTitulo;
    private TextView tvRecientePedidosSubtitulo;

    private View rowRecienteAgenda;
    private TextView tvRecienteAgendaTitulo;
    private TextView tvRecienteAgendaSubtitulo;

    private View rowRecienteInventario;
    private TextView tvRecienteInventarioTitulo;
    private TextView tvRecienteInventarioSubtitulo;

    private View rowRecienteFinanzas;
    private TextView tvRecienteFinanzasTitulo;
    private TextView tvRecienteFinanzasSubtitulo;

    private List<Pago> todosLosPagos = new ArrayList<>();
    private List<Pedido> todosLosPedidos = new ArrayList<>();
    private List<Equipo> todosLosEquipos = new ArrayList<>();

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            actualizarUI();
            refreshHandler.postDelayed(this, INTERVALO_REFRESCO_MS);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pagoRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPagoRepository();
        pedidoRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPedidoRepository();
        equipoRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getEquipoRepository();

        vincularVistas(view);
        configurarNavegacionFilas();
        observarDatos();
    }

    private void vincularVistas(View view) {
        tvDashCobrado = view.findViewById(R.id.tvDashCobrado);
        tvDashReembolsos = view.findViewById(R.id.tvDashReembolsos);
        tvDashNeto = view.findViewById(R.id.tvDashNeto);
        tvDashPorCobrar = view.findViewById(R.id.tvDashPorCobrar);

        tvDashEventosHoy = view.findViewById(R.id.tvDashEventosHoy);
        tvDashProximos7Dias = view.findViewById(R.id.tvDashProximos7Dias);
        tvDashApartadosPorPagar = view.findViewById(R.id.tvDashApartadosPorPagar);
        tvDashEquiposPorAsignar = view.findViewById(R.id.tvDashEquiposPorAsignar);
        tvDashPorRevisar = view.findViewById(R.id.tvDashPorRevisar);

        // Lo más reciente
        rowRecientePedidos = view.findViewById(R.id.rowRecientePedidos);
        tvRecientePedidosTitulo = view.findViewById(R.id.tvRecientePedidosTitulo);
        tvRecientePedidosSubtitulo = view.findViewById(R.id.tvRecientePedidosSubtitulo);

        rowRecienteAgenda = view.findViewById(R.id.rowRecienteAgenda);
        tvRecienteAgendaTitulo = view.findViewById(R.id.tvRecienteAgendaTitulo);
        tvRecienteAgendaSubtitulo = view.findViewById(R.id.tvRecienteAgendaSubtitulo);

        rowRecienteInventario = view.findViewById(R.id.rowRecienteInventario);
        tvRecienteInventarioTitulo = view.findViewById(R.id.tvRecienteInventarioTitulo);
        tvRecienteInventarioSubtitulo = view.findViewById(R.id.tvRecienteInventarioSubtitulo);

        rowRecienteFinanzas = view.findViewById(R.id.rowRecienteFinanzas);
        tvRecienteFinanzasTitulo = view.findViewById(R.id.tvRecienteFinanzasTitulo);
        tvRecienteFinanzasSubtitulo = view.findViewById(R.id.tvRecienteFinanzasSubtitulo);
    }

    private void configurarNavegacionFilas() {
        NavOptions navOptions = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.nav_dashboard, false)
                .build();

        rowRecientePedidos.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_pedidos, null, navOptions));

        rowRecienteAgenda.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_agenda, null, navOptions));

        rowRecienteInventario.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_inventario, null, navOptions));

        rowRecienteFinanzas.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_finanzas, null, navOptions));
    }

    private void observarDatos() {
        if (pagoRepository != null) {
            pagoRepository.getTodosLosPagos().observe(getViewLifecycleOwner(), pagos -> {
                todosLosPagos = pagos != null ? new ArrayList<>(pagos) : new ArrayList<>();
                actualizarUI();
            });
        }

        if (pedidoRepository != null) {
            pedidoRepository.getPedidos().observe(getViewLifecycleOwner(), pedidos -> {
                todosLosPedidos = pedidos != null ? new ArrayList<>(pedidos) : new ArrayList<>();
                actualizarUI();
            });
        }

        if (equipoRepository != null) {
            equipoRepository.getEquipos().observe(getViewLifecycleOwner(), equipos -> {
                todosLosEquipos = equipos != null ? new ArrayList<>(equipos) : new ArrayList<>();
                actualizarUI();
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        actualizarUI();
        refreshHandler.postDelayed(refreshRunnable, INTERVALO_REFRESCO_MS);
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        refreshHandler.removeCallbacksAndMessages(null);
    }

    private void actualizarUI() {
        if (!isAdded()) return;

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        int anio = cal.get(Calendar.YEAR);
        int mes0 = cal.get(Calendar.MONTH);
        long ahora = System.currentTimeMillis();

        // 1. Sección "Este mes"
        double[] resumen = FinanzasUtils.resumenMes(todosLosPagos, todosLosPedidos, anio, mes0);
        double cobrado = resumen[0];
        double reembolsos = resumen[1];
        double neto = resumen[2];
        double porCobrar = resumen[3];

        tvDashCobrado.setText(FormatUtils.formatoMoneda(cobrado));
        tvDashReembolsos.setText(FormatUtils.formatoMoneda(reembolsos));
        tvDashNeto.setText(FormatUtils.formatoMoneda(neto));
        if (neto < 0) {
            tvDashNeto.setTextColor(com.example.propuestasonidosonic.util.BadgeUtils.colorError(requireContext()));
        } else {
            tvDashNeto.setTextColor(MaterialColors.getColor(tvDashNeto, com.google.android.material.R.attr.colorOnSurface));
        }
        tvDashPorCobrar.setText(FormatUtils.formatoMoneda(porCobrar));

        // 2. Sección "Operación"
        int[] contadores = DashboardUtils.contadores(todosLosPedidos, ahora);
        int eventosHoy = contadores[0];
        int proximos7Dias = contadores[1];
        int apartadosPorPagar = contadores[2];
        int equiposPorAsignar = contadores[3];

        tvDashEventosHoy.setText(String.valueOf(eventosHoy));
        tvDashProximos7Dias.setText(String.valueOf(proximos7Dias));
        tvDashApartadosPorPagar.setText(String.valueOf(apartadosPorPagar));
        tvDashEquiposPorAsignar.setText(String.valueOf(equiposPorAsignar));

        if (equiposPorAsignar > 0) {
            tvDashEquiposPorAsignar.setTextColor(com.example.propuestasonidosonic.util.BadgeUtils.colorError(requireContext()));
        } else {
            tvDashEquiposPorAsignar.setTextColor(MaterialColors.getColor(tvDashEquiposPorAsignar, com.google.android.material.R.attr.colorOnSurface));
        }

        int porRevisar = DashboardUtils.contarEnRevision(todosLosPedidos);
        tvDashPorRevisar.setText(String.valueOf(porRevisar));
        if (porRevisar > 0) {
            tvDashPorRevisar.setTextColor(com.example.propuestasonidosonic.util.BadgeUtils.colorError(requireContext()));
        } else {
            tvDashPorRevisar.setTextColor(MaterialColors.getColor(tvDashPorRevisar, com.google.android.material.R.attr.colorOnSurface));
        }

        // 3. Sección "Lo más reciente"
        actualizarLoMasReciente(ahora);
    }

    private void actualizarLoMasReciente(long ahora) {
        // Pedidos -> Último pedido
        Pedido ultimoPed = ActividadRecienteUtils.ultimoPedido(todosLosPedidos);
        if (ultimoPed == null) {
            tvRecientePedidosTitulo.setText(R.string.reciente_pedidos_vacio);
            tvRecientePedidosSubtitulo.setVisibility(View.GONE);
        } else {
            tvRecientePedidosSubtitulo.setVisibility(View.VISIBLE);
            tvRecientePedidosTitulo.setText(getString(R.string.reciente_pedido_titulo_formato,
                    ultimoPed.getNumero(),
                    ultimoPed.getNombreEvento() != null ? ultimoPed.getNombreEvento() : ""));
            String estadoTexto = estadoPedidoTexto(ultimoPed.getEstado());
            String tiempoTexto = FormatUtils.tiempoRelativoFormateado(requireContext(), ahora, ultimoPed.getFechaCreacion());
            tvRecientePedidosSubtitulo.setText(getString(R.string.reciente_pedido_subtitulo_formato,
                    ultimoPed.getClienteNombre() != null ? ultimoPed.getClienteNombre() : "",
                    estadoTexto,
                    tiempoTexto));
        }

        // Agenda -> Próximo evento
        Pedido proxEvento = ActividadRecienteUtils.proximoEvento(todosLosPedidos, ahora);
        if (proxEvento == null) {
            tvRecienteAgendaTitulo.setText(R.string.reciente_agenda_vacio);
            tvRecienteAgendaSubtitulo.setVisibility(View.GONE);
        } else {
            tvRecienteAgendaSubtitulo.setVisibility(View.VISIBLE);
            tvRecienteAgendaTitulo.setText(proxEvento.getNombreEvento() != null ? proxEvento.getNombreEvento() : "");
            String fechaTexto = FormatUtils.formatoFecha(proxEvento.getInicioMillis());
            long llegadaOInicio = proxEvento.getBloqueoInicioMillis() > 0 ? proxEvento.getBloqueoInicioMillis() : proxEvento.getInicioMillis();
            String rangoHorario = FormatUtils.rangoHorario(llegadaOInicio, proxEvento.getFinMillis());
            tvRecienteAgendaSubtitulo.setText(getString(R.string.reciente_agenda_subtitulo_formato,
                    fechaTexto,
                    rangoHorario,
                    proxEvento.getClienteNombre() != null ? proxEvento.getClienteNombre() : ""));
        }

        // Inventario -> Último equipo agregado
        Equipo ultimoEq = ActividadRecienteUtils.ultimoEquipoAgregado(todosLosEquipos);
        if (ultimoEq == null) {
            tvRecienteInventarioTitulo.setText(R.string.reciente_inventario_vacio);
            tvRecienteInventarioSubtitulo.setVisibility(View.GONE);
        } else {
            tvRecienteInventarioSubtitulo.setVisibility(View.VISIBLE);
            tvRecienteInventarioTitulo.setText(getString(R.string.reciente_inventario_titulo_formato,
                    ultimoEq.getNombre(),
                    ultimoEq.getNumeroUnidad()));
            String tiempoTexto = FormatUtils.tiempoRelativoFormateado(requireContext(), ahora, ultimoEq.getFechaAlta());
            tvRecienteInventarioSubtitulo.setText(getString(R.string.reciente_inventario_subtitulo_formato,
                    ultimoEq.getCategoria() != null ? ultimoEq.getCategoria() : "",
                    tiempoTexto));
        }

        // Finanzas -> Último movimiento
        Pago ultPago = ActividadRecienteUtils.ultimoPago(todosLosPagos);
        if (ultPago == null) {
            tvRecienteFinanzasTitulo.setText(R.string.reciente_finanzas_vacio);
            tvRecienteFinanzasSubtitulo.setVisibility(View.GONE);
        } else {
            tvRecienteFinanzasSubtitulo.setVisibility(View.VISIBLE);
            String tipoTexto = obtenerTipoPagoTexto(ultPago.getTipo());
            tvRecienteFinanzasTitulo.setText(getString(R.string.reciente_finanzas_titulo_formato,
                    tipoTexto,
                    FormatUtils.formatoMoneda(ultPago.getMonto())));

            long numeroPedido = 0;
            if (ultPago.getPedidoId() != null) {
                for (Pedido p : todosLosPedidos) {
                    if (ultPago.getPedidoId().equals(p.getId())) {
                        numeroPedido = p.getNumero();
                        break;
                    }
                }
            }
            String tiempoTexto = FormatUtils.tiempoRelativoFormateado(requireContext(), ahora, ultPago.getFecha());
            tvRecienteFinanzasSubtitulo.setText(getString(R.string.reciente_finanzas_subtitulo_formato,
                    numeroPedido,
                    tiempoTexto));
        }
    }

    private String obtenerTipoPagoTexto(String tipo) {
        if (Pago.TIPO_ANTICIPO.equals(tipo)) return "Anticipo";
        if (Pago.TIPO_ABONO.equals(tipo)) return "Abono";
        if (Pago.TIPO_LIQUIDACION.equals(tipo)) return "Pago total";
        if (Pago.TIPO_REEMBOLSO.equals(tipo)) return "Reembolso";
        return tipo != null ? tipo : "Pago";
    }

    private String estadoPedidoTexto(String estado) {
        if (estado == null) return "";
        switch (estado) {
            case Pedido.ESTADO_APARTADO:
                return getString(R.string.badge_apartado);
            case Pedido.ESTADO_EN_REVISION:
                return getString(R.string.badge_en_revision);
            case Pedido.ESTADO_CONFIRMADO:
                return getString(R.string.badge_confirmado);
            case Pedido.ESTADO_PAGADO:
                return getString(R.string.badge_pagado);
            case Pedido.ESTADO_COMPLETADO:
                return getString(R.string.badge_completado);
            case Pedido.ESTADO_CANCELADO:
                return getString(R.string.badge_cancelado);
            case Pedido.ESTADO_RECHAZADO:
                return getString(R.string.badge_rechazado);
            case Pedido.ESTADO_VENCIDO:
                return getString(R.string.badge_vencido);
            default:
                return estado;
        }
    }
}
