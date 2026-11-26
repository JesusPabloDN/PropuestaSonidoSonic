package com.example.propuestasonidosonic.ui.socio;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.util.BadgeUtils;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.domain.FinanzasUtils;
import com.example.propuestasonidosonic.domain.EstadosPedido;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class FinanzasFragment extends Fragment {

    private static final String KEY_ANIO = "anio";
    private static final String KEY_MES0 = "mes0";
    private static final String KEY_FILTRO = "filtro";

    private PedidoRepository pedidoRepository;

    private int anio;
    private int mes0;
    private int filtroActual = FinanzasUtils.FILTRO_TODOS;

    private ImageButton btnMesAnterior;
    private TextView tvMesFinanzas;
    private ImageButton btnMesSiguiente;
    private MaterialButton btnMesActual;

    private TextView tvKpiCobrado;
    private TextView tvKpiPorCobrar;
    private TextView tvKpiEfectivo;
    private TextView tvKpiTransferencia;

    private MaterialButtonToggleGroup toggleFiltroFinanzas;
    private TextView tvEmptyMovimientos;
    private LinearLayout layoutMovimientosContainer;

    private List<Pedido> todosLosPedidos = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        anio = cal.get(Calendar.YEAR);
        mes0 = cal.get(Calendar.MONTH);

        if (savedInstanceState != null) {
            anio = savedInstanceState.getInt(KEY_ANIO, anio);
            mes0 = savedInstanceState.getInt(KEY_MES0, mes0);
            filtroActual = savedInstanceState.getInt(KEY_FILTRO, filtroActual);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_finanzas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pedidoRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPedidoRepository();

        vincularVistas(view);
        configurarEventos();
        observarDatos();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ANIO, anio);
        outState.putInt(KEY_MES0, mes0);
        outState.putInt(KEY_FILTRO, filtroActual);
    }

    private void vincularVistas(View view) {
        btnMesAnterior = view.findViewById(R.id.btnMesAnterior);
        tvMesFinanzas = view.findViewById(R.id.tvMesFinanzas);
        btnMesSiguiente = view.findViewById(R.id.btnMesSiguiente);
        btnMesActual = view.findViewById(R.id.btnMesActual);

        tvKpiCobrado = view.findViewById(R.id.tvKpiCobrado);
        tvKpiPorCobrar = view.findViewById(R.id.tvKpiPorCobrar);
        tvKpiEfectivo = view.findViewById(R.id.tvKpiEfectivo);
        tvKpiTransferencia = view.findViewById(R.id.tvKpiTransferencia);

        toggleFiltroFinanzas = view.findViewById(R.id.toggleFiltroFinanzas);
        tvEmptyMovimientos = view.findViewById(R.id.tvEmptyMovimientos);
        layoutMovimientosContainer = view.findViewById(R.id.layoutMovimientosContainer);

        // Configurar selección del toggle según filtroActual
        if (filtroActual == FinanzasUtils.FILTRO_COBRADOS) {
            toggleFiltroFinanzas.check(R.id.btnFiltroCobrados);
        } else if (filtroActual == FinanzasUtils.FILTRO_POR_COBRAR) {
            toggleFiltroFinanzas.check(R.id.btnFiltroPorCobrar);
        } else {
            toggleFiltroFinanzas.check(R.id.btnFiltroTodos);
        }
    }

    private void configurarEventos() {
        btnMesAnterior.setOnClickListener(v -> {
            mes0--;
            if (mes0 < 0) {
                mes0 = 11;
                anio--;
            }
            actualizarUI();
        });

        btnMesSiguiente.setOnClickListener(v -> {
            mes0++;
            if (mes0 > 11) {
                mes0 = 0;
                anio++;
            }
            actualizarUI();
        });

        btnMesActual.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
            anio = cal.get(Calendar.YEAR);
            mes0 = cal.get(Calendar.MONTH);
            actualizarUI();
        });

        toggleFiltroFinanzas.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnFiltroCobrados) {
                filtroActual = FinanzasUtils.FILTRO_COBRADOS;
            } else if (checkedId == R.id.btnFiltroPorCobrar) {
                filtroActual = FinanzasUtils.FILTRO_POR_COBRAR;
            } else {
                filtroActual = FinanzasUtils.FILTRO_TODOS;
            }
            actualizarListaMovimientos();
        });
    }

    private void observarDatos() {
        if (pedidoRepository != null) {
            pedidoRepository.getPedidos().observe(getViewLifecycleOwner(), pedidos -> {
                todosLosPedidos = pedidos != null ? new ArrayList<>(pedidos) : new ArrayList<>();
                actualizarUI();
            });
        }
    }

    private void actualizarUI() {
        if (!isAdded()) return;

        tvMesFinanzas.setText(FormatUtils.mesAnio(anio, mes0));

        // KPIs: lo cobrado (pagos confirmados por un socio) y lo que falta por cobrar
        double[] resumen = FinanzasUtils.resumenMes(todosLosPedidos, anio, mes0);
        tvKpiCobrado.setText(FormatUtils.formatoMoneda(resumen[0]));
        tvKpiPorCobrar.setText(FormatUtils.formatoMoneda(resumen[1]));
        tvKpiEfectivo.setText(FormatUtils.formatoMoneda(resumen[2]));
        tvKpiTransferencia.setText(FormatUtils.formatoMoneda(resumen[3]));

        actualizarListaMovimientos();
    }

    private void actualizarListaMovimientos() {
        if (!isAdded()) return;

        List<Pedido> movimientos = FinanzasUtils.movimientosDelMes(todosLosPedidos, anio, mes0, filtroActual);
        layoutMovimientosContainer.removeAllViews();

        if (movimientos.isEmpty()) {
            tvEmptyMovimientos.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyMovimientos.setVisibility(View.GONE);

        Context ctx = requireContext();
        LayoutInflater inflater = LayoutInflater.from(ctx);

        for (Pedido ped : movimientos) {
            View itemView = inflater.inflate(R.layout.item_movimiento_finanzas, layoutMovimientosContainer, false);
            TextView tvFolio = itemView.findViewById(R.id.tvMovimientoFolio);
            TextView tvTipo = itemView.findViewById(R.id.tvMovimientoTipo);
            TextView tvEventoCliente = itemView.findViewById(R.id.tvMovimientoEventoCliente);
            TextView tvFechaMetodo = itemView.findViewById(R.id.tvMovimientoFechaMetodo);
            TextView tvMonto = itemView.findViewById(R.id.tvMovimientoMonto);

            tvFolio.setText(getString(R.string.folio_formato, ped.getNumero()));

            String nomEvento = ped.getNombreEvento() != null ? ped.getNombreEvento() : "";
            String nomCliente = ped.getClienteNombre() != null ? ped.getClienteNombre() : "";
            if (!nomEvento.isEmpty() && !nomCliente.isEmpty()) {
                tvEventoCliente.setText(nomEvento + " · " + nomCliente);
            } else {
                tvEventoCliente.setText(nomEvento.isEmpty() ? nomCliente : nomEvento);
            }

            tvMonto.setText(FormatUtils.formatoMoneda(ped.getTotal()));
            if (ped.isPagoConfirmado()) {
                tvTipo.setText(R.string.movimiento_cobrado);
                tvFechaMetodo.setText(FormatUtils.fechaHoraCorta(ped.getFechaPagoMillis())
                        + " · " + EstadosPedido.etiquetaMetodo(ped.getMetodoPago()));
                tvMonto.setTextColor(BadgeUtils.colorAtributo(ctx, R.attr.colorEstadoDisponible));
            } else {
                tvTipo.setText(Pedido.ESTADO_POR_COBRAR.equals(ped.getEstado())
                        ? R.string.movimiento_por_cobrar_vencido : R.string.movimiento_por_cobrar);
                tvFechaMetodo.setText(getString(R.string.movimiento_fecha_evento,
                        FormatUtils.formatoFecha(ped.getInicioMillis())));
                tvMonto.setTextColor(Pedido.ESTADO_POR_COBRAR.equals(ped.getEstado())
                        ? BadgeUtils.colorError(ctx)
                        : MaterialColors.getColor(tvMonto, com.google.android.material.R.attr.colorOnSurfaceVariant));
            }

            itemView.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("pedidoId", ped.getId());
                Navigation.findNavController(requireView()).navigate(R.id.action_finanzas_to_detalle, args);
            });

            layoutMovimientosContainer.addView(itemView);
        }
    }
}
