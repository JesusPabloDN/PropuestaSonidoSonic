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

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.data.PagoRepository;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.domain.DashboardUtils;
import com.example.propuestasonidosonic.domain.FinanzasUtils;
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

    private TextView tvDashCobrado;
    private TextView tvDashReembolsos;
    private TextView tvDashNeto;
    private TextView tvDashPorCobrar;

    private TextView tvDashEventosHoy;
    private TextView tvDashProximos7Dias;
    private TextView tvDashApartadosPorPagar;
    private TextView tvDashEquiposPorAsignar;
    private TextView tvDashPorRevisar;

    private List<Pago> todosLosPagos = new ArrayList<>();
    private List<Pedido> todosLosPedidos = new ArrayList<>();

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

        vincularVistas(view);
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
    }
}
